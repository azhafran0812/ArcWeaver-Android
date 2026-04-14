package com.storymaker.arcweaver.data.repository

import com.storymaker.arcweaver.data.dao.ProjectDao
import com.storymaker.arcweaver.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun insertProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    suspend fun getProjectById(id: Int): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    suspend fun syncProjectStats(projectId: Int) {
        projectDao.syncProjectStats(projectId, System.currentTimeMillis())
    }
}