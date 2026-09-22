package com.example.ui.questions

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIDraftDto
import com.example.data.model.CustomerQuestionDto
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    viewModel: QuestionsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Müşteri Soruları & AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = PrimaryOrange,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "AŞAMA 5",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "İnsan Onaylı Yapay Zekâ Yanıt İş Akışı",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncQuestions() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.testTag("btn_sync_questions")
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                        } else {
                            Icon(imageVector = Icons.Outlined.CloudSync, contentDescription = "Trendyol Sorularını Çek", tint = PrimaryOrange)
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.loadSummary()
                            viewModel.loadQuestions()
                        },
                        modifier = Modifier.testTag("btn_refresh_questions")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yenile", tint = PrimaryOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // KPI Summary Row
            QuestionsSummaryHeader(summary = uiState.summary)

            // Filter Chips
            QuestionsFilterBar(
                selectedFilter = uiState.selectedStatusFilter,
                onFilterSelected = { viewModel.setStatusFilter(it) },
                searchQuery = uiState.searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) }
            )

            // Content List
            if (uiState.isLoading && uiState.questions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else if (uiState.questions.isEmpty()) {
                EmptyQuestionsPlaceholder(onSyncClick = { viewModel.syncQuestions() })
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.questions, key = { it.id }) { question ->
                        CustomerQuestionCard(
                            question = question,
                            onClick = { viewModel.selectQuestion(question) }
                        )
                    }
                }
            }
        }

        // Modal Bottom Sheet / Detail Dialog for selected question
        uiState.selectedQuestion?.let { question ->
            QuestionDetailModal(
                question = question,
                uiState = uiState,
                onDismiss = { viewModel.selectQuestion(null) },
                onGenerateAiDraft = { qid -> viewModel.generateAiDraft(qid) },
                onToneSelected = { viewModel.setSelectedTone(it) },
                onSlotSelected = { viewModel.setSelectedSlot(it) },
                onInstructionsChange = { viewModel.setAdditionalInstructions(it) },
                onStartEdit = { draftId, text -> viewModel.startEditingDraft(draftId, text) },
                onCancelEdit = { viewModel.cancelEditingDraft() },
                onSaveEdit = { draftId, qid -> viewModel.saveEditedDraft(draftId, qid) },
                onUpdateEditText = { viewModel.updateEditedAnswerText(it) },
                onApprove = { draftId, qid -> viewModel.approveDraft(draftId, qid) },
                onSend = { qid, draftId -> viewModel.sendApprovedReply(qid, draftId) },
                onReject = { draftId, qid -> viewModel.rejectDraft(draftId, qid) }
            )
        }
    }
}

@Composable
fun QuestionsSummaryHeader(summary: com.example.data.model.CustomerQuestionsSummaryDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryKpiItem(title = "Toplam", count = summary.totalCount, color = MaterialTheme.colorScheme.onSurface)
            SummaryKpiItem(title = "Yeni", count = summary.newCount, color = InfoBlue)
            SummaryKpiItem(title = "Taslak Hazır", count = summary.draftGeneratedCount, color = PrimaryOrange)
            SummaryKpiItem(title = "Onaylanan", count = summary.approvedCount, color = ProfitGreen)
            SummaryKpiItem(title = "Yanıtlanan", count = summary.sentCount, color = Color(0xFF7C3AED))
        }
    }
}

