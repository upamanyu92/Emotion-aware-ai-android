package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.EvaluationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evaluation: EvaluationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evaluations: List<EvaluationEntity>)

    @Query("SELECT * FROM ai_evaluations WHERE messageId = :messageId ORDER BY metricName")
    suspend fun getForMessage(messageId: Long): List<EvaluationEntity>

    @Query("SELECT * FROM ai_evaluations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<EvaluationEntity>

    @Query("SELECT AVG(score) FROM ai_evaluations WHERE metricName = :metricName AND timestamp >= :since")
    suspend fun averageScoreSince(metricName: String, since: Long): Float?

    @Query("SELECT AVG(score) FROM ai_evaluations WHERE timestamp >= :since")
    suspend fun overallAverageSince(since: Long): Float?

    @Query("SELECT * FROM ai_evaluations ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EvaluationEntity>>

    @Query("SELECT metricName, AVG(score) as score FROM ai_evaluations WHERE timestamp >= :since GROUP BY metricName")
    suspend fun averagesByMetricSince(since: Long): List<MetricAverage>
}

/** Projection class for aggregated metric averages. */
data class MetricAverage(
    val metricName: String,
    val score: Float
)
