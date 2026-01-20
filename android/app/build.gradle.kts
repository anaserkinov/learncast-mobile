plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "me.anasmusa.learncast"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "me.anasmusa.learncast"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystores/debug.jks")
            storePassword = "learncast_app"
            keyAlias = "learncast_app"
            keyPassword = "learncast_app"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val idSuffix = project.findProperty("environment")?.toString()
            if (idSuffix == "dev") {
                applicationIdSuffix = ".dev"
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
    buildFeatures {
        compose = true
    }

    lint {
        quiet = true
        xmlReport = false
        textReport = false
        checkDependencies = true
    }
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
        freeCompilerArgs.addAll(
            listOf(
                "-Xexplicit-backing-fields",
                "-Xcontext-parameters"
            )
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material3)

    implementation(projects.android.lib)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
