package me.anasmusa.learncast.data.local.db.download

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType

@Entity(tableName = TableNames.DOWNLOAD_STATE)
class DownloadStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val referenceId: Long,
    val referenceUuid: String,
    val referenceType: ReferenceType,
    val audioPath: String,
    val startMs: Long?,
    val endMs: Long?,
    val state: DownloadState,
    val percentDownloaded: Float,
)
