package me.anasmusa.learncast.core.platform

enum class Os {
    Android,
    Ios,
}

expect val os: Os

fun isAndroid() = os == Os.Android

fun isIOS() = os == Os.Ios
