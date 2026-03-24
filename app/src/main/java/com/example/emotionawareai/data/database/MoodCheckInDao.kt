package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.MoodCheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodCheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: MoodCheckInEntity): Long

    @Query("SELECT * FROM mood_checkins ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 30): List<MoodCheckInEntity>

    @Query("SELECT * FROM mood_checkins WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSince(since: Long): List<MoodCheckInEntity>

    @Query("SELECT * FROM mood_checkins ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MoodCheckInEntity>>

    @Query("SELECT COUNT(*) FROM mood_checkins WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT AVG(moodScore) FROM mood_checkins WHERE timestamp >= :since")
    suspend fun averageMoodSince(since: Long): Float?
}
