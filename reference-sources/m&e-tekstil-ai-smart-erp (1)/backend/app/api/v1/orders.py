"""
FastAPI Endpoints for Phase 6: Orders, Sales & Net Profit Analysis.
Includes order management, financial breakdown, sales analytics, product ranking, and pricing simulator.
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from backend.app.database.session import get_db
from backend.app.schemas.order import (
    OrderDetailDto,
    OrderSummaryItemDto,
    SalesSummaryDto,
    DailySalesPointDto,
    ProductProfitRankingDto,
    ProfitSimulationRequest,
    ProfitSimulationResponse,
    OrderStatusUpdateRequest,
)
from backend.app.services.sales.sales_service import SalesService
from backend.app.services.profit.profit_service import ProfitCalculationService

router = APIRouter(prefix="/orders", tags=["Orders & Profit Analysis"])


@router.get("", response_model=List[OrderSummaryItemDto])
def list_orders(
    status: Optional[str] = Query(None, description="Order status filter (Created, Shipped, Delivered, Cancelled, Returned, ALL)"),
    search: Optional[str] = Query(None, description="Search term for order number, customer name, or city"),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    sort_by: str = Query("date_desc", description="Sort criteria: date_desc, date_asc, profit_desc, amount_desc"),
    db: Session = Depends(get_db),
):
    """
    Returns list of orders with dynamic profit breakdown and margin analysis.
    """
    return SalesService.list_orders(
        db=db,
        status=status,
        search=search,
        limit=limit,
        offset=offset,
        sort_by=sort_by,
    )


@router.get("/summary", response_model=SalesSummaryDto)
def get_sales_summary(db: Session = Depends(get_db)):
    """
    Returns aggregated sales KPIs, total revenue, commission, shipping, COGS, and overall profit margin.
    """
    return SalesService.get_sales_summary(db)


@router.get("/analytics/daily", response_model=List[DailySalesPointDto])
def get_daily_sales_analytics(
    days: int = Query(7, ge=1, le=60),
    db: Session = Depends(get_db),
):
    """
    Returns daily trend data of sales revenue, net profit, and margin percentages.
    """
    return SalesService.get_daily_sales_analytics(db=db, days=days)


@router.get("/analytics/product-ranking", response_model=List[ProductProfitRankingDto])
def get_product_profit_ranking(
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """
    Returns ranking of products by total net profit with low-margin warnings.
    """
    return SalesService.get_product_profit_ranking(db=db, limit=limit)


@router.post("/simulate", response_model=ProfitSimulationResponse)
def simulate_pricing(request: ProfitSimulationRequest):
    """
    Simulates what-if pricing scenario: calculates net profit, margin, breakeven price, and recommendations.
    """
    return ProfitCalculationService.simulate_pricing(request)


@router.get("/{order_id}", response_model=OrderDetailDto)
def get_order_detail(order_id: int, db: Session = Depends(get_db)):
    """
    Returns detailed order breakdown with itemized cost, commission, shipping, tax, and profit waterfall.
    """
    order = SalesService.get_order_detail(db=db, order_id=order_id)
    if not order:
        raise HTTPException(status_code=404, detail=f"Sipariş (ID: {order_id}) bulunamadı.")
    return order


@router.put("/{order_id}/status", response_model=OrderDetailDto)
def update_order_status(
    order_id: int,
    request: OrderStatusUpdateRequest,
    db: Session = Depends(get_db),
):
    """
    Updates status of an order and recalculates realized vs estimated profit.
    """
    updated = SalesService.update_order_status(
        db=db,
        order_id=order_id,
        new_status=request.status,
        custom_realized_profit=request.custom_realized_profit,
    )
    if not updated:
        raise HTTPException(status_code=404, detail=f"Sipariş (ID: {order_id}) bulunamadı.")
    return updated
