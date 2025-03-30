package com.xr.notes.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xr.notes.models.Label
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef

/**
 * Main database class for the Notes application.
 * Uses Room to handle database operations.
 */
@Database(
    entities = [Note::class, Label::class, NoteLabelCrossRef::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
public abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the Note DAO
     */
    public abstract fun noteDao(): NoteDao

    /**
     * Provides access to the Label DAO
     */
    public abstract fun labelDao(): LabelDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton database instance.
         */
        public fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}