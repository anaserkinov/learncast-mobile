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
actual fun readStringFile(locale: String): List<Pair<String, String>> {
    var file = "strings"
    if (locale != "en") {
        file += "-$locale"
    }
    val bundle =
        NSBundle.allFrameworks.find {
            (it as NSBundle).bundlePath.endsWith("/lib.framework")
        } as? NSBundle ?: return emptyList()

    val path = bundle.pathForResource(file, "xml") ?: ""
    val xmlContent = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""

    return parseStringsXml(xmlContent)
}

@OptIn(ExperimentalForeignApi::class)
private fun parseStringsXml(xmlContent: String): List<Pair<String, String>> {
    val result = arrayListOf<Pair<String, String>>()

    val byteArray = xmlContent.encodeToByteArray()
    val data =
        byteArray.usePinned {
            NSData.dataWithBytes(it.addressOf(0), byteArray.size.toULong())
        }

    val parser = NSXMLParser(data)
    parser.delegate =
        object : NSObject(), NSXMLParserDelegateProtocol {
            // "zero", "one", "two", "few", "many", "other"
            var key: String? = null
            var value: String? = null
            var pluralKey: String? = null

            override fun parser(
                parser: NSXMLParser,
                didStartElement: String,
                namespaceURI: String?,
                qualifiedName: String?,
                attributes: Map<Any?, *>,
            ) {
                when (didStartElement) {
                    "string" -> key = attributes["name"] as? String
                    "item" -> key = attributes["quantity"] as? String
                    "plurals" -> pluralKey = attributes["name"] as? String
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
                    result.add(key!! to value.toString())
                } else if (didEndElement == "item" && key != null && value != null) {
                    result.add("${pluralKey}_plurals_$key" to value!!)
                }
            }
        }

    if (!parser.parse()) {
        println("❌ XML Parsing Error on iOS")
    }

    return result
}
