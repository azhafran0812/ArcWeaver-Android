package com.storymaker.arcweaver.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.storymaker.arcweaver.model.Choice
import com.storymaker.arcweaver.model.StoryNode

class NodeViewModel : ViewModel() {
    // State untuk Form Input
    var characterName = mutableStateOf("")
    var dialogueText = mutableStateOf("")
    var newChoiceInput = mutableStateOf("")

    // State untuk Daftar Pilihan
    var choicesList = mutableStateListOf<Choice>()
    private var choiceIdCounter = 1

    // Fungsi OOP untuk menambah pilihan
    fun addChoice() {
        if (newChoiceInput.value.isNotBlank()) {
            choicesList.add(Choice(id = choiceIdCounter, choiceText = newChoiceInput.value))
            choiceIdCounter++
            newChoiceInput.value = "" // Kosongkan input
        }
    }

    // Fungsi untuk membungkus data menjadi Objek saat disimpan
    fun saveNode(): StoryNode {
        val finalNode = StoryNode(
            nodeId = 1, // Nanti ini bisa di-generate otomatis oleh Room DB
            characterName = characterName.value,
            dialogueText = dialogueText.value
        )

        choicesList.forEach { choice ->
            finalNode.addChoice(choice)
        }

        // Di sini nantinya Anda memanggil Room Database (Dao) untuk insert ke SQLite
        // repository.insertNode(finalNode)

        return finalNode
    }

    // Fungsi untuk mereset form setelah simpan
    fun clearForm() {
        characterName.value = ""
        dialogueText.value = ""
        choicesList.clear()
        choiceIdCounter = 1
    }
}