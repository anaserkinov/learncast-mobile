package me.anasmusa.learncast

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.StringReader

actual fun readStringFile(
    locale: String,
    onLoad: (List<String>) -> Unit,
) {
    var path = "values"
    if (locale != "en") {
        path += "-$locale"
    }
    path = "$path/strings.xml"
    val inputStream = ApplicationLoader.context.assets.open(path)
    val xmlContent = inputStream.bufferedReader().use(BufferedReader::readText)
    onLoad(
        parseStringsXml(xmlContent),
    )
}

fun parseStringsXml(xmlContent: String): List<String> {
    val result = arrayListOf<String>()

    try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType
        var key: String? = null
        var value: String? = null

        val quantities = arrayOf("zero", "one", "two", "few", "many", "other")
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "string") {
                        key = parser.getAttributeValue(null, "name")
                    } else if (parser.name == "item") {
                        key = parser.getAttributeValue(null, "quantity")
                    } else if (parser.name == "plurals") {
                        repeat(7) { result.add("") }
                    }
                }
                XmlPullParser.TEXT -> {
                    value = parser.text
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "string" && key != null && value != null) {
                        result.add(value.trim())
                    } else if (parser.name == "item" && key != null && value != null) {
                        val index = quantities.indexOf(key)
                        result[result.size - 6 + index] = value.trim()
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
    }

    return result
}
