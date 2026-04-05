package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: FeedbackEntity): Long

    @Query("SELECT * FROM user_feedback WHERE messageId = :messageId LIMIT 1")
    suspend fun getForMessage(messageId: Long): FeedbackEntity?

    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<FeedbackEntity>

    @Query("SELECT AVG(rating) FROM user_feedback WHERE timestamp >= :since")
    suspend fun averageRatingSince(since: Long): Float?

    @Query("SELECT COUNT(*) FROM user_feedback")
    suspend fun totalCount(): Int

    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<FeedbackEntity>>
}
