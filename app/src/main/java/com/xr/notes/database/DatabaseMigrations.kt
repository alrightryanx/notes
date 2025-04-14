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
            // For now, this migration doesn't need to make changes since the structure is the same
            // Just verify the tables exist and log the success

            // Check if tables exist to avoid exceptions
            val noteTableExists = try {
                database.query("SELECT 1 FROM notes LIMIT 1")
                true
            } catch (e: Exception) {
                Log.e("DatabaseMigration", "Notes table missing: ${e.message}")
                false
            } finally {
                // Close cursor if needed
            }

            val labelTableExists = try {
                database.query("SELECT 1 FROM labels LIMIT 1")
                true
            } catch (e: Exception) {
                Log.e("DatabaseMigration", "Labels table missing: ${e.message}")
                false
            } finally {
                // Close cursor if needed
            }

            val crossRefTableExists = try {
                database.query("SELECT 1 FROM note_label_cross_ref LIMIT 1")
                true
            } catch (e: Exception) {
                Log.e("DatabaseMigration", "Cross reference table missing: ${e.message}")
                false
            } finally {
                // Close cursor if needed
            }

            // If any tables are missing, log this critical issue
            if (!noteTableExists || !labelTableExists || !crossRefTableExists) {
                Log.e("DatabaseMigration", "Critical: Some tables are missing. This will likely require fallback to destructive migration.")
            } else {
                Log.d("DatabaseMigration", "Migration successful: all tables verified intact")
            }
        } catch (e: Exception) {
            // Log migration errors
            Log.e("DatabaseMigration", "Error during migration: ${e.message}")
            // Let Room handle the failure through fallbackToDestructiveMigration
            throw e
        }
    }
}