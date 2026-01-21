plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "me.anasmusa.learncast.lib"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            val crashlyticsCollectionEnabled = project.findProperty("crashlyticsEnabled")?.toString()?.toBoolean() ?: true
            manifestPlaceholders["crashlyticsCollectionEnabled"] = crashlyticsCollectionEnabled
        }
        release {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
        }
    }
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
        freeCompilerArgs.addAll(
            listOf("-Xexplicit-backing-fields")
        )
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android.set(true)
}

afterEvaluate {
    tasks.named("preBuild").configure {
        dependsOn(copyStringsToAndroid)
    }
}

val copyStringsToAndroid by tasks.registering(Copy::class) {
    group = "resources"
    description = "Copy KMP string resources to Android assets"

    from(layout.projectDirectory.dir("src/commonMain/resources"))
    into(layout.projectDirectory.dir("../android/lib/src/main/assets"))
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material3)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    api(projects.shared)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    implementation(libs.palette)

    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.haze)

    implementation(libs.androidx.media3.session)

    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    implementation(libs.app.update.ktx)
    implementation(libs.androidx.core.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
