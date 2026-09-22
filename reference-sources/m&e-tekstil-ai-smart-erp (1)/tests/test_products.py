"""
Automated unit and integration tests for Products, Variants, Barcodes, and Categories (Phase 3).
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from backend.app.models.product import Product, ProductVariant
from backend.app.models.inventory import InventoryBalance, StockMovement
from backend.app.models.system import AuditLog
from backend.app.services.product_service import ProductService


def test_ean13_checksum_and_generation(db_session: Session):
    # Test valid checksum calculation: 868000100001 -> check digit
    twelve = "868000100001"
    check = ProductService.calculate_ean13_checksum(twelve)
    assert len(check) == 1
    assert check.isdigit()

    # Generate barcode via service
    code = ProductService.generate_unique_barcode(db_session)
    assert len(code) == 13
    assert code.startswith("868")
    assert code[-1] == ProductService.calculate_ean13_checksum(code[:12])


def test_create_product_with_variants_and_stock(client: TestClient, db_session: Session):
    payload = {
        "title": "İpek Dokuma Şifon Elbise",
        "description": "Lüks kumaş, yazlık desenli uzun elbise",
        "category_name": "Elbise",
        "brand": "M&E Tekstil",
        "selling_price": 899.90,
        "vat_rate": 10.0,
        "variants": [
            {
                "color": "Siyah",
                "size": "38",
                "purchase_cost": 320.0,
                "selling_price": 899.90,
                "initial_stock": 25,
                "min_stock_threshold": 5
            },
            {
                "color": "Siyah",
                "size": "40",
                "purchase_cost": 320.0,
                "selling_price": 899.90,
                "initial_stock": 15,
                "min_stock_threshold": 5
            }
        ],
        "images": [
            {
                "image_url": "https://example.com/images/elbise_siyah.jpg",
                "is_primary": True
            }
        ]
    }

    response = client.post("/api/v1/products", json=payload)
    assert response.status_code == 201
    data = response.json()

    assert data["id"] is not None
    assert data["product_code"].startswith("MET-")
    assert data["title"] == "İpek Dokuma Şifon Elbise"
    assert data["category_name"] == "Elbise"
    assert len(data["variants"]) == 2
    assert data["total_stock"] == 40  # 25 + 15

    # Check that barcodes were auto-generated
    for v in data["variants"]:
        assert len(v["barcode"]) == 13
        assert v["barcode"].startswith("868")
        assert v["total_stock"] in [25, 15]

    # Verify AuditLog was recorded
    audit = db_session.query(AuditLog).filter(AuditLog.entity_name == "Product", AuditLog.entity_id == str(data["id"])).first()
    assert audit is not None
    assert audit.action == "CREATE"


def test_duplicate_product_code_error(client: TestClient):
    p1 = {
        "product_code": "CUSTOM-CODE-100",
        "title": "Keten Gömlek",
        "variants": []
    }
    res1 = client.post("/api/v1/products", json=p1)
    assert res1.status_code == 201

    res2 = client.post("/api/v1/products", json=p1)
    assert res2.status_code == 400
    assert "zaten kullanımda" in res2.json()["detail"]


def test_duplicate_barcode_error(client: TestClient):
    payload = {
        "title": "Pamuklu Pantolon",
        "variants": [
            {"color": "Bej", "size": "36", "barcode": "8681112223334"},
            {"color": "Bej", "size": "38", "barcode": "8681112223334"}  # duplicate
        ]
    }
    res = client.post("/api/v1/products", json=payload)
    assert res.status_code == 400
    assert "Tekrarlanan barkod" in res.json()["detail"] or "zaten kayıtlı" in res.json()["detail"] or "tekrarlanan barkod" in res.json()["detail"].lower()


def test_list_and_search_products(client: TestClient):
    # Setup test product
    client.post("/api/v1/products", json={
        "title": "Keten Şifon Elbise",
        "category_name": "Elbise",
        "variants": [{"color": "Mavi", "size": "38", "initial_stock": 5}]
    })

    # Search by keyword
    res = client.get("/api/v1/products?search=Elbise")
    assert res.status_code == 200
    items = res.json()
    assert len(items) >= 1
    assert any("Elbise" in item["title"] for item in items)

    # Search by category
    res_cat = client.get("/api/v1/products?category=Elbise")
    assert res_cat.status_code == 200
    assert len(res_cat.json()) >= 1


def test_get_categories_endpoint(client: TestClient):
    res = client.get("/api/v1/products/categories")
    assert res.status_code == 200
    categories = res.json()
    assert "Elbise" in categories
    assert "Bluz & Gömlek" in categories


def test_generate_barcode_endpoint(client: TestClient):
    res = client.get("/api/v1/products/generate-barcode")
    assert res.status_code == 200
    data = res.json()
    assert "barcode" in data
    assert len(data["barcode"]) == 13
    assert data["format"] == "EAN-13"


def test_add_and_update_variant(client: TestClient):
    # Create base product
    p_res = client.post("/api/v1/products", json={"title": "Midi Etek", "category_name": "Etek"})
    assert p_res.status_code == 201
    prod_id = p_res.json()["id"]

    # Add variant
    v_payload = {
        "color": "Haki",
        "size": "M",
        "purchase_cost": 150.0,
        "selling_price": 350.0,
        "initial_stock": 10
    }
    v_res = client.post(f"/api/v1/products/{prod_id}/variants", json=v_payload)
    assert v_res.status_code == 201
    var_data = v_res.json()
    var_id = var_data["id"]
    assert var_data["color"] == "Haki"
    assert var_data["size"] == "M"
    assert var_data["total_stock"] == 10

    # Update variant
    up_payload = {"selling_price": 399.0, "color": "Zeytin Yeşili"}
    up_res = client.put(f"/api/v1/products/variants/{var_id}", json=up_payload)
    assert up_res.status_code == 200
    assert up_res.json()["selling_price"] == 399.0
    assert up_res.json()["color"] == "Zeytin Yeşili"


def test_delete_product(client: TestClient):
    p_res = client.post("/api/v1/products", json={"title": "Silinecek Ürün"})
    assert p_res.status_code == 201
    prod_id = p_res.json()["id"]

    del_res = client.delete(f"/api/v1/products/{prod_id}")
    assert del_res.status_code == 200

    # Verify 404 on get
    get_res = client.get(f"/api/v1/products/{prod_id}")
    assert get_res.status_code == 404
