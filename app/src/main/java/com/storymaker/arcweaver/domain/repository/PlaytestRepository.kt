package com.storymaker.arcweaver.domain.repository

import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.StoryRepository

class PlaytestRepository(
    private val storyRepository: StoryRepository
) {

    // getnode pertama
    suspend fun getStartNode(): StoryNodeEntity? {
        return storyRepository.getFirstNode()
    }

    // getnode berdasarkan ID
    suspend fun getNodeById(nodeId: Int): StoryNodeEntity? {
        return storyRepository.getNodeById(nodeId)
    }

    // get semua pilihan dari node
    suspend fun getChoices(nodeId: Int): List<ChoiceEntity> {
        return storyRepository.getChoicesByNodeId(nodeId)
    }
}