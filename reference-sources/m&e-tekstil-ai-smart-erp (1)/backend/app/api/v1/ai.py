"""
API Router for AI Providers, Slot Management, Test Connections, and Task Execution.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from backend.app.config import settings
from backend.app.database.session import get_db
from backend.app.models.ai_provider import AIProviderConfig
from backend.app.models.system import AuditLog
from backend.app.services.ai.manager import AIProviderManager
from backend.app.schemas.ai import (
    AIProviderSlotResponse,
    AIProviderConfigUpdate,
    TestConnectionResponse,
    AIGenerateRequest,
    AIImageAnalyzeRequest,
)
from backend.app.services.ai.base import AIGenerationResult, AIImageAnalysisResult

router = APIRouter(prefix="/ai", tags=["AI Providers"])


@router.get("/providers", response_model=List[AIProviderSlotResponse])
def get_all_ai_providers(db: Session = Depends(get_db)):
    """Lists all 3 AI Provider slots with masked credentials and real health status."""
    results = []
    for slot in [1, 2, 3]:
        cfg = AIProviderManager.get_slot_config(slot, db)
        masked = settings.mask_key(cfg.get("api_key"))
        results.append(
            AIProviderSlotResponse(
                slot=slot,
                provider_name=cfg["provider_name"],
                model=cfg["model"],
                is_enabled=cfg["is_enabled"],
                base_url=cfg.get("base_url"),
                assigned_tasks=cfg.get("assigned_tasks", []),
                timeout_seconds=cfg["timeout_seconds"],
                masked_key=masked,
                key_configured=bool(cfg.get("api_key")),
                last_health_status=cfg.get("last_health_status", "UNCHECKED"),
            )
        )
    return results


@router.get("/providers/{slot}", response_model=AIProviderSlotResponse)
def get_ai_provider_slot(slot: int, db: Session = Depends(get_db)):
    if slot not in [1, 2, 3]:
        raise HTTPException(status_code=400, detail="Geçersiz slot numarası. Sadece 1, 2 veya 3 desteklenir.")

    cfg = AIProviderManager.get_slot_config(slot, db)
    masked = settings.mask_key(cfg.get("api_key"))
    return AIProviderSlotResponse(
        slot=slot,
        provider_name=cfg["provider_name"],
        model=cfg["model"],
        is_enabled=cfg["is_enabled"],
        base_url=cfg.get("base_url"),
        assigned_tasks=cfg.get("assigned_tasks", []),
        timeout_seconds=cfg["timeout_seconds"],
        masked_key=masked,
        key_configured=bool(cfg.get("api_key")),
        last_health_status=cfg.get("last_health_status", "UNCHECKED"),
    )


@router.put("/providers/{slot}", response_model=AIProviderSlotResponse)
def update_ai_provider_slot(
    slot: int,
    payload: AIProviderConfigUpdate,
    db: Session = Depends(get_db),
):
    """Updates configuration for a specific AI Provider slot and persists in database."""
    if slot not in [1, 2, 3]:
        raise HTTPException(status_code=400, detail="Geçersiz slot numarası.")

    db_record = db.query(AIProviderConfig).filter(AIProviderConfig.slot == slot).first()
    if not db_record:
        # Create record from current fallback
        fallback = AIProviderManager.get_slot_config(slot, db)
        db_record = AIProviderConfig(
            slot=slot,
            provider_name=payload.provider_name or fallback["provider_name"],
            model=payload.model or fallback["model"],
            is_enabled=payload.is_enabled if payload.is_enabled is not None else fallback["is_enabled"],
            base_url=payload.base_url or fallback.get("base_url"),
            assigned_tasks=",".join(payload.assigned_tasks) if payload.assigned_tasks is not None else ",".join(fallback.get("assigned_tasks", [])),
            timeout_seconds=payload.timeout_seconds or fallback["timeout_seconds"],
        )
        db.add(db_record)
    else:
        if payload.provider_name is not None:
            db_record.provider_name = payload.provider_name
        if payload.model is not None:
            db_record.model = payload.model
        if payload.is_enabled is not None:
            db_record.is_enabled = payload.is_enabled
        if payload.base_url is not None:
            db_record.base_url = payload.base_url
        if payload.assigned_tasks is not None:
            db_record.assigned_tasks = ",".join(payload.assigned_tasks)
        if payload.timeout_seconds is not None:
            db_record.timeout_seconds = payload.timeout_seconds

    # Audit log entry (NEVER log the actual API key)
    audit = AuditLog(
        event_type="SETTINGS_UPDATE",
        entity_name="AIProviderConfig",
        entity_id=str(slot),
        action="UPDATE",
        details=f"Slot {slot} güncellendi: provider={db_record.provider_name}, model={db_record.model}, enabled={db_record.is_enabled}",
        actor="ADMIN",
    )
    db.add(audit)
    db.commit()
    db.refresh(db_record)

    cfg = AIProviderManager.get_slot_config(slot, db)
    return AIProviderSlotResponse(
        slot=slot,
        provider_name=db_record.provider_name,
        model=db_record.model,
        is_enabled=db_record.is_enabled,
        base_url=db_record.base_url,
        assigned_tasks=[t.strip() for t in db_record.assigned_tasks.split(",") if t.strip()],
        timeout_seconds=db_record.timeout_seconds,
        masked_key=settings.mask_key(cfg.get("api_key")),
        key_configured=bool(cfg.get("api_key")),
        last_health_status=db_record.last_health_status,
    )


@router.post("/providers/{slot}/test", response_model=TestConnectionResponse)
async def test_provider_connection(slot: int, db: Session = Depends(get_db)):
    """Executes a live test ping against the AI provider configured in slot 1, 2, or 3."""
    if slot not in [1, 2, 3]:
        raise HTTPException(status_code=400, detail="Geçersiz slot numarası.")

    res = await AIProviderManager.test_slot_connection(slot, db)
    return TestConnectionResponse(
        success=res.get("success", False),
        provider=res.get("provider", "unknown"),
        model=res.get("model", "unknown"),
        reply=res.get("reply"),
        error=res.get("error"),
        latency_ms=res.get("latency_ms", 0.0),
    )


@router.post("/generate", response_model=AIGenerationResult)
async def generate_ai_text(
    payload: AIGenerateRequest,
    db: Session = Depends(get_db),
):
    """Executes a task-routed text generation (e.g. Trendyol answer or report draft)."""
    return await AIProviderManager.execute_task_text(
        task_name=payload.task_name,
        prompt=payload.prompt,
        system_instruction=payload.system_instruction,
        db=db,
    )


@router.post("/analyze-image", response_model=AIImageAnalysisResult)
async def analyze_product_image(
    payload: AIImageAnalyzeRequest,
    db: Session = Depends(get_db),
):
    """Executes AI image quality and marketplace compliance inspection."""
    return await AIProviderManager.execute_image_analysis(
        image_url_or_b64=payload.image_url_or_b64,
        prompt=payload.prompt,
        db=db,
    )
