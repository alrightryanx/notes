package com.xr.notes

import android.content.Context
import com.xr.notes.database.AppDatabase
import com.xr.notes.database.LabelDao
import com.xr.notes.database.NoteDao
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.ActiveLabelsStore
import com.xr.notes.utils.AppPreferenceManager
import com.xr.notes.utils.BackupManager
import com.xr.notes.utils.Encryption
import com.xr.notes.utils.NotesFilterManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Singleton
    @Provides
    fun provideLabelDao(database: AppDatabase): LabelDao {
        return database.labelDao()
    }

    @Singleton
    @Provides
    fun provideEncryption(): Encryption {
        return Encryption()
    }

    @Singleton
    @Provides
    fun provideNotesRepository(
        noteDao: NoteDao,
        labelDao: LabelDao,
        encryption: Encryption
    ): NotesRepository {
        return NotesRepository(noteDao, labelDao, encryption)
    }

    @Singleton
    @Provides
    fun providePreferenceManager(@ApplicationContext context: Context): AppPreferenceManager {
        return AppPreferenceManager(context)
    }

    @Singleton
    @Provides
    fun provideBackupManager(@ApplicationContext context: Context): BackupManager {
        return BackupManager(context)
    }

    @Singleton
    @Provides
    fun provideNotesFilterManager(): NotesFilterManager {
        return NotesFilterManager()
    }

    @Singleton
    @Provides
    fun provideActiveLabelsStore(): ActiveLabelsStore {
        return ActiveLabelsStore()
    }
}