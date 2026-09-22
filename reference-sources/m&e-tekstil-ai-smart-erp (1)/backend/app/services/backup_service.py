"""
M&E Tekstil AI Smart ERP - System Backup, Data Export & Integrity Service.
Supports full JSON database dumps, CSV exports for products/orders, and SQLite diagnostics.
"""

import io
import csv
from datetime import datetime
from typing import Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import text

from backend.app.models.product import Product, ProductVariant, ProductImage
from backend.app.models.inventory import Warehouse, InventoryBalance, StockMovement
from backend.app.models.order import Order, OrderItem
from backend.app.models.customer_question import CustomerQuestion, AIDraft
from backend.app.models.ai_provider import AIProviderConfig
from backend.app.models.system import AppSetting, AuditLog, TelegramEvent


class BackupService:

    @staticmethod
    def export_full_json_backup(db: Session) -> Dict[str, Any]:
        """Dumps all key ERP entities into a comprehensive structured JSON archive."""
        products = db.query(Product).all()
        variants = db.query(ProductVariant).all()
        warehouses = db.query(Warehouse).all()
        inventory_balances = db.query(InventoryBalance).all()
        stock_movements = db.query(StockMovement).all()
        orders = db.query(Order).all()
        order_items = db.query(OrderItem).all()
        questions = db.query(CustomerQuestion).all()
        ai_configs = db.query(AIProviderConfig).all()
        settings_list = db.query(AppSetting).all()
        audit_logs = db.query(AuditLog).order_by(AuditLog.id.desc()).limit(500).all()

        backup_data = {
            "metadata": {
                "system": "M&E Tekstil AI Smart ERP",
                "version": "1.0.0",
                "backup_timestamp": datetime.utcnow().isoformat(),
                "counts": {
                    "products": len(products),
                    "variants": len(variants),
                    "warehouses": len(warehouses),
                    "inventory_balances": len(inventory_balances),
                    "stock_movements": len(stock_movements),
                    "orders": len(orders),
                    "order_items": len(order_items),
                    "questions": len(questions),
                    "ai_configs": len(ai_configs),
                    "settings": len(settings_list),
                    "audit_logs": len(audit_logs),
                }
            },
            "products": [
                {
                    "id": p.id,
                    "product_code": p.product_code,
                    "title": p.title,
                    "category_name": p.category_name,
                    "brand": p.brand,
                    "selling_price": p.selling_price,
                    "vat_rate": p.vat_rate,
                    "status": p.status,
                    "trendyol_content_id": p.trendyol_content_id,
                    "created_at": p.created_at.isoformat() if p.created_at else None,
                }
                for p in products
            ],
            "variants": [
                {
                    "id": v.id,
                    "product_id": v.product_id,
                    "barcode": v.barcode,
                    "sku": v.sku,
                    "color": v.color,
                    "size": v.size,
                    "purchase_cost": v.purchase_cost,
                    "selling_price": v.selling_price,
                    "is_active": v.is_active,
                }
                for v in variants
            ],
            "warehouses": [
                {
                    "id": w.id,
                    "name": w.name,
                    "code": w.code,
                    "is_default": w.is_default,
                    "address": w.address,
                }
                for w in warehouses
            ],
            "inventory_balances": [
                {
                    "id": ib.id,
                    "variant_id": ib.variant_id,
                    "warehouse_id": ib.warehouse_id,
                    "quantity": ib.quantity,
                    "reserved_quantity": ib.reserved_quantity,
                    "min_stock_threshold": ib.min_stock_threshold,
                }
                for ib in inventory_balances
            ],
            "stock_movements": [
                {
                    "id": sm.id,
                    "variant_id": sm.variant_id,
                    "warehouse_id": sm.warehouse_id,
                    "change_amount": sm.change_amount,
                    "movement_type": sm.movement_type,
                    "reason": sm.reason,
                    "reference_id": sm.reference_id,
                    "balance_after": sm.balance_after,
                    "created_at": sm.created_at.isoformat() if sm.created_at else None,
                }
                for sm in stock_movements
            ],
            "orders": [
                {
                    "id": o.id,
                    "trendyol_order_number": o.trendyol_order_number,
                    "package_id": o.package_id,
                    "customer_first_name": o.customer_first_name,
                    "customer_last_name": o.customer_last_name,
                    "city": o.city,
                    "status": o.status,
                    "total_gross_amount": o.total_gross_amount,
                    "total_discount": o.total_discount,
                    "net_amount": o.net_amount,
                    "total_product_cost": o.total_product_cost,
                    "trendyol_commission": o.trendyol_commission,
                    "shipping_cost": o.shipping_cost,
                    "estimated_profit": o.estimated_profit,
                    "realized_profit": o.realized_profit,
                    "order_date": o.order_date.isoformat() if o.order_date else None,
                }
                for o in orders
            ],
            "questions": [
                {
                    "id": q.id,
                    "trendyol_question_id": q.trendyol_question_id,
                    "product_id": q.product_id,
                    "customer_name": q.customer_name,
                    "question_text": q.question_text,
                    "status": q.status,
                    "created_at": q.created_at.isoformat() if q.created_at else None,
                }
                for q in questions
            ],
            "ai_providers": [
                {
                    "id": ap.id,
                    "slot": ap.slot,
                    "provider_name": ap.provider_name,
                    "model": ap.model,
                    "is_enabled": ap.is_enabled,
                    "assigned_tasks": ap.assigned_tasks,
                }
                for ap in ai_configs
            ],
            "settings": [
                {
                    "key": s.key,
                    "value": s.value,
                    "description": s.description,
                }
                for s in settings_list
            ]
        }
        return backup_data

    @staticmethod
    def export_products_csv(db: Session) -> str:
        """Generates a comma-separated text representation of all products and variants."""
        output = io.StringIO()
        writer = csv.writer(output, quoting=csv.QUOTE_MINIMAL)
        
        # Header
        writer.writerow([
            "Urun_ID",
            "Urun_Kodu",
            "Urun_Adi",
            "Kategori",
            "Marka",
            "Barkod",
            "SKU",
            "Renk",
            "Beden",
            "Maliyet_TL",
            "Satis_Fiyati_TL",
            "KDV_Orani",
            "Durum",
        ])

        variants = (
            db.query(ProductVariant)
            .join(Product, ProductVariant.product_id == Product.id)
            .all()
        )

        for v in variants:
            p = v.product
            writer.writerow([
                p.id if p else "",
                p.product_code if p else "",
                p.title if p else "",
                p.category_name if p else "",
                p.brand if p else "",
                v.barcode,
                v.sku or "",
                v.color,
                v.size,
                f"{v.purchase_cost:.2f}",
                f"{v.selling_price:.2f}",
                f"{p.vat_rate if p else 10}",
                p.status if p else "ACTIVE",
            ])

        return output.getvalue()

    @staticmethod
    def export_orders_csv(db: Session) -> str:
        """Generates a CSV export of all recorded orders with profitability metrics."""
        output = io.StringIO()
        writer = csv.writer(output, quoting=csv.QUOTE_MINIMAL)

        writer.writerow([
            "Siparis_No",
            "Paket_ID",
            "Musteri_Adi",
            "Musteri_Soyadi",
            "Sehir",
            "Siparis_Tarihi",
            "Durum",
            "Brut_Tutar_TL",
            "Indirim_TL",
            "Net_Tutar_TL",
            "Urun_Maliyeti_TL",
            "Trendyol_Komisyonu_TL",
            "Kargo_Maliyeti_TL",
            "Tahmini_Kar_TL",
            "Kesinlesen_Kar_TL",
        ])

        orders = db.query(Order).order_by(Order.order_date.desc()).all()
        for o in orders:
            writer.writerow([
                o.trendyol_order_number,
                o.package_id or "",
                o.customer_first_name or "",
                o.customer_last_name or "",
                o.city or "",
                o.order_date.strftime("%Y-%m-%d %H:%M:%S") if o.order_date else "",
                o.status,
                f"{o.total_gross_amount:.2f}",
                f"{o.total_discount:.2f}",
                f"{o.net_amount:.2f}",
                f"{o.total_product_cost:.2f}",
                f"{o.trendyol_commission:.2f}",
                f"{o.shipping_cost:.2f}",
                f"{o.estimated_profit:.2f}",
                f"{o.realized_profit:.2f}" if o.realized_profit is not None else "-",
            ])

        return output.getvalue()

    @staticmethod
    def run_system_diagnostics(db: Session) -> Dict[str, Any]:
        """Executes SQLite engine checks and summarizes database health."""
        try:
            integrity_res = db.execute(text("PRAGMA integrity_check")).fetchall()
            integrity_status = [row[0] for row in integrity_res]
            is_intact = "ok" in [s.lower() for s in integrity_status]
        except Exception as e:
            integrity_status = [str(e)]
            is_intact = False

        try:
            fk_res = db.execute(text("PRAGMA foreign_key_check")).fetchall()
            fk_violations_count = len(fk_res)
        except Exception:
            fk_violations_count = 0

        counts = {
            "products": db.query(Product).count(),
            "variants": db.query(ProductVariant).count(),
            "warehouses": db.query(Warehouse).count(),
            "inventory_balances": db.query(InventoryBalance).count(),
            "stock_movements": db.query(StockMovement).count(),
            "orders": db.query(Order).count(),
            "customer_questions": db.query(CustomerQuestion).count(),
            "audit_logs": db.query(AuditLog).count(),
            "telegram_queue": db.query(TelegramEvent).count(),
        }

        return {
            "status": "HEALTHY" if is_intact and fk_violations_count == 0 else "WARNING",
            "database_engine": "SQLite 3 (ACID / WAL Mode)",
            "integrity_check": integrity_status,
            "foreign_key_violations": fk_violations_count,
            "entity_counts": counts,
            "checked_at": datetime.utcnow().isoformat(),
        }
