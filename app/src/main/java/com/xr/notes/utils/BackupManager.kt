package com.xr.notes.utils

// File: app/src/main/java/com/example/notesapp/utils/BackupManager.kt

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xr.notes.models.Label
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    // Backup data classes
    data class BackupData(
        val notes: List<Note>,
        val labels: List<Label>,
        val noteLabelCrossRefs: List<NoteLabelCrossRef>,
        val backupDate: Date = Date()
    )

    suspend fun createBackup(
        notes: List<Note>,
        labels: List<Label>,
        noteLabelCrossRefs: List<NoteLabelCrossRef>,
        outputUri: Uri? = null
    ): Uri? = withContext(Dispatchers.IO) {

        val backupData = BackupData(notes, labels, noteLabelCrossRefs)
        val backupJson = gson.toJson(backupData)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFileName = "notes_backup_$timestamp.zip"

        if (outputUri != null) {
            // User selected a specific location to save the backup
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // Add the backup.json file to zip
                    zipOut.putNextEntry(ZipEntry("backup.json"))
                    zipOut.write(backupJson.toByteArray())
                    zipOut.closeEntry()
                }
            }
            return@withContext outputUri
        } else {
            // Create backup in app's files directory
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupDir, backupFileName)

            FileOutputStream(backupFile).use { fos ->
                ZipOutputStream(fos).use { zipOut ->
                    // Add the backup.json file to zip
                    zipOut.putNextEntry(ZipEntry("backup.json"))
                    zipOut.write(backupJson.toByteArray())
                    zipOut.closeEntry()
                }
            }

            // Return file Uri
            return@withContext Uri.fromFile(backupFile)
        }
    }

    suspend fun restoreBackup(backupUri: Uri): BackupData = withContext(Dispatchers.IO) {
        // Open backup file
        context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var backupData: BackupData? = null
                var zipEntry = zipIn.nextEntry

                while (zipEntry != null) {
                    if (zipEntry.name == "backup.json") {
                        // Read the backup.json content
                        val content = zipIn.bufferedReader().use { it.readText() }
                        backupData = gson.fromJson(content, BackupData::class.java)
                        break
                    }
                    zipEntry = zipIn.nextEntry
                }

                zipIn.closeEntry()

                return@withContext backupData ?: throw IllegalStateException("Invalid backup file format")
            }
        } ?: throw IllegalStateException("Cannot open backup file")
    }

    suspend fun scheduleBackup() {
        // This would be called by WorkManager for scheduled backups
        // Implementation to get data from repository and create backup
    }

    suspend fun listBackups(): List<Pair<String, Uri>> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return@withContext emptyList()

        return@withContext backupDir.listFiles()
            ?.filter { it.name.endsWith(".zip") }
            ?.map {
                val name = it.name
                val uri = Uri.fromFile(it)
                name to uri
            }
            ?.sortedByDescending { it.first } // Most recent backups first
            ?: emptyList()
    }

    suspend fun deleteBackup(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            if (backupUri.scheme == "file") {
                // Local file
                val path = backupUri.path ?: return@withContext false
                val file = File(path)
                return@withContext file.delete()
            } else {
                // Content URI (e.g., from SAF)
                val docFile = DocumentFile.fromSingleUri(context, backupUri)
                return@withContext docFile?.delete() ?: false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}