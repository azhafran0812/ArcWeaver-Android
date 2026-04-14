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
    // UBAH: Menambahkan projectId untuk mengambil node pertama dari proyek yang sedang dimainkan
    suspend fun getFirstNodeOfProject(projectId: Int): StoryNodeEntity? {
        return storyDao.getFirstNodeOfProject(projectId)
    }

    // Fungsi untuk Auto-Save (Sesuai rancangan diagram Playtest Interactive Story)
    suspend fun saveGameProgress(saveState: SaveStateEntity, playerState: PlayerStateEntity) {
        // 1. Simpan/Update informasi slot save
        val generatedSaveId = playtestDao.insertSaveState(saveState).toInt()

        // 2. Hubungkan state pemain dengan ID save tersebut dan simpan ke database
        val stateToSave = playerState.copy(saveId = generatedSaveId)
        playtestDao.insertPlayerState(stateToSave)
    }

    // Fungsi untuk memuat game yang tersimpan
    suspend fun loadPlayerState(saveId: Int): PlayerStateEntity? {
        return playtestDao.getPlayerStateBySaveId(saveId)
    }

    // Mengambil semua slot save (opsional, jika nanti butuh menu "Load Game")
    suspend fun getAllSaveFiles(): List<SaveStateEntity> {
        return playtestDao.getAllSaveStates()
    }
}