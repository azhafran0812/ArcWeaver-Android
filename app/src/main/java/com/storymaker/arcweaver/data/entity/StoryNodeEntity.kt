package com.storymaker.arcweaver.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_nodes")
data class StoryNodeEntity(
    @PrimaryKey(autoGenerate = true) val nodeId: Int = 0,
    val characterName: String,
    val dialogueText: String,
    val characterImageUri: String? = null,
    val canvasX: Float = 0f,
    val canvasY: Float = 0f
)