package com.storymaker.arcweaver.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "player_states",
    foreignKeys = [
        ForeignKey(
            entity = SaveStateEntity::class,
            parentColumns = arrayOf("saveId"),
            childColumns = arrayOf("saveId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoryNodeEntity::class,
            parentColumns = arrayOf("nodeId"),
            childColumns = arrayOf("currentNodeId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayerStateEntity(
    @PrimaryKey(autoGenerate = true) val stateId: Int = 0,
    val saveId: Int,
    val currentNodeId: Int,
    val variablesJson: String = "{}"
)