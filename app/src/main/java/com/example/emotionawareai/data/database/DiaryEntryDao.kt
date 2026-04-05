package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntryEntity): Long

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey ORDER BY timestamp ASC")
    suspend fun getEntriesForDate(dateKey: String): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey ORDER BY timestamp ASC")
    fun observeEntriesForDate(dateKey: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT DISTINCT dateKey FROM diary_entries ORDER BY dateKey DESC")
    suspend fun getAllDates(): List<String>

    @Query("SELECT DISTINCT dateKey FROM diary_entries ORDER BY dateKey DESC")
    fun observeAllDates(): Flow<List<String>>

    /** Get the latest daily summary for a given date (the most recent entry with non-empty summary). */
    @Query("SELECT dailySummary FROM diary_entries WHERE dateKey = :dateKey AND dailySummary != '' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getDailySummary(dateKey: String): String?

    /** Update the summary field for all entries on a given date. */
    @Query("UPDATE diary_entries SET dailySummary = :summary WHERE dateKey = :dateKey")
    suspend fun updateDailySummary(dateKey: String, summary: String)

    @Query("SELECT COUNT(*) FROM diary_entries WHERE dateKey = :dateKey")
    suspend fun countForDate(dateKey: String): Int

    @Query("DELETE FROM diary_entries WHERE dateKey = :dateKey")
    suspend fun deleteForDate(dateKey: String)
}
