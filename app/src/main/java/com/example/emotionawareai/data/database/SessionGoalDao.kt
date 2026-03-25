package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.emotionawareai.data.model.SessionGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SessionGoalEntity): Long

    @Update
    suspend fun update(goal: SessionGoalEntity)

    @Query("SELECT * FROM session_goals WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveGoals(): Flow<List<SessionGoalEntity>>

    @Query("SELECT * FROM session_goals WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveGoals(): List<SessionGoalEntity>

    @Query("SELECT * FROM session_goals ORDER BY createdAt DESC")
    suspend fun getAll(): List<SessionGoalEntity>

    @Query("UPDATE session_goals SET isActive = 0 WHERE id = :id")
    suspend fun archiveGoal(id: Long)

    @Query("UPDATE session_goals SET lastMentionedAt = :timestamp WHERE id = :id")
    suspend fun touchGoal(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE session_goals SET progressNote = :note WHERE id = :id")
    suspend fun updateProgress(id: Long, note: String)

    @Query("DELETE FROM session_goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM session_goals WHERE isActive = 1")
    suspend fun countActiveGoals(): Int
}
