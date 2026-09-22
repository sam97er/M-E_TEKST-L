"""
API Router for Immutable System Audit Logs (Phase 7).
Tracks:
- Inventory adjustments and transfers
- AI approvals and question replies
- Trendyol sync actions and status changes
- Telegram alerts, configuration updates, and system errors
"""

from typing import Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from backend.app.database.session import get_db
from backend.app.models.system import AuditLog
from backend.app.schemas.audit import AuditLogItem, AuditLogListResponse

router = APIRouter(prefix="/audit", tags=["Audit Logs"])


@router.get("/logs", response_model=AuditLogListResponse)
def list_audit_logs(
    event_type: Optional[str] = Query(None, description="Filter by event_type e.g. TELEGRAM_SENT, INVENTORY_CHANGE"),
    entity_name: Optional[str] = Query(None, description="Filter by entity e.g. Product, Order, TelegramEvent"),
    actor: Optional[str] = Query(None, description="Filter by actor e.g. SYSTEM, USER, AI_ASSISTANT"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db)
):
    """Returns chronologically ordered system audit log entries with granular filter options."""
    query = db.query(AuditLog)

    if event_type:
        query = query.filter(AuditLog.event_type == event_type.upper())
    if entity_name:
        query = query.filter(AuditLog.entity_name == entity_name)
    if actor:
        query = query.filter(AuditLog.actor == actor.upper())

    total = query.count()
    logs = query.order_by(AuditLog.created_at.desc()).offset(offset).limit(limit).all()

    return AuditLogListResponse(
        total=total,
        logs=[AuditLogItem.model_validate(log) for log in logs]
    )
