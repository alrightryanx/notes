package com.xr.notes.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.xr.notes.models.Label
import com.xr.notes.models.LabelWithNotes

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun getAllLabels(): LiveData<List<Label>>

    @Query("SELECT * FROM labels WHERE id = :labelId")
    fun getLabelById(labelId: Long): LiveData<Label>

    @Transaction
    @Query("SELECT * FROM labels WHERE id = :labelId")
    fun getLabelWithNotes(labelId: Long): LiveData<LabelWithNotes>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(label: Label): Long

    @Update
    suspend fun update(label: Label)

    @Delete
    suspend fun delete(label: Label)

    @Query("DELETE FROM labels")
    suspend fun deleteAllLabels()
}