"""
FastAPI Router for AI Smart Reports, Listing Quality Audits, and Pricing Advisory (Phase 8).
"""

from typing import Optional
from fastapi import APIRouter, Depends, Query, Path
from sqlalchemy.orm import Session

from backend.app.database.session import get_db
from backend.app.schemas.reports import (
    StagnantStockReportResponse,
    ListingAuditsResponse,
    PricingAdviceRequest,
    PricingAdviceResponse,
    ContentOptimizationRequest,
    ContentOptimizationResponse,
    ExecutiveReportResponse
)
from backend.app.services.reports.ai_reports_service import AIReportsService

router = APIRouter(prefix="/reports", tags=["AI Smart Reports & Audits"])


@router.get("/stagnant-stock", response_model=StagnantStockReportResponse)
def get_stagnant_stock_report(
    min_days: int = Query(20, ge=1, le=365, description="Kaç gündür satış olmayan ürünlerin hareketsiz sayılacağı"),
    db: Session = Depends(get_db)
):
    """
    Hareketsiz ve atıl stokları analiz eder, bağlı sermaye ve AI nakit kurtarma/indirim önerilerini listeler.
    """
    return AIReportsService.analyze_stagnant_stock(db, min_days_stagnant=min_days)


@router.get("/listing-audits", response_model=ListingAuditsResponse)
def get_listing_quality_audits(
    db: Session = Depends(get_db)
):
    """
    Tüm ürün ilanlarını Trendyol kalite ve SEO kriterlerine göre denetler, 0-100 puan ve eksiklikleri döner.
    """
    return AIReportsService.audit_product_listings(db)


@router.post("/pricing-advice/{product_id}", response_model=PricingAdviceResponse)
def get_pricing_advice(
    product_id: int = Path(..., description="Fiyat analizi yapılacak ürün ID'si"),
    request: PricingAdviceRequest = PricingAdviceRequest(),
    db: Session = Depends(get_db)
):
    """
    Seçilen ürün için Trendyol komisyonu (%20), kargo, KDV ve maliyetleri dikkate alarak başa-baş taban fiyat ve tavsiye fiyatı hesaplar.
    """
    return AIReportsService.generate_pricing_advice(db, product_id=product_id, req=request)


@router.post("/optimize-content/{product_id}", response_model=ContentOptimizationResponse)
def optimize_product_content(
    product_id: int = Path(..., description="İçeriği optimize edilecek ürün ID'si"),
    request: ContentOptimizationRequest = ContentOptimizationRequest(),
    db: Session = Depends(get_db)
):
    """
    Ürün için SEO uyumlu başlık, maddeli teknik vurgular ve zengin açıklama taslağı üretir.
    """
    return AIReportsService.optimize_product_content(db, product_id=product_id, req=request)


@router.get("/executive-summary", response_model=ExecutiveReportResponse)
def get_executive_summary(
    db: Session = Depends(get_db)
):
    """
    Genel işletme performansı, net kâr marjı, stok riski ve stratejik AI eylem planını içeren yönetici özetini sunar.
    """
    return AIReportsService.generate_executive_summary(db)
