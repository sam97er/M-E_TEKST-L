"""
Pydantic schemas for AI Smart Reports, Listing Quality Audits, and Pricing Optimization (Phase 8).
"""

from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field


class StagnantProductItem(BaseModel):
    product_id: int
    variant_id: int
    product_code: str
    title: str
    color: Optional[str] = None
    size: Optional[str] = None
    barcode: str
    stock_quantity: int
    purchase_cost: float
    current_selling_price: float
    tied_capital_tl: float
    days_without_sale: int
    urgency_level: str  # LOW, MEDIUM, CRITICAL
    ai_recommendation: str
    suggested_discount_percent: float
    suggested_clearance_price: float
    projected_capital_recovered_tl: float


class StagnantStockReportResponse(BaseModel):
    total_stagnant_variants_count: int
    total_stagnant_units: int
    total_tied_capital_tl: float
    potential_cash_recovery_tl: float
    critical_items_count: int
    items: List[StagnantProductItem]


class QualityDeficiency(BaseModel):
    severity: str  # HIGH, MEDIUM, LOW
    field: str
    message: str
    suggestion: str


class ProductQualityAuditItem(BaseModel):
    product_id: int
    product_code: str
    title: str
    category_name: Optional[str] = None
    quality_score: int  # 0 to 100
    grade: str  # A, B, C, D, F
    title_quality_score: int
    description_quality_score: int
    image_quality_score: int
    has_primary_image: bool
    images_count: int
    has_fabric_composition: bool
    has_washing_instructions: bool
    has_size_chart_info: bool
    deficiencies: List[QualityDeficiency]
    ai_quick_fix_summary: str


class ListingAuditsResponse(BaseModel):
    total_audited_products: int
    average_quality_score: float
    high_quality_count: int  # score >= 80
    medium_quality_count: int  # 50 <= score < 80
    needs_improvement_count: int  # score < 50
    products: List[ProductQualityAuditItem]


class PricingAdviceRequest(BaseModel):
    target_margin_percent: Optional[float] = Field(25.0, ge=5.0, le=80.0)
    market_demand_level: Optional[str] = "NORMAL"  # LOW, NORMAL, HIGH


class PricingAdviceResponse(BaseModel):
    product_id: int
    product_code: str
    title: str
    current_selling_price: float
    unit_purchase_cost: float
    estimated_trendyol_commission_tl: float
    shipping_cost_tl: float
    tax_cost_tl: float
    packaging_cost_tl: float
    current_estimated_net_profit_tl: float
    current_profit_margin_percent: float
    break_even_minimum_price: float
    suggested_optimal_price: float
    suggested_optimal_margin_percent: float
    suggested_flash_deal_price: float
    ai_pricing_strategy: str
    reasoning: List[str]


class ContentOptimizationRequest(BaseModel):
    focus_keywords: Optional[List[str]] = None
    target_audience: Optional[str] = "Genç & Dinamik Günlük Giyim"
    tone_of_voice: Optional[str] = "Trend, Şık ve Bilgilendirici"


class ContentOptimizationResponse(BaseModel):
    product_id: int
    product_code: str
    original_title: str
    original_description: Optional[str] = None
    optimized_title: str
    optimized_bullet_points: List[str]
    optimized_full_description: str
    suggested_tags: List[str]
    seo_score_improvement: str
    status: str = "DRAFT"  # Requires human approval before saving


class ExecutiveSummaryMetric(BaseModel):
    title: str
    value: str
    trend: str
    status_type: str  # SUCCESS, WARNING, DANGER, INFO


class ExecutiveReportResponse(BaseModel):
    report_date: str
    generated_by_model: str
    overall_health_score: int  # 0 to 100
    headline: str
    executive_summary_text: str
    key_metrics: List[ExecutiveSummaryMetric]
    top_strengths: List[str]
    critical_bottlenecks: List[str]
    recommended_next_actions: List[str]
