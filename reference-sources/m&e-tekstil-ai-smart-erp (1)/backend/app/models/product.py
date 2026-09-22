"""
Product, Variant, and Product Image Models.
"""

from datetime import datetime
from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, Text, DateTime, Index
from sqlalchemy.orm import relationship
from backend.app.database.session import Base
from backend.app.models.base import TimestampMixin


class Product(Base, TimestampMixin):
    __tablename__ = "products"

    id = Column(Integer, primary_key=True, index=True)
    product_code = Column(String(100), unique=True, nullable=False, index=True)  # Ürün Kodu
    title = Column(String(255), nullable=False, index=True)  # Ürün Adı
    description = Column(Text, nullable=True)
    category_name = Column(String(100), nullable=True)
    brand = Column(String(100), default="M&E Tekstil", nullable=False)
    selling_price = Column(Float, default=0.0, nullable=False)
    vat_rate = Column(Float, default=10.0, nullable=False)  # KDV %
    status = Column(String(50), default="ACTIVE", nullable=False)  # ACTIVE, PASSIVE, ARCHIVED
    trendyol_content_id = Column(String(100), nullable=True, index=True)

    # Relationships
    variants = relationship("ProductVariant", back_populates="product", cascade="all, delete-orphan")
    images = relationship("ProductImage", back_populates="product", cascade="all, delete-orphan")
    questions = relationship("CustomerQuestion", back_populates="product")


class ProductVariant(Base, TimestampMixin):
    __tablename__ = "product_variants"

    id = Column(Integer, primary_key=True, index=True)
    product_id = Column(Integer, ForeignKey("products.id", ondelete="CASCADE"), nullable=False, index=True)
    barcode = Column(String(100), unique=True, nullable=False, index=True)
    sku = Column(String(100), unique=True, nullable=True, index=True)
    color = Column(String(50), nullable=False, index=True)  # Renk
    size = Column(String(50), nullable=False, index=True)  # Beden (S, M, L, XL, 38, 40 etc.)
    purchase_cost = Column(Float, default=0.0, nullable=False)  # Alış Maliyeti
    selling_price = Column(Float, default=0.0, nullable=False)  # Satış Fiyatı
    is_active = Column(Boolean, default=True, nullable=False)

    # Relationships
    product = relationship("Product", back_populates="variants")
    inventory_balances = relationship("InventoryBalance", back_populates="variant", cascade="all, delete-orphan")
    stock_movements = relationship("StockMovement", back_populates="variant")


class ProductImage(Base, TimestampMixin):
    __tablename__ = "product_images"

    id = Column(Integer, primary_key=True, index=True)
    product_id = Column(Integer, ForeignKey("products.id", ondelete="CASCADE"), nullable=False, index=True)
    image_url = Column(String(500), nullable=False)
    is_primary = Column(Boolean, default=False, nullable=False)
    review_status = Column(String(50), default="PENDING", nullable=False)  # PENDING, APPROVED, NEEDS_REVIEW
    quality_score = Column(Float, nullable=True)  # 0.0 to 10.0
    feedback = Column(Text, nullable=True)

    product = relationship("Product", back_populates="images")
