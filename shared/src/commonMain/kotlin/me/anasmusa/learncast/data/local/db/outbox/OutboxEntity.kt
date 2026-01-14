package me.anasmusa.learncast.data.local.db.outbox

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.OutboxStatus
import me.anasmusa.learncast.data.model.ReferenceType

@Entity(tableName = TableNames.OUTBOX)
class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val referenceId: Long,
    val referenceUuid: String,
    val referenceType: ReferenceType,
    val actionType: ActionType,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastTriedAt: LocalDateTime?,
    val status: OutboxStatus,
)
