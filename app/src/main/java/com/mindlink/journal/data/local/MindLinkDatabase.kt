package com.mindlink.journal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mindlink.journal.data.local.converters.Converters
import com.mindlink.journal.data.local.dao.EmbeddingDao
import com.mindlink.journal.data.local.dao.NoteDao
import com.mindlink.journal.data.local.dao.NoteFtsDao
import com.mindlink.journal.data.local.dao.NoteLinkDao
import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import com.mindlink.journal.data.local.entity.NoteEntity
import com.mindlink.journal.data.local.entity.NoteFtsEntity
import com.mindlink.journal.data.local.entity.NoteLinkEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteFtsEntity::class,
        NoteEmbeddingEntity::class,
        NoteLinkEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MindLinkDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun noteFtsDao(): NoteFtsDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun noteLinkDao(): NoteLinkDao

    companion object {
        @Volatile
        private var INSTANCE: MindLinkDatabase? = null

        fun getInstance(context: Context): MindLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindLinkDatabase::class.java,
                    "mindlink_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
