package me.anasmusa.learncast.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import me.anasmusa.learncast.data.repository.abstraction.StorageRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class StorageState(
    val isLoading: Boolean = false,
    val cacheSize: String? = null,
    val downloadSize: String? = null,
) : BaseState

sealed interface StorageIntent : BaseIntent {
    object ClearCache : StorageIntent

    object ClearDownloads : StorageIntent
}

sealed interface StorageEvent : BaseEvent

class StorageViewModel(
    private val storageRepository: StorageRepository,
    private val queueRepository: QueueRepository,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<StorageState, StorageIntent, StorageEvent>() {
    final override val state: StateFlow<StorageState>
        field = MutableStateFlow(StorageState())

    init {
        viewModelScope.launch {
            launch {
                val cacheSize = storageRepository.getCacheSize()
                val downloadSize = storageRepository.getDownloadSize()
                state.update {
                    it.copy(
                        cacheSize = format(cacheSize),
                        downloadSize = format(downloadSize),
                    )
                }
            }
        }
    }

    override fun handle(intent: StorageIntent) {
        super.handle(intent)
        when (intent) {
            StorageIntent.ClearCache -> clearCache()
            StorageIntent.ClearDownloads -> clearDownload()
        }
    }

    private fun clearCache() {
        state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            playerRepository.stopService()
            val cacheSize = storageRepository.clearCaches()
            state.update {
                it.copy(
                    cacheSize = format(cacheSize),
                )
            }
            val queuedItems = queueRepository.getQueuedItems()
            withContext(Dispatchers.Main) {
                playerRepository.restoreService()
                playerRepository.setToQueue(queuedItems)
            }
            state.update {
                it.copy(isLoading = false)
            }
        }
    }

    private fun clearDownload() {
        state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            playerRepository.stopService()
            val downloadSize = storageRepository.clearDownloads()
            state.update {
                it.copy(
                    downloadSize = format(downloadSize),
                )
            }
            val queuedItems = queueRepository.getQueuedItems()
            withContext(Dispatchers.Main) {
                playerRepository.restoreService()
                playerRepository.setToQueue(queuedItems)
            }
            state.update {
                it.copy(isLoading = false)
            }
        }
    }

    private fun format(mb: Float): String {
        val value: Float
        val sign =
            if (mb > 1000) {
                value = mb / 1000
                "GB"
            } else {
                value = mb
                "MB"
            }
        val rounded = kotlin.math.round(value * 10) / 10
        val text =
            if (rounded % 1.0 == 0.0) {
                rounded.toLong().toString()
            } else {
                rounded.toString()
            }
        return "$text $sign"
    }
}
