package me.anasmusa.learncast.data.local.db.outbox

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import me.anasmusa.learncast.data.local.db.TableNames

@Entity(
    tableName = TableNames.SNIP_OUTBOX,
    foreignKeys = [
        ForeignKey(
            entity = OutboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["outboxId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
class SnipOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val outboxId: Long,
    val clientSnipId: String,
    val lessonId: Long,
    val startMs: Long,
    val endMs: Long,
    val note: String?,
)

class SnipWithOutbox(
    @Embedded
    val snip: SnipOutboxEntity,
    @Embedded(prefix = "outbox_")
    val outbox: OutboxEntity,
)
