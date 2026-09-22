"""
API Router for Telegram Bot Notifications, Configuration, and Event Queue Management (Phase 7).
"""

from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from datetime import datetime

from backend.app.database.session import get_db
from backend.app.models.system import TelegramEvent
from backend.app.schemas.telegram import (
    TelegramConfigResponse,
    TelegramConfigUpdate,
    TelegramEventItem,
    TelegramEventsListResponse,
    TelegramSendTestRequest,
    TelegramSendTestResponse,
    TelegramRetryResponse,
)
from backend.app.services.telegram.notifier import TelegramNotifier
from backend.app.services.sales.sales_service import SalesService

router = APIRouter(prefix="/telegram", tags=["Telegram Notifications"])


@router.get("/config", response_model=TelegramConfigResponse)
def get_telegram_config(db: Session = Depends(get_db)):
    """Retrieves current Telegram Bot configuration status and notification category flags."""
    cfg = TelegramNotifier.get_config(db)
    return TelegramConfigResponse(**cfg)


@router.put("/config", response_model=TelegramConfigResponse)
def update_telegram_config(
    payload: TelegramConfigUpdate,
    db: Session = Depends(get_db)
):
    """Updates Telegram Bot settings (Bot Token, Chat ID, and event category toggles)."""
    update_dict = payload.model_dump(exclude_unset=True)
    updated = TelegramNotifier.update_config(db, update_dict)
    return TelegramConfigResponse(**updated)


@router.get("/events", response_model=TelegramEventsListResponse)
def list_telegram_events(
    status: Optional[str] = Query(None, description="Filter by status: PENDING, SENT, FAILED, SKIPPED"),
    event_type: Optional[str] = Query(None, description="Filter by event_type: NEW_ORDER, LOW_STOCK, etc."),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db)
):
    """Lists queued and historic Telegram notification events with delivery status."""
    query = db.query(TelegramEvent)

    if status:
        query = query.filter(TelegramEvent.status == status.upper())
    if event_type:
        query = query.filter(TelegramEvent.event_type == event_type.upper())

    total = query.count()
    events = query.order_by(TelegramEvent.created_at.desc()).offset(offset).limit(limit).all()

    pending_count = db.query(TelegramEvent).filter(TelegramEvent.status == "PENDING").count()
    failed_count = db.query(TelegramEvent).filter(TelegramEvent.status == "FAILED").count()
    sent_count = db.query(TelegramEvent).filter(TelegramEvent.status == "SENT").count()

    return TelegramEventsListResponse(
        total=total,
        pending_count=pending_count,
        failed_count=failed_count,
        sent_count=sent_count,
        events=[TelegramEventItem.model_validate(e) for e in events]
    )


@router.post("/send-test", response_model=TelegramSendTestResponse)
def send_test_telegram_message(
    payload: TelegramSendTestRequest,
    db: Session = Depends(get_db)
):
    """Sends an immediate test alert to verify Telegram Bot credentials and chat connectivity."""
    test_msg = payload.message or (
        "🤖 <b>M&E Tekstil ERP Telegram Bildirim Testi</b>\n\n"
        "Bağlantı başarıyla sağlandı! Siparişler, kritik stok uyarıları ve müşteri soruları bu kanaldan iletilecektir."
    )

    event = TelegramNotifier.queue_event(
        db=db,
        event_type=payload.event_type or "SYSTEM_TEST",
        message=test_msg,
        auto_send=True
    )

    return TelegramSendTestResponse(
        success=(event.status == "SENT"),
        event_id=event.id,
        status=event.status,
        message="Test bildirimi gönderildi" if event.status == "SENT" else f"Bildirim kuyruğa alındı / durum: {event.status}",
        error=event.error_message
    )


@router.post("/retry", response_model=TelegramRetryResponse)
def retry_failed_telegram_events(
    max_retries: int = Query(5, ge=1, le=10),
    db: Session = Depends(get_db)
):
    """Triggers immediate re-attempt of all PENDING or FAILED events in the queue."""
    result = TelegramNotifier.retry_failed_events(db, max_retries=max_retries)
    return TelegramRetryResponse(
        retried_count=result["retried_count"],
        success_count=result["success_count"],
        failed_count=result["failed_count"],
        message=f"{result['retried_count']} bildirim yeniden denendi: {result['success_count']} başarılı, {result['failed_count']} başarısız."
    )


@router.post("/send-daily-digest", response_model=TelegramSendTestResponse)
def send_daily_digest_now(db: Session = Depends(get_db)):
    """Calculates today's sales and profit summary and dispatches an instant Telegram digest."""
    today_str = datetime.utcnow().strftime("%Y-%m-%d")
    summary = SalesService.get_sales_summary(db)

    event = TelegramNotifier.notify_daily_digest(
        db=db,
        date_str=today_str,
        total_orders=summary.total_orders_count,
        gross_sales=summary.total_net_sales,
        net_profit=summary.total_estimated_net_profit,
        margin_percent=summary.overall_profit_margin_percent
    )

    return TelegramSendTestResponse(
        success=(event.status == "SENT"),
        event_id=event.id,
        status=event.status,
        message="Günlük özet bildirimi iletildi" if event.status == "SENT" else f"Bildirim oluşturuldu (Durum: {event.status})",
        error=event.error_message
    )


@router.delete("/events")
def clear_telegram_events(
    older_than_days: int = Query(30, ge=0),
    status: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    """Cleans up delivered or old events from the Telegram queue."""
    query = db.query(TelegramEvent)
    if status:
        query = query.filter(TelegramEvent.status == status.upper())
    else:
        query = query.filter(TelegramEvent.status.in_(["SENT", "SKIPPED"]))

    deleted_count = query.delete(synchronize_session=False)
    db.commit()
    return {"message": f"{deleted_count} adet bildirim kaydı temizlendi."}
