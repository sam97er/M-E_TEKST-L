"""
Pydantic schemas for Telegram Bot Notification system and Event Queue (Phase 7).
"""

from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field


class TelegramConfigResponse(BaseModel):
    enabled: bool
    bot_token_masked: str
    chat_id: Optional[str] = None
    is_configured: bool
    notify_new_order: bool = True
    notify_low_stock: bool = True
    notify_new_question: bool = True
    notify_daily_digest: bool = True
    notify_system_error: bool = True


class TelegramConfigUpdate(BaseModel):
    enabled: Optional[bool] = None
    bot_token: Optional[str] = None
    chat_id: Optional[str] = None
    notify_new_order: Optional[bool] = None
    notify_low_stock: Optional[bool] = None
    notify_new_question: Optional[bool] = None
    notify_daily_digest: Optional[bool] = None
    notify_system_error: Optional[bool] = None


class TelegramEventItem(BaseModel):
    id: int
    event_type: str
    message: str
    status: str
    retry_count: int
    sent_at: Optional[datetime] = None
    error_message: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


class TelegramEventsListResponse(BaseModel):
    total: int
    pending_count: int
    failed_count: int
    sent_count: int
    events: List[TelegramEventItem]


class TelegramSendTestRequest(BaseModel):
    message: Optional[str] = None
    event_type: str = "SYSTEM_TEST"


class TelegramSendTestResponse(BaseModel):
    success: bool
    event_id: int
    status: str
    message: str
    error: Optional[str] = None


class TelegramRetryResponse(BaseModel):
    retried_count: int
    success_count: int
    failed_count: int
    message: str
