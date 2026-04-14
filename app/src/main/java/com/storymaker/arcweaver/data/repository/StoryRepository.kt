package com.storymaker.arcweaver.data.repository

import com.storymaker.arcweaver.data.dao.StoryDao
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import kotlinx.coroutines.flow.Flow

class StoryRepository(private val storyDao: StoryDao) {


    fun getNodesByProject(projectId: Int): Flow<List<StoryNodeEntity>> {
        return storyDao.getNodesByProjectId(projectId)
    }


    suspend fun insertNodeWithChoices(node: StoryNodeEntity, choices: List<ChoiceEntity>) {
        // 1. Simpan Node dan ambil ID yang di-generate otomatis
        // Note: 'node' yang dilempar dari ViewModel sudah harus berisi projectId yang valid
        val generatedNodeId = storyDao.insertNode(node).toInt()

        // 2. Pasang ID tersebut ke setiap Choice lalu simpan
        choices.forEach { choice ->
            val choiceWithParent = choice.copy(parentNodeId = generatedNodeId)
            storyDao.insertChoice(choiceWithParent)
        }
    }

    suspend fun deleteNode(node: StoryNodeEntity) {
        storyDao.deleteNode(node)
    }

    // --- FITUR EDIT ---
    suspend fun getNodeById(id: Int): StoryNodeEntity? = storyDao.getNodeById(id)

    suspend fun getChoicesByNodeId(id: Int): List<ChoiceEntity> = storyDao.getChoicesByNodeId(id)

    // Fungsi khusus untuk Update (Hapus pilihan lama, masukkan pilihan baru)
    suspend fun updateNodeWithChoices(node: StoryNodeEntity, choices: List<ChoiceEntity>) {
        storyDao.updateNode(node)
        storyDao.deleteChoicesByNodeId(node.nodeId) // Bersihkan cabang lama
        choices.forEach { choice ->
            storyDao.insertChoice(choice.copy(parentNodeId = node.nodeId)) // Masukkan cabang baru
        }
    }

    suspend fun updateNode(node: StoryNodeEntity) {
        storyDao.updateNode(node)
    }


    suspend fun getFirstNode(projectId: Int): StoryNodeEntity? {
        return storyDao.getFirstNodeOfProject(projectId)
    }

    fun getNodesByProjectIdFlow(projectId: Int): Flow<List<StoryNodeEntity>> {
        return storyDao.getNodesByProjectIdFlow(projectId)
    }


}