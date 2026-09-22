"""
Pydantic Schemas for Phase 6: Orders, Sales & Net Profit Analysis.
Includes comprehensive cost breakdown, commissions, taxes, margins, and profit simulator.
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, ConfigDict


class OrderItemDto(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    order_id: int
    variant_id: Optional[int] = None
    barcode: str
    product_name: str
    quantity: int
    unit_price: float
    unit_purchase_cost: float
    commission_rate: float
    vat_rate: float
    total_price: float = 0.0
    total_cost: float = 0.0
    item_gross_profit: float = 0.0


class ProfitBreakdownDto(BaseModel):
    gross_amount: float
    discount_amount: float
    net_sales_amount: float
    product_purchase_cost: float
    trendyol_commission_amount: float
    trendyol_commission_rate_avg: float
    shipping_cost: float
    service_fee: float
    tax_amount: float
    packaging_and_handling: float
    total_expenses: float
    estimated_net_profit: float
    realized_net_profit: Optional[float] = None
    profit_margin_percent: float
    return_on_cost_percent: float
    is_profitable: bool


class OrderDetailDto(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    trendyol_order_number: str
    package_id: Optional[str] = None
    customer_name: str
    city: Optional[str] = None
    status: str
    order_date: Optional[datetime] = None
    item_count: int = 0
    breakdown: ProfitBreakdownDto
    items: List[OrderItemDto] = Field(default_factory=list)


class OrderSummaryItemDto(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    trendyol_order_number: str
    customer_name: str
    city: Optional[str] = None
    status: str
    order_date: Optional[datetime] = None
    item_count: int
    net_amount: float
    estimated_profit: float
    realized_profit: Optional[float] = None
    profit_margin_percent: float


class SalesSummaryDto(BaseModel):
    total_orders_count: int = 0
    delivered_orders_count: int = 0
    cancelled_or_returned_count: int = 0
    total_gross_revenue: float = 0.0
    total_net_sales: float = 0.0
    total_product_cogs: float = 0.0
    total_commission_paid: float = 0.0
    total_shipping_paid: float = 0.0
    total_tax_paid: float = 0.0
    total_other_expenses: float = 0.0
    total_estimated_net_profit: float = 0.0
    total_realized_net_profit: float = 0.0
    overall_profit_margin_percent: float = 0.0
    average_order_value: float = 0.0
    return_rate_percent: float = 0.0


class DailySalesPointDto(BaseModel):
    date: str
    order_count: int
    gross_revenue: float
    net_profit: float
    profit_margin_percent: float


class ProductProfitRankingDto(BaseModel):
    product_id: Optional[int] = None
    product_name: str
    barcode: str
    total_units_sold: int
    total_revenue: float
    total_cost: float
    total_profit: float
    profit_margin_percent: float
    is_low_margin: bool


class ProfitSimulationRequest(BaseModel):
    sale_price: float
    purchase_cost: float
    commission_rate_percent: float = 20.0
    shipping_cost: float = 38.50
    packaging_cost: float = 12.00
    vat_rate_percent: float = 10.0
    service_fee: float = 8.49


class ProfitSimulationResponse(BaseModel):
    sale_price: float
    purchase_cost: float
    commission_amount: float
    shipping_cost: float
    service_fee: float
    tax_amount: float
    packaging_cost: float
    total_cost: float
    net_profit: float
    profit_margin_percent: float
    roi_percent: float
    breakeven_price: float
    target_price_for_20_percent_margin: float
    is_profitable: bool
    recommendation: str


class OrderStatusUpdateRequest(BaseModel):
    status: str  # Created, Picking, Invoiced, Shipped, Delivered, Cancelled, Returned
    custom_realized_profit: Optional[float] = None
