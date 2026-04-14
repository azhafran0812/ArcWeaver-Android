package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ProjectEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.ProjectRepository
import com.storymaker.arcweaver.data.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectDashboardViewModel(
    private val projectId: Int,
    private val projectRepository: ProjectRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

    // Menyimpan detail proyek saat ini
    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project.asStateFlow()

    // Mengambil HANYA daftar node yang sesuai dengan projectId
    val nodes: StateFlow<List<StoryNodeEntity>> = storyRepository.getNodesByProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            _project.value = projectRepository.getProjectById(projectId)
        }
    }

    fun updateProjectDetails(title: String, description: String) {
        viewModelScope.launch {
            project.value?.let { currentProject ->
                // Memperbarui entitas dengan judul, deskripsi, dan waktu terbaru
                val updatedProject = currentProject.copy(
                    title = title,
                    description = description,
                    updatedAt = System.currentTimeMillis()
                )
                projectRepository.updateProject(updatedProject)

                // Refresh data UI setelah disimpan
                _project.value = updatedProject
            }
        }
    }


    fun deleteNode(node: StoryNodeEntity) {
        viewModelScope.launch {
            storyRepository.deleteNode(node)
            // Sinkronisasi ulang karena ada node yang hilang
            projectRepository.syncProjectStats(node.projectId)
        }
    }

    fun deleteCurrentProject(onComplete: () -> Unit) {
        viewModelScope.launch {
            project.value?.let {
                projectRepository.deleteProject(it)
                onComplete()
            }
        }
    }
}

// Factory untuk mengirimkan repository dan projectId ke ViewModel
class ProjectDashboardViewModelFactory(
    private val projectId: Int,
    private val projectRepository: ProjectRepository,
    private val storyRepository: StoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectDashboardViewModel(projectId, projectRepository, storyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}