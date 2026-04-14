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
            // Mengambil node pertama sebagai patokan (atau kita bisa ambil semua langsung)
            val firstNode = storyRepository.getFirstNode(projectId)

            // Karena DAO Anda mungkin mengembalikan Flow di layar lain, di sini kita
            // asumsikan ada cara untuk mengambil semua node. Jika menggunakan Flow, collect di sini.
            // Untuk kesederhanaan simulasi, kita kumpulkan manual dari repositori:

            // [!] PERHATIAN: Pastikan Anda memiliki fungsi getNodesByProjectId (suspend/Flow) di DAO.
            // Di sini saya mengandalkan flow yang sudah ada di aplikasi Anda.
            storyRepository.getNodesByProjectIdFlow(projectId).collect { nodeList ->
                nodes = nodeList

                // Ambil semua choices untuk menggambar garis
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

    // Dipanggil setiap kali user selesai men-drag (menyeret) node di kanvas
    fun updateNodePosition(node: StoryNodeEntity, newX: Float, newY: Float) {
        viewModelScope.launch {
            val updatedNode = node.copy(canvasX = newX, canvasY = newY)

            // Update UI State langsung agar terasa mulus
            nodes = nodes.map { if (it.nodeId == node.nodeId) updatedNode else it }

            // Simpan ke database di latar belakang
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