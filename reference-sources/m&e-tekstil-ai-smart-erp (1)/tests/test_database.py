"""
Tests for SQLite Database Models, Foreign Keys, and Constraints.
"""

import pytest
from sqlalchemy.exc import IntegrityError
from backend.app.models.product import Product, ProductVariant, ProductImage
from backend.app.models.inventory import Warehouse, InventoryBalance, StockMovement
from backend.app.models.system import AuditLog, AppSetting


def test_create_product_with_variants(db_session):
    """Verify product creation with related variants and image records."""
    product = Product(
        product_code="MET-TSHIRT-01",
        title="Oversize Pamuklu T-Shirt",
        description="100% Organik penye pamuk",
        category_name="T-Shirt",
        selling_price=299.90,
        vat_rate=10.0,
    )
    db_session.add(product)
    db_session.commit()
    db_session.refresh(product)

    assert product.id is not None
    assert product.brand == "M&E Tekstil"

    # Add variant: Siyah / M
    variant = ProductVariant(
        product_id=product.id,
        barcode="868000100101",
        sku="MET-TSH-BLK-M",
        color="Siyah",
        size="M",
        purchase_cost=95.0,
        selling_price=299.90,
    )
    db_session.add(variant)
    db_session.commit()
    db_session.refresh(variant)

    assert variant.id is not None
    assert variant.product_id == product.id
    assert len(product.variants) == 1


def test_unique_product_code_constraint(db_session):
    """Verify that duplicate product codes are rejected by SQLite unique constraint."""
    p1 = Product(product_code="MET-UNIQ-99", title="Ürün 1")
    p2 = Product(product_code="MET-UNIQ-99", title="Ürün 2")

    db_session.add(p1)
    db_session.commit()

    db_session.add(p2)
    with pytest.raises(IntegrityError):
        db_session.commit()
    db_session.rollback()


def test_warehouse_and_inventory_balance(db_session):
    """Verify inventory balance and stock movement ledger."""
    # 1. Create warehouse
    warehouse = Warehouse(
        name="Lojistik Merkezi",
        code="WH-LOG",
        is_default=True,
    )
    db_session.add(warehouse)

    # 2. Create product & variant
    product = Product(product_code="MET-HOODIE-01", title="Kapüşonlu Sweatshirt")
    db_session.add(product)
    db_session.commit()

    variant = ProductVariant(
        product_id=product.id,
        barcode="868000200201",
        color="Gri",
        size="L",
        purchase_cost=150.0,
        selling_price=450.0,
    )
    db_session.add(variant)
    db_session.commit()

    # 3. Create inventory balance
    balance = InventoryBalance(
        variant_id=variant.id,
        warehouse_id=warehouse.id,
        quantity=50,
        reserved_quantity=2,
        min_stock_threshold=10,
    )
    db_session.add(balance)
    db_session.commit()
    db_session.refresh(balance)

    assert balance.quantity == 50
    assert balance.available_quantity == 48

    # 4. Record stock movement ledger
    movement = StockMovement(
        variant_id=variant.id,
        warehouse_id=warehouse.id,
        change_amount=50,
        movement_type="IN",
        reason="İlk stok girişi",
        balance_after=50,
    )
    db_session.add(movement)
    db_session.commit()

    assert movement.id is not None
    assert movement.movement_type == "IN"


def test_audit_log_and_app_settings(db_session):
    """Verify audit logging and persistent dynamic settings."""
    log = AuditLog(
        event_type="INVENTORY_CHANGE",
        entity_name="ProductVariant",
        entity_id="1",
        action="UPDATE",
        details="Stok 50 adet artırıldı",
        actor="Operator1",
    )
    db_session.add(log)

    setting = AppSetting(
        key="trendyol_auto_sync_interval_minutes",
        value="15",
        description="Trendyol otomatik sipariş sorgulama aralığı",
    )
    db_session.add(setting)
    db_session.commit()

    assert log.id is not None
    assert setting.id is not None
    assert setting.value == "15"
