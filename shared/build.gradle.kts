@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.squareupWire)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.skie)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

val generateStringResources by tasks.registering(GenerateStringResourcesTask::class) {
    inputDir.set(layout.projectDirectory.dir("src/commonMain/resources"))
    outputDir.set(
        layout.buildDirectory.dir("generated/source/kmp_strings/main/kotlin")
    )
}

kotlin {
    androidLibrary {
        namespace = "me.anasmusa.learncast.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf("-XXLanguage:+ExplicitBackingFields")
        )
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateStringResources)
            dependencies {
                implementation(libs.coroutine)

                implementation(libs.koin.core)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)

                implementation(libs.kotlinx.serialization.json)

                api(libs.napier)

                api(libs.androidx.lifecycle.viewmodel)
                api(libs.kotlinx.datetime)
                implementation(libs.datastore.core.okio)
                implementation(libs.napier)

                implementation(libs.paging.common)

                implementation(libs.room.paging)
                implementation(libs.room.runtime)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.paging.runtime)

                implementation(libs.koin.android)

                implementation(libs.androidx.media3.session)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.exoplayer.workmanager)
                implementation(libs.androidx.media3.exoplayer.okhttp)

                implementation(libs.coil)

                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging)

                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)

                implementation(libs.androidx.work.runtime)
            }
        }
        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqlite.bundled)
            }
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
//    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

wire {
    kotlin {
    }

    sourcePath {
        srcDir("src/commonMain/proto")
    }
}

skie {
    features {
        enableSwiftUIObservingPreview = true
    }
    analytics {
        disableUpload.set(true)
    }
}

val copyStringsToIos by tasks.registering(Copy::class) {
    group = "resources"
    description = "Copy KMP string resources to iOS resources"
    from(layout.projectDirectory.dir("src/commonMain/resources"))
    into(layout.projectDirectory.dir("../ios/Resources"))
}

tasks.matching { it.name.startsWith("embedAndSign") }.configureEach {
    dependsOn(copyStringsToIos)
}
