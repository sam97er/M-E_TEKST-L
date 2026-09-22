"""
Inventory, Multi-Warehouse, Balances, and Stock Movement Ledger.
Formula: Ürün Kodu + Renk + Beden + Depo + Adet
"""

from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, Text, UniqueConstraint, Index
from sqlalchemy.orm import relationship
from backend.app.database.session import Base
from backend.app.models.base import TimestampMixin


class Warehouse(Base, TimestampMixin):
    __tablename__ = "warehouses"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), unique=True, nullable=False)  # Depo Adı (e.g. Ana Depo, Trendyol Depo)
    code = Column(String(50), unique=True, nullable=False, index=True)
    is_default = Column(Boolean, default=False, nullable=False)
    address = Column(Text, nullable=True)

    balances = relationship("InventoryBalance", back_populates="warehouse", cascade="all, delete-orphan")
    movements = relationship("StockMovement", back_populates="warehouse")


class InventoryBalance(Base, TimestampMixin):
    """Real-time physical inventory snapshot per Variant and Warehouse."""
    __tablename__ = "inventory_balances"

    id = Column(Integer, primary_key=True, index=True)
    variant_id = Column(Integer, ForeignKey("product_variants.id", ondelete="CASCADE"), nullable=False, index=True)
    warehouse_id = Column(Integer, ForeignKey("warehouses.id", ondelete="RESTRICT"), nullable=False, index=True)
    quantity = Column(Integer, default=0, nullable=False)  # Adet
    reserved_quantity = Column(Integer, default=0, nullable=False)  # Ayrılmış (Bekleyen siparişler)
    min_stock_threshold = Column(Integer, default=5, nullable=False)  # Kritik Stok Eşiği

    __table_args__ = (
        UniqueConstraint("variant_id", "warehouse_id", name="uq_variant_warehouse"),
    )

    variant = relationship("ProductVariant", back_populates="inventory_balances")
    warehouse = relationship("Warehouse", back_populates="balances")

    @property
    def available_quantity(self) -> int:
        return max(0, self.quantity - self.reserved_quantity)


class StockMovement(Base, TimestampMixin):
    """Auditable Ledger recording every inventory increment, decrement, and transfer."""
    __tablename__ = "stock_movements"

    id = Column(Integer, primary_key=True, index=True)
    variant_id = Column(Integer, ForeignKey("product_variants.id", ondelete="CASCADE"), nullable=False, index=True)
    warehouse_id = Column(Integer, ForeignKey("warehouses.id", ondelete="RESTRICT"), nullable=False, index=True)
    change_amount = Column(Integer, nullable=False)  # Positive for IN/RETURN, Negative for OUT/SALE
    movement_type = Column(String(50), nullable=False)  # IN, OUT, ADJUSTMENT, TRANSFER, RETURN, TRENDYOL_SALE
    reason = Column(String(255), nullable=True)  # Açıklama / Neden
    reference_id = Column(String(100), nullable=True, index=True)  # e.g. OrderNumber or TransferID
    balance_after = Column(Integer, nullable=False)  # İşlem sonrası bakiye

    variant = relationship("ProductVariant", back_populates="stock_movements")
    warehouse = relationship("Warehouse", back_populates="movements")
