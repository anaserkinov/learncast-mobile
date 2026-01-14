package me.anasmusa.learncast

import me.anasmusa.learncast.Resource.strings
import kotlin.toString

object Resource {
    private var currentLocale: String? = null
    internal val strings = ArrayList<String>()

    val isLoaded: Boolean
        get() = currentLocale != null && strings.isNotEmpty()

    fun setLocale(
        locale: String,
        onLoad: () -> Unit,
    ) {
        if (currentLocale == locale) {
            return
        }
        readStringFile(locale) {
            setStrings(locale, it)
            onLoad()
        }
    }

    fun setStrings(
        locale: String,
        list: List<String>,
    ) {
        this.strings.clear()
        this.strings.add("")
        this.strings.addAll(list)
        currentLocale = locale
    }
}

fun Int.string(): String = string(emptyArray<Any?>())

fun Int.string(arg: Any?): String = string(args = arrayOf(arg))

fun Int.string(vararg args: Any?): String {
    val source = strings[this]
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

fun Int.quantityString(arg: Any?): String = quantityString(args = arrayOf(arg))

// "zero", "one", "two", "few", "many", "other"
fun Int.quantityString(vararg args: Any?): String {
    val quantity = (args[0] as Number).toDouble()
    val offset =
        if (quantity > 1.0) {
            6
        } else {
            2
        }
    return strings[this + offset].replaceFirst("%d", (args[0] as Number).toString())
}

expect fun readStringFile(
    locale: String,
    onLoad: (List<String>) -> Unit,
)
