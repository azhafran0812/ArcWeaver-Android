package com.storymaker.arcweaver.data.repository

import com.storymaker.arcweaver.data.dao.VariableDao
import com.storymaker.arcweaver.data.entity.VariableEntity
import kotlinx.coroutines.flow.Flow

class VariableRepository(private val variableDao: VariableDao) {

    fun getVariablesByProject(projectId: Int): Flow<List<VariableEntity>> {
        return variableDao.getVariablesByProject(projectId)
    }

    suspend fun insertVariable(variable: VariableEntity) {
        variableDao.insertVariable(variable)
    }

    suspend fun updateVariable(variable: VariableEntity) {
        variableDao.updateVariable(variable)
    }

    suspend fun deleteVariable(variable: VariableEntity) {
        variableDao.deleteVariable(variable)
    }
}