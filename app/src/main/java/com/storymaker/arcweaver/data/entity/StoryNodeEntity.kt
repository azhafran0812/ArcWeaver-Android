package com.storymaker.arcweaver.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "story_nodes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = arrayOf("projectId"),
            childColumns = arrayOf("projectId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StoryNodeEntity(
    @PrimaryKey(autoGenerate = true) val nodeId: Int = 0,
    val projectId: Int,
    val characterName: String,
    val dialogueText: String,
    val characterImageUri: String? = null,
    val nextNodeId: Int? = null,
    val canvasX: Float = 0f,
    val canvasY: Float = 0f
)