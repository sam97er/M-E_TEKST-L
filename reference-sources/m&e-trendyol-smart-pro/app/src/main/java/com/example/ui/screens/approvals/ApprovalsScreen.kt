package com.example.ui.screens.approvals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AiTaskEntity
import com.example.data.repository.MAndETekstilRepository
import com.example.domain.model.AiApprovalStatus
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApprovalsScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val tasks by repository.aiTasks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var taskToEdit by remember { mutableStateOf<AiTaskEntity?>(null) }
    var editedContentText by remember { mutableStateOf("") }

    val pendingTasks = tasks.filter { it.status == AiApprovalStatus.BEKLIYOR || it.status == AiApprovalStatus.INCELENIYOR }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "AI Onay & İnceleme Merkezi",
            subtitle = "${pendingTasks.size} Görev Onay Bekliyor • Güvenli Denetim"
        )

        if (pendingTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Hepsi Onaylı",
                        tint = SuccessGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bekleyen AI önerisi bulunmuyor.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tüm AI müşteri yanıtları ve fiyat önerileri incelendi.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "🛡️ İnsan Denetimi İlkesi: Yapay zeka tarafından üretilen hiçbir içerik veya fiyat değişikliği bu ekranda onaylanmadan Trendyol'a aktarılmaz.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                items(pendingTasks, key = { it.id }) { task ->
                    AiTaskCardItem(
                        task = task,
                        onApprove = {
                            scope.launch {
                                repository.updateAiTaskStatus(task.id, AiApprovalStatus.UYGULANDI, task.outputData)
                                snackbarHostState.showSnackbar("Görev #${task.id} onaylandı ve uygulandı.")
                            }
                        },
                        onReject = {
                            scope.launch {
                                repository.updateAiTaskStatus(task.id, AiApprovalStatus.REDDEDILDI, "")
                                snackbarHostState.showSnackbar("Görev #${task.id} reddedildi.")
                            }
                        },
                        onEditAndApprove = {
                            taskToEdit = task
                            editedContentText = task.outputData
                        }
                    )
                }
            }
        }
    }

    // Edit and Approve Modal
    if (taskToEdit != null) {
        val task = taskToEdit!!
        AlertDialog(
            onDismissRequest = { taskToEdit = null },
            title = { Text("AI Çıktısını Düzenle ve Onayla") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Görev Türü: ${task.taskType.title} (#${task.targetReferenceId})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editedContentText,
                        onValueChange = { editedContentText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp).testTag("approval_content_editor"),
                        label = { Text("Onaylanacak Nihai Metin") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateAiTaskStatus(task.id, AiApprovalStatus.DUZENLENDI, editedContentText)
                            taskToEdit = null
                            snackbarHostState.showSnackbar("Düzenlenen AI görevi onaylandı.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Düzenlemeyi Onayla")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToEdit = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun AiTaskCardItem(
    task: AiTaskEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onEditAndApprove: () -> Unit
) {
    val timeStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(task.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth().testTag("ai_task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = TrendyolOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.taskType.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                StatusPill(text = "Slot ${task.slotNumber}: ${task.providerName}", colorType = StatusColorType.INFO)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hedef: ${task.targetReferenceId} • $timeStr • Model: ${task.modelName}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = task.outputData.ifBlank { "(Yanıt henüz üretilmedi veya boş)" },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).testTag("approve_task_button_${task.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Onayla", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Onayla", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEditAndApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Düzenle", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Reddet", tint = DangerRed, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
