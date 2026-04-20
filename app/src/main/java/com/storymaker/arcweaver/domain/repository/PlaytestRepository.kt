package com.storymaker.arcweaver.domain.repository

import com.storymaker.arcweaver.data.dao.PlaytestDao
import com.storymaker.arcweaver.data.dao.StoryDao
import com.storymaker.arcweaver.data.entity.PlayerStateEntity
import com.storymaker.arcweaver.data.entity.SaveStateEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity

class PlaytestRepository(
    private val playtestDao: PlaytestDao,
    private val storyDao: StoryDao
) {

    suspend fun getFirstNodeOfProject(projectId: Int): StoryNodeEntity? {
        return storyDao.getFirstNodeOfProject(projectId)
    }

    suspend fun saveGameProgress(saveState: SaveStateEntity, playerState: PlayerStateEntity) {

        val generatedSaveId = playtestDao.insertSaveState(saveState).toInt()

        val stateToSave = playerState.copy(saveId = generatedSaveId)
        playtestDao.insertPlayerState(stateToSave)
    }

    // Fungsi untuk memuat game yang tersimpan
    suspend fun loadPlayerState(saveId: Int): PlayerStateEntity? {
        return playtestDao.getPlayerStateBySaveId(saveId)
    }

    suspend fun getAllSaveFiles(): List<SaveStateEntity> {
        return playtestDao.getAllSaveStates()
    }
}