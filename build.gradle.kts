// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

extra.apply {
    set("androidXVersion", "1.7.0")
    set("versionCompiler", 36)
    set("versionTarget", 36)
    set("minSdkVersion", 29)
    set("versionCode", 1)
    set("versionNameString", "1.0")
    set("javaSourceCompatibility", JavaVersion.VERSION_11)
    set("javaTargetCompatibility", JavaVersion.VERSION_11)
    set("versionBuildTool", "36.0.0")
    set("kotlinCoreVersion", "1.17.0")
    set("kotlinCoroutines", "1.9.0")
    set("materialVersion", "1.12.0")
    set("constraintlayoutVersion", "2.2.1")
    set("lifecycle_version", "2.10.0")
}
