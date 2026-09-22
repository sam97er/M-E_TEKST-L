"""
Tests for Phase 6: Orders, Sales & Net Profit Analysis.
Verifies cost breakdown, commissions, returns/cancellations, pricing simulator, and analytics.
"""

import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.database.session import SessionLocal, Base, engine
from backend.app.models.order import Order, OrderItem
from backend.app.schemas.order import ProfitSimulationRequest
from backend.app.services.profit.profit_service import ProfitCalculationService
from backend.app.services.sales.sales_service import SalesService

client = TestClient(app)


@pytest.fixture(autouse=True)
def setup_db():
    Base.metadata.create_all(bind=engine)
    yield


def test_profit_calculation_breakdown_formula():
    """
    Verifies that cost components (COGS, 20% Comm, Shipping, VAT, Service Fee, Packaging)
    are calculated accurately.
    """
    order = Order(
        trendyol_order_number="TEST-ORD-CALC-1",
        customer_first_name="Mehmet",
        customer_last_name="Test",
        city="Ankara",
        status="Created",
        total_gross_amount=1000.0,
        total_discount=0.0,
        net_amount=1000.0,
        total_product_cost=300.0,
        trendyol_commission=200.0,
        shipping_cost=38.50,
        tax_cost=100.0,
        other_expenses=12.0,
    )
    breakdown = ProfitCalculationService.calculate_order_breakdown(order)

    # Expected: Net 1000 - (300 COGS + 200 Comm + 38.50 Ship + 8.49 Fee + 100 Tax + 12 Pack)
    # Total expenses = 658.99, Net Profit = 341.01
    assert breakdown.gross_amount == 1000.0
    assert breakdown.net_sales_amount == 1000.0
    assert breakdown.product_purchase_cost == 300.0
    assert breakdown.trendyol_commission_amount == 200.0
    assert breakdown.shipping_cost == 38.50
    assert breakdown.service_fee == 8.49
    assert breakdown.tax_amount == 100.0
    assert breakdown.packaging_and_handling == 12.0
    assert breakdown.total_expenses == 658.99
    assert breakdown.estimated_net_profit == 341.01
    assert breakdown.profit_margin_percent == 34.1
    assert breakdown.is_profitable is True


def test_profit_calculation_returned_order():
    """
    Returned order must reflect negative profit (loss of double shipping + packaging).
    """
    order = Order(
        trendyol_order_number="TEST-ORD-RET-1",
        customer_first_name="Ayşe",
        customer_last_name="İade",
        city="İzmir",
        status="Returned",
        total_gross_amount=500.0,
        total_discount=0.0,
        net_amount=500.0,
        shipping_cost=38.50,
        other_expenses=12.0,
    )
    breakdown = ProfitCalculationService.calculate_order_breakdown(order)

    # 38.50 * 2 + 12 = 89.00 TL loss
    assert breakdown.estimated_net_profit == -89.00
    assert breakdown.is_profitable is False


def test_pricing_simulator_endpoint():
    """
    Tests what-if pricing simulator with healthy margin vs loss scenario.
    """
    # 1. Healthy margin scenario
    req_healthy = {
        "sale_price": 600.0,
        "purchase_cost": 180.0,
        "commission_rate_percent": 20.0,
        "shipping_cost": 38.50,
        "packaging_cost": 12.00,
        "vat_rate_percent": 10.0,
        "service_fee": 8.49,
    }
    res = client.post("/api/v1/orders/simulate", json=req_healthy)
    assert res.status_code == 200
    data = res.json()
    assert data["is_profitable"] is True
    assert data["net_profit"] > 100.0
    assert data["breakeven_price"] > 0
    assert "Mükemmel" in data["recommendation"] or "İyi" in data["recommendation"]

    # 2. Loss scenario (sale price too low)
    req_loss = {
        "sale_price": 100.0,
        "purchase_cost": 80.0,
        "commission_rate_percent": 20.0,
        "shipping_cost": 38.50,
        "packaging_cost": 12.00,
        "vat_rate_percent": 10.0,
        "service_fee": 8.49,
    }
    res_loss = client.post("/api/v1/orders/simulate", json=req_loss)
    assert res_loss.status_code == 200
    data_loss = res_loss.json()
    assert data_loss["is_profitable"] is False
    assert data_loss["net_profit"] < 0
    assert "ZARARINA" in data_loss["recommendation"]


def test_list_orders_and_filters():
    """
    Tests GET /api/v1/orders with search, status filters, and sorting.
    """
    res = client.get("/api/v1/orders")
    assert res.status_code == 200
    orders = res.json()
    assert isinstance(orders, list)
    assert len(orders) > 0

    first_order = orders[0]
    assert "trendyol_order_number" in first_order
    assert "estimated_profit" in first_order
    assert "profit_margin_percent" in first_order

    # Status filter
    res_delivered = client.get("/api/v1/orders?status=Delivered")
    assert res_delivered.status_code == 200
    for o in res_delivered.json():
        assert o["status"].lower() == "delivered"

    # Search filter
    res_search = client.get(f"/api/v1/orders?search={first_order['trendyol_order_number']}")
    assert res_search.status_code == 200
    assert len(res_search.json()) >= 1


def test_order_detail_and_status_update():
    """
    Tests GET /api/v1/orders/{id} and PUT /api/v1/orders/{id}/status.
    """
    # Get an order id from list
    res_list = client.get("/api/v1/orders?limit=1")
    orders = res_list.json()
    assert len(orders) > 0
    order_id = orders[0]["id"]

    # 1. Get Detail
    res_detail = client.get(f"/api/v1/orders/{order_id}")
    assert res_detail.status_code == 200
    detail = res_detail.json()
    assert detail["id"] == order_id
    assert "breakdown" in detail
    assert "items" in detail
    assert detail["breakdown"]["gross_amount"] > 0

    # 2. Update Status to Delivered
    res_update = client.put(
        f"/api/v1/orders/{order_id}/status",
        json={"status": "Delivered"}
    )
    assert res_update.status_code == 200
    updated = res_update.json()
    assert updated["status"] == "Delivered"
    assert updated["breakdown"]["realized_net_profit"] is not None


def test_sales_summary_and_analytics_endpoints():
    """
    Tests /summary, /analytics/daily, and /analytics/product-ranking.
    """
    # 1. Summary
    res_summary = client.get("/api/v1/orders/summary")
    assert res_summary.status_code == 200
    sum_data = res_summary.json()
    assert sum_data["total_orders_count"] > 0
    assert sum_data["total_gross_revenue"] > 0
    assert sum_data["total_estimated_net_profit"] > 0

    # 2. Daily analytics
    res_daily = client.get("/api/v1/orders/analytics/daily?days=7")
    assert res_daily.status_code == 200
    daily_list = res_daily.json()
    assert len(daily_list) == 7
    assert "gross_revenue" in daily_list[0]
    assert "net_profit" in daily_list[0]

    # 3. Product Profit Ranking
    res_ranking = client.get("/api/v1/orders/analytics/product-ranking?limit=10")
    assert res_ranking.status_code == 200
    ranking = res_ranking.json()
    assert isinstance(ranking, list)
    if ranking:
        assert "product_name" in ranking[0]
        assert "total_profit" in ranking[0]
        assert "is_low_margin" in ranking[0]
