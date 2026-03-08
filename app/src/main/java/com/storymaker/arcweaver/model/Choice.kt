package com.storymaker.arcweaver.model
// Objek yang mewakili satu pilihan jawaban
data class Choice(
    var id: Int,
    var choiceText: String,
    var targetNodeId: Int? = null // Nullable, karena saat baru dibuat mungkin belum disambung ke node lain
)