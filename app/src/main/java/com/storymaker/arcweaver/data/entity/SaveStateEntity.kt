package com.storymaker.arcweaver.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_states")
data class SaveStateEntity(
    @PrimaryKey(autoGenerate = true) val saveId: Int = 0,
    val saveName: String,
    val lastSavedAt: Long = System.currentTimeMillis()
)