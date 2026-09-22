"""
Health Check and Diagnostics API Route.
"""

import time
from datetime import datetime
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from sqlalchemy import text, inspect

from backend.app.config import settings
from backend.app.database.session import get_db, engine
from backend.app.schemas.health import (
    HealthResponse,
    DatabaseHealth,
    AIProviderHealth,
    TrendyolHealth,
    TelegramHealth,
)

router = APIRouter(tags=["Health & System"])


@router.get("/health", response_model=HealthResponse, status_code=status.HTTP_200_OK)
def check_health(db: Session = Depends(get_db)):
    """
    Performs live system diagnostics:
    - Tests SQLite database connection and execution latency.
    - Inspects registered tables.
    - Inspects configuration status of the 3 AI provider slots.
    - Inspects Trendyol and Telegram integration status.
    """
    # 1. Database Health
    start_time = time.perf_counter()
    db_connected = False
    db_error = None
    tables_count = 0
    try:
        db.execute(text("SELECT 1")).scalar()
        inspector = inspect(engine)
        tables = inspector.get_table_names()
        tables_count = len(tables)
        db_connected = True
    except Exception as exc:
        db_error = str(exc)
    latency_ms = round((time.perf_counter() - start_time) * 1000, 2)

    db_health = DatabaseHealth(
        connected=db_connected,
        dialect=engine.dialect.name,
        tables_count=tables_count,
        latency_ms=latency_ms,
        error=db_error,
    )

    # 2. AI Providers Status (Slots 1, 2, 3)
    ai_list = [
        AIProviderHealth(
            slot=1,
            name=settings.AI_PROVIDER_1_NAME,
            model=settings.AI_PROVIDER_1_MODEL,
            enabled=settings.AI_PROVIDER_1_ENABLED,
            key_configured=bool(settings.AI_PROVIDER_1_KEY),
            masked_key=settings.mask_key(settings.AI_PROVIDER_1_KEY),
            assigned_tasks=[t.strip() for t in settings.AI_PROVIDER_1_TASKS.split(",") if t.strip()],
        ),
        AIProviderHealth(
            slot=2,
            name=settings.AI_PROVIDER_2_NAME,
            model=settings.AI_PROVIDER_2_MODEL,
            enabled=settings.AI_PROVIDER_2_ENABLED,
            key_configured=bool(settings.AI_PROVIDER_2_KEY),
            masked_key=settings.mask_key(settings.AI_PROVIDER_2_KEY),
            assigned_tasks=[t.strip() for t in settings.AI_PROVIDER_2_TASKS.split(",") if t.strip()],
        ),
        AIProviderHealth(
            slot=3,
            name=settings.AI_PROVIDER_3_NAME,
            model=settings.AI_PROVIDER_3_MODEL,
            enabled=settings.AI_PROVIDER_3_ENABLED,
            key_configured=bool(settings.AI_PROVIDER_3_KEY),
            masked_key=settings.mask_key(settings.AI_PROVIDER_3_KEY),
            assigned_tasks=[t.strip() for t in settings.AI_PROVIDER_3_TASKS.split(",") if t.strip()],
        ),
    ]

    # 3. Trendyol Health
    trendyol_health = TrendyolHealth(
        configured=bool(settings.TRENDYOL_SUPPLIER_ID and settings.TRENDYOL_API_KEY and settings.TRENDYOL_API_SECRET),
        supplier_id=settings.TRENDYOL_SUPPLIER_ID,
        api_key_configured=bool(settings.TRENDYOL_API_KEY),
    )

    # 4. Telegram Health
    telegram_health = TelegramHealth(
        enabled=settings.TELEGRAM_ENABLED,
        configured=bool(settings.TELEGRAM_BOT_TOKEN and settings.TELEGRAM_CHAT_ID),
    )

    # Overall system health computation
    overall_status = "healthy" if db_connected else "unhealthy"

    return HealthResponse(
        status=overall_status,
        app_name=settings.APP_NAME,
        app_env=settings.APP_ENV,
        version="1.0.0",
        timestamp=datetime.utcnow(),
        database=db_health,
        ai_providers=ai_list,
        trendyol=trendyol_health,
        telegram=telegram_health,
    )
