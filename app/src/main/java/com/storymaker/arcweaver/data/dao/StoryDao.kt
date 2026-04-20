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

    @Insert
    suspend fun insertNodeReturnId(node: StoryNodeEntity): Long

    @Insert
    suspend fun insertChoicesList(choices: List<ChoiceEntity>)

    // --- READ --- >

    @Query("SELECT * FROM story_nodes WHERE projectId = :projectId ORDER BY nodeId ASC")
    fun getNodesByProjectId(projectId: Int): Flow<List<StoryNodeEntity>>

    // FUNGSI Reactive Flow
    @Query("SELECT * FROM story_nodes WHERE projectId = :projectId")
    fun getNodesByProjectIdFlow(projectId: Int): Flow<List<StoryNodeEntity>>

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


    @Query("SELECT * FROM story_nodes WHERE projectId = :projectId ORDER BY nodeId ASC LIMIT 1")
    suspend fun getFirstNodeOfProject(projectId: Int): StoryNodeEntity?

    // --- OPERASI CHOICES ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoices(choices: List<ChoiceEntity>)

    @Query("SELECT * FROM choices WHERE parentNodeId = :nodeId")
    suspend fun getChoicesByNodeId(nodeId: Int): List<ChoiceEntity>

    @Query("DELETE FROM choices WHERE parentNodeId = :nodeId")
    suspend fun deleteChoicesByNodeId(nodeId: Int)
    // --- TRANSAKSI GABUNGAN (NODE + CHOICES) ---

    @Transaction
    suspend fun insertNodeWithChoices(node: StoryNodeEntity, choices: List<ChoiceEntity>) {
        // 1. Simpan Node
        val generatedNodeId = insertNode(node).toInt()

        // 2. Pasang ID Node
        if (choices.isNotEmpty()) {
            val choicesWithParentId = choices.map { it.copy(parentNodeId = generatedNodeId) }
            insertChoices(choicesWithParentId)
        }
    }

    @Transaction
    suspend fun updateNodeWithChoices(node: StoryNodeEntity, choices: List<ChoiceEntity>) {
        // 1. Update data Node
        updateNode(node)

        // 2. Hapus semua Choice lama yang menempel pada Node
        deleteChoicesByNodeId(node.nodeId)

        // 3. Masukkan daftar Choice yang baru/diperbarui
        if (choices.isNotEmpty()) {
            val choicesWithParentId = choices.map { it.copy(parentNodeId = node.nodeId) }
            insertChoices(choicesWithParentId)
        }
    }
}