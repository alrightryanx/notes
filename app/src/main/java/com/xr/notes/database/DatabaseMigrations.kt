package com.xr.notes.database

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from version 1 to 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Log migration process
        Log.d("DatabaseMigration", "Starting migration from version 1 to 2")

        try {
            // Verify tables exist and have proper structure
            database.execSQL("SELECT id, content, isEncrypted, createdAt, modifiedAt FROM notes LIMIT 1")
            database.execSQL("SELECT id, name, color FROM labels LIMIT 1")
            database.execSQL("SELECT noteId, labelId FROM note_label_cross_ref LIMIT 1")

            // Migration successful
            Log.d("DatabaseMigration", "Migration successful: verified all tables intact")
        } catch (e: Exception) {
            // Log migration errors
            Log.e("DatabaseMigration", "Error during migration: ${e.message}")

            // If we catch exceptions here, the migration will fail and Room will
            // use fallbackToDestructiveMigration if enabled. If we want to handle
            // specific errors, we could potentially create missing tables here.
            throw e
        }
    }
}