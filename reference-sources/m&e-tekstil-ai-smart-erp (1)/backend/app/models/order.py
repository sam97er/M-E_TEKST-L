"""
Orders, Order Items, and Profit Calculation Models.
Distinguishes Estimated vs Realized Net Profit.
"""

from datetime import datetime
from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, DateTime, Text, Index
from sqlalchemy.orm import relationship
from backend.app.database.session import Base
from backend.app.models.base import TimestampMixin


class Order(Base, TimestampMixin):
    __tablename__ = "orders"

    id = Column(Integer, primary_key=True, index=True)
    trendyol_order_number = Column(String(100), unique=True, nullable=False, index=True)
    package_id = Column(String(100), nullable=True, index=True)
    customer_first_name = Column(String(100), nullable=True)
    customer_last_name = Column(String(100), nullable=True)
    city = Column(String(100), nullable=True)
    status = Column(String(50), default="Created", nullable=False, index=True)  # Created, Picking, Invoiced, Shipped, Delivered, Cancelled, Returned
    
    # Financial metrics
    total_gross_amount = Column(Float, default=0.0, nullable=False)  # Brüt Tutar
    total_discount = Column(Float, default=0.0, nullable=False)      # İndirim
    net_amount = Column(Float, default=0.0, nullable=False)          # Müşteri Ödemesi
    
    total_product_cost = Column(Float, default=0.0, nullable=False)  # Toplam Ürün Maliyeti
    trendyol_commission = Column(Float, default=0.0, nullable=False) # Komisyon Kesintisi
    shipping_cost = Column(Float, default=0.0, nullable=False)       # Kargo Maliyeti
    tax_cost = Column(Float, default=0.0, nullable=False)            # KDV/Vergi
    other_expenses = Column(Float, default=0.0, nullable=False)      # Paketleme/Diğer

    estimated_profit = Column(Float, default=0.0, nullable=False)    # Tahmini Kâr
    realized_profit = Column(Float, nullable=True)                   # Kesinleşmiş Kâr (Mutabakat sonrası)
    order_date = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)

    items = relationship("OrderItem", back_populates="order", cascade="all, delete-orphan")


class OrderItem(Base, TimestampMixin):
    __tablename__ = "order_items"

    id = Column(Integer, primary_key=True, index=True)
    order_id = Column(Integer, ForeignKey("orders.id", ondelete="CASCADE"), nullable=False, index=True)
    variant_id = Column(Integer, ForeignKey("product_variants.id", ondelete="SET NULL"), nullable=True, index=True)
    barcode = Column(String(100), nullable=False)
    product_name = Column(String(255), nullable=False)
    quantity = Column(Integer, default=1, nullable=False)
    unit_price = Column(Float, default=0.0, nullable=False)
    unit_purchase_cost = Column(Float, default=0.0, nullable=False)
    commission_rate = Column(Float, default=0.20, nullable=False)  # e.g. 20%
    vat_rate = Column(Float, default=0.10, nullable=False)

    order = relationship("Order", back_populates="items")
    variant = relationship("ProductVariant")
