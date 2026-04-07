package com.storymaker.arcweaver.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "choices")
data class ChoiceEntity(
    @PrimaryKey(autoGenerate = true) val choiceId: Int = 0,
    val parentNodeId: Int,
    val choiceText: String,
    val targetNodeId: Int? = null,
    val requiredCondition: String? = null
)