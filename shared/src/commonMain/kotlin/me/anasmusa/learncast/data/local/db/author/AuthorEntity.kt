package me.anasmusa.learncast.data.local.db.author

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames

@Entity(tableName = TableNames.AUTHOR)
class AuthorEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val avatarPath: String?,
    val createdAt: LocalDateTime,
    val lessonCount: Long,
)
