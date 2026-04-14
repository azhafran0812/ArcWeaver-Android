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
            onDelete = ForeignKey.CASCADE // Jika slot save dihapus, progress ini ikut terhapus
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
    val saveId: Int, // FK ke tabel save_states
    val currentNodeId: Int, // FK ke tabel story_nodes (posisi pemain saat ini)
    val variablesJson: String = "{}" // Menyimpan inventory/variabel dalam format JSON
)