package me.anasmusa.learncast.core.resource

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.StringReader

class AndroidResourceManager(
    private val context: Context,
) : ResourceManager {
    override fun readStringFile(locale: String): List<Pair<String, String>> {
        var path = "strings"
        if (locale != "en") {
            path += "-$locale"
        }
        path = "$path.xml"
        return context.assets.open(path).use {
            parseStringsXml(
                it.bufferedReader().use(BufferedReader::readText),
            )
        }
    }

    fun parseStringsXml(xmlContent: String): List<Pair<String, String>> {
        val result = arrayListOf<Pair<String, String>>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var key: String? = null
            var value: String? = null
            var pluralKey: String? = null

            // "zero", "one", "two", "few", "many", "other"
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "string" -> key = parser.getAttributeValue(null, "name")
                            "item" -> key = parser.getAttributeValue(null, "quantity")
                            "plurals" -> pluralKey = parser.getAttributeValue(null, "name")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        value = parser.text
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "string" && key != null && value != null) {
                            result.add(key to value)
                        } else if (parser.name == "item" && key != null && value != null) {
                            result.add("${pluralKey}_plurals_$key" to value)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
        }

        return result
    }
}
