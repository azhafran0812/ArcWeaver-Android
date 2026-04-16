package com.storymaker.arcweaver.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.PlayerStateEntity
import com.storymaker.arcweaver.data.entity.SaveStateEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.data.repository.VariableRepository
import com.storymaker.arcweaver.domain.repository.PlaytestRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlaytestViewModel(
    private val projectId: Int,
    private val storyRepository: StoryRepository,
    private val variableRepository: VariableRepository,
    private val playtestRepository: PlaytestRepository
) : ViewModel() {

    var currentNode by mutableStateOf<StoryNodeEntity?>(null)
    var currentChoices by mutableStateOf<List<ChoiceEntity>>(emptyList())
    var isLoading by mutableStateOf(true)
    var isStoryEnded by mutableStateOf(false)

    // Player State (Simulator)
    var variablesState = mutableStateMapOf<String, String>()
    var nodesVisited by mutableIntStateOf(0)
    var storyPath = mutableStateListOf<String>()

    // Save States
    var savedGames by mutableStateOf<List<SaveStateEntity>>(emptyList())

    // --- SNACKBAR / UI EVENTS STATE ---
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        startGame()
        fetchSaveStates()
    }

    fun startGame() {
        viewModelScope.launch {
            isLoading = true
            isStoryEnded = false
            nodesVisited = 0
            storyPath.clear()
            variablesState.clear()

            // Muat variabel bawaan proyek
            val projectVars = variableRepository.getVariablesByProject(projectId).firstOrNull() ?: emptyList()
            projectVars.forEach { variablesState[it.name] = it.initialValue }

            val firstNode = storyRepository.getFirstNode(projectId)
            if (firstNode != null) moveToNode(firstNode) else isStoryEnded = true
            isLoading = false
        }
    }

    // --- FITUR BARU: JUMP, SAVE, LOAD, EDIT VAR ---

    fun jumpToNode(nodeId: Int) {
        viewModelScope.launch {
            isLoading = true
            val node = storyRepository.getNodeById(nodeId)
            if (node != null) {
                isStoryEnded = false
                moveToNode(node)
            }
            isLoading = false
        }
    }

    fun updateVariable(key: String, newValue: String) {
        variablesState[key] = newValue
    }

    private fun fetchSaveStates() {
        viewModelScope.launch {
            savedGames = playtestRepository.getAllSaveFiles()
        }
    }

    fun saveProgress(saveName: String) {
        viewModelScope.launch {
            // Serialisasi map variabel menjadi format string sederhana (key=value;key=value)
            val serializedVars = variablesState.entries.joinToString(";") { "${it.key}=${it.value}" }

            val saveState = SaveStateEntity(saveName = saveName)
            val playerState = PlayerStateEntity(
                saveId = 0, // Akan di-override oleh Repository
                currentNodeId = currentNode?.nodeId ?: 0,
                variablesJson = serializedVars
            )
            playtestRepository.saveGameProgress(saveState, playerState)
            fetchSaveStates() // Refresh list
        }
    }

    fun loadProgress(saveId: Int) {
        viewModelScope.launch {
            isLoading = true
            val playerState = playtestRepository.loadPlayerState(saveId)
            if (playerState != null) {
                // Deserialisasi variabel
                variablesState.clear()
                if (playerState.variablesJson.isNotBlank()) {
                    playerState.variablesJson.split(";").forEach { pair ->
                        val parts = pair.split("=")
                        if (parts.size == 2) variablesState[parts[0]] = parts[1]
                    }
                }
                // Lompat ke Node yang disimpan
                jumpToNode(playerState.currentNodeId)
            }
            isLoading = false
        }
    }

    // --- TRANSAKSI NODE & LOGIKA ---
    private suspend fun moveToNode(node: StoryNodeEntity) {
        currentNode = node
        nodesVisited++
        storyPath.add(node.characterName.ifBlank { "Node #${node.nodeId}" })
        currentChoices = storyRepository.getChoicesByNodeId(node.nodeId)
    }

    fun makeChoice(choice: ChoiceEntity) {
        viewModelScope.launch {
            isLoading = true
            applyEffect(choice.effect)
            val targetId = choice.targetNodeId
            if (targetId != null && targetId != 0) {
                val nextNode = storyRepository.getNodeById(targetId)
                if (nextNode != null) moveToNode(nextNode) else isStoryEnded = true
            } else isStoryEnded = true
            isLoading = false
        }
    }

    fun continueLinear() {
        viewModelScope.launch {
            isLoading = true
            val targetId = currentNode?.nextNodeId
            if (targetId != null && targetId != 0) {
                val nextNode = storyRepository.getNodeById(targetId)
                if (nextNode != null) moveToNode(nextNode) else isStoryEnded = true
            } else isStoryEnded = true
            isLoading = false
        }
    }

    fun isConditionMet(conditionString: String?): Boolean {
        if (conditionString.isNullOrBlank()) return true

        // Memecah string berdasarkan koma (,) atau &&
        val conditions = conditionString.split(",", "&&").map { it.trim() }.filter { it.isNotEmpty() }

        for (condition in conditions) {
            val isMet = try {
                val operator = condition.findAnyOf(listOf("==", "!=", "<=", ">=", "<", ">"))?.second ?: "=="
                val parts = condition.split(operator).map { it.trim() }

                if (parts.size != 2) true else {
                    val varName = parts[0]
                    val targetVal = parts[1]
                    val currentVal = variablesState[varName] ?: ""

                    try {
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
                    } catch (e: NumberFormatException) {
                        // Jika bukan angka (Boolean / String)
                        when (operator) {
                            "==" -> currentVal.equals(targetVal, ignoreCase = true)
                            "!=" -> !currentVal.equals(targetVal, ignoreCase = true)
                            else -> false
                        }
                    }
                }
            } catch (e: Exception) { true }

            // Logika AND: Jika ada SATU saja syarat yang tidak terpenuhi, batalkan semua.
            if (!isMet) return false
        }

        // Jika lolos semua pemeriksaan di atas, berarti semua syarat terpenuhi
        return true
    }

    private fun applyEffect(effectString: String?) {
        if (effectString.isNullOrBlank()) return

        // Memecah string berdasarkan koma (,) atau &&
        val effects = effectString.split(",", "&&").map { it.trim() }.filter { it.isNotEmpty() }

        for (effect in effects) {
            try {
                val operator = effect.findAnyOf(listOf("=", "+", "-"))?.second ?: "="
                val parts = effect.split(operator).map { it.trim() }

                if (parts.size != 2) continue

                val varName = parts[0]
                val valPart = parts[1]
                val currentVal = variablesState[varName] ?: "0"

                if (operator == "=") {
                    variablesState[varName] = valPart // Set nilai mutlak
                } else {
                    val currentInt = currentVal.toIntOrNull() ?: 0
                    val deltaInt = valPart.toIntOrNull() ?: 0
                    // Tambah atau kurang
                    variablesState[varName] = if (operator == "+") (currentInt + deltaInt).toString() else (currentInt - deltaInt).toString()
                }

                // --- TRIGGER SNACKBAR NOTIFICATION DI SINI ---
                viewModelScope.launch {
                    _uiEvents.emit("📈 $varName updated to ${variablesState[varName]}")
                }

            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}

class PlaytestViewModelFactory(
    private val projectId: Int,
    private val storyRepository: StoryRepository,
    private val variableRepository: VariableRepository,
    private val playtestRepository: PlaytestRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaytestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaytestViewModel(projectId, storyRepository, variableRepository, playtestRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}