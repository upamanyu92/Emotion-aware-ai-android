package com.example.emotionawareai.manager

import android.util.Log
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.data.database.SessionGoalDao
import com.example.emotionawareai.data.database.WeeklyInsightDao
import com.example.emotionawareai.data.model.SessionGoalEntity
import com.example.emotionawareai.data.model.WeeklyInsightEntity
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.GrowthArea
import com.example.emotionawareai.domain.model.SessionGoal
import com.example.emotionawareai.domain.model.WeeklyInsight
import com.example.emotionawareai.domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates weekly insight reports from mood check-ins, conversation history, and goal data.
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
        private val WEEK_MS = TimeUnit.DAYS.toMillis(7)
    }

    fun observeInsights(): Flow<List<WeeklyInsight>> =
        weeklyInsightDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getLatestInsight(): WeeklyInsight? = withContext(Dispatchers.IO) {
        weeklyInsightDao.getLatest()?.toDomain()
    }

    suspend fun generateCurrentWeekInsight(activeConversationId: Long): WeeklyInsight =
        withContext(Dispatchers.IO) {
            val weekStart = currentWeekStart()
            val recentMessages = repository.getRecentMessages(activeConversationId, limit = 50)
            val activeGoals = sessionGoalDao.getActiveGoals()

            val avgMood = moodCheckInDao.averageMoodSince(weekStart) ?: 3.0f
            val checkInCount = moodCheckInDao.countSince(weekStart)
            val dominantEmotion = inferDominantEmotion(
                recentMessages.filter { it.isFromUser }
                    .map { it.emotion }
                    .filter { it != Emotion.UNKNOWN }
            )
            val themes = extractThemes(recentMessages.filter { it.isFromUser }.map { it.content })
            val goalTitles = activeGoals.map { it.title }
            val narrative = buildNarrative(avgMood, checkInCount, dominantEmotion, themes, goalTitles)
            val nextSteps = buildNextSteps(avgMood, themes, activeGoals)

            val entity = WeeklyInsightEntity(
                weekStartTimestamp = weekStart,
                dominantEmotion = dominantEmotion.name,
                moodAverage = avgMood,
                checkInCount = checkInCount,
                topThemesJson = encodeList(themes.take(5)),
                narrative = narrative,
                suggestedNextStepsJson = encodeList(nextSteps.take(3)),
                generatedAt = System.currentTimeMillis()
            )
            weeklyInsightDao.insert(entity)
            Log.i(TAG, "Generated weekly insight: mood=$avgMood, checkIns=$checkInCount, themes=${themes.size}")
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

    private fun extractThemes(messages: List<String>): List<String> {
        val themeKeywords = mapOf(
            "work stress" to listOf("work", "job", "boss", "deadline", "office", "colleague", "meeting", "project"),
            "relationships" to listOf("partner", "friend", "family", "relationship", "love", "argue", "fight", "lonely"),
            "anxiety" to listOf("anxious", "anxiety", "worry", "worried", "nervous", "panic", "scared"),
            "motivation" to listOf("motivation", "goal", "productivity", "procrastinate", "focus", "achieve"),
            "sleep" to listOf("sleep", "tired", "exhausted", "insomnia", "rest", "fatigue"),
            "self-esteem" to listOf("confident", "confidence", "self-worth", "doubt", "insecure"),
            "mindfulness" to listOf("mindful", "present", "breath", "meditation", "calm", "overwhelmed"),
            "grief" to listOf("grief", "loss", "miss", "heartbreak", "mourn")
        )
        val lowerText = messages.joinToString(" ").lowercase()
        return themeKeywords
            .mapValues { (_, kws) -> kws.count { kw -> lowerText.contains(kw) } }
            .filter { it.value > 0 }
            .entries.sortedByDescending { it.value }
            .map { it.key }
    }

    private fun buildNarrative(
        avgMood: Float,
        checkInCount: Int,
        emotion: Emotion,
        themes: List<String>,
        goals: List<String>
    ): String {
        val moodDesc = when {
            avgMood >= 4.0f -> "strong"
            avgMood >= 3.0f -> "steady"
            avgMood >= 2.0f -> "challenging"
            else -> "difficult"
        }
        val themePhrase = if (themes.isNotEmpty())
            "Your conversations touched on ${themes.take(2).joinToString(" and ")}. "
        else ""
        val checkInPhrase = if (checkInCount > 0)
            "You checked in $checkInCount time${if (checkInCount != 1) "s" else ""} this week. "
        else "You didn't log any check-ins this week — that's okay. "
        val goalPhrase = if (goals.isNotEmpty()) "You're working on ${goals.first()}. " else ""
        return "This was a $moodDesc week emotionally. $checkInPhrase$themePhrase${goalPhrase}Keep going — small steps create lasting change."
    }

    private fun buildNextSteps(
        avgMood: Float,
        themes: List<String>,
        goals: List<SessionGoalEntity>
    ): List<String> {
        val steps = mutableListOf<String>()
        if (avgMood < 3.0f) steps.add("Try a 5-minute breathing exercise each morning this week")
        if ("anxiety" in themes) steps.add("Write down 3 things you can control today")
        if ("sleep" in themes) steps.add("Set a consistent wind-down routine 30 minutes before bed")
        if ("relationships" in themes) steps.add("Reach out to one person who supports you")
        if (goals.isNotEmpty()) steps.add("Spend 10 minutes reflecting on your goal: ${goals.first().title}")
        if (steps.isEmpty()) steps.add("Continue your daily check-ins to track your progress")
        return steps
    }

    private fun encodeList(items: List<String>): String {
        if (items.isEmpty()) return "[]"
        return items.joinToString(",", "[", "]") { "\"${it.replace("\"", "'")}\"" }
    }

    private fun WeeklyInsightEntity.toDomain(): WeeklyInsight {
        fun decodeList(json: String): List<String> =
            json.removeSurrounding("[", "]").split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        return WeeklyInsight(
            id = id,
            weekStartTimestamp = weekStartTimestamp,
            dominantEmotion = Emotion.fromLabel(dominantEmotion),
            moodAverage = moodAverage,
            checkInCount = checkInCount,
            topThemes = decodeList(topThemesJson),
            narrative = narrative,
            suggestedNextSteps = decodeList(suggestedNextStepsJson),
            generatedAt = generatedAt
        )
    }
}
