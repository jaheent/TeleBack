package com.teleback.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    suspend fun getAllSnapshotsDirect(): List<SnapshotEntity>

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): SnapshotEntity?

    @Query("SELECT * FROM snapshots WHERE snapshotId = :id LIMIT 1")
    suspend fun getSnapshotById(id: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    @Query("DELETE FROM snapshots WHERE snapshotId = :id")
    suspend fun deleteSnapshot(id: String)
}
