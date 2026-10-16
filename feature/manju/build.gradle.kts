plugins {
    alias(libs.plugins.taixu.android.feature)
}

android {
    namespace = "top.wkbin.taixu.feature.manju"
    resourcePrefix = "fmanju_"
}

dependencies {
    implementation(project(":runtime"))
    // ProviderClient：分镜拆解复用太墟已配置的模型，无需额外 API Key
    implementation(project(":harness"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.bundles.test.robolectric)
    testImplementation(libs.koin.test)
}
