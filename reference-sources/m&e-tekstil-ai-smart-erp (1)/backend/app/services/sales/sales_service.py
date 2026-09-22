"""
Sales and Order Management Service for Phase 6.
Handles Order querying, status updates, sales KPI rollups, daily trend analytics, and product profit ranking.
"""

from datetime import datetime, timedelta
from typing import List, Optional, Tuple, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import func, desc, or_
from backend.app.models.order import Order, OrderItem
from backend.app.models.product import Product, ProductVariant
from backend.app.models.system import AuditLog
from backend.app.schemas.order import (
    OrderDetailDto,
    OrderItemDto,
    OrderSummaryItemDto,
    SalesSummaryDto,
    DailySalesPointDto,
    ProductProfitRankingDto,
)
from backend.app.services.profit.profit_service import ProfitCalculationService


class SalesService:

    @classmethod
    def seed_initial_orders_if_needed(cls, db: Session):
        """
        Seeds initial diverse orders if order table is empty to allow immediate analytics testing.
        """
        count = db.query(Order).count()
        if count > 0:
            return

        variants = db.query(ProductVariant).all()
        now = datetime.utcnow()

        sample_orders = [
            {
                "order_number": "TY-904821034",
                "customer": ("Elif", "Yıldız"),
                "city": "İstanbul",
                "status": "Delivered",
                "date": now - timedelta(days=5),
                "items": [
                    {"name": "Luxury Slim Fit Pamuklu Gömlek - Beyaz M", "qty": 1, "price": 649.90, "cost": 190.0, "barcode": "8680001001"},
                    {"name": "İtalyan Yünlü Dokuma Pantolon - Antrasit 32", "qty": 1, "price": 899.90, "cost": 280.0, "barcode": "8680002001"},
                ]
            },
            {
                "order_number": "TY-904821035",
                "customer": ("Burak", "Kaya"),
                "city": "İzmir",
                "status": "Delivered",
                "date": now - timedelta(days=4),
                "items": [
                    {"name": "Oversize Premium Kapüşonlu Sweatshirt - Haki L", "qty": 2, "price": 549.90, "cost": 160.0, "barcode": "8680003001"},
                ]
            },
            {
                "order_number": "TY-904821036",
                "customer": ("Canan", "Öztürk"),
                "city": "Ankara",
                "status": "Shipped",
                "date": now - timedelta(days=2),
                "items": [
                    {"name": "Premium İpek Karışımlı Triko Kazak - Bej S", "qty": 1, "price": 799.90, "cost": 240.0, "barcode": "8680004001"},
                ]
            },
            {
                "order_number": "TY-904821037",
                "customer": ("Mert", "Demir"),
                "city": "Bursa",
                "status": "Created",
                "date": now - timedelta(hours=6),
                "items": [
                    {"name": "Saf Keten Rahat Kesim Gömlek - Lacivert XL", "qty": 1, "price": 599.90, "cost": 175.0, "barcode": "8680005001"},
                    {"name": "Basic Pamuklu Atlet 2'li Paket - Siyah", "qty": 1, "price": 249.90, "cost": 70.0, "barcode": "8680006001"},
                ]
            },
            {
                "order_number": "TY-904821038",
                "customer": ("Selin", "Aydın"),
                "city": "Antalya",
                "status": "Returned",
                "date": now - timedelta(days=6),
                "items": [
                    {"name": "Çift Taraflı Su Geçirmez Trençkot - Haki M", "qty": 1, "price": 1299.90, "cost": 420.0, "barcode": "8680007001"},
                ]
            }
        ]

        for s in sample_orders:
            gross = sum(it["price"] * it["qty"] for it in s["items"])
            cogs = sum(it["cost"] * it["qty"] for it in s["items"])
            comm = round(gross * 0.20, 2)
            shipping = 38.50
            tax = round(gross * 0.10, 2)
            packaging = 12.0
            profit = round(gross - (cogs + comm + shipping + tax + packaging + 8.49), 2)

            ord_obj = Order(
                trendyol_order_number=s["order_number"],
                customer_first_name=s["customer"][0],
                customer_last_name=s["customer"][1],
                city=s["city"],
                status=s["status"],
                total_gross_amount=gross,
                total_discount=0.0,
                net_amount=gross,
                total_product_cost=cogs,
                trendyol_commission=comm,
                shipping_cost=shipping,
                tax_cost=tax,
                other_expenses=packaging,
                estimated_profit=profit,
                realized_profit=profit if s["status"] == "Delivered" else (-89.0 if s["status"] == "Returned" else None),
                order_date=s["date"],
            )
            db.add(ord_obj)
            db.flush()

            for it in s["items"]:
                # Try linking to variant if matching barcode
                matched_v = next((v for v in variants if v.barcode == it["barcode"]), None)
                item_obj = OrderItem(
                    order_id=ord_obj.id,
                    variant_id=matched_v.id if matched_v else None,
                    barcode=it["barcode"],
                    product_name=it["name"],
                    quantity=it["qty"],
                    unit_price=it["price"],
                    unit_purchase_cost=it["cost"],
                    commission_rate=0.20,
                    vat_rate=0.10,
                )
                db.add(item_obj)

        db.commit()

    @classmethod
    def list_orders(
        cls,
        db: Session,
        status: Optional[str] = None,
        search: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
        sort_by: str = "date_desc",
    ) -> List[OrderSummaryItemDto]:
        """
        Returns paginated list of orders with dynamic profit calculation.
        """
        cls.seed_initial_orders_if_needed(db)
        query = db.query(Order)

        if status and status.upper() != "ALL":
            query = query.filter(Order.status.ilike(f"%{status}%"))

        if search and search.strip():
            term = f"%{search.strip()}%"
            query = query.filter(
                or_(
                    Order.trendyol_order_number.ilike(term),
                    Order.customer_first_name.ilike(term),
                    Order.customer_last_name.ilike(term),
                    Order.city.ilike(term),
                )
            )

        if sort_by == "date_desc":
            query = query.order_by(desc(Order.order_date))
        elif sort_by == "date_asc":
            query = query.order_by(Order.order_date.asc())
        elif sort_by == "profit_desc":
            query = query.order_by(desc(Order.estimated_profit))
        elif sort_by == "amount_desc":
            query = query.order_by(desc(Order.net_amount))
        else:
            query = query.order_by(desc(Order.order_date))

        orders = query.offset(offset).limit(limit).all()

        results = []
        for ord in orders:
            breakdown = ProfitCalculationService.calculate_order_breakdown(ord)
            customer_full = f"{ord.customer_first_name or ''} {ord.customer_last_name or ''}".strip()
            if not customer_full:
                customer_full = "Trendyol Müşterisi"

            results.append(
                OrderSummaryItemDto(
                    id=ord.id,
                    trendyol_order_number=ord.trendyol_order_number,
                    customer_name=customer_full,
                    city=ord.city,
                    status=ord.status,
                    order_date=ord.order_date,
                    item_count=len(ord.items) if ord.items else 1,
                    net_amount=breakdown.net_sales_amount,
                    estimated_profit=breakdown.estimated_net_profit,
                    realized_profit=breakdown.realized_net_profit,
                    profit_margin_percent=breakdown.profit_margin_percent,
                )
            )
        return results

    @classmethod
    def get_order_detail(cls, db: Session, order_id: int) -> Optional[OrderDetailDto]:
        """
        Retrieves detailed order view with itemized profit breakdown.
        """
        ord = db.query(Order).filter(Order.id == order_id).first()
        if not ord:
            return None

        breakdown = ProfitCalculationService.calculate_order_breakdown(ord)
        customer_full = f"{ord.customer_first_name or ''} {ord.customer_last_name or ''}".strip()
        if not customer_full:
            customer_full = "Trendyol Müşterisi"

        items_dto = []
        for it in ord.items:
            total_price = round(float(it.unit_price) * int(it.quantity), 2)
            total_cost = round(float(it.unit_purchase_cost) * int(it.quantity), 2)
            item_gross_profit = round(total_price - total_cost - (total_price * float(it.commission_rate)), 2)

            items_dto.append(
                OrderItemDto(
                    id=it.id,
                    order_id=it.order_id,
                    variant_id=it.variant_id,
                    barcode=it.barcode,
                    product_name=it.product_name,
                    quantity=it.quantity,
                    unit_price=round(float(it.unit_price), 2),
                    unit_purchase_cost=round(float(it.unit_purchase_cost), 2),
                    commission_rate=float(it.commission_rate),
                    vat_rate=float(it.vat_rate),
                    total_price=total_price,
                    total_cost=total_cost,
                    item_gross_profit=item_gross_profit,
                )
            )

        return OrderDetailDto(
            id=ord.id,
            trendyol_order_number=ord.trendyol_order_number,
            package_id=ord.package_id,
            customer_name=customer_full,
            city=ord.city,
            status=ord.status,
            order_date=ord.order_date,
            item_count=len(ord.items) if ord.items else 1,
            breakdown=breakdown,
            items=items_dto,
        )

    @classmethod
    def update_order_status(
        cls, db: Session, order_id: int, new_status: str, custom_realized_profit: Optional[float] = None
    ) -> Optional[OrderDetailDto]:
        """
        Updates order status and re-computes realized profit.
        """
        ord = db.query(Order).filter(Order.id == order_id).first()
        if not ord:
            return None

        old_status = ord.status
        ord.status = new_status

        breakdown = ProfitCalculationService.calculate_order_breakdown(ord)
        if custom_realized_profit is not None:
            ord.realized_profit = custom_realized_profit
        elif new_status.upper() in ("DELIVERED", "TESLİM EDİLDİ"):
            ord.realized_profit = breakdown.estimated_net_profit
        elif new_status.upper() in ("CANCELLED", "İPTAL"):
            ord.realized_profit = -float(ord.other_expenses or 12.0)
        elif new_status.upper() in ("RETURNED", "İADE"):
            ord.realized_profit = -((float(ord.shipping_cost or 38.50) * 2.0) + float(ord.other_expenses or 12.0))

        audit = AuditLog(
            event_type="ORDER_STATUS_UPDATE",
            entity_name="Order",
            entity_id=str(ord.id),
            action="UPDATE",
            details=f"Sipariş {ord.trendyol_order_number} durumu {old_status} -> {new_status} olarak güncellendi.",
            actor="OPERATOR",
        )
        db.add(audit)
        db.commit()
        db.refresh(ord)

        return cls.get_order_detail(db, ord.id)

    @classmethod
    def get_sales_summary(cls, db: Session) -> SalesSummaryDto:
        """
        Aggregates sales metrics, total COGS, commissions, and overall net profits.
        """
        cls.seed_initial_orders_if_needed(db)
        orders = db.query(Order).all()

        total_orders = len(orders)
        if total_orders == 0:
            return SalesSummaryDto()

        delivered_count = sum(1 for o in orders if o.status.upper() in ("DELIVERED", "TESLİM EDİLDİ"))
        returned_or_cancelled = sum(1 for o in orders if o.status.upper() in ("CANCELLED", "İPTAL", "RETURNED", "İADE"))

        total_gross = sum(float(o.total_gross_amount or 0.0) for o in orders)
        total_net_sales = sum(float(o.net_amount or 0.0) for o in orders)
        total_cogs = 0.0
        total_comm = 0.0
        total_shipping = 0.0
        total_tax = 0.0
        total_other = 0.0
        total_estimated_profit = 0.0
        total_realized_profit = 0.0

        for ord in orders:
            bd = ProfitCalculationService.calculate_order_breakdown(ord)
            total_cogs += bd.product_purchase_cost
            total_comm += bd.trendyol_commission_amount
            total_shipping += bd.shipping_cost
            total_tax += bd.tax_amount
            total_other += bd.packaging_and_handling + bd.service_fee
            total_estimated_profit += bd.estimated_net_profit
            if bd.realized_net_profit is not None:
                total_realized_profit += bd.realized_net_profit

        overall_margin = round((total_estimated_profit / total_net_sales * 100.0), 2) if total_net_sales > 0 else 0.0
        aov = round(total_net_sales / total_orders, 2) if total_orders > 0 else 0.0
        return_rate = round((returned_or_cancelled / total_orders * 100.0), 2) if total_orders > 0 else 0.0

        return SalesSummaryDto(
            total_orders_count=total_orders,
            delivered_orders_count=delivered_count,
            cancelled_or_returned_count=returned_or_cancelled,
            total_gross_revenue=round(total_gross, 2),
            total_net_sales=round(total_net_sales, 2),
            total_product_cogs=round(total_cogs, 2),
            total_commission_paid=round(total_comm, 2),
            total_shipping_paid=round(total_shipping, 2),
            total_tax_paid=round(total_tax, 2),
            total_other_expenses=round(total_other, 2),
            total_estimated_net_profit=round(total_estimated_profit, 2),
            total_realized_net_profit=round(total_realized_profit, 2),
            overall_profit_margin_percent=overall_margin,
            average_order_value=aov,
            return_rate_percent=return_rate,
        )

    @classmethod
    def get_daily_sales_analytics(cls, db: Session, days: int = 7) -> List[DailySalesPointDto]:
        """
        Generates daily breakdown of sales and profit for trend visualization.
        """
        cls.seed_initial_orders_if_needed(db)
        orders = db.query(Order).all()

        daily_data: Dict[str, Dict[str, Any]] = {}
        now = datetime.utcnow()

        # Initialize past N days
        for i in range(days - 1, -1, -1):
            d_str = (now - timedelta(days=i)).strftime("%Y-%m-%d")
            daily_data[d_str] = {"order_count": 0, "gross": 0.0, "profit": 0.0}

        for ord in orders:
            d_str = ord.order_date.strftime("%Y-%m-%d") if ord.order_date else now.strftime("%Y-%m-%d")
            if d_str in daily_data:
                bd = ProfitCalculationService.calculate_order_breakdown(ord)
                daily_data[d_str]["order_count"] += 1
                daily_data[d_str]["gross"] += bd.net_sales_amount
                daily_data[d_str]["profit"] += bd.estimated_net_profit

        result = []
        for d_str, val in sorted(daily_data.items()):
            margin = round((val["profit"] / val["gross"] * 100.0), 2) if val["gross"] > 0 else 0.0
            result.append(
                DailySalesPointDto(
                    date=d_str,
                    order_count=val["order_count"],
                    gross_revenue=round(val["gross"], 2),
                    net_profit=round(val["profit"], 2),
                    profit_margin_percent=margin,
                )
            )
        return result

    @classmethod
    def get_product_profit_ranking(cls, db: Session, limit: int = 20) -> List[ProductProfitRankingDto]:
        """
        Ranks products by total net profit and highlights low-margin items (< 15%).
        """
        cls.seed_initial_orders_if_needed(db)
        items = db.query(OrderItem).all()

        product_map: Dict[str, Dict[str, Any]] = {}
        for it in items:
            key = it.product_name
            if key not in product_map:
                product_map[key] = {
                    "product_name": it.product_name,
                    "barcode": it.barcode,
                    "qty": 0,
                    "revenue": 0.0,
                    "cost": 0.0,
                    "comm_rate": float(it.commission_rate or 0.20),
                }

            qty = int(it.quantity or 1)
            price = float(it.unit_price or 0.0)
            cost = float(it.unit_purchase_cost or 0.0)

            product_map[key]["qty"] += qty
            product_map[key]["revenue"] += price * qty
            product_map[key]["cost"] += cost * qty

        ranking = []
        for _, data in product_map.items():
            rev = data["revenue"]
            cogs = data["cost"]
            comm = rev * data["comm_rate"]
            # Est. item net profit after tax & packaging allocated
            net_profit = round(rev - cogs - comm - (rev * 0.10) - (data["qty"] * 15.0), 2)
            margin = round((net_profit / rev * 100.0), 2) if rev > 0 else 0.0

            ranking.append(
                ProductProfitRankingDto(
                    product_name=data["product_name"],
                    barcode=data["barcode"],
                    total_units_sold=data["qty"],
                    total_revenue=round(rev, 2),
                    total_cost=round(cogs, 2),
                    total_profit=net_profit,
                    profit_margin_percent=margin,
                    is_low_margin=margin < 15.0,
                )
            )

        ranking.sort(key=lambda x: x.total_profit, reverse=True)
        return ranking[:limit]
