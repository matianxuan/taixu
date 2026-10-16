package top.wkbin.taixu.di.feature.manju

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import top.wkbin.taixu.runtime.LinuxRuntime
import kotlinx.coroutines.flow.first
import top.wkbin.taixu.core.datastore.ManjuImageEndpoint
import top.wkbin.taixu.core.datastore.manjuImageConfig
import top.wkbin.taixu.core.datastore.setManjuImageConfig
import top.wkbin.taixu.core.datastore.SettingsDataStore
import top.wkbin.taixu.ui.manju.ManjuImageConfigStore
import top.wkbin.taixu.ui.manju.ManjuImageEndpointConfig
import top.wkbin.taixu.ui.manju.ManjuPaths
import top.wkbin.taixu.ui.manju.ImageGenerator
import top.wkbin.taixu.ui.manju.ManjuEngine
import top.wkbin.taixu.ui.manju.ManjuViewModel
import top.wkbin.taixu.ui.manju.StoryboardLlm
import top.wkbin.taixu.ui.manju.StoryboardPlanner
import top.wkbin.taixu.ui.manju.VideoComposer
import top.wkbin.taixu.ui.manju.VoiceSynthesizer
import top.wkbin.taixu.ui.manju.impl.FfmpegVideoComposer
import top.wkbin.taixu.ui.manju.impl.ArkImageGenerator
import top.wkbin.taixu.ui.manju.impl.CompositeImageGenerator
import top.wkbin.taixu.ui.manju.impl.LocalFileImageGenerator
import top.wkbin.taixu.ui.manju.impl.PlaceholderVoiceSynthesizer
import top.wkbin.taixu.ui.manju.impl.ProviderStoryboardLlm

/**
 * feature:manju 模块的依赖注册。
 *
 * 分镜拆解复用太墟既有 ProviderClient（用户已配置的模型）；
 * 出图与配音默认用占位实现，接入真实供应商时只需替换这里的绑定，
 * 上层编排逻辑无需改动。
 */
val featureManjuModule = module {
    // 出图配置读写适配：把 core:datastore 的加密存储桥接成漫剧模块的窄接口，
    // 使 ViewModel/Engine 不直接依赖 datastore，测试可换成内存实现。
    single<ManjuImageConfigStore> {
        val store = get<SettingsDataStore>()
        object : ManjuImageConfigStore {
            override suspend fun current(): ManjuImageEndpointConfig = store.manjuImageConfig.first().let {
                ManjuImageEndpointConfig(it.baseUrl, it.model, it.apiKey)
            }

            override suspend fun save(value: ManjuImageEndpointConfig) {
                store.setManjuImageConfig(
                    ManjuImageEndpoint(baseUrl = value.baseUrl, model = value.model, apiKey = value.apiKey)
                )
            }
        }
    }

    single<StoryboardLlm> { ProviderStoryboardLlm(providerClient = get()) }
    // 出图调度：优先取「收件箱」里手动放好的图（零成本），
    // 没有则回退到已配置的 API（火山方舟 doubao-seedream，即梦同源）。
    single { LocalFileImageGenerator(inboxDir = { get<ManjuPaths>().hostInboxDir }) }
    single<ImageGenerator> {
        CompositeImageGenerator(
            inbox = get(),
            api = ArkImageGenerator(
                httpClient = get(),
                endpointProvider = {
                    get<ManjuImageConfigStore>().current().let {
                        ManjuImageEndpoint(baseUrl = it.baseUrl, model = it.model, apiKey = it.apiKey)
                    }
                },
            ),
        )
    }
    single<VoiceSynthesizer> { PlaceholderVoiceSynthesizer() }
    single<VideoComposer> { FfmpegVideoComposer(linuxRuntime = lazy { get<LinuxRuntime>() }) }

    single { StoryboardPlanner(llm = get()) }

    // 宿主/guest 双路径：/workspace 是 PRoot bind mount，二者指向同一份数据。
    single {
        val workspaceHost = get<LinuxRuntime>().workspacePath()
        ManjuPaths(
            hostWorkDir = java.io.File(workspaceHost, ManjuPaths.RELATIVE_DIR).absolutePath,
            guestWorkDir = ManjuPaths.GUEST_MANJU_DIR,
        )
    }

    single {
        ManjuEngine(
            imageGenerator = get(),
            voiceSynthesizer = get(),
            videoComposer = get(),
            paths = get(),
        )
    }

    viewModel { ManjuViewModel(planner = get(), engine = get(), paths = get(), imageConfig = get()) }
}
