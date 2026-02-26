package me.anasmusa.learncast.core.resource

import org.koin.mp.KoinPlatform

object Resource {
    private var currentLocale = "en"
    private val strings = HashMap<String, String>()

    val isLoaded: Boolean
        get() = strings.isNotEmpty()

    fun setLocale(
        locale: String,
        onLoad: () -> Unit,
    ) {
        val resourceManager = KoinPlatform.getKoin().get<ResourceManager>()

        if (strings.isEmpty()) {
            setStrings("en", resourceManager.readStringFile("en"))
            if (locale == "en") onLoad()
        }
        if (currentLocale == locale) {
            return
        }
        setStrings(
            locale,
            resourceManager.readStringFile(locale),
        )
        onLoad()
    }

    fun setStrings(
        locale: String,
        list: List<Pair<String, String>>,
    ) {
        list.forEach { strings[it.first] = it.second }
        currentLocale = locale
    }

    fun String.string(): String = string(*emptyArray())

    fun String.string(vararg args: Any?): String {
        val source = strings[this] ?: return ""
        if (args.isEmpty()) return source
        val result = StringBuilder(source.length)
        var i = 0
        var argsIndex = 0
        while (i < source.length) {
            if (source[i] == '%' && i != source.length - 1 && argsIndex < args.size) {
                when (source[i + 1]) {
                    's' -> {
                        result.append(args[argsIndex++])
                        i++
                    }
                    'd' -> {
                        result.append(args[argsIndex++])
                        i++
                    }
                }
            } else {
                result.append(source[i])
            }
            i++
        }
        return result.toString()
    }

    fun String.quantityString(arg: Any?): String = quantityString(args = arrayOf(arg))

    // "zero", "one", "two", "few", "many", "other"
    fun String.quantityString(vararg args: Any?): String {
        val quantity = (args[0] as Number).toDouble()
        val pluralItem =
            when {
                quantity > 1.0 -> "other"
                else -> "one"
            }
        return strings["${this}_plurals_$pluralItem"]
            ?.replaceFirst("%d", (args[0] as Number).toString())
            ?: ""
    }
}
