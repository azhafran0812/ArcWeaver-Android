package com.storymaker.arcweaver.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.StoryRepository
import kotlinx.coroutines.launch
import com.storymaker.arcweaver.data.repository.ProjectRepository

class NodeViewModel(
    private val repository: StoryRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    // UI States
    var characterName by mutableStateOf("")
    var dialogueText by mutableStateOf("")
    var characterImageUri by mutableStateOf("")
    var nextNodeId by mutableStateOf("")
    var choicesList by mutableStateOf<List<ChoiceEntity>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadNodeForEdit(nodeId: Int) {
        viewModelScope.launch {
            isLoading = true
            val node = repository.getNodeById(nodeId)
            if (node != null) {
                characterName = node.characterName
                dialogueText = node.dialogueText
                characterImageUri = node.characterImageUri ?: ""
                nextNodeId = node.nextNodeId?.toString() ?: ""
                choicesList = repository.getChoicesByNodeId(nodeId)
            }
            isLoading = false
        }
    }

    // Menambah pilihan kosong dengan parameter lengkap (Condition & Effect)
    fun addEmptyChoice() {
        choicesList = choicesList + ChoiceEntity(
            parentNodeId = 0,
            choiceText = "",
            targetNodeId = null,
            requiredCondition = null,
            effect = null
        )
    }

    fun removeChoice(index: Int) {
        choicesList = choicesList.toMutableList().apply { removeAt(index) }
    }

    fun updateChoiceText(index: Int, newText: String) {
        val currentList = choicesList.toMutableList()
        currentList[index] = currentList[index].copy(choiceText = newText)
        choicesList = currentList
    }

    fun updateChoiceTarget(index: Int, targetIdStr: String) {
        val targetId = targetIdStr.toIntOrNull()
        val currentList = choicesList.toMutableList()
        currentList[index] = currentList[index].copy(targetNodeId = targetId)
        choicesList = currentList
    }

    fun updateChoiceCondition(index: Int, condition: String) {
        val currentList = choicesList.toMutableList()
        currentList[index] = currentList[index].copy(requiredCondition = condition.takeIf { it.isNotBlank() })
        choicesList = currentList
    }

    // BARU: Menyimpan Efek Variabel (Action) saat pilihan ini ditekan
    fun updateChoiceEffect(index: Int, effectAction: String) {
        val currentList = choicesList.toMutableList()
        currentList[index] = currentList[index].copy(effect = effectAction.takeIf { it.isNotBlank() })
        choicesList = currentList
    }

    fun saveStoryNode(projectId: Int, nodeId: Int?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val node = StoryNodeEntity(
                nodeId = nodeId ?: 0,
                projectId = projectId,
                characterName = characterName,
                dialogueText = dialogueText,
                characterImageUri = characterImageUri.takeIf { it.isNotBlank() },
                nextNodeId = nextNodeId.toIntOrNull()
            )


            if (nodeId == null || nodeId == 0) {
                repository.insertNodeWithChoices(node, choicesList)
            } else {
                repository.updateNodeWithChoices(node, choicesList)
            }
            projectRepository.syncProjectStats(projectId)

            onComplete()
        }
    }
}

class NodeViewModelFactory(
    private val repository: StoryRepository,
    private val projectRepository: ProjectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NodeViewModel(repository, projectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}