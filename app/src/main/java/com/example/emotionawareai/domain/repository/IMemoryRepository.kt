package com.example.emotionawareai.domain.repository

import com.example.emotionawareai.domain.model.MemoryFragment
import com.example.emotionawareai.domain.model.WeeklyInsight
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the long-term memory layer that powers RAG context injection.
 *
 * Implementations persist [MemoryFragment]s (goals, patterns, facts, events)
 * and surface the most semantically relevant ones for a given user query.
 */
interface IMemoryRepository {

    /** Persists [fragment] and returns its generated row ID. */
    suspend fun storeFragment(fragment: MemoryFragment): Long

    /**
     * Retrieves up to [limit] fragments most relevant to [query].
     *
     * The current implementation uses keyword overlap scoring; this can be
     * replaced by cosine similarity against stored embeddings in a future phase.
     */
    suspend fun retrieveRelevant(query: String, limit: Int = 5): List<MemoryFragment>

    /** Returns all fragments whose type is [MemoryFragment.type] == GOAL. */
    suspend fun getGoals(): List<MemoryFragment>

    /** Returns all stored fragments. */
    suspend fun getAll(): List<MemoryFragment>

    /** Returns the [limit] most recent weekly insight summaries. */
    suspend fun getWeeklyInsights(limit: Int = 4): List<WeeklyInsight>

    /** Persists a [WeeklyInsight] and returns its generated row ID. */
    suspend fun storeWeeklyInsight(insight: WeeklyInsight): Long

    /** Observes all weekly insights ordered by descending week start. */
    fun observeWeeklyInsights(): Flow<List<WeeklyInsight>>
}
