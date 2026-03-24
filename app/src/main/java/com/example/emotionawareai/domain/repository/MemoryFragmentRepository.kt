package com.example.emotionawareai.domain.repository

import android.util.Log
import com.example.emotionawareai.data.database.MemoryFragmentDao
import com.example.emotionawareai.data.database.WeeklyInsightDao
import com.example.emotionawareai.data.model.MemoryFragmentEntity
import com.example.emotionawareai.data.model.WeeklyInsightEntity
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MemoryFragment
import com.example.emotionawareai.domain.model.MemoryFragmentType
import com.example.emotionawareai.domain.model.WeeklyInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [IMemoryRepository].
 *
 * Retrieval uses a multi-keyword overlap scoring strategy:
 * 1. Tokenise the query into lower-case, stop-word-filtered terms.
 * 2. Query the DB with a LIKE search per keyword.
 * 3. Score each fragment by how many unique query keywords it matches.
 * 4. Return the top-[limit] fragments by score (descending).
 *
 * This provides functional RAG without an on-device embedding model.
 * When an embedding model becomes available the [storeFragment] path can
 * populate a float-array embedding field and retrieval can switch to cosine
 * similarity.
 */
@Singleton
class MemoryFragmentRepository @Inject constructor(
    private val fragmentDao: MemoryFragmentDao,
    private val insightDao: WeeklyInsightDao
) : IMemoryRepository {

    override suspend fun storeFragment(fragment: MemoryFragment): Long =
        withContext(Dispatchers.IO) {
            val entity = fragment.toEntity()
            val id = fragmentDao.insert(entity)
            Log.d(TAG, "Stored memory fragment: id=$id type=${fragment.type} keywords=${fragment.keywords}")
            id
        }

    override suspend fun retrieveRelevant(query: String, limit: Int): List<MemoryFragment> =
        withContext(Dispatchers.IO) {
            val keywords = extractKeywords(query)
            if (keywords.isEmpty()) return@withContext emptyList()

            // Collect candidate fragments for each keyword, then score by overlap.
            val candidateMap = mutableMapOf<Long, Pair<MemoryFragmentEntity, Int>>()
            for (keyword in keywords) {
                val results = fragmentDao.searchByKeyword(keyword, limit * 2)
                for (entity in results) {
                    val existing = candidateMap[entity.id]
                    candidateMap[entity.id] = if (existing == null) {
                        entity to 1
                    } else {
                        existing.first to (existing.second + 1)
                    }
                }
            }

            candidateMap.values
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first.toDomain() }
                .also { Log.d(TAG, "RAG retrieved ${it.size} fragments for query length=${query.length}") }
        }

    override suspend fun getGoals(): List<MemoryFragment> =
        withContext(Dispatchers.IO) {
            fragmentDao.getByType(MemoryFragmentType.GOAL.name).map { it.toDomain() }
        }

    override suspend fun getAll(): List<MemoryFragment> =
        withContext(Dispatchers.IO) {
            fragmentDao.getAll().map { it.toDomain() }
        }

    override suspend fun getWeeklyInsights(limit: Int): List<WeeklyInsight> =
        withContext(Dispatchers.IO) {
            insightDao.getRecent(limit).map { it.toDomain() }
        }

    override suspend fun storeWeeklyInsight(insight: WeeklyInsight): Long =
        withContext(Dispatchers.IO) {
            val entity = insight.toEntity()
            val id = insightDao.insert(entity)
            Log.d(TAG, "Stored weekly insight: id=$id weekStart=${insight.weekStartTimestamp}")
            id
        }

    override fun observeWeeklyInsights(): Flow<List<WeeklyInsight>> =
        insightDao.observeAll().map { list -> list.map { it.toDomain() } }

    // ── Keyword extraction ────────────────────────────────────────────────────

    /**
     * Tokenises [text] into meaningful lower-case terms by stripping common
     * English stop words and short tokens.
     */
    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf(
            "i", "a", "an", "the", "is", "it", "in", "on", "at", "to", "for",
            "of", "and", "or", "but", "not", "my", "me", "we", "you", "he",
            "she", "they", "this", "that", "was", "are", "be", "been", "being",
            "have", "has", "do", "did", "will", "would", "could", "should",
            "with", "from", "by", "as", "so", "if", "up", "out", "about",
            "just", "like", "what", "how", "when", "where", "who", "which",
            "can", "your", "its", "our", "their", "there", "then", "than"
        )
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun MemoryFragment.toEntity(): MemoryFragmentEntity = MemoryFragmentEntity(
        id = id,
        conversationId = conversationId,
        content = content,
        keywords = keywords.joinToString(","),
        emotionTag = emotion.name,
        fragmentType = type.name,
        createdAt = createdAt
    )

    private fun MemoryFragmentEntity.toDomain(): MemoryFragment = MemoryFragment(
        id = id,
        conversationId = conversationId,
        content = content,
        keywords = keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        emotion = Emotion.fromLabel(emotionTag),
        type = runCatching { MemoryFragmentType.valueOf(fragmentType) }
            .getOrDefault(MemoryFragmentType.FACT),
        createdAt = createdAt
    )

    private fun WeeklyInsight.toEntity(): WeeklyInsightEntity = WeeklyInsightEntity(
        id = id,
        weekStartTimestamp = weekStartTimestamp,
        dominantEmotion = dominantEmotion.name,
        emotionFrequencies = emotionFrequencies.entries
            .joinToString(",", "{", "}") { (e, c) -> "\"${e.name}\":$c" },
        summary = summary,
        trackedGoals = trackedGoals.joinToString(",", "[", "]") { "\"$it\"" },
        createdAt = createdAt
    )

    private fun WeeklyInsightEntity.toDomain(): WeeklyInsight {
        val freqMap = parseEmotionFrequencies(emotionFrequencies)
        val goals = parseGoalsList(trackedGoals)
        return WeeklyInsight(
            id = id,
            weekStartTimestamp = weekStartTimestamp,
            dominantEmotion = Emotion.fromLabel(dominantEmotion),
            emotionFrequencies = freqMap,
            summary = summary,
            trackedGoals = goals,
            createdAt = createdAt
        )
    }

    /** Parses a simple JSON object like {"HAPPY":5,"SAD":2} without a JSON library. */
    private fun parseEmotionFrequencies(json: String): Map<Emotion, Int> {
        val result = mutableMapOf<Emotion, Int>()
        val cleaned = json.trim().removeSurrounding("{", "}")
        if (cleaned.isBlank()) return result
        cleaned.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().toIntOrNull() ?: 0
                result[Emotion.fromLabel(key)] = value
            }
        }
        return result
    }

    /** Parses a simple JSON array like ["goal1","goal2"] without a JSON library. */
    private fun parseGoalsList(json: String): List<String> {
        val cleaned = json.trim().removeSurrounding("[", "]")
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "MemoryFragmentRepository"
    }
}
