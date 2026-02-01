package me.anasmusa.learncast.data

import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository
import me.anasmusa.learncast.data.repository.implementation.DownloadRepositoryImpl
import org.koin.core.module.Module

internal actual fun Module.platformModule() {
    factory<DownloadRepository> {
        DownloadRepositoryImpl()
    }
}
