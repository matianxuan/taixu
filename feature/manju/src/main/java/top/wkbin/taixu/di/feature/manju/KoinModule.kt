package top.wkbin.taixu.di.feature.manju

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import top.wkbin.taixu.runtime.LinuxRuntime
import top.wkbin.taixu.ui.manju.ManjuPaths
import top.wkbin.taixu.ui.manju.ImageGenerator
import top.wkbin.taixu.ui.manju.ManjuEngine
import top.wkbin.taixu.ui.manju.ManjuViewModel
import top.wkbin.taixu.ui.manju.StoryboardLlm
import top.wkbin.taixu.ui.manju.StoryboardPlanner
import top.wkbin.taixu.ui.manju.VideoComposer
import top.wkbin.taixu.ui.manju.VoiceSynthesizer
import top.wkbin.taixu.ui.manju.impl.FfmpegVideoComposer
import top.wkbin.taixu.ui.manju.impl.PlaceholderImageGenerator
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
    single<StoryboardLlm> { ProviderStoryboardLlm(providerClient = get()) }
    single<ImageGenerator> { PlaceholderImageGenerator() }
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

    viewModel { ManjuViewModel(planner = get(), engine = get(), paths = get()) }
}
