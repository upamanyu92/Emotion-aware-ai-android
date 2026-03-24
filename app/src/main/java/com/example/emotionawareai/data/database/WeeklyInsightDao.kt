package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.WeeklyInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyInsightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: WeeklyInsightEntity): Long

    @Query("SELECT * FROM weekly_insights ORDER BY weekStartTimestamp DESC")
    fun observeAll(): Flow<List<WeeklyInsightEntity>>

    @Query("SELECT * FROM weekly_insights ORDER BY weekStartTimestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 12): List<WeeklyInsightEntity>

    @Query("SELECT * FROM weekly_insights WHERE weekStartTimestamp = :weekStart LIMIT 1")
    suspend fun getByWeekStart(weekStart: Long): WeeklyInsightEntity?

    @Query("SELECT * FROM weekly_insights ORDER BY weekStartTimestamp DESC LIMIT 1")
    suspend fun getLatest(): WeeklyInsightEntity?
}
