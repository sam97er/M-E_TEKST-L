"""
AI Provider Configuration Model for 3 Independent Slots.
"""

from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, Float, DateTime, Text
from backend.app.database.session import Base
from backend.app.models.base import TimestampMixin


class AIProviderConfig(Base, TimestampMixin):
    __tablename__ = "ai_providers"

    id = Column(Integer, primary_key=True, index=True)
    slot = Column(Integer, unique=True, nullable=False, index=True)  # 1, 2, or 3
    provider_name = Column(String(50), nullable=False)  # gemini, openai, custom_llm, anthropic
    is_enabled = Column(Boolean, default=False, nullable=False)
    model = Column(String(100), nullable=False)
    base_url = Column(String(255), nullable=True)
    assigned_tasks = Column(String(255), default="", nullable=False)  # comma separated: customer_reply, image_analysis, reports, pricing
    timeout_seconds = Column(Integer, default=30, nullable=False)
    retry_limit = Column(Integer, default=2, nullable=False)
    
    last_health_status = Column(String(50), default="UNCHECKED", nullable=False)  # HEALTHY, ERROR, UNCHECKED
    last_health_check = Column(DateTime, nullable=True)
    last_error_message = Column(Text, nullable=True)
