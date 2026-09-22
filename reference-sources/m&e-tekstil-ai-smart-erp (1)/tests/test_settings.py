"""
Automated unit and integration tests for General ERP Settings (Phase 2).
"""

from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from backend.app.models.system import AppSetting, AuditLog


def test_get_general_settings(client: TestClient):
    response = client.get("/api/v1/settings")
    assert response.status_code == 200
    data = response.json()
    assert "app_name" in data
    assert "database_url" in data
    assert "active_ai_slots_count" in data
    assert "trendyol_configured" in data
    assert "telegram_configured" in data


def test_update_or_create_app_setting(client: TestClient, db_session: Session):
    payload = {
        "key": "DEFAULT_STOCK_SAFETY_MARGIN",
        "value": "5",
        "description": "Otomatik stok emniyet payı adedi",
    }
    response = client.post("/api/v1/settings", json=payload)
    assert response.status_code == 200
    res_data = response.json()
    assert res_data["key"] == "DEFAULT_STOCK_SAFETY_MARGIN"
    assert res_data["value"] == "5"

    # Verify AuditLog was recorded
    audit = db_session.query(AuditLog).filter(AuditLog.entity_id == "DEFAULT_STOCK_SAFETY_MARGIN").first()
    assert audit is not None
    assert audit.action in ["CREATE", "UPDATE"]


def test_trendyol_settings_test_endpoint(client: TestClient):
    response = client.post("/api/v1/settings/trendyol/test")
    assert response.status_code == 200
    data = response.json()
    assert "configured" in data
    assert "supplier_id" in data
    assert "api_key_status" in data


def test_telegram_settings_test_endpoint(client: TestClient):
    response = client.post("/api/v1/settings/telegram/test")
    assert response.status_code == 200
    data = response.json()
    assert "enabled" in data
    assert "configured" in data
