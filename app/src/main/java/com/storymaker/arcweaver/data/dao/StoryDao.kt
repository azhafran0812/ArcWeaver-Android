package com.storymaker.arcweaver.data.dao

import androidx.room.*
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    // --- CREATE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: StoryNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoice(choice: ChoiceEntity): Long

    // --- READ ---
    @Query("SELECT * FROM story_nodes")
    fun getAllNodes(): Flow<List<StoryNodeEntity>>

    // --- UPDATE ---
    @Update
    suspend fun updateNode(node: StoryNodeEntity): Int

    @Update
    suspend fun updateChoice(choice: ChoiceEntity): Int

    // --- DELETE ---
    @Delete
    suspend fun deleteNode(node: StoryNodeEntity)

    // --- FITUR EDIT ---
    @Query("SELECT * FROM story_nodes WHERE nodeId = :id LIMIT 1")
    suspend fun getNodeById(id: Int): StoryNodeEntity?

    @Query("SELECT * FROM choices WHERE parentNodeId = :nodeId")
    suspend fun getChoicesByNodeId(nodeId: Int): List<ChoiceEntity>

    @Query("DELETE FROM choices WHERE parentNodeId = :nodeId")
    suspend fun deleteChoicesByNodeId(nodeId: Int)

    @Query("SELECT * FROM story_nodes ORDER BY nodeId ASC LIMIT 1")
    suspend fun getFirstNode(): StoryNodeEntity?
}