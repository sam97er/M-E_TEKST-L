"""
Pydantic Schemas for Customer Questions & AI Reply Workflow (Phase 5).
Strict human approval cycle: NEW -> DRAFT_GENERATED -> APPROVED -> SENT
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, ConfigDict


class AIDraftDto(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    question_id: int
    provider_name: str
    model_used: str
    raw_prompt: Optional[str] = None
    generated_answer: str
    user_edited_answer: Optional[str] = None
    final_answer: str
    status: str  # DRAFT, APPROVED, SENT, REJECTED
    approved_by: Optional[str] = None
    approved_at: Optional[datetime] = None
    sent_at: Optional[datetime] = None
    error_message: Optional[str] = None
    created_at: Optional[datetime] = None


class CustomerQuestionDto(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    trendyol_question_id: str
    customer_name: str
    question_text: str
    status: str  # NEW, DRAFT_GENERATED, NEEDS_REVIEW, APPROVED, SENT, REJECTED
    trendyol_created_at: Optional[datetime] = None
    created_at: Optional[datetime] = None
    product_id: Optional[int] = None
    product_title: Optional[str] = None
    product_code: Optional[str] = None
    product_category: Optional[str] = None
    drafts: List[AIDraftDto] = Field(default_factory=list)


class CustomerQuestionsSummaryDto(BaseModel):
    total_count: int
    new_count: int
    draft_generated_count: int
    approved_count: int
    sent_count: int
    rejected_count: int


class GenerateDraftRequest(BaseModel):
    provider_slot: Optional[int] = Field(None, description="1 (Gemini), 2 (OpenAI), 3 (Custom), or null for auto")
    tone: Optional[str] = Field("KURUMSAL", description="KURUMSAL, SAMIMI, KISA_VE_OZ")
    additional_instructions: Optional[str] = Field(None, description="Ek talimatlar (örn: Beden tablosu detayı ver)")


class EditDraftRequest(BaseModel):
    edited_answer: str = Field(..., min_length=2, description="Kullanıcı tarafından düzenlenen cevap metni")


class ApproveDraftRequest(BaseModel):
    operator_name: str = Field("OPERATOR", description="Onaylayan operatör kullanıcı adı")


class SendReplyRequest(BaseModel):
    draft_id: Optional[int] = Field(None, description="Gönderilecek onaylı taslak ID'si (boşsa en son onaylı taslak)")
    operator_name: str = Field("OPERATOR", description="Gönderimi tetikleyen operatör")


class RejectDraftRequest(BaseModel):
    reason: Optional[str] = Field(None, description="Reddetme sebebi")


class QuestionActionResponse(BaseModel):
    success: bool
    status: str
    message: str
    question_id: int
    draft_id: Optional[int] = None
    details: Optional[Dict[str, Any]] = None
