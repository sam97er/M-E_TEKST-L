"""
Schemas for Trendyol Supplier Partner API Integration (Phase 4).
Covers Credentials, Connection Testing, Rate Limiting, Stock/Price updates,
Order sync, Customer Question sync, and Webhook/Event payloads.
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class TrendyolConfigOut(BaseModel):
    configured: bool
    supplier_id: Optional[str] = None
    base_url: str
    api_key_masked: str
    api_secret_masked: str
    mock_mode: bool = False
    rate_limit_remaining: int = 50
    rate_limit_window_seconds: int = 60


class TrendyolConfigUpdate(BaseModel):
    supplier_id: str
    api_key: str
    api_secret: str
    mock_mode: Optional[bool] = False


class TrendyolTestConnectionResponse(BaseModel):
    success: bool
    status_code: int
    message: str
    supplier_id: Optional[str] = None
    latency_ms: float
    is_mock: bool = False
    details: Optional[Dict[str, Any]] = None


class TrendyolStockPriceItem(BaseModel):
    barcode: str
    quantity: int
    sale_price: float
    list_price: Optional[float] = None


class TrendyolStockPriceBatchRequest(BaseModel):
    items: List[TrendyolStockPriceItem]


class TrendyolBatchResult(BaseModel):
    batch_request_id: str
    status: str  # COMPLETED, PROCESSING, FAILED
    item_count: int
    message: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class TrendyolSyncRequest(BaseModel):
    sync_type: str = Field("ALL", description="ALL, ORDERS, QUESTIONS, PRODUCTS, INVENTORY")
    force_mock: Optional[bool] = False


class TrendyolSyncResponse(BaseModel):
    sync_id: int
    sync_type: str
    status: str
    items_processed: int
    orders_synced: int = 0
    questions_synced: int = 0
    products_synced: int = 0
    duration_ms: float
    message: str
    created_at: datetime = Field(default_factory=datetime.utcnow)


class TrendyolOrderDto(BaseModel):
    order_number: str
    customer_name: str
    city: str
    status: str
    total_gross_amount: float
    net_amount: float
    estimated_profit: float
    order_date: str
    item_count: int


class TrendyolQuestionDto(BaseModel):
    question_id: str
    product_title: str
    customer_name: str
    question_text: str
    status: str
    created_at: Optional[str] = None


class TrendyolDashboardSummary(BaseModel):
    is_connected: bool
    supplier_id: Optional[str] = None
    mock_mode: bool
    last_sync_time: Optional[str] = None
    last_sync_status: Optional[str] = None
    total_orders_synced: int
    total_questions_synced: int
    active_trendyol_products_count: int
    rate_limit_info: str
