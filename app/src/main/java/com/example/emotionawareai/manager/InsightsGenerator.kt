package com.example.emotionawareai.manager

import android.util.Log
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.data.database.SessionGoalDao
import com.example.emotionawareai.data.database.WeeklyInsightDao
import com.example.emotionawareai.data.model.SessionGoalEntity
import com.example.emotionawareai.data.model.WeeklyInsightEntity
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.GrowthArea
import com.example.emotionawareai.domain.model.KeywordExtractor
import com.example.emotionawareai.domain.model.SessionGoal
import com.example.emotionawareai.domain.model.WeeklyInsight
import com.example.emotionawareai.domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates weekly insight reports from mood check-ins, conversation history, and goal data.
 *
 * Uses [KeywordExtractor] (shared with the RAG layer) for consistent theme extraction.
 */
@Singleton
class InsightsGenerator @Inject constructor(
    private val moodCheckInDao: MoodCheckInDao,
    private val sessionGoalDao: SessionGoalDao,
    private val weeklyInsightDao: WeeklyInsightDao,
    private val repository: ConversationRepository
) {
    companion object {
        private const val TAG = "InsightsGenerator"
    }

    fun observeInsights(): Flow<List<WeeklyInsight>> =
        weeklyInsightDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getLatestInsight(): WeeklyInsight? = withContext(Dispatchers.IO) {
        weeklyInsightDao.getRecent(1).firstOrNull()?.toDomain()
    }

    /**
     * Generates (or regenerates) the insight for the current week and persists it.
     * Safe to call multiple times — overwrites any existing entry for the same week.
     */
    suspend fun generateCurrentWeekInsight(activeConversationId: Long): WeeklyInsight =
        withContext(Dispatchers.IO) {
            val weekStart = currentWeekStart()
            val recentMessages = repository.getRecentMessages(activeConversationId, limit = 50)
            val activeGoals = sessionGoalDao.getActiveGoals()

            val dominantEmotion = inferDominantEmotion(
                recentMessages.filter { it.isFromUser }.map { it.emotion }
                    .filter { it != Emotion.UNKNOWN }
            )
            val emotionFrequencies = recentMessages.filter { it.isFromUser }
                .map { it.emotion }
                .filter { it != Emotion.UNKNOWN }
                .groupingBy { it }
                .eachCount()

            val goalTitles = activeGoals.map { it.title }
            val summary = buildSummary(dominantEmotion, emotionFrequencies, goalTitles)

            val entity = WeeklyInsightEntity(
                weekStartTimestamp = weekStart,
                dominantEmotion = dominantEmotion.name,
                emotionFrequencies = encodeFrequencies(emotionFrequencies),
                summary = summary,
                trackedGoals = encodeList(goalTitles.take(5)),
                createdAt = System.currentTimeMillis()
            )
            weeklyInsightDao.insert(entity)
            Log.i(TAG, "Generated weekly insight: dominantEmotion=$dominantEmotion, goals=${goalTitles.size}")
            entity.toDomain()
        }

    private fun currentWeekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun inferDominantEmotion(emotions: List<Emotion>): Emotion {
        if (emotions.isEmpty()) return Emotion.NEUTRAL
        return emotions.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: Emotion.NEUTRAL
    }

    private fun buildSummary(
        dominant: Emotion,
        frequencies: Map<Emotion, Int>,
        goals: List<String>
    ): String {
        val total = frequencies.values.sum().coerceAtLeast(1)
        val dominantPct = ((frequencies[dominant] ?: 0) * 100 / total)
        val goalPhrase = if (goals.isNotEmpty()) " You're actively working on: ${goals.first()}." else ""
        return "This week your dominant mood was ${dominant.displayName} (${dominantPct}% of signals).$goalPhrase Keep reflecting — small steps lead to lasting change."
    }

    private fun encodeFrequencies(frequencies: Map<Emotion, Int>): String {
        if (frequencies.isEmpty()) return "{}"
        return frequencies.entries.joinToString(",", "{", "}") { (e, c) ->
            "\"${e.name}\":$c"
        }
    }

    private fun encodeList(items: List<String>): String {
        if (items.isEmpty()) return "[]"
        return items.joinToString(",", "[", "]") { "\"${it.replace("\"", "'")}\"" }
    }

    private fun WeeklyInsightEntity.toDomain(): WeeklyInsight {
        fun decodeFrequencies(json: String): Map<Emotion, Int> {
            if (json == "{}") return emptyMap()
            return json.removeSurrounding("{", "}").split(",")
                .mapNotNull { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        val emotion = Emotion.fromLabel(parts[0].trim().removeSurrounding("\""))
                        val count = parts[1].trim().toIntOrNull() ?: 0
                        emotion to count
                    } else null
                }.toMap()
        }
        fun decodeList(json: String): List<String> =
            json.removeSurrounding("[", "]").split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        return WeeklyInsight(
            id = id,
            weekStartTimestamp = weekStartTimestamp,
            dominantEmotion = Emotion.fromLabel(dominantEmotion),
            emotionFrequencies = decodeFrequencies(emotionFrequencies),
            summary = summary,
            trackedGoals = decodeList(trackedGoals),
            createdAt = createdAt
        )
    }
}
