package me.anasmusa.learncast

import me.anasmusa.learncast.data.local.localModule
import me.anasmusa.learncast.data.network.networkModule


fun getModules() = listOf(
    localModule(),
    networkModule(),
    dataModule(),
    uiModule()
)