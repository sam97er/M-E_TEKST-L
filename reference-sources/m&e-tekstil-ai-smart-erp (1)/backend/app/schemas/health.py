"""
Health and System Diagnostic Schemas.
"""

from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


class DatabaseHealth(BaseModel):
    connected: bool
    dialect: str
    tables_count: int
    latency_ms: float
    error: Optional[str] = None


class AIProviderHealth(BaseModel):
    slot: int
    name: str
    model: str
    enabled: bool
    key_configured: bool
    masked_key: str
    assigned_tasks: List[str]


class TrendyolHealth(BaseModel):
    configured: bool
    supplier_id: Optional[str] = None
    api_key_configured: bool


class TelegramHealth(BaseModel):
    enabled: bool
    configured: bool


class HealthResponse(BaseModel):
    status: str = Field(..., description="Overall health status: healthy, degraded, or unhealthy")
    app_name: str
    app_env: str
    version: str = "1.0.0"
    timestamp: datetime
    database: DatabaseHealth
    ai_providers: List[AIProviderHealth]
    trendyol: TrendyolHealth
    telegram: TelegramHealth
