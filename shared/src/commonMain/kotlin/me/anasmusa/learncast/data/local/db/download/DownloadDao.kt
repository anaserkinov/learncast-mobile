package me.anasmusa.learncast.data.local.db.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType

@Dao
interface DownloadDao {
    @Insert
    suspend fun insert(item: DownloadStateEntity): Long

    @Query("SELECT * FROM ${TableNames.DOWNLOAD_STATE} WHERE id = :id")
    suspend fun getById(id: Long): DownloadStateEntity?

    @Query("SELECT * FROM ${TableNames.DOWNLOAD_STATE} WHERE referenceId = :referenceId AND referenceUuid = :referenceUuid AND referenceType = :referenceType")
    suspend fun get(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
    ): DownloadStateEntity?

    @Query("UPDATE ${TableNames.DOWNLOAD_STATE} SET state = :state, percentDownloaded = :percentDownloaded WHERE id = :id")
    suspend fun update(
        id: Long,
        state: DownloadState,
        percentDownloaded: Float,
    )

    @Query("SELECT EXISTS(SELECT id FROM ${TableNames.DOWNLOAD_STATE} WHERE audioPath = :audioPath)")
    suspend fun isInUse(audioPath: String): Boolean

    @Query("DELETE FROM ${TableNames.DOWNLOAD_STATE} WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM ${TableNames.DOWNLOAD_STATE} WHERE audioPath = :audioPath")
    suspend fun delete(audioPath: String)

    @Query("DELETE FROM ${TableNames.DOWNLOAD_STATE}")
    suspend fun clear()
}
