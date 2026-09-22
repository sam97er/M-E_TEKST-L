"""
Unit and Integration Tests for AI Reports, Quality Audits & Pricing Advisory (Phase 8).
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from backend.app.models.product import Product, ProductVariant, ProductImage
from backend.app.models.inventory import InventoryBalance, Warehouse


@pytest.fixture(scope="function")
def seed_report_data(db_session: Session):
    # Retrieve or create default warehouse
    wh = db_session.query(Warehouse).filter(Warehouse.code == "MAIN").first()
    if not wh:
        wh = Warehouse(code="MAIN", name="Ana Depo", is_default=True)
        db_session.add(wh)
        db_session.flush()

    # Product 1: High Quality
    p1 = Product(
        product_code="REP-TSH-001",
        title="M&E Tekstil Unisex %100 Pamuklu Oversize T-Shirt",
        brand="M&E Tekstil",
        category_name="T-Shirt",
        description="%100 Doğal Pamuklu kumaş. 30 derecede yıkayınız. Manken: 185 cm, 78 kg L beden.",
        selling_price=450.0,
        status="ACTIVE"
    )
    db_session.add(p1)
    db_session.flush()

    img1 = ProductImage(product_id=p1.id, image_url="https://example.com/img1.jpg", is_primary=True)
    img2 = ProductImage(product_id=p1.id, image_url="https://example.com/img2.jpg", is_primary=False)
    db_session.add_all([img1, img2])

    v1 = ProductVariant(
        product_id=p1.id,
        barcode="8689999000011",
        color="Siyah",
        size="M",
        purchase_cost=150.0,
        selling_price=450.0,
        is_active=True
    )
    db_session.add(v1)
    db_session.flush()

    b1 = InventoryBalance(variant_id=v1.id, warehouse_id=wh.id, quantity=15)
    db_session.add(b1)

    # Product 2: Low Quality & Dead Stock
    p2 = Product(
        product_code="REP-SWT-002",
        title="Sweatshirt",  # Short title, no keywords
        brand="M&E",
        category_name="Sweatshirt",
        description="Giyilebilir.",  # Missing fabric, wash, size
        selling_price=300.0,
        status="ACTIVE"
    )
    db_session.add(p2)
    db_session.flush()

    v2 = ProductVariant(
        product_id=p2.id,
        barcode="8689999000028",
        color="Gri",
        size="L",
        purchase_cost=180.0,
        selling_price=300.0,
        is_active=True
    )
    db_session.add(v2)
    db_session.flush()

    b2 = InventoryBalance(variant_id=v2.id, warehouse_id=wh.id, quantity=30)
    db_session.add(b2)

    db_session.commit()
    return p1, p2


def test_stagnant_stock_report(client: TestClient, seed_report_data):
    """Test stagnant and dead stock detection endpoint."""
    response = client.get("/api/v1/reports/stagnant-stock?min_days=10")
    assert response.status_code == 200
    data = response.json()

    assert data["total_stagnant_variants_count"] >= 2
    assert data["total_stagnant_units"] >= 45
    assert data["total_tied_capital_tl"] > 0
    assert len(data["items"]) >= 2

    # Check first item details
    item = data["items"][0]
    assert "tied_capital_tl" in item
    assert "ai_recommendation" in item
    assert "suggested_clearance_price" in item
    assert item["urgency_level"] in ["LOW", "MEDIUM", "CRITICAL"]


def test_listing_quality_audits(client: TestClient, seed_report_data):
    """Test product listing audit and scoring endpoint."""
    response = client.get("/api/v1/reports/listing-audits")
    assert response.status_code == 200
    data = response.json()

    assert data["total_audited_products"] >= 2
    assert data["average_quality_score"] > 0
    assert len(data["products"]) >= 2

    # Find the low quality product (REP-SWT-002) and check that deficiencies are detected
    low_q_item = next(p for p in data["products"] if p["product_code"] == "REP-SWT-002")
    assert low_q_item["quality_score"] < 80
    assert len(low_q_item["deficiencies"]) > 0
    assert any(d["field"] == "images" for d in low_q_item["deficiencies"])
    assert any(d["field"] == "description" for d in low_q_item["deficiencies"])

    # High quality product (REP-TSH-001) should have high score
    high_q_item = next(p for p in data["products"] if p["product_code"] == "REP-TSH-001")
    assert high_q_item["has_primary_image"] is True
    assert high_q_item["images_count"] == 2


def test_pricing_advice_calculation(client: TestClient, seed_report_data):
    """Test dynamic pricing and break-even calculation for a product."""
    p1, _ = seed_report_data

    payload = {
        "target_margin_percent": 30.0,
        "market_demand_level": "HIGH"
    }
    response = client.post(f"/api/v1/reports/pricing-advice/{p1.id}", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["product_id"] == p1.id
    assert data["unit_purchase_cost"] == 150.0
    assert data["break_even_minimum_price"] > 150.0
    assert data["suggested_optimal_price"] > data["break_even_minimum_price"]
    assert data["suggested_flash_deal_price"] > data["break_even_minimum_price"]
    assert len(data["reasoning"]) >= 3


def test_pricing_advice_nonexistent_product(client: TestClient):
    """Test pricing advice for non-existent product returns 404."""
    response = client.post("/api/v1/reports/pricing-advice/9999", json={})
    assert response.status_code == 404


def test_optimize_product_content(client: TestClient, seed_report_data):
    """Test AI product SEO title and description generation."""
    p1, _ = seed_report_data

    payload = {
        "target_audience": "Trend Gençler",
        "tone_of_voice": "Dinamik"
    }
    response = client.post(f"/api/v1/reports/optimize-content/{p1.id}", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["product_id"] == p1.id
    assert "M&E Tekstil" in data["optimized_title"]
    assert len(data["optimized_bullet_points"]) >= 3
    assert "Kumaş" in data["optimized_full_description"]
    assert len(data["suggested_tags"]) > 0
    assert data["status"] == "DRAFT"


def test_executive_summary_report(client: TestClient, seed_report_data):
    """Test executive synthesis report generation."""
    response = client.get("/api/v1/reports/executive-summary")
    assert response.status_code == 200
    data = response.json()

    assert data["overall_health_score"] > 0
    assert len(data["key_metrics"]) == 4
    assert len(data["top_strengths"]) > 0
    assert len(data["critical_bottlenecks"]) > 0
    assert len(data["recommended_next_actions"]) > 0
