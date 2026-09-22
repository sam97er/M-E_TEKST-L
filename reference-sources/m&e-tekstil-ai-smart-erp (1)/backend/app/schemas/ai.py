"""
Pydantic schemas for AI Providers and Generation endpoints.
"""

from typing import List, Optional
from pydantic import BaseModel, Field


class AIProviderConfigUpdate(BaseModel):
    provider_name: Optional[str] = None
    model: Optional[str] = None
    is_enabled: Optional[bool] = None
    base_url: Optional[str] = None
    assigned_tasks: Optional[List[str]] = None
    timeout_seconds: Optional[int] = Field(None, ge=5, le=120)
    api_key: Optional[str] = None  # Optional secret update


class AIProviderSlotResponse(BaseModel):
    slot: int
    provider_name: str
    model: str
    is_enabled: bool
    base_url: Optional[str] = None
    assigned_tasks: List[str]
    timeout_seconds: int
    masked_key: str
    key_configured: bool
    last_health_status: str


class TestConnectionResponse(BaseModel):
    success: bool
    provider: str
    model: str
    reply: Optional[str] = None
    error: Optional[str] = None
    latency_ms: float = 0.0


class AIGenerateRequest(BaseModel):
    task_name: str = Field(default="customer_reply", description="Assigned task type: customer_reply, daily_report, pricing_recommendation")
    prompt: str
    system_instruction: Optional[str] = None
    temperature: float = 0.7
    max_tokens: int = 1000


class AIImageAnalyzeRequest(BaseModel):
    image_url_or_b64: str
    prompt: str = Field(default="Tekstil ürün görselinin Trendyol standartlarına ve kumaş dikiş kalitesine uygunluğunu analiz et.")
