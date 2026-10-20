package com.teleback.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SnapshotEntity::class, PendingUploadEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Adds the resume journal. Existing snapshots are preserved. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_uploads` (
                        `fileSha256` TEXT NOT NULL,
                        `chunkIndex` INTEGER NOT NULL,
                        `relativePath` TEXT NOT NULL,
                        `chunkCount` INTEGER NOT NULL,
                        `partSize` INTEGER NOT NULL,
                        `partSha256` TEXT NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `fileId` TEXT NOT NULL,
                        `encrypted` INTEGER NOT NULL,
                        `keyFingerprint` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`fileSha256`, `chunkIndex`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teleback.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
