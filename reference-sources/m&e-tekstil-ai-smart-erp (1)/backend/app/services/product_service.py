"""
Product and Variant Business Service.
Handles product code generation, EAN-13 standard barcode calculations,
variant lifecycle, initial inventory allocations, and audit logging.
"""

import random
import time
from typing import List, Optional, Tuple
from fastapi import HTTPException, status
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import or_, func

from backend.app.models.product import Product, ProductVariant, ProductImage
from backend.app.models.inventory import Warehouse, InventoryBalance, StockMovement
from backend.app.models.system import AuditLog
from backend.app.schemas.product import (
    ProductCreate,
    ProductUpdate,
    ProductVariantCreate,
    ProductVariantUpdate,
    ProductImageCreate,
    ProductOut,
    ProductVariantOut,
    ProductImageOut
)


class ProductService:

    @staticmethod
    def calculate_ean13_checksum(twelve_digits: str) -> str:
        """Calculates standard EAN-13 check digit."""
        if len(twelve_digits) != 12 or not twelve_digits.isdigit():
            raise ValueError("12 digits required to calculate EAN-13 checksum")
        total = 0
        for idx, char in enumerate(twelve_digits):
            num = int(char)
            total += num * 1 if idx % 2 == 0 else num * 3
        checksum = (10 - (total % 10)) % 10
        return str(checksum)

    @classmethod
    def generate_unique_barcode(cls, db: Session, prefix: str = "868") -> str:
        """
        Generates a valid, unique 13-digit Turkish EAN-13 barcode.
        Prefix defaults to 868 (GS1 Turkey).
        """
        for _ in range(50):
            # 3 digits prefix + 9 random digits = 12 digits
            random_part = "".join([str(random.randint(0, 9)) for _ in range(9)])
            base12 = f"{prefix}{random_part}"
            check_digit = cls.calculate_ean13_checksum(base12)
            candidate = f"{base12}{check_digit}"

            # Verify uniqueness in DB
            exists = db.query(ProductVariant.id).filter(ProductVariant.barcode == candidate).first()
            if not exists:
                return candidate

        # Fallback to high-entropy timestamp if collisions occur
        ts_suffix = str(int(time.time() * 1000))[-9:]
        base12 = f"{prefix}{ts_suffix}"
        return f"{base12}{cls.calculate_ean13_checksum(base12)}"

    @classmethod
    def generate_unique_product_code(cls, db: Session, prefix: str = "MET") -> str:
        """
        Generates human-readable unique product code, e.g. MET-1001, MET-1002.
        """
        count = db.query(func.count(Product.id)).scalar() or 0
        candidate_num = 1001 + count
        while True:
            code = f"{prefix}-{candidate_num}"
            exists = db.query(Product.id).filter(Product.product_code == code).first()
            if not exists:
                return code
            candidate_num += 1

    @classmethod
    def get_or_create_default_warehouse(cls, db: Session) -> Warehouse:
        wh = db.query(Warehouse).filter(Warehouse.is_default == True).first()
        if not wh:
            wh = db.query(Warehouse).first()
        if not wh:
            wh = Warehouse(
                name="Ana Depo",
                code="DEP-01",
                is_default=True,
                address="Merkez Depo & Sevkiyat"
            )
            db.add(wh)
            db.commit()
            db.refresh(wh)
        return wh

    @classmethod
    def create_product_with_variants(
        cls,
        db: Session,
        product_in: ProductCreate,
        operator: str = "SYSTEM"
    ) -> ProductOut:
        # 1. Determine Product Code
        product_code = product_in.product_code.strip() if product_in.product_code else None
        if not product_code:
            product_code = cls.generate_unique_product_code(db)
        else:
            existing = db.query(Product.id).filter(Product.product_code == product_code).first()
            if existing:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"'{product_code}' ürün kodu zaten kullanımda."
                )

        # 2. Check variant barcodes for duplicates
        seen_barcodes = set()
        for v in product_in.variants:
            if v.barcode:
                v_bc = v.barcode.strip()
                if v_bc in seen_barcodes:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail=f"İstek içinde tekrarlanan barkod tespit edildi: {v_bc}"
                    )
                seen_barcodes.add(v_bc)
                existing_bc = db.query(ProductVariant.id).filter(ProductVariant.barcode == v_bc).first()
                if existing_bc:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail=f"'{v_bc}' barkodu sistemde zaten kayıtlı."
                    )

        # 3. Create Product
        product = Product(
            product_code=product_code,
            title=product_in.title.strip(),
            description=product_in.description,
            category_name=product_in.category_name,
            brand=product_in.brand,
            selling_price=product_in.selling_price,
            vat_rate=product_in.vat_rate,
            status=product_in.status,
            trendyol_content_id=product_in.trendyol_content_id,
        )
        db.add(product)
        db.flush()

        default_wh = None
        has_initial_stock = any(v.initial_stock > 0 for v in product_in.variants)
        if has_initial_stock:
            default_wh = cls.get_or_create_default_warehouse(db)

        # 4. Create Variants & allocate initial stock
        for v in product_in.variants:
            final_barcode = v.barcode.strip() if v.barcode else cls.generate_unique_barcode(db)
            sku = v.sku.strip() if v.sku else f"{product.product_code}-{v.color[:3].upper()}-{v.size}"

            variant = ProductVariant(
                product_id=product.id,
                barcode=final_barcode,
                sku=sku,
                color=v.color.strip(),
                size=v.size.strip(),
                purchase_cost=v.purchase_cost,
                selling_price=v.selling_price if v.selling_price > 0 else product.selling_price,
                is_active=v.is_active,
            )
            db.add(variant)
            db.flush()

            if v.initial_stock > 0 and default_wh:
                balance = InventoryBalance(
                    variant_id=variant.id,
                    warehouse_id=default_wh.id,
                    quantity=v.initial_stock,
                    reserved_quantity=0,
                    min_stock_threshold=v.min_stock_threshold,
                )
                db.add(balance)
                movement = StockMovement(
                    variant_id=variant.id,
                    warehouse_id=default_wh.id,
                    change_amount=v.initial_stock,
                    movement_type="IN",
                    reason="Ürün Açılış Stok Bakiyesi",
                    reference_id=f"INIT-{product.product_code}",
                    balance_after=v.initial_stock,
                )
                db.add(movement)

        # 5. Create Images
        for img in product_in.images:
            p_img = ProductImage(
                product_id=product.id,
                image_url=img.image_url.strip(),
                is_primary=img.is_primary,
                review_status="APPROVED",
            )
            db.add(p_img)

        # 6. Audit Log
        audit = AuditLog(
            event_type="INVENTORY_CHANGE",
            action="CREATE",
            entity_name="Product",
            entity_id=str(product.id),
            details=f"Ürün oluşturuldu: {product.product_code} ({product.title}) - {len(product_in.variants)} varyant",
            actor=operator
        )
        db.add(audit)

        db.commit()
        return cls.get_product_by_id(db, product.id)

    @classmethod
    def get_product_by_id(cls, db: Session, product_id: int) -> ProductOut:
        product = (
            db.query(Product)
            .options(
                joinedload(Product.variants).joinedload(ProductVariant.inventory_balances),
                joinedload(Product.images)
            )
            .filter(Product.id == product_id)
            .first()
        )
        if not product:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{product_id} ID'li ürün bulunamadı."
            )
        return cls._map_to_product_out(product)

    @classmethod
    def list_products(
        cls,
        db: Session,
        search: Optional[str] = None,
        category: Optional[str] = None,
        status_filter: Optional[str] = None,
        skip: int = 0,
        limit: int = 100
    ) -> List[ProductOut]:
        query = (
            db.query(Product)
            .options(
                joinedload(Product.variants).joinedload(ProductVariant.inventory_balances),
                joinedload(Product.images)
            )
        )

        if category and category != "Tümü":
            query = query.filter(Product.category_name.ilike(f"%{category.strip()}%"))

        if status_filter:
            query = query.filter(Product.status == status_filter)

        if search:
            s = f"%{search.strip()}%"
            # Subquery to find product IDs matching variant barcode
            variant_match_ids = (
                db.query(ProductVariant.product_id)
                .filter(ProductVariant.barcode.ilike(s))
                .scalar_subquery()
            )
            query = query.filter(
                or_(
                    Product.product_code.ilike(s),
                    Product.title.ilike(s),
                    Product.category_name.ilike(s),
                    Product.id.in_(variant_match_ids)
                )
            )

        products = query.order_by(Product.id.desc()).offset(skip).limit(limit).all()
        return [cls._map_to_product_out(p) for p in products]

    @classmethod
    def update_product(
        cls,
        db: Session,
        product_id: int,
        product_in: ProductUpdate,
        operator: str = "SYSTEM"
    ) -> ProductOut:
        product = db.query(Product).filter(Product.id == product_id).first()
        if not product:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{product_id} ID'li ürün bulunamadı."
            )

        update_data = product_in.model_dump(exclude_unset=True)
        for key, value in update_data.items():
            setattr(product, key, value)

        audit = AuditLog(
            event_type="INVENTORY_CHANGE",
            action="UPDATE",
            entity_name="Product",
            entity_id=str(product.id),
            details=f"Ürün güncellendi: {list(update_data.keys())}",
            actor=operator
        )
        db.add(audit)
        db.commit()
        return cls.get_product_by_id(db, product_id)

    @classmethod
    def delete_product(cls, db: Session, product_id: int, operator: str = "SYSTEM") -> bool:
        product = db.query(Product).filter(Product.id == product_id).first()
        if not product:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{product_id} ID'li ürün bulunamadı."
            )

        code = product.product_code
        db.delete(product)

        audit = AuditLog(
            event_type="INVENTORY_CHANGE",
            action="DELETE",
            entity_name="Product",
            entity_id=str(product_id),
            details=f"Ürün silindi: {code}",
            actor=operator
        )
        db.add(audit)
        db.commit()
        return True

    @classmethod
    def add_variant(
        cls,
        db: Session,
        product_id: int,
        variant_in: ProductVariantCreate,
        operator: str = "SYSTEM"
    ) -> ProductVariantOut:
        product = db.query(Product).filter(Product.id == product_id).first()
        if not product:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{product_id} ID'li ürün bulunamadı."
            )

        barcode = variant_in.barcode.strip() if variant_in.barcode else cls.generate_unique_barcode(db)
        existing = db.query(ProductVariant.id).filter(ProductVariant.barcode == barcode).first()
        if existing:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"'{barcode}' barkodu zaten kullanımda."
            )

        sku = variant_in.sku.strip() if variant_in.sku else f"{product.product_code}-{variant_in.color[:3].upper()}-{variant_in.size}"

        variant = ProductVariant(
            product_id=product.id,
            barcode=barcode,
            sku=sku,
            color=variant_in.color.strip(),
            size=variant_in.size.strip(),
            purchase_cost=variant_in.purchase_cost,
            selling_price=variant_in.selling_price if variant_in.selling_price > 0 else product.selling_price,
            is_active=variant_in.is_active
        )
        db.add(variant)
        db.flush()

        if variant_in.initial_stock > 0:
            wh = cls.get_or_create_default_warehouse(db)
            balance = InventoryBalance(
                variant_id=variant.id,
                warehouse_id=wh.id,
                quantity=variant_in.initial_stock,
                reserved_quantity=0,
                min_stock_threshold=variant_in.min_stock_threshold
            )
            db.add(balance)
            movement = StockMovement(
                variant_id=variant.id,
                warehouse_id=wh.id,
                change_amount=variant_in.initial_stock,
                movement_type="IN",
                reason="Yeni Varyant Stok Girişi",
                reference_id=f"VAR-{variant.barcode}",
                balance_after=variant_in.initial_stock
            )
            db.add(movement)

        audit = AuditLog(
            event_type="INVENTORY_CHANGE",
            action="CREATE",
            entity_name="ProductVariant",
            entity_id=str(variant.id),
            details=f"Yeni varyant eklendi ({variant.color} - {variant.size}, Barkod: {variant.barcode})",
            actor=operator
        )
        db.add(audit)
        db.commit()
        db.refresh(variant)

        return ProductVariantOut(
            id=variant.id,
            product_id=variant.product_id,
            barcode=variant.barcode,
            sku=variant.sku,
            color=variant.color,
            size=variant.size,
            purchase_cost=variant.purchase_cost,
            selling_price=variant.selling_price,
            is_active=variant.is_active,
            total_stock=variant_in.initial_stock,
            available_stock=variant_in.initial_stock,
            created_at=variant.created_at
        )

    @classmethod
    def update_variant(
        cls,
        db: Session,
        variant_id: int,
        variant_in: ProductVariantUpdate,
        operator: str = "SYSTEM"
    ) -> ProductVariantOut:
        variant = (
            db.query(ProductVariant)
            .options(joinedload(ProductVariant.inventory_balances))
            .filter(ProductVariant.id == variant_id)
            .first()
        )
        if not variant:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{variant_id} ID'li varyant bulunamadı."
            )

        if variant_in.barcode and variant_in.barcode != variant.barcode:
            exists = db.query(ProductVariant.id).filter(ProductVariant.barcode == variant_in.barcode).first()
            if exists:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"'{variant_in.barcode}' barkodu zaten kullanımda."
                )

        update_data = variant_in.model_dump(exclude_unset=True)
        for key, value in update_data.items():
            setattr(variant, key, value)

        audit = AuditLog(
            event_type="INVENTORY_CHANGE",
            action="UPDATE",
            entity_name="ProductVariant",
            entity_id=str(variant.id),
            details=f"Varyant güncellendi: {list(update_data.keys())}",
            actor=operator
        )
        db.add(audit)
        db.commit()
        db.refresh(variant)

        total_stock = sum(b.quantity for b in variant.inventory_balances)
        avail_stock = sum(b.available_quantity for b in variant.inventory_balances)

        return ProductVariantOut(
            id=variant.id,
            product_id=variant.product_id,
            barcode=variant.barcode,
            sku=variant.sku,
            color=variant.color,
            size=variant.size,
            purchase_cost=variant.purchase_cost,
            selling_price=variant.selling_price,
            is_active=variant.is_active,
            total_stock=total_stock,
            available_stock=avail_stock,
            created_at=variant.created_at
        )

    @classmethod
    def delete_variant(cls, db: Session, variant_id: int, operator: str = "SYSTEM") -> bool:
        variant = db.query(ProductVariant).filter(ProductVariant.id == variant_id).first()
        if not variant:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{variant_id} ID'li varyant bulunamadı."
            )
        barcode = variant.barcode
        db.delete(variant)

        audit = AuditLog(
            event_type="INVENTORY_CHANGE",
            action="DELETE",
            entity_name="ProductVariant",
            entity_id=str(variant_id),
            details=f"Varyant silindi: {barcode}",
            actor=operator
        )
        db.add(audit)
        db.commit()
        return True

    @classmethod
    def add_image(
        cls,
        db: Session,
        product_id: int,
        image_in: ProductImageCreate,
        operator: str = "SYSTEM"
    ) -> ProductImageOut:
        product = db.query(Product).filter(Product.id == product_id).first()
        if not product:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{product_id} ID'li ürün bulunamadı."
            )

        if image_in.is_primary:
            # Set other images of this product to not primary
            db.query(ProductImage).filter(ProductImage.product_id == product_id).update({"is_primary": False})

        img = ProductImage(
            product_id=product.id,
            image_url=image_in.image_url.strip(),
            is_primary=image_in.is_primary,
            review_status="APPROVED"
        )
        db.add(img)
        db.commit()
        db.refresh(img)
        return ProductImageOut.model_validate(img)

    @classmethod
    def _map_to_product_out(cls, product: Product) -> ProductOut:
        total_product_stock = 0
        variants_out = []

        for v in product.variants:
            v_total = sum(b.quantity for b in v.inventory_balances)
            v_avail = sum(b.available_quantity for b in v.inventory_balances)
            total_product_stock += v_total

            variants_out.append(
                ProductVariantOut(
                    id=v.id,
                    product_id=v.product_id,
                    barcode=v.barcode,
                    sku=v.sku,
                    color=v.color,
                    size=v.size,
                    purchase_cost=v.purchase_cost,
                    selling_price=v.selling_price,
                    is_active=v.is_active,
                    total_stock=v_total,
                    available_stock=v_avail,
                    created_at=v.created_at
                )
            )

        images_out = [ProductImageOut.model_validate(img) for img in product.images]

        return ProductOut(
            id=product.id,
            product_code=product.product_code,
            title=product.title,
            description=product.description,
            category_name=product.category_name,
            brand=product.brand,
            selling_price=product.selling_price,
            vat_rate=product.vat_rate,
            status=product.status,
            trendyol_content_id=product.trendyol_content_id,
            total_stock=total_product_stock,
            variants=variants_out,
            images=images_out,
            created_at=product.created_at,
            updated_at=product.updated_at
        )
