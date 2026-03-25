package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.MemoryFragmentEntity

@Dao
interface MemoryFragmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fragment: MemoryFragmentEntity): Long

    @Query("SELECT * FROM memory_fragments ORDER BY createdAt DESC")
    suspend fun getAll(): List<MemoryFragmentEntity>

    @Query(
        """
        SELECT * FROM memory_fragments
        WHERE conversationId = :conversationId
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByConversation(conversationId: Long): List<MemoryFragmentEntity>

    @Query(
        """
        SELECT * FROM memory_fragments
        WHERE fragmentType = :type
        ORDER BY createdAt DESC
        """
    )
    suspend fun getByType(type: String): List<MemoryFragmentEntity>

    /**
     * Finds fragments whose keyword list contains [keyword] as a sub-string.
     * Used for multi-keyword RAG retrieval assembled in Kotlin.
     */
    @Query(
        """
        SELECT * FROM memory_fragments
        WHERE keywords LIKE '%' || :keyword || '%'
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchByKeyword(keyword: String, limit: Int): List<MemoryFragmentEntity>

    @Query("DELETE FROM memory_fragments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory_fragments")
    suspend fun deleteAll()
}
