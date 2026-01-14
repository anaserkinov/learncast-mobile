package me.anasmusa.learncast.data.local.db.pagingstate

import androidx.room.Entity
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames

@Entity(tableName = TableNames.PAGING_STATE, primaryKeys = ["resourceType", "queryKey"])
class PagingStateEntity(
    val resourceType: String,
    val queryKey: String,
    val lastDeletionSync: LocalDateTime,
)
