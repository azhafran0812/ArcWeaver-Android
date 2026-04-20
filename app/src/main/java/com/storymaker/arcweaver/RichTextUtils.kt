package com.storymaker.arcweaver

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

// Fungsi untuk membaca Markdown-lite (**Tebal** dan *Miring*)
fun parseRichText(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Regex untuk mencari **tebal** ATAU *miring*
        val regex = Regex("\\*\\*(.*?)\\*\\*|\\*(.*?)\\*")
        val matches = regex.findAll(text)

        for (match in matches) {
            append(text.substring(currentIndex, match.range.first))

            val boldText = match.groups[1]?.value
            val italicText = match.groups[2]?.value

            if (boldText != null) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(boldText)
                pop()
            } else if (italicText != null) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(italicText)
                pop()
            }
            currentIndex = match.range.last + 1
        }
        append(text.substring(currentIndex))
    }
}