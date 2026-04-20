package com.storymaker.arcweaver.model

class StoryNode(
    val nodeId: Int,
    var characterName: String,
    var dialogueText: String
) {
    private val choices: MutableList<Choice> = mutableListOf()

    fun addChoice(newChoice: Choice) {
        choices.add(newChoice)
    }

    fun getChoices(): List<Choice> {
        return choices
    }

    fun getBranchCount(): Int {
        return choices.size
    }
}