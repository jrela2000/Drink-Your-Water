package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CompletionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionLogDao {
    @Query("SELECT * FROM completion_logs ORDER BY completedAt DESC")
    fun getAllLogs(): Flow<List<CompletionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CompletionLog): Long

    @Query("DELETE FROM completion_logs")
    suspend fun deleteAll()
}
