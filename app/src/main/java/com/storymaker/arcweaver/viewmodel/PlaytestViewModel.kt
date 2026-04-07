package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaytestViewModel(private val repository: StoryRepository) : ViewModel() {

    // Menyimpan state adegan saat ini
    private val _currentNode = MutableStateFlow<StoryNodeEntity?>(null)
    val currentNode: StateFlow<StoryNodeEntity?> = _currentNode.asStateFlow()

    // Menyimpan daftar pilihan untuk adegan tersebut
    private val _currentChoices = MutableStateFlow<List<ChoiceEntity>>(emptyList())
    val currentChoices: StateFlow<List<ChoiceEntity>> = _currentChoices.asStateFlow()

    // Menyimpan status apakah game sudah tamat
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    // Fungsi memulai game
    fun startGame() {
        viewModelScope.launch {
            _isGameOver.value = false
            val initialNode = repository.getFirstNode()

            if (initialNode != null) {
                _currentNode.value = initialNode
                _currentChoices.value = repository.getChoicesByNodeId(initialNode.nodeId)
            } else {
                _isGameOver.value = true // Tidak ada cerita di database
            }
        }
    }

    // Fungsi memproses pilihan pemain
    fun makeChoice(choice: ChoiceEntity) {
        viewModelScope.launch {
            val targetId = choice.targetNodeId
            if (targetId != null) {
                val nextNode = repository.getNodeById(targetId)
                if (nextNode != null) {
                    _currentNode.value = nextNode
                    _currentChoices.value = repository.getChoicesByNodeId(nextNode.nodeId)
                } else {
                    _isGameOver.value = true // Target node hilang
                }
            } else {
                _isGameOver.value = true // Pilihan tidak disambungkan (End)
            }
        }
    }

    // Fungsi untuk mengakhiri game
    fun endGame() {
        _isGameOver.value = true
    }
}

class PlaytestViewModelFactory(private val repository: StoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaytestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaytestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}