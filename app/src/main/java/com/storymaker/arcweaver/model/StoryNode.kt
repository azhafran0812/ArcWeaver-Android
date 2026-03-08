package com.storymaker.arcweaver.model
// Objek utama yang mewakili satu blok cerita
class StoryNode(
    val nodeId: Int,
    var characterName: String,
    var dialogueText: String
) {
    // List untuk menyimpan objek-objek Choice (Pilihan cabang)
    private val choices: MutableList<Choice> = mutableListOf()

    // Method OOP untuk menambahkan pilihan baru ke dalam node ini
    fun addChoice(newChoice: Choice) {
        choices.add(newChoice)
    }

    // Method untuk mengambil daftar pilihan (Encapsulation)
    fun getChoices(): List<Choice> {
        return choices
    }

    // Method untuk menghitung jumlah cabang
    fun getBranchCount(): Int {
        return choices.size
    }
}