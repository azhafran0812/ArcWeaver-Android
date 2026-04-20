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

class VisualMapViewModel(
    private val projectId: Int,
    private val storyRepository: StoryRepository
) : ViewModel() {

    var nodes by mutableStateOf<List<StoryNodeEntity>>(emptyList())
    var choices by mutableStateOf<List<ChoiceEntity>>(emptyList())
    var isLoading by mutableStateOf(true)

    init {
        loadVisualMap()
    }

    private fun loadVisualMap() {
        viewModelScope.launch {
            isLoading = true

            val firstNode = storyRepository.getFirstNode(projectId)

            storyRepository.getNodesByProjectIdFlow(projectId).collect { nodeList ->
                nodes = nodeList

                val allChoices = mutableListOf<ChoiceEntity>()
                for (node in nodeList) {
                    val nodeChoices = storyRepository.getChoicesByNodeId(node.nodeId)
                    allChoices.addAll(nodeChoices)
                }
                choices = allChoices
                isLoading = false
            }
        }
    }

    fun updateNodePosition(node: StoryNodeEntity, newX: Float, newY: Float) {
        viewModelScope.launch {
            val updatedNode = node.copy(canvasX = newX, canvasY = newY)

            nodes = nodes.map { if (it.nodeId == node.nodeId) updatedNode else it }

            storyRepository.updateNode(updatedNode)
        }
    }
}

class VisualMapViewModelFactory(
    private val projectId: Int,
    private val storyRepository: StoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisualMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VisualMapViewModel(projectId, storyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}