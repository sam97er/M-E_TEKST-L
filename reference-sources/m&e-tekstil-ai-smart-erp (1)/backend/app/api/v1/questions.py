"""
API Endpoints for Customer Questions & AI Response Review Workflow (Phase 5).
Guarantees human approval before any marketplace transmission.
"""

import logging
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from backend.app.database.session import get_db
from backend.app.models.customer_question import CustomerQuestion, AIDraft
from backend.app.models.product import Product
from backend.app.schemas.question import (
    CustomerQuestionDto,
    AIDraftDto,
    CustomerQuestionsSummaryDto,
    GenerateDraftRequest,
    EditDraftRequest,
    ApproveDraftRequest,
    SendReplyRequest,
    RejectDraftRequest,
    QuestionActionResponse,
)
from backend.app.services.ai.question_reply_service import QuestionReplyService
from backend.app.services.trendyol.trendyol_service import TrendyolService

logger = logging.getLogger("metekstil_api")

router = APIRouter(prefix="/questions", tags=["Customer Questions & AI Reply Engine"])


def _map_question_to_dto(q: CustomerQuestion) -> CustomerQuestionDto:
    """Helper to enrich CustomerQuestion with product metadata and drafts."""
    draft_dtos = [
        AIDraftDto(
            id=d.id,
            question_id=d.question_id,
            provider_name=d.provider_name,
            model_used=d.model_used,
            raw_prompt=d.raw_prompt,
            generated_answer=d.generated_answer,
            user_edited_answer=d.user_edited_answer,
            final_answer=d.final_answer,
            status=d.status,
            approved_by=d.approved_by,
            approved_at=d.approved_at,
            sent_at=d.sent_at,
            error_message=d.error_message,
            created_at=d.created_at,
        )
        for d in q.drafts
    ]

    prod = q.product
    return CustomerQuestionDto(
        id=q.id,
        trendyol_question_id=q.trendyol_question_id,
        customer_name=q.customer_name,
        question_text=q.question_text,
        status=q.status,
        trendyol_created_at=q.trendyol_created_at,
        created_at=q.created_at,
        product_id=q.product_id,
        product_title=prod.title if prod else None,
        product_code=prod.product_code if prod else None,
        product_category=prod.category_name if prod else None,
        drafts=draft_dtos,
    )


@router.get("", response_model=List[CustomerQuestionDto])
def list_questions(
    status: Optional[str] = Query(None, description="NEW, DRAFT_GENERATED, APPROVED, SENT, REJECTED"),
    search: Optional[str] = Query(None, description="Müşteri adı veya soru metninde arama"),
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db),
):
    """Lists customer questions with optional status filter and search query."""
    query = db.query(CustomerQuestion)

    if status and status.upper() != "ALL":
        query = query.filter(CustomerQuestion.status == status.upper())

    if search:
        search_pattern = f"%{search.strip()}%"
        query = query.filter(
            (CustomerQuestion.customer_name.ilike(search_pattern)) |
            (CustomerQuestion.question_text.ilike(search_pattern)) |
            (CustomerQuestion.trendyol_question_id.ilike(search_pattern))
        )

    questions = query.order_by(CustomerQuestion.id.desc()).offset(offset).limit(limit).all()
    return [_map_question_to_dto(q) for q in questions]


@router.get("/summary", response_model=CustomerQuestionsSummaryDto)
def get_questions_summary(db: Session = Depends(get_db)):
    """Provides KPI counts for customer questions in various workflow states."""
    total = db.query(CustomerQuestion).count()
    new_q = db.query(CustomerQuestion).filter(CustomerQuestion.status == "NEW").count()
    draft_q = db.query(CustomerQuestion).filter(CustomerQuestion.status == "DRAFT_GENERATED").count()
    app_q = db.query(CustomerQuestion).filter(CustomerQuestion.status == "APPROVED").count()
    sent_q = db.query(CustomerQuestion).filter(CustomerQuestion.status == "SENT").count()
    rej_q = db.query(CustomerQuestion).filter(CustomerQuestion.status == "REJECTED").count()

    return CustomerQuestionsSummaryDto(
        total_count=total,
        new_count=new_q,
        draft_generated_count=draft_q,
        approved_count=app_q,
        sent_count=sent_q,
        rejected_count=rej_q,
    )


@router.post("/sync", response_model=QuestionActionResponse)
def sync_questions_from_trendyol(db: Session = Depends(get_db)):
    """Triggers immediate synchronization of incoming customer questions from Trendyol."""
    try:
        synced_count = TrendyolService.sync_customer_questions(db)
        return QuestionActionResponse(
            success=True,
            status="SUCCESS",
            message=f"{synced_count} yeni Trendyol sorusu çekildi ve veritabanına işlendi.",
            question_id=0,
            details={"synced_count": synced_count},
        )
    except Exception as e:
        logger.exception("Failed to sync questions")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Soru senkronizasyonu hatası: {str(e)}",
        )


