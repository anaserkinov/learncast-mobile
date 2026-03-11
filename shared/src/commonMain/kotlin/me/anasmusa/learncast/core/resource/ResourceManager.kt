package me.anasmusa.learncast.core.resource

interface ResourceManager {
    fun readStringFile(locale: String): List<Pair<String, String>>
}
