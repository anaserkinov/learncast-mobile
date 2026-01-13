package me.anasmusa.learncast.data.local.db.outbox

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import me.anasmusa.learncast.data.local.db.TableNames

@Entity(
    tableName = TableNames.LISTEN_OUTBOX,
    foreignKeys = [
        ForeignKey(
            entity = OutboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["outboxId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class ListenOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val outboxId: Long,
    val sessionId: String
)