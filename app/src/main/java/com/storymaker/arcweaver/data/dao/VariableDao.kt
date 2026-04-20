package com.storymaker.arcweaver.data.dao

import androidx.room.*
import com.storymaker.arcweaver.data.entity.VariableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariableDao {
    // Mengambil semua variabel, diurutkan sesuai abjad
    @Query("SELECT * FROM variables WHERE projectId = :projectId ORDER BY name ASC")
    fun getVariablesByProject(projectId: Int): Flow<List<VariableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariable(variable: VariableEntity): Long

    @Update
    suspend fun updateVariable(variable: VariableEntity)

    @Delete
    suspend fun deleteVariable(variable: VariableEntity)
}