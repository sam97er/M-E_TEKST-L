"""
Base model and common mixins for SQLAlchemy models.
"""

from datetime import datetime
from sqlalchemy import Column, DateTime, Integer
from backend.app.database.session import Base


class TimestampMixin:
    """Provides created_at and updated_at datetime stamps."""
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
