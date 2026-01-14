package me.anasmusa.learncast

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.dataWithBytes
import platform.Foundation.stringWithContentsOfFile
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual fun readStringFile(
    locale: String,
    onLoad: (List<String>) -> Unit,
) {
    var file = "strings"
    if (locale != "en") {
        file += "-$locale"
    }
    val path = NSBundle.mainBundle.pathForResource(file, "xml") ?: ""
    val xmlContent = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""
    onLoad(
        parseStringsXml(xmlContent),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun parseStringsXml(xmlContent: String): List<String> {
    val result = arrayListOf<String>()

    val byteArray = xmlContent.encodeToByteArray()
    val data =
        byteArray.usePinned {
            NSData.dataWithBytes(it.addressOf(0), byteArray.size.toULong())
        }

    val parser = NSXMLParser(data)
    parser.delegate =
        object : NSObject(), NSXMLParserDelegateProtocol {
            val quantities = arrayOf("zero", "one", "two", "few", "many", "other")
            var key: String? = null
            var value: String? = null

            override fun parser(
                parser: NSXMLParser,
                didStartElement: String,
                namespaceURI: String?,
                qualifiedName: String?,
                attributes: Map<Any?, *>,
            ) {
                if (didStartElement == "string") {
                    key = attributes["name"] as? String
                } else if (didStartElement == "item") {
                    key = attributes["quantity"] as? String
                } else if (didStartElement == "plurals") {
                    repeat(7) { result.add("") }
                }
            }

            override fun parser(
                parser: NSXMLParser,
                foundCharacters: String,
            ) {
                value = foundCharacters
            }

            override fun parser(
                parser: NSXMLParser,
                didEndElement: String,
                namespaceURI: String?,
                qualifiedName: String?,
            ) {
                if (didEndElement == "string" && key != null && value != null) {
                    result.add(value.toString().trim())
                } else if (didEndElement == "item" && key != null && value != null) {
                    val index = quantities.indexOf(key)
                    result[result.size - 6 + index] = value.toString().trim()
                }
            }
        }

    if (!parser.parse()) {
        println("❌ XML Parsing Error on iOS")
    }

    return result
}
