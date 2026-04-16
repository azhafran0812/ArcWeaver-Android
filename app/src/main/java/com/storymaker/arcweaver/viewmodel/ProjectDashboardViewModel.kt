package com.storymaker.arcweaver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storymaker.arcweaver.data.entity.ProjectEntity
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.ProjectRepository
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.data.entity.ChoiceEntity
import com.storymaker.arcweaver.data.entity.VariableEntity
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.storymaker.arcweaver.data.repository.VariableRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class ProjectDashboardViewModel(
    private val projectId: Int,
    private val projectRepository: ProjectRepository,
    private val storyRepository: StoryRepository,
    private val variableRepository: VariableRepository
) : ViewModel() {

    // Menyimpan detail proyek saat ini
    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project.asStateFlow()

    // Mengambil HANYA daftar node yang sesuai dengan projectId
    val nodes: StateFlow<List<StoryNodeEntity>> = storyRepository.getNodesByProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            _project.value = projectRepository.getProjectById(projectId)
        }
    }

    fun updateProjectDetails(title: String, description: String) {
        viewModelScope.launch {
            project.value?.let { currentProject ->
                // Memperbarui entitas dengan judul, deskripsi, dan waktu terbaru
                val updatedProject = currentProject.copy(
                    title = title,
                    description = description,
                    updatedAt = System.currentTimeMillis()
                )
                projectRepository.updateProject(updatedProject)

                // Refresh data UI setelah disimpan
                _project.value = updatedProject
            }
        }
    }


    fun deleteNode(node: StoryNodeEntity) {
        viewModelScope.launch {
            storyRepository.deleteNode(node)
            // Sinkronisasi ulang karena ada node yang hilang
            projectRepository.syncProjectStats(node.projectId)
        }
    }

    fun deleteCurrentProject(onComplete: () -> Unit) {
        viewModelScope.launch {
            project.value?.let {
                projectRepository.deleteProject(it)
                onComplete()
            }
        }
    }

    // --- LOGIKA EKSPOR JSON ---
    fun exportProjectToJson(context: Context, uri: Uri, variables: List<VariableEntity>) {
        viewModelScope.launch {
            try {
                val currentProject = project.value ?: return@launch
                val allNodes = nodes.value

                // Merangkai Nodes dan Choices menjadi satu kesatuan
                val nodeExportList = allNodes.map { node ->
                    val choices = storyRepository.getChoicesByNodeId(node.nodeId)
                    NodeExportData(node, choices)
                }

                // Menyatukan Semuanya
                val exportData = ArcWeaverExportData(currentProject, variables, nodeExportList)

                val gson = Gson()
                val jsonString = gson.toJson(exportData)

                // Menyimpan ke file lokal HP
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- LOGIKA IMPOR JSON (MENGGANTI PROYEK INI) ---
    fun importProjectFromJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // Membaca file JSON
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonString != null) {
                    val gson = Gson()
                    val importedData = gson.fromJson(jsonString, ArcWeaverExportData::class.java)

                    // 1. Perbarui Judul & Deskripsi Proyek
                    val updatedProject = importedData.project.copy(projectId = projectId)
                    projectRepository.updateProject(updatedProject)

                    // 2. Bersihkan Node dan Variable Lama
                    val oldNodes = storyRepository.getNodesByProjectIdFlow(projectId).first()
                    oldNodes.forEach { storyRepository.deleteNode(it) }

                    val oldVars = variableRepository.getVariablesByProject(projectId).first()
                    oldVars.forEach { variableRepository.deleteVariable(it) }

                    // 3. Masukkan Variable Baru dari JSON
                    importedData.variables.forEach {
                        variableRepository.insertVariable(it.copy(varId = 0, projectId = projectId))
                    }

                    // 4. ALGORITMA TWO-PASS UNTUK MENJAHIT ULANG ID (AUTO-ADAPT)
                    val idMap = mutableMapOf<Int, Int>() // Format: <ID Lama, ID Baru>

                    // TAHAP 1: Buat semua Node baru dan catat perubahan ID-nya
                    importedData.nodes.forEach { nodeData ->
                        val oldNodeId = nodeData.node.nodeId
                        val newNode = nodeData.node.copy(nodeId = 0, projectId = projectId)

                        // Insert ke database dan tangkap ID baru hasil AutoGenerate
                        val newId = storyRepository.insertNodeReturnId(newNode)
                        idMap[oldNodeId] = newId // Simpan ke dalam kamus pemetaan
                    }

                    // TAHAP 2: Perbaiki semua sambungan (Target) berdasarkan kamus pemetaan
                    importedData.nodes.forEach { nodeData ->
                        val oldNodeId = nodeData.node.nodeId
                        val newId = idMap[oldNodeId] ?: return@forEach // Lewati jika gagal map

                        // A. Perbaiki nextNodeId (jika cerita berjalan linear tanpa pilihan)
                        val fixedNextNodeId = nodeData.node.nextNodeId?.let { oldTarget -> idMap[oldTarget] }

                        // Update Node dengan nextNodeId yang sudah benar
                        val finalNode = storyRepository.getNodeById(newId)?.copy(nextNodeId = fixedNextNodeId)
                        if (finalNode != null) {
                            storyRepository.updateNode(finalNode)
                        }

                        // B. Perbaiki targetNodeId di dalam semua Pilihan (Choices)
                        val fixedChoices = nodeData.choices.map { choice ->
                            choice.copy(
                                choiceId = 0, // Reset ID pilihan
                                parentNodeId = newId, // Sambungkan ke Node pemilik yang baru
                                targetNodeId = choice.targetNodeId?.let { oldTarget -> idMap[oldTarget] } // Arahkan ke cabang baru
                            )
                        }

                        // Insert pilihan yang sudah diperbaiki ke database
                        storyRepository.insertChoicesOnly(fixedChoices)
                    }

                    projectRepository.syncProjectStats(projectId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// Factory
class ProjectDashboardViewModelFactory(
    private val projectId: Int,
    private val projectRepository: ProjectRepository,
    private val storyRepository: StoryRepository,
    private val variableRepository: VariableRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectDashboardViewModel(projectId, projectRepository, storyRepository, variableRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Data Class Wrapper untuk JSON
data class ArcWeaverExportData(
    val project: com.storymaker.arcweaver.data.entity.ProjectEntity,
    val variables: List<VariableEntity>,
    val nodes: List<NodeExportData>
)

data class NodeExportData(
    val node: com.storymaker.arcweaver.data.entity.StoryNodeEntity,
    val choices: List<ChoiceEntity>
)