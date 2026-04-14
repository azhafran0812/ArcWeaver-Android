package com.storymaker.arcweaver.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.data.repository.VariableRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PlaytestViewModel(
    private val projectId: Int,
    private val storyRepository: StoryRepository,
    private val variableRepository: VariableRepository
) : ViewModel() {

    // Status Cerita
    var currentNode by mutableStateOf<StoryNodeEntity?>(null)
    var currentChoices by mutableStateOf<List<ChoiceEntity>>(emptyList())
    var isLoading by mutableStateOf(true)
    var isStoryEnded by mutableStateOf(false)

    // Player State (Simulator)
    var variablesState = mutableStateMapOf<String, String>()
    var nodesVisited by mutableIntStateOf(0)
    var storyPath = mutableStateListOf<String>()

    init {
        startGame()
    }

    fun startGame() {
        viewModelScope.launch {
            isLoading = true
            isStoryEnded = false
            nodesVisited = 0
            storyPath.clear()
            variablesState.clear()

            // 1. Muat semua variabel default (Initial Value) ke dalam State Simulator
            val projectVars = variableRepository.getVariablesByProject(projectId).firstOrNull() ?: emptyList()
            projectVars.forEach {
                variablesState[it.name] = it.initialValue
            }

            // 2. Ambil Node Pertama
            val firstNode = storyRepository.getFirstNode(projectId)
            if (firstNode != null) {
                moveToNode(firstNode)
            } else {
                isStoryEnded = true
            }
            isLoading = false
        }
    }

    // Fungsi Transisi Antar Adegan
    private suspend fun moveToNode(node: StoryNodeEntity) {
        currentNode = node
        nodesVisited++
        storyPath.add(node.characterName.ifBlank { "Node #${node.nodeId}" })
        currentChoices = storyRepository.getChoicesByNodeId(node.nodeId)
    }

    // Eksekusi saat pemain memilih opsi
    fun makeChoice(choice: ChoiceEntity) {
        viewModelScope.launch {
            isLoading = true

            // 1. Terapkan Efek (Action) jika ada
            applyEffect(choice.effect)

            // 2. Pindah ke target node
            val targetId = choice.targetNodeId
            if (targetId != null && targetId != 0) {
                val nextNode = storyRepository.getNodeById(targetId)
                if (nextNode != null) moveToNode(nextNode) else isStoryEnded = true
            } else {
                isStoryEnded = true
            }
            isLoading = false
        }
    }

    // Lanjut jika cerita Linear (Tidak ada pilihan, menggunakan Next Node ID)
    fun continueLinear() {
        viewModelScope.launch {
            isLoading = true
            val targetId = currentNode?.nextNodeId
            if (targetId != null && targetId != 0) {
                val nextNode = storyRepository.getNodeById(targetId)
                if (nextNode != null) moveToNode(nextNode) else isStoryEnded = true
            } else {
                isStoryEnded = true
            }
            isLoading = false
        }
    }

    // --- LOGIC ENGINE PARSER ---

    // Mengevaluasi apakah pilihan boleh muncul (Condition)
    fun isConditionMet(condition: String?): Boolean {
        if (condition.isNullOrBlank()) return true // Jika tidak ada syarat, selalu boleh

        return try {
            val parts = condition.split("==","!=","<=",">=","<",">").map { it.trim() }
            if (parts.size != 2) return true

            val varName = parts[0]
            val targetVal = parts[1]
            val operator = condition.findAnyOf(listOf("==", "!=", "<=", ">=", "<", ">"))?.second ?: "=="
            val currentVal = variablesState[varName] ?: ""

            // Coba perbandingan angka (Integer)
            val currentInt = currentVal.toInt()
            val targetInt = targetVal.toInt()
            when (operator) {
                "==" -> currentInt == targetInt
                "!=" -> currentInt != targetInt
                ">" -> currentInt > targetInt
                "<" -> currentInt < targetInt
                ">=" -> currentInt >= targetInt
                "<=" -> currentInt <= targetInt
                else -> false
            }
        } catch (e: Exception) {
            // Perbandingan String/Boolean jika bukan angka
            val operator = condition.findAnyOf(listOf("==", "!="))?.second ?: "=="
            val varName = condition.split(operator)[0].trim()
            val targetVal = condition.split(operator)[1].trim()
            val currentVal = variablesState[varName] ?: "false"

            when (operator) {
                "==" -> currentVal.equals(targetVal, ignoreCase = true)
                "!=" -> !currentVal.equals(targetVal, ignoreCase = true)
                else -> false
            }
        }
    }

    // Mengeksekusi perubahan nilai variabel (Action)
    private fun applyEffect(effect: String?) {
        if (effect.isNullOrBlank()) return

        try {
            val parts = effect.split("=", "+", "-").map { it.trim() }
            if (parts.size != 2) return

            val varName = parts[0]
            val valPart = parts[1]
            val operator = effect.findAnyOf(listOf("=", "+", "-"))?.second ?: "="
            val currentVal = variablesState[varName] ?: "0"

            if (operator == "=") {
                variablesState[varName] = valPart // Set nilai langsung
            } else {
                // Matematika pertambahan/pengurangan
                val currentInt = currentVal.toIntOrNull() ?: 0
                val deltaInt = valPart.toIntOrNull() ?: 0
                variablesState[varName] = if (operator == "+") (currentInt + deltaInt).toString() else (currentInt - deltaInt).toString()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}

class PlaytestViewModelFactory(
    private val projectId: Int,
    private val storyRepository: StoryRepository,
    private val variableRepository: VariableRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaytestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaytestViewModel(projectId, storyRepository, variableRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}