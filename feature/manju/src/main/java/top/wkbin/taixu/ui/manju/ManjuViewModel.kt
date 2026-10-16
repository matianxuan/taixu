package top.wkbin.taixu.ui.manju

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 漫剧制作界面的状态与动作。
 *
 * 状态用单个不可变 [UiState] 承载，通过 StateFlow 暴露，
 * UI 侧用 collectAsStateWithLifecycle 订阅。
 */
class ManjuViewModel(
    private val planner: StoryboardPlanner,
    private val engine: ManjuEngine,
    private val paths: ManjuPaths,
) : ViewModel() {

    data class UiState(
        val script: String = "",
        val globalStyle: String = "",
        val storyboard: Storyboard? = null,
        val isPlanning: Boolean = false,
        val isGenerating: Boolean = false,
        val exportedPath: String? = null,
        val message: String? = null,
    ) {
        /** 整体进度，用于进度条 */
        val progress: Float
            get() {
                val sb = storyboard ?: return 0f
                return if (sb.shots.isEmpty()) 0f else sb.readyCount.toFloat() / sb.shots.size
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onScriptChange(value: String) = _uiState.update { it.copy(script = value) }

    fun onStyleChange(value: String) = _uiState.update { it.copy(globalStyle = value) }

    /** 拆解剧本为分镜。 */
    fun planStoryboard() {
        val state = _uiState.value
        if (state.script.isBlank()) {
            _uiState.update { it.copy(message = "请先输入剧本") }
            return
        }
        _uiState.update { it.copy(isPlanning = true, message = null, exportedPath = null) }
        viewModelScope.launch {
            planner.plan(state.script, state.globalStyle)
                .onSuccess { sb ->
                    _uiState.update {
                        it.copy(isPlanning = false, storyboard = sb, message = "已拆解 ${sb.shots.size} 个镜头")
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isPlanning = false, message = "拆解失败：${e.message}") }
                }
        }
    }

    /** 生成所有镜头。 */
    fun generateAll() {
        val sb = _uiState.value.storyboard ?: return
        if (_uiState.value.isGenerating) return
        _uiState.update { it.copy(isGenerating = true, message = null) }
        viewModelScope.launch {
            val updated = engine.generateShots(sb) { shot ->
                // 每镜完成即刷新，用户能看到逐镜推进
                _uiState.update { st ->
                    val cur = st.storyboard ?: return@update st
                    st.copy(storyboard = cur.copy(shots = cur.shots.map { if (it.index == shot.index) shot else it }))
                }
            }
            val failed = updated.shots.count { it.status is ShotStatus.Failed }
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    storyboard = updated,
                    message = if (failed > 0) "完成，${failed} 个镜头失败可重试" else "全部镜头生成完成",
                )
            }
        }
    }

    /** 重试单个镜头。 */
    fun retryShot(index: Int) {
        val sb = _uiState.value.storyboard ?: return
        val shot = sb.shots.find { it.index == index } ?: return
        _uiState.update { st ->
            st.copy(storyboard = sb.copy(shots = sb.shots.map { if (it.index == index) it.copy(status = ShotStatus.Pending) else it }))
        }
        viewModelScope.launch {
            engine.generateShots(sb.copy(shots = listOf(shot.copy(status = ShotStatus.Pending)))) { updated ->
                _uiState.update { st ->
                    val cur = st.storyboard ?: return@update st
                    st.copy(storyboard = cur.copy(shots = cur.shots.map { if (it.index == updated.index) updated else it }))
                }
            }
        }
    }

    /** 导出整片。 */
    fun export() {
        val sb = _uiState.value.storyboard ?: return
        viewModelScope.launch {
            val out = paths.hostFile("manju_export.mp4")
            engine.export(sb, out)
                .onSuccess { _uiState.update { st -> st.copy(exportedPath = it, message = "导出成功") } }
                .onFailure { e -> _uiState.update { st -> st.copy(message = "导出失败：${e.message}") } }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