@Composable
fun SummaryKpiItem(title: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuestionsFilterBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Müşteri adı veya soru ara...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Temizle", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = BorderColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("input_search_questions")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Chips
        val filters = listOf(
            "ALL" to "Tümü",
            "NEW" to "Yeni Sorular",
            "DRAFT_GENERATED" to "Taslak Hazır",
            "APPROVED" to "Onaylandı",
            "SENT" to "Gönderildi"
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { onFilterSelected(key) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerQuestionCard(
    question: CustomerQuestionDto,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_question_${question.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Customer Name & Question ID & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = question.customerName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = question.customerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = question.trendyolQuestionId,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                QuestionStatusBadge(status = question.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Badge if linked
            if (!question.productTitle.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Checkroom, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${question.productCode ?: ""} • ${question.productTitle}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Question Text Bubble
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "\"${question.questionText}\"",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Draft state indicator & Action hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val latestDraft = question.drafts.lastOrNull()
                if (latestDraft != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (latestDraft.status == "APPROVED") ProfitGreen else PrimaryOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${latestDraft.providerName} (${latestDraft.status})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (latestDraft.status == "APPROVED") ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.HourglassEmpty, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Taslak Bekleniyor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "İncele & Yanıtla", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryOrange)
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryOrange)
                }
            }
        }
    }
}

@Composable
fun QuestionStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "NEW" -> Triple(InfoContainer, InfoBlue, "Yeni Soru")
        "DRAFT_GENERATED" -> Triple(PrimaryOrange.copy(alpha = 0.15f), PrimaryOrange, "Taslak Hazır")
        "APPROVED" -> Triple(ProfitGreen.copy(alpha = 0.15f), ProfitGreen, "Onaylandı")
        "SENT" -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "Trendyol'a Gönderildi")
        "REJECTED" -> Triple(ErrorContainer, LossRed, "Reddedildi")
        else -> Triple(Color(0xFFECEFF1), Color(0xFF455A64), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailModal(
    question: CustomerQuestionDto,
    uiState: QuestionsUiState,
    onDismiss: () -> Unit,
    onGenerateAiDraft: (Int) -> Unit,
    onToneSelected: (String) -> Unit,
    onSlotSelected: (Int?) -> Unit,
    onInstructionsChange: (String) -> Unit,
    onStartEdit: (Int, String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (Int, Int) -> Unit,
    onUpdateEditText: (String) -> Unit,
    onApprove: (Int, Int) -> Unit,
    onSend: (Int, Int?) -> Unit,
    onReject: (Int, Int) -> Unit
) {
    val latestDraft = question.drafts.lastOrNull()
    val isEditing = uiState.editingDraftId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with question meta and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Soru Detayı & Yanıt Hazırlama",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${question.customerName} • ${question.trendyolQuestionId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                QuestionStatusBadge(status = question.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Workflow Step Indicator
            WorkflowStepIndicator(currentStatus = question.status)

            Spacer(modifier = Modifier.height(14.dp))

            // Customer Question Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Müşteri Sorusu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = question.questionText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Generation Options & Trigger (if no draft or want to regenerate)
            if (question.status in listOf("NEW", "DRAFT_GENERATED", "REJECTED")) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Yapay Zekâ Ayarları",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Tone Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val tones = listOf(
                                "KURUMSAL" to "Kurumsal",
                                "SAMIMI" to "Samimi",
                                "KISA_VE_OZ" to "Kısa & Öz"
                            )
                            tones.forEach { (toneKey, toneLabel) ->
                                val isSelected = uiState.selectedTone == toneKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToneSelected(toneKey) },
                                    label = { Text(toneLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Generate Button
                        Button(
                            onClick = { onGenerateAiDraft(question.id) },
                            enabled = !uiState.isGeneratingDraft,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_generate_ai_draft")
                        ) {
                            if (uiState.isGeneratingDraft) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Yanıt Hazırlanıyor...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (latestDraft == null) "Yapay Zekâ Taslağı Oluştur" else "Yeniden Taslak Üret",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Latest Draft Display & Action Buttons
            if (latestDraft != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (latestDraft.status == "APPROVED") ProfitGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (latestDraft.status == "APPROVED") ProfitGreen else PrimaryOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.SmartToy, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Taslağı (${latestDraft.providerName})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            QuestionStatusBadge(status = latestDraft.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = uiState.editedAnswerText,
                                onValueChange = onUpdateEditText,
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_edit_draft_text"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = PrimaryOrange
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onCancelEdit) {
                                    Text("İptal", color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onSaveEdit(latestDraft.id, question.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Kaydet")
                                }
                            }
                        } else {
                            Text(
                                text = latestDraft.finalAnswer,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp,
                                modifier = Modifier.testTag("text_final_answer")
                            )

                            if (latestDraft.userEditedAnswer != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Operatör tarafından düzenlendi", fontSize = 11.sp, color = TextSecondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }

                            if (latestDraft.status in listOf("DRAFT", "APPROVED")) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onStartEdit(latestDraft.id, latestDraft.finalAnswer) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryOrange)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Metni Düzenle", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    if (latestDraft.status == "DRAFT") {
                                        TextButton(
                                            onClick = { onReject(latestDraft.id, question.id) }
                                        ) {
                                            Text("Reddet", color = LossRed, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Critical Approval & Send Buttons (Strict Human-in-the-Loop Enforcement)
                if (latestDraft.status == "DRAFT") {
                    // Approval is required before sending
                    Button(
                        onClick = { onApprove(latestDraft.id, question.id) },
                        enabled = !uiState.isApproving,
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_approve_draft")
                    ) {
                        if (uiState.isApproving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("İnsan Onayı Ver (Approve)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔒 Güvenlik Kuralı: Trendyol'a gönderim öncesinde operatör onayı zorunludur.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (latestDraft.status == "APPROVED") {
                    // Ready to transmit to Trendyol
                    Button(
                        onClick = { onSend(question.id, latestDraft.id) },
                        enabled = !uiState.isSending,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_send_to_trendyol")
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trendyol'a Cevabı Gönder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else if (latestDraft.status == "SENT") {
                    Surface(
                        color = ProfitGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Cevap Trendyol'a Başarıyla İletildi", fontWeight = FontWeight.Bold, color = ProfitGreen, fontSize = 13.sp)
                                Text(text = "Onaylayan: ${latestDraft.approvedBy ?: "OPERATOR"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkflowStepIndicator(currentStatus: String) {
    val steps = listOf(
        "Yeni" to (currentStatus in listOf("NEW", "DRAFT_GENERATED", "APPROVED", "SENT")),
        "AI Taslağı" to (currentStatus in listOf("DRAFT_GENERATED", "APPROVED", "SENT")),
        "İnsan Onayı" to (currentStatus in listOf("APPROVED", "SENT")),
        "Gönderildi" to (currentStatus == "SENT")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (label, isCompleted) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) PrimaryOrange else Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text(text = "${index + 1}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface else TextSecondary
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (steps[index + 1].second) PrimaryOrange else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun EmptyQuestionsPlaceholder(onSyncClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.QuestionAnswer,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PrimaryOrange.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Görüntülenecek Müşteri Sorusu Bulunamadı",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Trendyol mağazanızdaki yeni soruları çekmek için senkronize edebilirsiniz.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trendyol'dan Soruları Çek")
            }
        }
    }
}

