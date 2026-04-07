package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.StoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 1. Data class untuk menampung input pilihan sementara dari UI (Editor)
data class ChoiceDraft(
    val text: String = "",
    val targetNodeId: Int? = null,
    val condition: String = "" // Untuk fitur Advanced Mode (Logika Variabel)
)

class NodeViewModel(private val repository: StoryRepository) : ViewModel() {

    // Membaca aliran data node dari database secara reaktif ke UI
    val allNodes: StateFlow<List<StoryNodeEntity>> = repository.allNodes
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. Fungsi menyimpan ATAU mengedit Node
    fun saveStoryNode(
        nodeId: Int? = null, // Jika null = buat baru, jika ada angka = mode edit
        characterName: String,
        dialogue: String,
        imageUri: String?,
        choices: List<ChoiceDraft>
    ) {
        viewModelScope.launch {
            // Bungkus data utama menjadi StoryNodeEntity (Gunakan nama nodeToSave)
            val nodeToSave = StoryNodeEntity(
                nodeId = nodeId ?: 0,
                characterName = characterName,
                dialogueText = dialogue,
                characterImageUri = imageUri
            )

            // Petakan List<ChoiceDraft> menjadi List<ChoiceEntity>
            val choiceEntities = choices.map { draft ->
                ChoiceEntity(
                    parentNodeId = nodeId ?: 0,
                    choiceText = draft.text,
                    targetNodeId = draft.targetNodeId,
                    requiredCondition = draft.condition.ifBlank { null }
                )
            }

            // Cek apakah ini Node Baru atau Edit Node Lama
            if (nodeId == null) {
                // Mode Buat Baru
                repository.insertNodeWithChoices(nodeToSave, choiceEntities)
            } else {
                // Mode Edit
                repository.updateNodeWithChoices(nodeToSave, choiceEntities)
            }
        }
    }

    // 3. Fungsi menghapus Story Node
    fun deleteNode(node: StoryNodeEntity) {
        viewModelScope.launch {
            repository.deleteNode(node)
        }
    }

    // 4. Fungsi mengambil data saat layar edit dibuka
    suspend fun loadNodeForEdit(nodeId: Int): Pair<StoryNodeEntity, List<ChoiceDraft>>? {
        val node = repository.getNodeById(nodeId) ?: return null
        val choices = repository.getChoicesByNodeId(nodeId).map {
            ChoiceDraft(
                text = it.choiceText,
                targetNodeId = it.targetNodeId,
                condition = it.requiredCondition ?: ""
            )
        }
        return Pair(node, choices)
    }
}

// 5. Factory
class NodeViewModelFactory(private val repository: StoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NodeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}