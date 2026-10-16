package top.wkbin.taixu.ui.manju

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.ui.components.EmptyPanel
import top.wkbin.taixu.ui.components.NoticeBanner
import top.wkbin.taixu.ui.components.RuntimeButton
import top.wkbin.taixu.ui.components.RuntimeCard
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeOutlinedTextField
import top.wkbin.taixu.ui.components.RuntimeTopBar
import top.wkbin.taixu.ui.components.SectionHeader
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf

/**
 * 漫剧制作主界面：脚本输入 → 分镜板 → 导出。
 */
@Composable
fun ManjuScreen(
    onBack: () -> Unit,
    viewModel: ManjuViewModel? = previewOrNull { koinViewModel() },
) {
    val state by viewModel?.uiState?.collectAsStateWithLifecycle()
        ?: previewStateOf(ManjuViewModel.UiState())

    // 出图配置表单的本地编辑态：Key 不回显已保存值，留空即沿用原值。
    var cfgBaseUrl by remember(state.showImageConfig) { mutableStateOf(state.imageBaseUrl) }
    var cfgModel by remember(state.showImageConfig) { mutableStateOf(state.imageModel) }
    var cfgApiKey by remember(state.showImageConfig) { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { RuntimeTopBar(title = "AI 漫剧", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { msg ->
                NoticeBanner(text = msg, onDismiss = { viewModel?.consumeMessage() })
            }

            // 出图收件箱：手动出图的入口，零 API 成本
            RuntimeCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("出图收件箱", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "把图按 shot_0.png、shot_1.png… 命名放进该目录，生成时优先取用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.inboxPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (state.inboxShots.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "已就绪：${state.inboxShots.sorted().joinToString(", ") { "shot_$it" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    RuntimeButton(onClick = { viewModel?.refreshInbox() }, tonal = true) {
                        RuntimeIcon(RuntimeIconName.Refresh)
                    }
                }
            }

            // 出图供应商配置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RuntimeButton(onClick = { viewModel?.onToggleImageConfig() }, tonal = true) {
                    RuntimeIcon(RuntimeIconName.Image)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.showImageConfig) "收起出图配置" else "出图配置")
                }
                Text(
                    text = if (state.imageKeyConfigured) "已配置" else "未配置（出图会失败）",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.imageKeyConfigured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            if (state.showImageConfig) {
                RuntimeCard(modifier = Modifier.fillMaxWidth()) {
                    RuntimeOutlinedTextField(
                        value = cfgBaseUrl,
                        onValueChange = { cfgBaseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("接口地址") },
                        placeholder = { Text("https://ark.cn-beijing.volces.com/api/v3") },
                    )
                    Spacer(Modifier.height(8.dp))
                    RuntimeOutlinedTextField(
                        value = cfgModel,
                        onValueChange = { cfgModel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("模型") },
                        placeholder = { Text("doubao-seedream-4-0-250828") },
                    )
                    Spacer(Modifier.height(8.dp))
                    RuntimeOutlinedTextField(
                        value = cfgApiKey,
                        onValueChange = { cfgApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        placeholder = { Text(if (state.imageKeyConfigured) "留空则沿用已保存的 Key" else "火山方舟 API Key") },
                    )
                    Spacer(Modifier.height(12.dp))
                    RuntimeButton(
                        onClick = {
                            viewModel?.onSaveImageConfig(cfgBaseUrl, cfgModel, cfgApiKey)
                            cfgApiKey = ""
                        },
                    ) {
                        RuntimeIcon(RuntimeIconName.Check)
                        Spacer(Modifier.width(8.dp))
                        Text("保存")
                    }
                }
            }

            // 脚本输入
            SectionHeader(title = "剧本", subtitle = "粘贴小说片段或自己写一段剧情")
            RuntimeOutlinedTextField(
                value = state.script,
                onValueChange = { viewModel?.onScriptChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                enabled = !state.isPlanning && !state.isGenerating,
                label = { Text("剧本内容") },
                placeholder = { Text("例如：雨夜，少年推开旧书店的门……") },
            )

            RuntimeOutlinedTextField(
                value = state.globalStyle,
                onValueChange = { viewModel?.onStyleChange(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isPlanning && !state.isGenerating,
                label = { Text("全局画风（可选）") },
                placeholder = { Text("例如：国漫2D，赛璐璐上色，冷色调") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RuntimeButton(
                    onClick = { viewModel?.planStoryboard() },
                    enabled = !state.isPlanning && !state.isGenerating,
                ) {
                    RuntimeIcon(RuntimeIconName.Sparkles)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isPlanning) "拆解中…" else "拆解分镜")
                }
                RuntimeButton(
                    onClick = { viewModel?.generateAll() },
                    enabled = state.storyboard != null && !state.isGenerating && !state.isPlanning,
                    tonal = true,
                ) {
                    RuntimeIcon(RuntimeIconName.Play)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isGenerating) "生成中…" else "生成全部")
                }
            }

            // 分镜板
            val storyboard = state.storyboard
            if (storyboard == null) {
                EmptyPanel(
                    icon = RuntimeIconName.Image,
                    title = "还没有分镜",
                    description = "输入剧本后点击「拆解分镜」，AI 会把它切成若干镜头",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SectionHeader(
                    title = "分镜板",
                    subtitle = "${storyboard.shots.size} 个镜头 · 约 ${storyboard.totalDurationSec} 秒 · 已完成 ${storyboard.readyCount}",
                )
                if (state.isGenerating) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(storyboard.shots, key = { it.index }) { shot ->
                        ShotRow(shot = shot, onRetry = { viewModel?.retryShot(shot.index) })
                    }
                    item {
                        RuntimeButton(
                            onClick = { viewModel?.export() },
                            enabled = storyboard.readyCount > 0 && !state.isGenerating,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RuntimeIcon(RuntimeIconName.Download)
                            Spacer(Modifier.width(8.dp))
                            Text("导出漫剧")
                        }
                    }
                    state.exportedPath?.let { path ->
                        item {
                            NoticeBanner(text = "已导出：$path")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShotRow(shot: Shot, onRetry: () -> Unit) {
    RuntimeCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${shot.index + 1}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = shot.imagePrompt, style = MaterialTheme.typography.bodyMedium)
                if (shot.camera.isNotBlank()) {
                    Text(
                        text = shot.camera,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                shot.dialogue.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "「$it」",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ShotStatusChip(shot = shot, onRetry = onRetry)
        }
    }
}

@Composable
private fun ShotStatusChip(shot: Shot, onRetry: () -> Unit) {
    when (val status = shot.status) {
        is ShotStatus.Pending -> Text(
            text = "${shot.durationSec}s",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is ShotStatus.Generating -> Text(
            text = "生成中…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        is ShotStatus.Ready -> RuntimeIcon(
            name = RuntimeIconName.Check,
            tint = MaterialTheme.colorScheme.primary,
        )

        is ShotStatus.Failed -> RuntimeButton(onClick = onRetry) {
            Text("重试")
        }
    }
}
