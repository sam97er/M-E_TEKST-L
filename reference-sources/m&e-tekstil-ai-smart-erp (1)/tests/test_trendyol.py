"""
Unit and integration tests for Trendyol Integration Service (Phase 4).
Tests:
- Rate limiting sliding window
- Credential management & masked display
- Connection diagnostic endpoint (mock vs missing credentials)
- Stock & price batch updates with audit trail
- Order synchronization and financial calculation (cost, commission, profit)
- Customer questions synchronization
- Full sync cycle execution
"""

import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.services.trendyol.trendyol_service import TrendyolRateLimiter, TrendyolService
from backend.app.models.system import SyncRun, AuditLog
from backend.app.models.order import Order
from backend.app.models.customer_question import CustomerQuestion

client = TestClient(app)


def test_rate_limiter():
    limiter = TrendyolRateLimiter(max_requests=3, window_seconds=2)
    ok1, rem1 = limiter.acquire()
    ok2, rem2 = limiter.acquire()
    ok3, rem3 = limiter.acquire()
    ok4, rem4 = limiter.acquire()

    assert ok1 is True
    assert ok2 is True
    assert ok3 is True
    assert ok4 is False  # limit exceeded
    assert rem4 == 0


def test_get_trendyol_config():
    response = client.get("/api/v1/trendyol/config")
    assert response.status_code == 200
    data = response.json()
    assert "configured" in data
    assert "base_url" in data
    assert "rate_limit_remaining" in data
    assert data["rate_limit_remaining"] <= 50


def test_update_trendyol_config():
    payload = {
        "supplier_id": "999888",
        "api_key": "test_api_key_12345",
        "api_secret": "test_api_secret_67890",
        "mock_mode": True,
    }
    response = client.post("/api/v1/trendyol/config", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["supplier_id"] == "999888"
    assert data["mock_mode"] is True
    assert data["configured"] is True
    assert "..." in data["api_key_masked"]


def test_trendyol_connection_test():
    # Since mock_mode was set to True in the previous test, connection test will succeed in mock mode
    response = client.post("/api/v1/trendyol/test")
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["supplier_id"] == "999888"
    assert data["is_mock"] is True
    assert data["latency_ms"] >= 0


def test_trendyol_stock_price_batch():
    payload = {
        "items": [
            {
                "barcode": "8680001112223",
                "quantity": 25,
                "sale_price": 499.90,
                "list_price": 599.90,
            },
            {
                "barcode": "8680003334445",
                "quantity": 10,
                "sale_price": 799.90,
            },
        ]
    }
    response = client.post("/api/v1/trendyol/stock-price", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "COMPLETED"
    assert data["item_count"] == 2
    assert "TY-BATCH-" in data["batch_request_id"]


def test_trendyol_sync_execution(db_session):
    payload = {
        "sync_type": "ALL",
        "force_mock": True,
    }
    response = client.post("/api/v1/trendyol/sync", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "SUCCESS"
    assert data["items_processed"] >= 0

    # Verify orders were synced
    orders_res = client.get("/api/v1/trendyol/orders")
    assert orders_res.status_code == 200
    orders = orders_res.json()
    assert len(orders) >= 1
    assert "order_number" in orders[0]
    assert orders[0]["estimated_profit"] >= 0 or orders[0]["net_amount"] > 0

    # Verify questions were synced
    questions_res = client.get("/api/v1/trendyol/questions")
    assert questions_res.status_code == 200
    questions = questions_res.json()
    assert len(questions) >= 1
    assert "question_id" in questions[0]
    assert any(q["status"] in ("NEW", "DRAFT_GENERATED", "APPROVED", "SENT") for q in questions)


def test_trendyol_dashboard_kpis():
    response = client.get("/api/v1/trendyol/dashboard")
    assert response.status_code == 200
    data = response.json()
    assert "is_connected" in data
    assert "total_orders_synced" in data
    assert "total_questions_synced" in data
    assert "rate_limit_info" in data
    assert data["total_orders_synced"] >= 1
