package com.mindlink.journal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 7,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN fontSizeScale REAL DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN fontSizeScale REAL DEFAULT NULL")
                } catch (_: Exception) {}

                db.execSQL("CREATE TABLE IF NOT EXISTS `note_embeddings_new` (`noteId` INTEGER NOT NULL, `vector` TEXT NOT NULL, `dimension` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`noteId`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                try {
                    db.execSQL("INSERT OR IGNORE INTO `note_embeddings_new` (`noteId`, `vector`, `dimension`, `updatedAt`) SELECT `noteId`, `vector`, `dimension`, `updatedAt` FROM `note_embeddings`")
                } catch (_: Exception) {}
                db.execSQL("DROP TABLE IF EXISTS `note_embeddings`")
                db.execSQL("ALTER TABLE `note_embeddings_new` RENAME TO `note_embeddings`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_embeddings_noteId` ON `note_embeddings` (`noteId`)")
            }
        }

        val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN fontSizeScale REAL DEFAULT NULL")
                } catch (_: Exception) {}

                db.execSQL("CREATE TABLE IF NOT EXISTS `note_embeddings_new` (`noteId` INTEGER NOT NULL, `vector` TEXT NOT NULL, `dimension` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`noteId`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                try {
                    db.execSQL("INSERT OR IGNORE INTO `note_embeddings_new` (`noteId`, `vector`, `dimension`, `updatedAt`) SELECT `noteId`, `vector`, `dimension`, `updatedAt` FROM `note_embeddings`")
                } catch (_: Exception) {}
                db.execSQL("DROP TABLE IF EXISTS `note_embeddings`")
                db.execSQL("ALTER TABLE `note_embeddings_new` RENAME TO `note_embeddings`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_embeddings_noteId` ON `note_embeddings` (`noteId`)")
            }
        }

        fun getInstance(context: Context): MindLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindLinkDatabase::class.java,
                    "mindlink_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_5_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
