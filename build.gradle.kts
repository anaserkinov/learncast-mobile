plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.crashlytics) apply false
}

val installGitHook = tasks.register("installGitHook", Copy::class) {
    from("$rootDir/pre-commit")
    into("$rootDir/.git/hooks")
    filePermissions {
        user { read = true; write = true; execute = true }
        group { read = true; execute = true }
        other { read = true; execute = true }
    }
}

allprojects {
    afterEvaluate {
        tasks.findByName("preBuild")?.dependsOn(installGitHook)
    }
}
