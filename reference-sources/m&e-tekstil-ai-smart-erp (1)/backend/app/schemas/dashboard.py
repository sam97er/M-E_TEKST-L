"""
Pydantic schemas for Consolidated Executive Dashboard (Phase 9).
"""

from typing import List, Optional
from pydantic import BaseModel


class SystemServiceStatusDto(BaseModel):
    service_name: str
    is_operational: bool
    status_label: str
    details: Optional[str] = None


class SystemPulseDto(BaseModel):
    overall_health: str  # HEALTHY, WARNING, CRITICAL
    active_ai_provider: str
    ai_model_name: str
    ai_is_active: bool
    trendyol_connected: bool
    trendyol_last_sync: Optional[str] = None
    telegram_bot_active: bool
    telegram_pending_queue_count: int
    services: List[SystemServiceStatusDto] = []


class SalesKpisDto(BaseModel):
    today_orders_count: int = 0
    today_gross_revenue_tl: float = 0.0
    today_net_profit_tl: float = 0.0
    today_profit_margin_percent: float = 0.0
    seven_day_orders_count: int = 0
    seven_day_gross_revenue_tl: float = 0.0
    seven_day_net_profit_tl: float = 0.0
    seven_day_profit_margin_percent: float = 0.0


class QuestionKpisDto(BaseModel):
    unanswered_count: int = 0
    draft_ready_for_approval_count: int = 0
    sent_today_count: int = 0
    avg_response_time_minutes: int = 0


class InventoryKpisDto(BaseModel):
    total_active_products: int = 0
    total_variant_skus: int = 0
    total_physical_units: int = 0
    out_of_stock_skus_count: int = 0
    low_stock_skus_count: int = 0
    stagnant_tied_capital_tl: float = 0.0
    stagnant_units_count: int = 0


class DashboardAlertItemDto(BaseModel):
    id: str
    alert_type: str  # CRITICAL, WARNING, INFO
    title: str
    message: str
    action_target_section: str  # QUESTIONS, ORDERS, REPORTS, PRODUCTS, SETTINGS, TELEGRAM
    action_label: str


class ActivityFeedItemDto(BaseModel):
    id: str
    activity_type: str  # ORDER, QUESTION, STOCK, AUDIT, TELEGRAM
    title: str
    description: str
    timestamp: str
    icon_hint: str
    status_tag: Optional[str] = None


class DashboardSummaryResponse(BaseModel):
    generated_at: str
    system_pulse: SystemPulseDto
    sales_kpis: SalesKpisDto
    question_kpis: QuestionKpisDto
    inventory_kpis: InventoryKpisDto
    critical_alerts: List[DashboardAlertItemDto] = []
    recent_activities: List[ActivityFeedItemDto] = []
