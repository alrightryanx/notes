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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the Note DAO
     */
    abstract fun noteDao(): NoteDao

    /**
     * Provides access to the Label DAO
     */
    abstract fun labelDao(): LabelDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton database instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database"
                )
                    // Add migrations
                    .addMigrations(MIGRATION_1_2)
                    // Enable destructive migration as last resort
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}