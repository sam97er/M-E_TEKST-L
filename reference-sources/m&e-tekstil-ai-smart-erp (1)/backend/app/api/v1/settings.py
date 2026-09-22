"""
API Router for General ERP Settings, Secret Management, and External Integrations Status.
"""

from typing import Dict, Any
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from backend.app.config import settings
from backend.app.database.session import get_db
from backend.app.models.system import AppSetting, AuditLog
from backend.app.schemas.settings import (
    GeneralSettingsResponse,
    AppSettingItem,
    AppSettingUpdate,
)
from backend.app.services.ai.manager import AIProviderManager

router = APIRouter(prefix="/settings", tags=["Settings"])


@router.get("", response_model=GeneralSettingsResponse)
def get_general_settings(db: Session = Depends(get_db)):
    """Retrieves high-level ERP settings, integration statuses, and configuration flags."""
    db_settings = db.query(AppSetting).all()
    setting_items = [
        AppSettingItem(key=s.key, value=s.value, description=s.description)
        for s in db_settings
    ]

    active_ai_count = 0
    for slot in [1, 2, 3]:
        cfg = AIProviderManager.get_slot_config(slot, db)
        if cfg.get("is_enabled"):
            active_ai_count += 1

    return GeneralSettingsResponse(
        app_name=settings.APP_NAME,
        app_env=settings.APP_ENV,
        database_url=settings.DATABASE_URL,
        trendyol_configured=bool(settings.TRENDYOL_API_KEY and settings.TRENDYOL_API_SECRET and settings.TRENDYOL_SUPPLIER_ID),
        trendyol_supplier_id=settings.TRENDYOL_SUPPLIER_ID if settings.TRENDYOL_SUPPLIER_ID else None,
        telegram_enabled=settings.TELEGRAM_ENABLED,
        telegram_configured=bool(settings.TELEGRAM_BOT_TOKEN and settings.TELEGRAM_CHAT_ID),
        active_ai_slots_count=active_ai_count,
        settings=setting_items,
    )


@router.post("", response_model=AppSettingItem)
def update_or_create_app_setting(
    payload: AppSettingUpdate,
    db: Session = Depends(get_db),
):
    """Sets a dynamic configuration parameter in the database with audit tracking."""
    existing = db.query(AppSetting).filter(AppSetting.key == payload.key).first()
    if existing:
        existing.value = payload.value
        if payload.description:
            existing.description = payload.description
        action = "UPDATE"
    else:
        existing = AppSetting(
            key=payload.key,
            value=payload.value,
            description=payload.description,
        )
        db.add(existing)
        action = "CREATE"

    audit = AuditLog(
        event_type="SETTINGS_UPDATE",
        entity_name="AppSetting",
        entity_id=payload.key,
        action=action,
        details=f"Ayar güncellendi: {payload.key}={payload.value}",
        actor="ADMIN",
    )
    db.add(audit)
    db.commit()
    db.refresh(existing)

    return AppSettingItem(
        key=existing.key,
        value=existing.value,
        description=existing.description,
    )


@router.post("/trendyol/test")
def test_trendyol_settings():
    """Validates whether Trendyol Supplier credentials are set properly."""
    configured = bool(settings.TRENDYOL_API_KEY and settings.TRENDYOL_API_SECRET and settings.TRENDYOL_SUPPLIER_ID)
    return {
        "configured": configured,
        "supplier_id": settings.TRENDYOL_SUPPLIER_ID or "Girilmemiş",
        "base_url": settings.TRENDYOL_BASE_URL,
        "api_key_status": settings.mask_key(settings.TRENDYOL_API_KEY) if settings.TRENDYOL_API_KEY else "Boş",
        "api_secret_status": settings.mask_key(settings.TRENDYOL_API_SECRET) if settings.TRENDYOL_API_SECRET else "Boş",
        "ready": configured,
    }


@router.post("/telegram/test")
def test_telegram_settings():
    """Validates Telegram notification setup."""
    return {
        "enabled": settings.TELEGRAM_ENABLED,
        "configured": bool(settings.TELEGRAM_BOT_TOKEN and settings.TELEGRAM_CHAT_ID),
        "bot_token_masked": settings.mask_key(settings.TELEGRAM_BOT_TOKEN),
        "chat_id_masked": settings.mask_key(settings.TELEGRAM_CHAT_ID),
    }
