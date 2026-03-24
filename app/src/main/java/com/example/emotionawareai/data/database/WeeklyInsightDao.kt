package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.emotionawareai.data.model.WeeklyInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: WeeklyInsightEntity): Long

    @Update
    suspend fun update(insight: WeeklyInsightEntity)

    @Query("SELECT * FROM weekly_insights ORDER BY weekStartTimestamp DESC")
    fun observeAll(): Flow<List<WeeklyInsightEntity>>

    @Query(
        """
        SELECT * FROM weekly_insights
        ORDER BY weekStartTimestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getRecent(limit: Int): List<WeeklyInsightEntity>

    @Query(
        """
        SELECT * FROM weekly_insights
        WHERE weekStartTimestamp = :weekStart
        LIMIT 1
        """
    )
    suspend fun getByWeekStart(weekStart: Long): WeeklyInsightEntity?
}
