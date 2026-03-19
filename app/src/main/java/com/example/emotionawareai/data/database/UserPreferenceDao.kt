package com.example.emotionawareai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emotionawareai.data.model.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: UserPreferenceEntity)

    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    suspend fun getByKey(key: String): UserPreferenceEntity?

    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    fun observeByKey(key: String): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences")
    suspend fun getAll(): List<UserPreferenceEntity>

    @Query("DELETE FROM user_preferences WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM user_preferences")
    suspend fun clearAll()
}
