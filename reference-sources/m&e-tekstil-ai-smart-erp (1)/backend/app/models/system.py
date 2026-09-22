"""
System Models: Audit Log, App Settings, Sync Runs, Telegram Events, AI Reports.
"""

from datetime import datetime
from sqlalchemy import Column, Integer, String, Text, DateTime, Boolean, Index
from backend.app.database.session import Base
from backend.app.models.base import TimestampMixin


class AuditLog(Base):
    """Immutable audit trail for every critical operation."""
    __tablename__ = "audit_logs"

    id = Column(Integer, primary_key=True, index=True)
    event_type = Column(String(50), nullable=False, index=True)  # INVENTORY_CHANGE, AI_REPLY_APPROVED, SYNC_EVENT, SETTINGS_UPDATE
    entity_name = Column(String(50), nullable=False)             # Product, InventoryBalance, AIDraft, etc.
    entity_id = Column(String(100), nullable=True, index=True)
    action = Column(String(50), nullable=False)                  # CREATE, UPDATE, DELETE, APPROVE, SEND, ERROR
    details = Column(Text, nullable=True)                        # JSON / text details
    actor = Column(String(100), default="SYSTEM", nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)


class AppSetting(Base, TimestampMixin):
    """Persistent dynamic key-value settings stored locally."""
    __tablename__ = "app_settings"

    id = Column(Integer, primary_key=True, index=True)
    key = Column(String(100), unique=True, nullable=False, index=True)
    value = Column(Text, nullable=False)
    description = Column(String(255), nullable=True)


class SyncRun(Base):
    """Execution log for Trendyol and AI batch sync tasks."""
    __tablename__ = "sync_runs"

    id = Column(Integer, primary_key=True, index=True)
    sync_type = Column(String(50), nullable=False, index=True)  # PRODUCTS, ORDERS, QUESTIONS, INVENTORY
    status = Column(String(50), nullable=False)                 # STARTED, SUCCESS, PARTIAL, FAILED
    items_processed = Column(Integer, default=0, nullable=False)
    error_details = Column(Text, nullable=True)
    started_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    finished_at = Column(DateTime, nullable=True)


class TelegramEvent(Base, TimestampMixin):
    """Queue and history of Telegram notifications."""
    __tablename__ = "telegram_events"

    id = Column(Integer, primary_key=True, index=True)
    event_type = Column(String(50), nullable=False, index=True)  # NEW_ORDER, LOW_STOCK, NEW_QUESTION, SYSTEM_ERROR
    message = Column(Text, nullable=False)
    status = Column(String(50), default="PENDING", nullable=False)  # PENDING, SENT, FAILED
    retry_count = Column(Integer, default=0, nullable=False)
    sent_at = Column(DateTime, nullable=True)
    error_message = Column(Text, nullable=True)


class AIReport(Base, TimestampMixin):
    """Structured AI business intelligence and diagnostic reports."""
    __tablename__ = "ai_reports"

    id = Column(Integer, primary_key=True, index=True)
    report_type = Column(String(50), nullable=False, index=True)  # DAILY_SUMMARY, STAGNANT_STOCK, PRICING, IMAGE_QUALITY
    title = Column(String(255), nullable=False)
    summary = Column(Text, nullable=False)
    recommendations_json = Column(Text, nullable=False)  # Stored JSON array of action recommendations
    status = Column(String(50), default="NEW", nullable=False)  # NEW, REVIEWED, ACTIONED
