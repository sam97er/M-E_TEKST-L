package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AIDraftDto(
    @Json(name = "id") val id: Int,
    @Json(name = "question_id") val questionId: Int,
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "model_used") val modelUsed: String,
    @Json(name = "raw_prompt") val rawPrompt: String? = null,
    @Json(name = "generated_answer") val generatedAnswer: String,
    @Json(name = "user_edited_answer") val userEditedAnswer: String? = null,
    @Json(name = "final_answer") val finalAnswer: String,
    @Json(name = "status") val status: String, // DRAFT, APPROVED, SENT, REJECTED
    @Json(name = "approved_by") val approvedBy: String? = null,
    @Json(name = "approved_at") val approvedAt: String? = null,
    @Json(name = "sent_at") val sentAt: String? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CustomerQuestionDto(
    @Json(name = "id") val id: Int,
    @Json(name = "trendyol_question_id") val trendyolQuestionId: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "question_text") val questionText: String,
    @Json(name = "status") val status: String, // NEW, DRAFT_GENERATED, NEEDS_REVIEW, APPROVED, SENT, REJECTED
    @Json(name = "trendyol_created_at") val trendyolCreatedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "product_id") val productId: Int? = null,
    @Json(name = "product_title") val productTitle: String? = null,
    @Json(name = "product_code") val productCode: String? = null,
    @Json(name = "product_category") val productCategory: String? = null,
    @Json(name = "drafts") val drafts: List<AIDraftDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CustomerQuestionsSummaryDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "new_count") val newCount: Int = 0,
    @Json(name = "draft_generated_count") val draftGeneratedCount: Int = 0,
    @Json(name = "approved_count") val approvedCount: Int = 0,
    @Json(name = "sent_count") val sentCount: Int = 0,
    @Json(name = "rejected_count") val rejectedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class GenerateDraftRequest(
    @Json(name = "provider_slot") val providerSlot: Int? = null,
    @Json(name = "tone") val tone: String = "KURUMSAL", // KURUMSAL, SAMIMI, KISA_VE_OZ
    @Json(name = "additional_instructions") val additionalInstructions: String? = null
)

@JsonClass(generateAdapter = true)
data class EditDraftRequest(
    @Json(name = "edited_answer") val editedAnswer: String
)

@JsonClass(generateAdapter = true)
data class ApproveDraftRequest(
    @Json(name = "operator_name") val operatorName: String = "OPERATOR"
)

@JsonClass(generateAdapter = true)
data class SendReplyRequest(
    @Json(name = "draft_id") val draftId: Int? = null,
    @Json(name = "operator_name") val operatorName: String = "OPERATOR"
)

@JsonClass(generateAdapter = true)
data class RejectDraftRequest(
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class QuestionActionResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "question_id") val questionId: Int,
    @Json(name = "draft_id") val draftId: Int? = null,
    @Json(name = "details") val details: Map<String, Any?>? = null
)
