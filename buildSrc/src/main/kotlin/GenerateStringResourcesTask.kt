import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

abstract class GenerateStringResourcesTask : DefaultTask() {

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        generateStringResources(
            inputDir.get().toString(),
            outputDir.get().toString()
        )
    }

    private fun generateStringResources(
        inputDir: String,
        outputDir: String
    ) {
        val inputFile = File("$inputDir/strings.xml")
        val outputFile = File("$outputDir/Strings.kt")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile)
        doc.documentElement.normalize()

        val constants = mutableListOf<String>()

        let {
            val strings = doc.getElementsByTagName("string")
            for (i in 0 until strings.length) {
                val node = strings.item(i) ?: continue
                val name = node.attributes.getNamedItem("name").nodeValue
                constants.add("    const val ${name.uppercase()} = \"$name\"")
            }
        }

        let {
            val plurals = doc.getElementsByTagName("plurals")
            for (i in 0 until plurals.length) {
                val node = plurals.item(i) ?: continue
                val name = node.attributes.getNamedItem("name").nodeValue
                constants.add("    const val ${name.uppercase()} = \"$name\"")
            }
        }

        val content =
            """
            |package me.anasmusa.learncast
            |
            |object Strings {
            |${constants.joinToString("\n")}
            |}
            """.trimMargin()

        outputFile.parentFile.mkdirs()
        outputFile.writeText(content)
    }

}