@router.get("/{question_id}", response_model=CustomerQuestionDto)
def get_question_detail(question_id: int, db: Session = Depends(get_db)):
    """Fetches full question details including drafts and linked product context."""
    question = db.query(CustomerQuestion).filter(CustomerQuestion.id == question_id).first()
    if not question:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Müşteri sorusu bulunamadı.")
    return _map_question_to_dto(question)


@router.post("/{question_id}/draft", response_model=AIDraftDto)
async def generate_draft(
    question_id: int,
    request: GenerateDraftRequest,
    db: Session = Depends(get_db),
):
    """
    Generates an AI draft response using the configured AI provider.
    Saves draft in DRAFT state. Zero automatic transmission.
    """
    try:
        draft = await QuestionReplyService.generate_draft_for_question(
            question_id=question_id,
            provider_slot=request.provider_slot,
            tone=request.tone or "KURUMSAL",
            additional_instructions=request.additional_instructions,
            db=db,
        )
        return AIDraftDto(
            id=draft.id,
            question_id=draft.question_id,
            provider_name=draft.provider_name,
            model_used=draft.model_used,
            raw_prompt=draft.raw_prompt,
            generated_answer=draft.generated_answer,
            user_edited_answer=draft.user_edited_answer,
            final_answer=draft.final_answer,
            status=draft.status,
            approved_by=draft.approved_by,
            approved_at=draft.approved_at,
            sent_at=draft.sent_at,
            error_message=draft.error_message,
            created_at=draft.created_at,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        logger.exception("Draft generation failed")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))


@router.put("/drafts/{draft_id}", response_model=AIDraftDto)
def edit_draft(
    draft_id: int,
    request: EditDraftRequest,
    db: Session = Depends(get_db),
):
    """Enables human operator to refine or edit the draft before approval."""
    try:
        draft = QuestionReplyService.edit_draft(draft_id, request.edited_answer, db)
        return AIDraftDto(
            id=draft.id,
            question_id=draft.question_id,
            provider_name=draft.provider_name,
            model_used=draft.model_used,
            raw_prompt=draft.raw_prompt,
            generated_answer=draft.generated_answer,
            user_edited_answer=draft.user_edited_answer,
            final_answer=draft.final_answer,
            status=draft.status,
            approved_by=draft.approved_by,
            approved_at=draft.approved_at,
            sent_at=draft.sent_at,
            error_message=draft.error_message,
            created_at=draft.created_at,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/drafts/{draft_id}/approve", response_model=AIDraftDto)
def approve_draft(
    draft_id: int,
    request: ApproveDraftRequest,
    db: Session = Depends(get_db),
):
    """
    Human verification step: Marks draft as APPROVED by operator.
    Only approved drafts are eligible for transmission.
    """
    try:
        draft = QuestionReplyService.approve_draft(draft_id, request.operator_name, db)
        return AIDraftDto(
            id=draft.id,
            question_id=draft.question_id,
            provider_name=draft.provider_name,
            model_used=draft.model_used,
            raw_prompt=draft.raw_prompt,
            generated_answer=draft.generated_answer,
            user_edited_answer=draft.user_edited_answer,
            final_answer=draft.final_answer,
            status=draft.status,
            approved_by=draft.approved_by,
            approved_at=draft.approved_at,
            sent_at=draft.sent_at,
            error_message=draft.error_message,
            created_at=draft.created_at,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/drafts/{draft_id}/reject", response_model=AIDraftDto)
def reject_draft(
    draft_id: int,
    request: RejectDraftRequest,
    db: Session = Depends(get_db),
):
    """Rejects a draft response."""
    try:
        draft = QuestionReplyService.reject_draft(draft_id, request.reason, db)
        return AIDraftDto(
            id=draft.id,
            question_id=draft.question_id,
            provider_name=draft.provider_name,
            model_used=draft.model_used,
            raw_prompt=draft.raw_prompt,
            generated_answer=draft.generated_answer,
            user_edited_answer=draft.user_edited_answer,
            final_answer=draft.final_answer,
            status=draft.status,
            approved_by=draft.approved_by,
            approved_at=draft.approved_at,
            sent_at=draft.sent_at,
            error_message=draft.error_message,
            created_at=draft.created_at,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/{question_id}/send", response_model=QuestionActionResponse)
def send_approved_reply(
    question_id: int,
    request: SendReplyRequest,
    db: Session = Depends(get_db),
):
    """
    Sends the human-verified, approved response to Trendyol.
    Fails if the draft has NOT been approved.
    """
    try:
        result = QuestionReplyService.send_reply_to_trendyol(
            question_id=question_id,
            draft_id=request.draft_id,
            operator_name=request.operator_name,
            db=db,
        )
        return QuestionActionResponse(
            success=result["success"],
            status=result["status"],
            message=result["message"],
            question_id=result["question_id"],
            draft_id=result.get("draft_id"),
            details=result.get("details"),
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception as e:
        logger.exception("Send reply threw exception")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(e))
