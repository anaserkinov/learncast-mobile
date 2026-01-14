package me.anasmusa.learncast

import me.anasmusa.learncast.core.coreModule
import me.anasmusa.learncast.data.dataModule
import me.anasmusa.learncast.ui.uiModule

fun getModules() =
    listOf(
        coreModule(),
        dataModule(),
        uiModule(),
    )
