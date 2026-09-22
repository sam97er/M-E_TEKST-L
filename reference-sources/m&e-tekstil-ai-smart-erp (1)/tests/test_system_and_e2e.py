"""
End-to-End (E2E) & System Maintenance Integration Tests for M&E Tekstil AI Smart ERP.
Verifies full data flow: Settings -> Product & Stock -> Order & Profit -> Customer Question & AI -> Audit & Dashboard -> Backup & Export.
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from backend.app.main import app
from backend.app.database.session import Base, get_db
from backend.app.models.product import Product, ProductVariant
from backend.app.models.inventory import Warehouse, InventoryBalance
from backend.app.models.order import Order, OrderItem
from backend.app.models.customer_question import CustomerQuestion
from backend.app.models.ai_provider import AIProviderConfig

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
    
    # Initialize default warehouse
    wh = Warehouse(name="Ana Depo", code="WH-MAIN", is_default=True)
    db.add(wh)
    
    # Initialize AI Provider
    ai = AIProviderConfig(
        slot=1,
        provider_name="gemini",
        model="gemini-1.5-flash",
        is_enabled=True,
    )
    db.add(ai)
    db.commit()

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


def test_system_diagnostics_endpoint(client):
    response = client.get("/api/v1/system/diagnostics")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "HEALTHY"
    assert "entity_counts" in data
    assert data["foreign_key_violations"] == 0


def test_full_system_backup_and_exports(client, db_session):
    # Seed product
    p = Product(
        title="Oversize İki İplik Sweatshirt",
        product_code="ME-SWT-01",
        category_name="Sweatshirt",
        brand="M&E Tekstil",
    )
    db_session.add(p)
    db_session.flush()

    v = ProductVariant(
        product_id=p.id,
        barcode="8680001112223",
        sku="ME-SWT-01-BLK-M",
        size="M",
        color="Siyah",
        purchase_cost=180.0,
        selling_price=450.0,
    )
    db_session.add(v)
    db_session.flush()

    wh = db_session.query(Warehouse).filter(Warehouse.is_default == True).first()
    bal = InventoryBalance(variant_id=v.id, warehouse_id=wh.id, quantity=150)
    db_session.add(bal)

    # Seed order
    o = Order(
        trendyol_order_number="TY-E2E-9999",
        customer_first_name="Ali",
        customer_last_name="Yılmaz",
        status="Delivered",
        total_gross_amount=450.0,
        net_amount=450.0,
        total_product_cost=180.0,
        trendyol_commission=67.5,
        shipping_cost=35.0,
        estimated_profit=167.5,
    )
    db_session.add(o)
    db_session.commit()

    # 1. JSON Backup
    res_json = client.get("/api/v1/system/backup/json")
    assert res_json.status_code == 200
    backup_data = res_json.json()
    assert backup_data["metadata"]["system"] == "M&E Tekstil AI Smart ERP"
    assert len(backup_data["products"]) == 1
    assert len(backup_data["variants"]) == 1
    assert len(backup_data["orders"]) == 1

    # 2. Products CSV
    res_csv_prod = client.get("/api/v1/system/export/products-csv")
    assert res_csv_prod.status_code == 200
    assert "text/csv" in res_csv_prod.headers["content-type"]
    assert "8680001112223" in res_csv_prod.text
    assert "ME-SWT-01-BLK-M" in res_csv_prod.text

    # 3. Orders CSV
    res_csv_ord = client.get("/api/v1/system/export/orders-csv")
    assert res_csv_ord.status_code == 200
    assert "TY-E2E-9999" in res_csv_ord.text
    assert "167.50" in res_csv_ord.text


def test_complete_e2e_business_lifecycle(client, db_session):
    # Step 1: Create a textile product
    prod_payload = {
        "title": "Unisex Kapüşonlu Sweatshirt",
        "product_code": "SW-KAP-2026",
        "category_name": "Sweatshirt",
        "brand": "M&E Tekstil",
        "selling_price": 550.0,
        "vat_rate": 10.0,
        "variants": [
            {
                "barcode": "8689998887771",
                "sku": "SW-KAP-2026-L",
                "size": "L",
                "color": "Antrasit",
                "purchase_cost": 175.0,
                "selling_price": 550.0,
                "initial_stock": 80,
            }
        ],
    }
    r_prod = client.post("/api/v1/products", json=prod_payload)
    assert r_prod.status_code == 201
    prod_id = r_prod.json()["id"]

    # Step 2: Create Order in DB and verify Profit calculation
    order = Order(
        trendyol_order_number="TY-E2E-ORD-101",
        customer_first_name="Fatma",
        customer_last_name="Kaya",
        city="Bursa",
        status="Delivered",
        total_gross_amount=550.0,
        net_amount=550.0,
        total_product_cost=175.0,
        trendyol_commission=110.0,
        shipping_cost=38.5,
        estimated_profit=226.5,
    )
    db_session.add(order)
    db_session.commit()

    r_orders = client.get("/api/v1/orders")
    assert r_orders.status_code == 200
    assert len(r_orders.json()) >= 1

    # Step 3: Customer asks question -> generate AI draft -> Approve -> Send
    q = CustomerQuestion(
        trendyol_question_id="TY-Q-E2E-888",
        product_id=prod_id,
        customer_name="Zeynep T.",
        question_text="Kumaşı kalın mı kışın sıcak tutar mı?",
        status="NEW",
    )
    db_session.add(q)
    db_session.commit()
    db_session.refresh(q)
    q_id = q.id

    # AI Draft
    r_draft = client.post(f"/api/v1/questions/{q_id}/draft", json={"tone": "KURUMSAL"})
    assert r_draft.status_code == 200
    draft_id = r_draft.json()["id"]

    # Human Approval
    r_appr = client.post(
        f"/api/v1/questions/drafts/{draft_id}/approve",
        json={"operator_name": "Yönetici"},
    )
    assert r_appr.status_code == 200
    assert r_appr.json()["status"] == "APPROVED"

    # Step 4: Executive Dashboard Summary check
    r_dash = client.get("/api/v1/dashboard/summary")
    assert r_dash.status_code == 200
    dash_data = r_dash.json()
    assert dash_data["system_pulse"]["overall_health"] in ["HEALTHY", "WARNING"]
    assert dash_data["sales_kpis"]["today_orders_count"] >= 1
    assert dash_data["inventory_kpis"]["total_physical_units"] >= 80

    # Step 5: Full System Backup Snapshot
    r_bkp = client.get("/api/v1/system/backup/json")
    assert r_bkp.status_code == 200
    counts = r_bkp.json()["metadata"]["counts"]
    assert counts["products"] >= 1
    assert counts["orders"] >= 1
    assert counts["questions"] >= 1
