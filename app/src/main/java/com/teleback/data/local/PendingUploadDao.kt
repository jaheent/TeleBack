package com.teleback.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingUploadDao {
    @Query("SELECT * FROM pending_uploads WHERE fileSha256 = :sha ORDER BY chunkIndex")
    suspend fun forFile(sha: String): List<PendingUploadEntity>

    @Query("SELECT * FROM pending_uploads")
    suspend fun all(): List<PendingUploadEntity>

    @Query("SELECT COUNT(*) FROM pending_uploads")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PendingUploadEntity)

    @Query("DELETE FROM pending_uploads WHERE fileSha256 = :sha AND chunkIndex = :chunkIndex")
    suspend fun delete(sha: String, chunkIndex: Int)
}
