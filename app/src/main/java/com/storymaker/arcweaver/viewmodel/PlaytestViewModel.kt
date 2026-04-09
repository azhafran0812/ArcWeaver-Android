package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.domain.engine.PlaytestEngine
import com.storymaker.arcweaver.domain.repository.PlaytestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaytestViewModel(
    private val repository: PlaytestRepository,
    private val engine: PlaytestEngine = PlaytestEngine()
) : ViewModel() {

    // 🔹 State Node sekarang
    private val _currentNode = MutableStateFlow<StoryNodeEntity?>(null)
    val currentNode: StateFlow<StoryNodeEntity?> = _currentNode.asStateFlow()

    // 🔹 State pilihan
    private val _currentChoices = MutableStateFlow<List<ChoiceEntity>>(emptyList())
    val currentChoices: StateFlow<List<ChoiceEntity>> = _currentChoices.asStateFlow()

    // 🔹 State game over
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    // 🔹 State variables JSON
    private var currentVariablesJson: String? = "{}"


    fun startGame() {
        viewModelScope.launch {
            _isGameOver.value = false
            currentVariablesJson = "{}"

            val startNode = repository.getStartNode()

            if (startNode != null) {
                _currentNode.value = startNode
                _currentChoices.value = repository.getChoices(startNode.nodeId)
            } else {
                _isGameOver.value = true
            }
        }
    }


    fun makeChoice(choice: ChoiceEntity) {
        viewModelScope.launch {

            val result = engine.processChoice(
                currentVariablesJson = currentVariablesJson,
                requiredCondition = choice.condition,
                targetNodeId = choice.targetNodeId ?: -1
            )

            if (!result.isSuccess) {
                //gagal
                return@launch
            }

            val nextId = result.nextNodeId

            if (nextId != null && nextId != -1) {
                val nextNode = repository.getNodeById(nextId)

                if (nextNode != null) {
                    _currentNode.value = nextNode
                    _currentChoices.value = repository.getChoices(nextId)
                    currentVariablesJson = result.updatedVariablesJson
                } else {
                    _isGameOver.value = true
                }
            } else {
                _isGameOver.value = true
            }
        }
    }

    fun endGame() {
        _isGameOver.value = true
    }
}

class PlaytestViewModelFactory(
    private val repository: PlaytestRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaytestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaytestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}