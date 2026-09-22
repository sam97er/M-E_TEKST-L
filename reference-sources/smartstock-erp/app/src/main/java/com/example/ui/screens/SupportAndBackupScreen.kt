package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.BackupLog
import com.example.data.model.SupportTicket
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.ui.theme.ErpDanger
import com.example.ui.theme.ErpNavyPrimary
import com.example.ui.theme.ErpStrings
import com.example.ui.theme.ErpSuccess
import com.example.ui.theme.ErpTealSecondary
import com.example.ui.theme.ErpWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupportAndBackupScreen(
    tickets: List<SupportTicket>,
    backups: List<BackupLog>,
    language: AppLanguage,
    onCreateBackup: () -> Unit,
    onOpenNewTicket: () -> Unit,
    onResolveTicket: (SupportTicket) -> Unit
) {
    var autoBackupEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Security & Data Backup Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErpNavyPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0x33FFFFFF),
                                shape = CircleShape,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الأمان والنسخ الاحتياطي الدوري",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "تشفير كامل للبيانات بتقنية AES-256 GCM",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ErpStrings.get("periodic_backup", language),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "إنشاء نسخة احتياطية مشفرة يومياً الساعة 02:00 ص",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { autoBackupEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCreateBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = ErpTealSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ErpStrings.get("create_backup", language),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Backup History
        item {
            Text(
                text = ErpStrings.get("backup_history", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(backups) { backup ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = ErpSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = backup.backupName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${backup.recordCount} سجل • ${backup.sizeKb} KB • ${backup.encryptionType}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(backup.timestamp)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Customer Technical Support Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = ErpNavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ErpStrings.get("support_tickets", language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenNewTicket,
                    colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("new_ticket_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تذكرة جديدة", fontSize = 12.sp)
                }
            }
        }

        items(tickets) { ticket ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = when (ticket.priority) {
                                    TicketPriority.URGENT -> Color(0xFFFEE2E2)
                                    TicketPriority.HIGH -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFE0E7FF)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ticket.priority.titleAr,
                                    color = when (ticket.priority) {
                                        TicketPriority.URGENT -> ErpDanger
                                        TicketPriority.HIGH -> Color(0xFFB45309)
                                        else -> ErpNavyPrimary
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ticket.ticketCode,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Surface(
                            color = when (ticket.status) {
                                TicketStatus.OPEN -> Color(0xFFFEF3C7)
                                TicketStatus.IN_PROGRESS -> Color(0xFFDBEAFE)
                                TicketStatus.RESOLVED -> Color(0xFFD1FAE5)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = ticket.status.titleAr,
                                color = when (ticket.status) {
                                    TicketStatus.OPEN -> Color(0xFFB45309)
                                    TicketStatus.IN_PROGRESS -> Color(0xFF1D4ED8)
                                    TicketStatus.RESOLVED -> ErpSuccess
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = ticket.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ticket.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "العميل: ${ticket.customerName} (${ticket.customerContact})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (ticket.responseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ملاحظات الدعم: ${ticket.responseNotes}",
                            fontSize = 11.sp,
                            color = ErpTealSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (ticket.status != TicketStatus.RESOLVED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onResolveTicket(ticket) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "تم الحل والمتابعة", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
