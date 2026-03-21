package dev.perfoverlay.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecordingSession::class, StatSample::class, AnomalyEventEntity::class],
    version = 4,
    exportSchema = false
)
abstract class PerfOverlayDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: PerfOverlayDatabase? = null

        fun getInstance(context: Context): PerfOverlayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PerfOverlayDatabase::class.java,
                    "perf_overlay_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
