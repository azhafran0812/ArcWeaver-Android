package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.storymaker.arcweaver.data.entity.ProjectEntity
import com.storymaker.arcweaver.data.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ProjectRepository) : ViewModel() {

    val projectList: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewProject(title: String, description: String) {
        viewModelScope.launch {
            val newProject = ProjectEntity(title = title, description = description)
            repository.insertProject(newProject)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }
}

class HomeViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}