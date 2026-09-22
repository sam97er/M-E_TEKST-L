"""
API Router for Trendyol Supplier Partner API (Phase 4).
Exposes endpoints for:
- GET /api/v1/trendyol/config: Connection details & rate limits
- POST /api/v1/trendyol/config: Update credentials & mock mode
- POST /api/v1/trendyol/test: Ping & test connection
- POST /api/v1/trendyol/sync: Trigger real-time sync (orders, questions, products)
- POST /api/v1/trendyol/stock-price: Batch update stock and prices on Trendyol
- GET /api/v1/trendyol/dashboard: High-level KPI status for Android
- GET /api/v1/trendyol/orders: Synced orders
- GET /api/v1/trendyol/questions: Synced customer questions
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import desc

from backend.app.config import settings
from backend.app.database.session import get_db
from backend.app.models.system import AppSetting, AuditLog, SyncRun
from backend.app.models.order import Order
from backend.app.models.customer_question import CustomerQuestion
from backend.app.models.product import Product
from backend.app.schemas.trendyol import (
    TrendyolConfigOut,
    TrendyolConfigUpdate,
    TrendyolTestConnectionResponse,
    TrendyolStockPriceBatchRequest,
    TrendyolBatchResult,
    TrendyolSyncRequest,
    TrendyolSyncResponse,
    TrendyolOrderDto,
    TrendyolQuestionDto,
    TrendyolDashboardSummary,
)
from backend.app.services.trendyol.trendyol_service import TrendyolService, rate_limiter

router = APIRouter(prefix="/trendyol", tags=["Trendyol"])


@router.get("/config", response_model=TrendyolConfigOut)
def get_trendyol_config(db: Session = Depends(get_db)):
    """Retrieves current Trendyol API settings, masked keys, and rate limit status."""
    supplier_id, api_key, api_secret, mock_mode = TrendyolService.get_credentials(db)
    return TrendyolConfigOut(
        configured=bool(supplier_id and api_key and api_secret),
        supplier_id=supplier_id,
        base_url=settings.TRENDYOL_BASE_URL,
        api_key_masked=settings.mask_key(api_key),
        api_secret_masked=settings.mask_key(api_secret),
        mock_mode=mock_mode,
        rate_limit_remaining=rate_limiter.get_remaining(),
        rate_limit_window_seconds=rate_limiter.window_seconds,
    )


@router.post("/config", response_model=TrendyolConfigOut)
def update_trendyol_config(payload: TrendyolConfigUpdate, db: Session = Depends(get_db)):
    """Updates Trendyol Supplier ID, API Key, Secret, and Mock mode in database AppSetting."""
    keys_map = {
        "trendyol_supplier_id": payload.supplier_id,
        "trendyol_api_key": payload.api_key,
        "trendyol_api_secret": payload.api_secret,
        "trendyol_mock_mode": "true" if payload.mock_mode else "false",
    }
    for k, val in keys_map.items():
        item = db.query(AppSetting).filter(AppSetting.key == k).first()
        if item:
            item.value = val
        else:
            item = AppSetting(key=k, value=val, description="Trendyol API Entegrasyon Ayarı")
            db.add(item)

    audit = AuditLog(
        event_type="SETTINGS_UPDATE",
        entity_name="TrendyolConfig",
        entity_id=payload.supplier_id,
        action="UPDATE",
        details="Trendyol API kimlik bilgileri güncellendi.",
        actor="ADMIN",
    )
    db.add(audit)
    db.commit()

    return get_trendyol_config(db)


@router.post("/test", response_model=TrendyolTestConnectionResponse)
def test_trendyol_connection(db: Session = Depends(get_db)):
    """Pings Trendyol Supplier Partner API and measures latency or diagnose auth errors."""
    return TrendyolService.test_connection(db)


@router.post("/sync", response_model=TrendyolSyncResponse)
def run_trendyol_sync(payload: TrendyolSyncRequest, db: Session = Depends(get_db)):
    """Executes a synchronization cycle for Orders, Questions, or Products."""
    return TrendyolService.run_full_sync(db, sync_type=payload.sync_type)


@router.post("/stock-price", response_model=TrendyolBatchResult)
def update_trendyol_stock_price(
    payload: TrendyolStockPriceBatchRequest, db: Session = Depends(get_db)
):
    """Submits batch price and stock updates to Trendyol."""
    return TrendyolService.update_stock_and_price_batch(payload.items, db)


@router.get("/dashboard", response_model=TrendyolDashboardSummary)
def get_trendyol_dashboard(db: Session = Depends(get_db)):
    """Returns high-level statistics of Trendyol synchronization for Android screens."""
    supplier_id, api_key, api_secret, mock_mode = TrendyolService.get_credentials(db)
    last_sync = db.query(SyncRun).order_by(desc(SyncRun.started_at)).first()

    total_orders = db.query(Order).count()
    total_questions = db.query(CustomerQuestion).count()
    total_products = db.query(Product).count()

    rem = rate_limiter.get_remaining()
    rate_info = f"{rem}/50 İstek (Dakikalık Havuz)"

    return TrendyolDashboardSummary(
        is_connected=bool(supplier_id and api_key and api_secret),
        supplier_id=supplier_id,
        mock_mode=mock_mode,
        last_sync_time=last_sync.started_at.strftime("%d.%m.%Y %H:%M") if last_sync else None,
        last_sync_status=last_sync.status if last_sync else "Henüz Yok",
        total_orders_synced=total_orders,
        total_questions_synced=total_questions,
        active_trendyol_products_count=total_products,
        rate_limit_info=rate_info,
    )


@router.get("/orders", response_model=List[TrendyolOrderDto])
def list_synced_orders(limit: int = 50, db: Session = Depends(get_db)):
    """Lists orders synced from Trendyol."""
    orders = db.query(Order).order_by(desc(Order.order_date)).limit(limit).all()
    results = []
    for ord in orders:
        results.append(
            TrendyolOrderDto(
                order_number=ord.trendyol_order_number,
                customer_name=f"{ord.customer_first_name or ''} {ord.customer_last_name or ''}".strip() or "Trendyol Müşterisi",
                city=ord.city or "Bilinmiyor",
                status=ord.status,
                total_gross_amount=ord.total_gross_amount,
                net_amount=ord.net_amount,
                estimated_profit=ord.estimated_profit,
                order_date=ord.order_date.strftime("%d.%m.%Y %H:%M") if ord.order_date else "",
                item_count=len(ord.items),
            )
        )
    return results


@router.get("/questions", response_model=List[TrendyolQuestionDto])
def list_synced_questions(limit: int = 50, db: Session = Depends(get_db)):
    """Lists customer questions synced from Trendyol."""
    questions = db.query(CustomerQuestion).order_by(desc(CustomerQuestion.created_at)).limit(limit).all()
    results = []
    for q in questions:
        p_title = q.product.title if q.product else "Tekstil Ürünü"
        results.append(
            TrendyolQuestionDto(
                question_id=q.trendyol_question_id,
                product_title=p_title,
                customer_name=q.customer_name,
                question_text=q.question_text,
                status=q.status,
                created_at=q.trendyol_created_at.strftime("%d.%m.%Y %H:%M") if q.trendyol_created_at else None,
            )
        )
    return results
