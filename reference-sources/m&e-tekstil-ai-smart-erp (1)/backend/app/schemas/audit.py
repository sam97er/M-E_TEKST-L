"""
Pydantic schemas for System Audit Logs (Phase 7).
"""

from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel


class AuditLogItem(BaseModel):
    id: int
    event_type: str
    entity_name: str
    entity_id: Optional[str] = None
    action: str
    details: Optional[str] = None
    actor: str
    created_at: datetime

    class Config:
        from_attributes = True


class AuditLogListResponse(BaseModel):
    total: int
    logs: List[AuditLogItem]
