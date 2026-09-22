"""
Models package for M&E Tekstil AI Smart ERP.
Imports all models so SQLAlchemy Base.metadata collects all tables.
"""

from backend.app.models.base import TimestampMixin
from backend.app.models.product import Product, ProductVariant, ProductImage
from backend.app.models.inventory import Warehouse, InventoryBalance, StockMovement
from backend.app.models.order import Order, OrderItem
from backend.app.models.customer_question import CustomerQuestion, AIDraft
from backend.app.models.ai_provider import AIProviderConfig
from backend.app.models.system import AuditLog, AppSetting, SyncRun, TelegramEvent, AIReport

__all__ = [
    "TimestampMixin",
    "Product",
    "ProductVariant",
    "ProductImage",
    "Warehouse",
    "InventoryBalance",
    "StockMovement",
    "Order",
    "OrderItem",
    "CustomerQuestion",
    "AIDraft",
    "AIProviderConfig",
    "AuditLog",
    "AppSetting",
    "SyncRun",
    "TelegramEvent",
    "AIReport",
]
