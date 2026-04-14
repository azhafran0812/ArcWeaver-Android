package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.VariableEntity
import com.storymaker.arcweaver.data.repository.VariableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VariableViewModel(private val repository: VariableRepository) : ViewModel() {

    fun getVariables(projectId: Int): StateFlow<List<VariableEntity>> =
        repository.getVariablesByProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVariable(projectId: Int, name: String, type: String, initialValue: String) {
        viewModelScope.launch {
            repository.insertVariable(VariableEntity(projectId = projectId, name = name, type = type, initialValue = initialValue))
        }
    }

    fun updateVariable(variable: VariableEntity) {
        viewModelScope.launch { repository.updateVariable(variable) }
    }

    fun deleteVariable(variable: VariableEntity) {
        viewModelScope.launch { repository.deleteVariable(variable) }
    }
}

class VariableViewModelFactory(private val repository: VariableRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VariableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VariableViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}