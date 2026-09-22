"""
Unit & Integration Tests for Consolidated Executive Dashboard (Phase 9).
"""

import pytest
from datetime import datetime, timedelta
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from backend.app.models.product import Product, ProductVariant
from backend.app.models.inventory import InventoryBalance, Warehouse
from backend.app.models.customer_question import CustomerQuestion, AIDraft
from backend.app.models.order import Order
from backend.app.models.system import AppSetting, TelegramEvent, AuditLog


@pytest.fixture(scope="function")
def seed_dashboard_data(db_session: Session):
    wh = db_session.query(Warehouse).filter(Warehouse.code == "MAIN").first()
    if not wh:
        wh = Warehouse(code="MAIN", name="Ana Depo", is_default=True)
        db_session.add(wh)
        db_session.flush()

    # Settings
    db_session.add(AppSetting(key="trendyol_api_key", value="test_ty_key", description="Trendyol API Key"))
    db_session.add(AppSetting(key="telegram_bot_token", value="123456:ABC-DEF", description="Telegram Bot Token"))
    db_session.flush()

    # Product & Variants
    p1 = Product(
        product_code="DASH-TSH-01",
        title="Oversize Erkek T-Shirt",
        brand="M&E Tekstil",
        category_name="T-Shirt",
        selling_price=400.0,
        status="ACTIVE"
    )
    db_session.add(p1)
    db_session.flush()

    v1 = ProductVariant(
        product_id=p1.id,
        barcode="8680000111111",
        color="Siyah",
        size="L",
        purchase_cost=120.0,
        selling_price=400.0,
        is_active=True,
        created_at=datetime.utcnow() - timedelta(days=25)
    )
    v2 = ProductVariant(
        product_id=p1.id,
        barcode="8680000111128",
        color="Beyaz",
        size="M",
        purchase_cost=120.0,
        selling_price=400.0,
        is_active=True,
        created_at=datetime.utcnow() - timedelta(days=5)
    )
    db_session.add_all([v1, v2])
    db_session.flush()

    # Balances: v1 has 20 (stagnant), v2 has 0 (out of stock)
    b1 = InventoryBalance(variant_id=v1.id, warehouse_id=wh.id, quantity=20)
    b2 = InventoryBalance(variant_id=v2.id, warehouse_id=wh.id, quantity=0)
    db_session.add_all([b1, b2])

    # Customer Questions: 1 DRAFT_GENERATED
    q1 = CustomerQuestion(
        trendyol_question_id="TY-Q-101",
        product_id=p1.id,
        customer_name="Ayşe K.",
        question_text="Kalıbı tam mı, 1 beden büyük mü almalıyım?",
        status="DRAFT_GENERATED"
    )
    db_session.add(q1)
    db_session.flush()

    d1 = AIDraft(
        question_id=q1.id,
        provider_name="gemini",
        model_used="gemini-1.5-flash",
        generated_answer="Ürünümüz rahat kalıptır, kendi bedeninizi tercih edebilirsiniz.",
        status="DRAFT"
    )
    db_session.add(d1)

    # Orders: 1 Order today
    o1 = Order(
        trendyol_order_number="TY-ORD-2026-001",
        customer_first_name="Mehmet",
        customer_last_name="Demir",
        status="Delivered",
        total_gross_amount=400.0,
        net_amount=400.0,
        total_product_cost=120.0,
        trendyol_commission=80.0,
        shipping_cost=38.50,
        realized_profit=140.0,
        order_date=datetime.utcnow()
    )
    db_session.add(o1)

    # Audit log
    db_session.add(AuditLog(
        event_type="PRODUCT_CREATED",
        entity_name="Product",
        entity_id=str(p1.id),
        action="CREATE",
        actor="Admin App",
        details="Yeni ürün tanımlandı"
    ))

    # Telegram event
    db_session.add(TelegramEvent(
        event_type="NEW_ORDER",
        message="Yeni Sipariş Alındı: #TY-ORD-2026-001",
        status="SENT"
    ))

    db_session.commit()
    return p1


def test_dashboard_summary_endpoint(client: TestClient, seed_dashboard_data):
    """Test consolidated dashboard overview API."""
    response = client.get("/api/v1/dashboard/summary")
    assert response.status_code == 200
    data = response.json()

    # System Pulse
    pulse = data["system_pulse"]
    assert pulse["overall_health"] in ["HEALTHY", "WARNING"]
    assert pulse["trendyol_connected"] is True
    assert pulse["telegram_bot_active"] is True
    assert len(pulse["services"]) == 4

    # Sales KPIs
    sales = data["sales_kpis"]
    assert sales["today_orders_count"] >= 1
    assert sales["today_gross_revenue_tl"] >= 400.0
    assert sales["today_net_profit_tl"] >= 140.0
    assert sales["today_profit_margin_percent"] > 0

    # Question KPIs
    questions = data["question_kpis"]
    assert questions["draft_ready_for_approval_count"] >= 1

    # Inventory KPIs
    inventory = data["inventory_kpis"]
    assert inventory["total_active_products"] >= 1
    assert inventory["total_variant_skus"] >= 2
    assert inventory["out_of_stock_skus_count"] >= 1
    assert inventory["stagnant_tied_capital_tl"] > 0

    # Critical Alerts
    alerts = data["critical_alerts"]
    assert len(alerts) >= 1
    alert_targets = [a["action_target_section"] for a in alerts]
    assert "QUESTIONS" in alert_targets or "PRODUCTS" in alert_targets

    # Recent Activity Feed
    feed = data["recent_activities"]
    assert len(feed) >= 2
    types = [f["activity_type"] for f in feed]
    assert "ORDER" in types or "QUESTION" in types or "AUDIT" in types
