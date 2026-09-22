"""
Pydantic schemas for App Settings and System Configuration.
"""

from typing import List, Optional
from pydantic import BaseModel


class AppSettingItem(BaseModel):
    key: str
    value: str
    description: Optional[str] = None


class AppSettingUpdate(BaseModel):
    key: str
    value: str
    description: Optional[str] = None


class GeneralSettingsResponse(BaseModel):
    app_name: str
    app_env: str
    database_url: str
    trendyol_configured: bool
    trendyol_supplier_id: Optional[str] = None
    telegram_enabled: bool
    telegram_configured: bool
    active_ai_slots_count: int
    settings: List[AppSettingItem]
