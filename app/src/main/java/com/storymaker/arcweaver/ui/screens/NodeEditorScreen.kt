package com.storymaker.arcweaver.ui.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storymaker.arcweaver.viewmodel.NodeViewModel
import com.storymaker.arcweaver.model.Choice
import com.storymaker.arcweaver.model.StoryNode


@Composable
fun NodeEditorScreen(viewModel: NodeViewModel = viewModel()) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("ArcWeaver Editor", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Form Karakter (Ambil data dari ViewModel)
        OutlinedTextField(
            value = viewModel.characterName.value,
            onValueChange = { viewModel.characterName.value = it },
            label = { Text("Nama Karakter") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Form Dialog
        OutlinedTextField(
            value = viewModel.dialogueText.value,
            onValueChange = { viewModel.dialogueText.value = it },
            label = { Text("Isi Dialog/Narasi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Form Tambah Cabang
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.newChoiceInput.value,
                onValueChange = { viewModel.newChoiceInput.value = it },
                label = { Text("Ketik pilihan baru...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.addChoice() }, // Panggil fungsi di ViewModel
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Tambah")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Daftar Pilihan (Observasi List dari ViewModel)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.choicesList) { choice ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = "Cabang: ${choice.choiceText}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Tombol Simpan
        Button(
            onClick = {
                val savedNode = viewModel.saveNode()
                Toast.makeText(context, "Tersimpan! ${savedNode.getBranchCount()} Cabang dibuat.", Toast.LENGTH_SHORT).show()
                viewModel.clearForm() // Bersihkan layar
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan Node Cerita")
        }
    }
}