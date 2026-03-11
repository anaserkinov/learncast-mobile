package me.anasmusa.learncast.core

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun Module.platformModule()

internal fun coreModule() =
    module {
        platformModule()
    }
