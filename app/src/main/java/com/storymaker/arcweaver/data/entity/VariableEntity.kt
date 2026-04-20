package com.storymaker.arcweaver.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "variables",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = arrayOf("projectId"),
            childColumns = arrayOf("projectId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VariableEntity(
    @PrimaryKey(autoGenerate = true) val varId: Int = 0,
    val projectId: Int,
    val name: String,
    val type: String,
    val initialValue: String
)