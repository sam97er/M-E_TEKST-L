"""
Test Suite for Telegram Bot Notification System & Audit Logs (Phase 7).
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from backend.app.main import app
from backend.app.database.session import Base, get_db
from backend.app.models.system import TelegramEvent, AuditLog, AppSetting
from backend.app.services.telegram.notifier import TelegramNotifier

# In-memory test SQLite DB
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session():
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def client(db_session):
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def test_telegram_config_get_and_update(client, db_session):
    """Verifies getting and updating Telegram configuration via REST API."""
    # 1. Get initial config
    res = client.get("/api/v1/telegram/config")
    assert res.status_code == 200
    data = res.json()
    assert "enabled" in data
    assert "is_configured" in data
    assert "bot_token_masked" in data

    # 2. Update config
    update_payload = {
        "enabled": True,
        "bot_token": "1234567890:ABCdefGHIjklMNOpqrsTUVwxyz",
        "chat_id": "-100987654321",
        "notify_new_order": True,
        "notify_low_stock": False,
        "notify_new_question": True,
        "notify_daily_digest": True,
    }
    put_res = client.put("/api/v1/telegram/config", json=update_payload)
    assert put_res.status_code == 200
    updated_data = put_res.json()
    assert updated_data["enabled"] is True
    assert updated_data["is_configured"] is True
    assert updated_data["chat_id"] == "-100987654321"
    assert updated_data["notify_low_stock"] is False
    assert "1234...wxyz" in updated_data["bot_token_masked"]


def test_telegram_send_test_message(client, db_session):
    """Verifies sending a test message and queuing the event."""
    # Set config first
    TelegramNotifier.update_config(db_session, {
        "enabled": True,
        "bot_token": "123456:FAKE_TOKEN_FOR_TESTING",
        "chat_id": "123456789",
    })

    payload = {
        "message": "<b>Test Alert</b> from pytest suite!",
        "event_type": "SYSTEM_TEST"
    }
    res = client.post("/api/v1/telegram/send-test", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["event_id"] > 0
    assert data["status"] in ("SENT", "FAILED")  # Will fail gracefully on dummy token

    # Check event in DB
    event = db_session.query(TelegramEvent).filter(TelegramEvent.id == data["event_id"]).first()
    assert event is not None
    assert event.event_type == "SYSTEM_TEST"


def test_telegram_category_toggle_skipping(client, db_session):
    """Ensures disabled categories are marked as SKIPPED without failing."""
    TelegramNotifier.update_config(db_session, {
        "enabled": True,
        "bot_token": "123456:FAKE_TOKEN",
        "chat_id": "123456",
        "notify_low_stock": False  # Disabled
    })

    event = TelegramNotifier.notify_low_stock(
        db=db_session,
        product_name="Test Sweatshirt",
        barcode="8680001234567",
        current_stock=2,
        threshold=5
    )

    assert event.status == "SKIPPED"
    assert "kapatılmış" in (event.error_message or "")


def test_telegram_retry_queue(client, db_session):
    """Verifies retrying failed or pending telegram events."""
    # Insert dummy failed events
    evt1 = TelegramEvent(
        event_type="NEW_ORDER",
        message="Order #1 test notification",
        status="FAILED",
        retry_count=1,
        error_message="Connection timeout"
    )
    evt2 = TelegramEvent(
        event_type="LOW_STOCK",
        message="Stock alert test notification",
        status="PENDING",
        retry_count=0
    )
    db_session.add_all([evt1, evt2])
    db_session.commit()

    # Trigger retry endpoint
    res = client.post("/api/v1/telegram/retry?max_retries=5")
    assert res.status_code == 200
    data = res.json()
    assert data["retried_count"] >= 2


def test_telegram_events_listing_and_filter(client, db_session):
    """Verifies listing and filtering telegram queue events."""
    evt1 = TelegramEvent(event_type="NEW_ORDER", message="Order 1", status="SENT")
    evt2 = TelegramEvent(event_type="LOW_STOCK", message="Stock alert", status="FAILED")
    evt3 = TelegramEvent(event_type="NEW_QUESTION", message="Question 1", status="PENDING")
    db_session.add_all([evt1, evt2, evt3])
    db_session.commit()

    # List all
    res = client.get("/api/v1/telegram/events")
    assert res.status_code == 200
    data = res.json()
    assert data["total"] >= 3
    assert data["sent_count"] >= 1
    assert data["failed_count"] >= 1
    assert data["pending_count"] >= 1

    # Filter by status
    res_filtered = client.get("/api/v1/telegram/events?status=FAILED")
    assert res_filtered.status_code == 200
    assert len(res_filtered.json()["events"]) >= 1
    assert res_filtered.json()["events"][0]["status"] == "FAILED"


def test_telegram_send_daily_digest(client, db_session):
    """Verifies generating and sending today's sales & profit digest."""
    res = client.post("/api/v1/telegram/send-daily-digest")
    assert res.status_code == 200
    data = res.json()
    assert data["event_id"] > 0

    evt = db_session.query(TelegramEvent).filter(TelegramEvent.id == data["event_id"]).first()
    assert evt is not None
    assert evt.event_type == "DAILY_DIGEST"
    assert "GÜNLÜK SATIŞ & KÂR ÖZETİ" in evt.message


def test_audit_logs_query(client, db_session):
    """Verifies querying system audit logs."""
    audit1 = AuditLog(
        event_type="INVENTORY_CHANGE",
        entity_name="ProductVariant",
        entity_id="101",
        action="CREATE",
        details="Yeni stok girişi",
        actor="ADMIN"
    )
    audit2 = AuditLog(
        event_type="TELEGRAM_SENT",
        entity_name="TelegramEvent",
        entity_id="202",
        action="SEND",
        details="Sipariş bildirimi gönderildi",
        actor="SYSTEM"
    )
    db_session.add_all([audit1, audit2])
    db_session.commit()

    # Query all
    res = client.get("/api/v1/audit/logs")
    assert res.status_code == 200
    data = res.json()
    assert data["total"] >= 2

    # Query by event_type filter
    res_filtered = client.get("/api/v1/audit/logs?event_type=INVENTORY_CHANGE")
    assert res_filtered.status_code == 200
    assert len(res_filtered.json()["logs"]) >= 1
    assert res_filtered.json()["logs"][0]["event_type"] == "INVENTORY_CHANGE"


def test_telegram_events_cleanup(client, db_session):
    """Verifies deleting sent/skipped events from queue."""
    evt1 = TelegramEvent(event_type="NEW_ORDER", message="Order 1", status="SENT")
    evt2 = TelegramEvent(event_type="LOW_STOCK", message="Stock alert", status="FAILED")
    db_session.add_all([evt1, evt2])
    db_session.commit()

    del_res = client.delete("/api/v1/telegram/events?status=SENT")
    assert del_res.status_code == 200

    remaining = db_session.query(TelegramEvent).all()
    assert len(remaining) == 1
    assert remaining[0].status == "FAILED"
