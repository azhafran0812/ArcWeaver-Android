package com.storymaker.arcweaver.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.storymaker.arcweaver.data.entity.PlayerStateEntity
import com.storymaker.arcweaver.data.entity.SaveStateEntity

@Dao
interface PlaytestDao {

    // Menyimpan atau menimpa (Auto-Save) informasi slot
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaveState(saveState: SaveStateEntity): Long

    // Menyimpan atau menimpa status pemain
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerState(playerState: PlayerStateEntity): Long

    // Mengambil status pemain berdasarkan slot save-nya
    @Query("SELECT * FROM player_states WHERE saveId = :saveId LIMIT 1")
    suspend fun getPlayerStateBySaveId(saveId: Int): PlayerStateEntity?

    // Mengambil semua slot save (untuk fitur Load Game nanti)
    @Query("SELECT * FROM save_states ORDER BY lastSavedAt DESC")
    suspend fun getAllSaveStates(): List<SaveStateEntity>
}