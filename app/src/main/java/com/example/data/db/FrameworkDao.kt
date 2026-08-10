package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Framework
import kotlinx.coroutines.flow.Flow

@Dao
interface FrameworkDao {
    @Query("SELECT * FROM frameworks ORDER BY isWater DESC, id ASC")
    fun getAllFrameworks(): Flow<List<Framework>>

    @Query("SELECT * FROM frameworks WHERE id = :id")
    suspend fun getFrameworkById(id: Long): Framework?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFramework(framework: Framework): Long

    @Update
    suspend fun updateFramework(framework: Framework)

    @Delete
    suspend fun deleteFramework(framework: Framework)

    @Query("DELETE FROM frameworks")
    suspend fun deleteAll()
}
