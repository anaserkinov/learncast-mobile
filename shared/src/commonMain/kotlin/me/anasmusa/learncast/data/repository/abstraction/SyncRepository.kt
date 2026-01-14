package me.anasmusa.learncast.data.repository.abstraction

interface SyncRepository {
    suspend fun sync(finishWhenDrained: Boolean)
}
