package com.storymaker.arcweaver.model
data class Choice(
    var id: Int,
    var choiceText: String,
    var targetNodeId: Int? = null
)