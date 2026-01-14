@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.utils.forEach
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.apply
import kotlin.collections.set

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.squareupWire)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.skie)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val xcf = XCFramework("Shared")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true

            xcf.add(this)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
        languageSettings.enableLanguageFeature("ContextParameters")
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
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

android {
    namespace = "me.anasmusa.learncast.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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

tasks.register("generateStringResources") {
    val inputFile = file("src/commonMain/resources/values/strings.xml")
    val outputFile = file("src/commonMain/kotlin/me/anasmusa/learncast/Strings.kt")

    doLast {
        val orders = HashMap<String, Int>()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile)
        doc.documentElement.normalize()

        var pluralFirstIndex: Int
        val constants = mutableListOf<String>()

        let {
            val strings = doc.getElementsByTagName("string")
            strings.forEach { node ->
                val index = constants.size
                val name = node.attributes.getNamedItem("name").nodeValue
                orders[name] = index
                constants.add("    const val ${name.uppercase()} = ${index + 1}")
            }
        }

        let {
            pluralFirstIndex = constants.size + 1
            val plurals = doc.getElementsByTagName("plurals")
            var index = constants.size
            plurals.forEach { node ->
                val name = node.attributes.getNamedItem("name").nodeValue
                orders[name] = index
                constants.add("    const val ${name.uppercase()} = ${index + 1}")
                index += 7
            }
        }

        val content =
            """
            |package me.anasmusa.learncast
            |
            |object Strings {
            |
            |    internal const val PLURAL_FIRST_INDEX = $pluralFirstIndex
            |
            |${constants.joinToString("\n")}
            |}
            """.trimMargin()

        outputFile.parentFile.mkdirs()
        outputFile.writeText(content)

        orderStrings("uz", orders)
//        orderStrings("ru", orders)

        // android
        listOf("values", "values-uz").forEach {
            val sourceFile = file("src/commonMain/resources/$it/strings.xml")
            val targetDir = file("$rootDir/android/lib/src/main/assets/$it/")
            targetDir.mkdirs()
            sourceFile.copyTo(targetDir.resolve("strings.xml"), overwrite = true)
        }

        // ios
        val targetDir = file("$rootDir/ios/Resources/")
        targetDir.mkdirs()
        listOf("", "-uz").forEach {
            val sourceFile = file("src/commonMain/resources/values$it/strings.xml")
            sourceFile.copyTo(targetDir.resolve("strings$it.xml"), overwrite = true)
        }
    }
}

fun orderStrings(locale: String, order: Map<String, Int>){
    val inputFile = file("src/commonMain/resources/values-$locale/strings.xml")

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile)
    doc.documentElement.normalize()

    val strings = doc.getElementsByTagName("string")
    val stringMap = mutableMapOf<String, String>()
    for (i in 0 until strings.length) {
        val node = strings.item(i)
        val name = node.attributes.getNamedItem("name").nodeValue
        val value = node.textContent
        stringMap[name] = value
    }
    val sortedStrings = stringMap.entries.sortedWith(
        compareBy { order[it.key] }
    )


    val plurals = doc.getElementsByTagName("plurals")
    val pluralMap = mutableMapOf<String, NodeList>()
    for (i in 0 until plurals.length) {
        val node = plurals.item(i)
        val name = node.attributes.getNamedItem("name").nodeValue
        val value = node.childNodes
        pluralMap[name] = value
    }
    val sortedPlurals = pluralMap.entries.sortedWith(
        compareBy { order[it.key] }
    )


    // Clear existing elements
    val root = doc.documentElement
    while (root.hasChildNodes()) {
        root.removeChild(root.firstChild)
    }

    // Re-add sorted elements
    for ((key, value) in sortedStrings) {
        val stringElement = doc.createElement("string")
        stringElement.setAttribute("name", key)
        stringElement.textContent = value
        root.appendChild(stringElement)
    }
    for ((key, value) in sortedPlurals) {
        val pluralElement = doc.createElement("plurals")
        pluralElement.setAttribute("name", key)
        value.forEach {
            pluralElement.appendChild(it)
        }
        root.appendChild(pluralElement)
    }

    // Write back to file
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")  // Enable indentation
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4") // Set indentation level
    }
    val source = DOMSource(doc)
    val result = StreamResult(inputFile)
    transformer.transform(source, result)
}
