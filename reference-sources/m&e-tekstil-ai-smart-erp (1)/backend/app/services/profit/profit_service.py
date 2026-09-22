"""
Profit and Cost Calculation Service for Phase 6.
Handles detailed breakdown: COGS, Trendyol Commission, Shipping, VAT, Service Fees, and Margins.
"""

from typing import Dict, Any, Optional
from backend.app.schemas.order import ProfitBreakdownDto, ProfitSimulationRequest, ProfitSimulationResponse
from backend.app.models.order import Order, OrderItem


class ProfitCalculationService:
    DEFAULT_COMMISSION_RATE = 0.20  # 20% Trendyol textile standard
    DEFAULT_SHIPPING_COST = 38.50   # TL
    DEFAULT_SERVICE_FEE = 8.49      # TL Trendyol per-order platform fee
    DEFAULT_PACKAGING_COST = 12.00  # TL ambalaj & barkodlama
    DEFAULT_VAT_RATE = 0.10         # 10% Tekstil KDV

    @classmethod
    def calculate_order_breakdown(cls, order: Order) -> ProfitBreakdownDto:
        """
        Calculates exact financial breakdown for an order and its items.
        """
        gross = float(order.total_gross_amount or 0.0)
        discount = float(order.total_discount or 0.0)
        net_sales = max(0.0, float(order.net_amount or (gross - discount)))

        # Product COGS
        if order.items:
            cogs = sum(float(item.unit_purchase_cost or 0.0) * int(item.quantity or 1) for item in order.items)
            comm_amount = sum(
                float(item.unit_price or 0.0) * int(item.quantity or 1) * float(item.commission_rate or cls.DEFAULT_COMMISSION_RATE)
                for item in order.items
            )
        else:
            cogs = float(order.total_product_cost or (gross * 0.35))
            comm_amount = float(order.trendyol_commission or (gross * cls.DEFAULT_COMMISSION_RATE))

        ship_val = float(order.shipping_cost or 0.0)
        shipping = ship_val if ship_val > 0 else cls.DEFAULT_SHIPPING_COST
        service_fee = cls.DEFAULT_SERVICE_FEE
        tax_val = float(order.tax_cost or 0.0)
        tax = tax_val if tax_val > 0 else (net_sales * cls.DEFAULT_VAT_RATE)
        pack_val = float(order.other_expenses or 0.0)
        packaging = pack_val if pack_val > 0 else cls.DEFAULT_PACKAGING_COST

        # Status handling: if Cancelled or Returned
        if order.status.upper() in ("CANCELLED", "İPTAL"):
            # Commission refunded, no product lost, but packaging wasted
            total_expenses = packaging
            estimated_profit = -packaging
            realized_profit = -packaging
        elif order.status.upper() in ("RETURNED", "İADE"):
            # Commission refunded, return shipping charged (double shipping), packaging lost
            total_expenses = (shipping * 2.0) + packaging
            estimated_profit = -total_expenses
            realized_profit = -total_expenses
        else:
            total_expenses = cogs + comm_amount + shipping + service_fee + tax + packaging
            estimated_profit = round(net_sales - total_expenses, 2)
            realized_profit = float(order.realized_profit) if order.realized_profit is not None else (
                estimated_profit if order.status.upper() in ("DELIVERED", "TESLİM EDİLDİ") else None
            )

        margin_percent = round((estimated_profit / net_sales * 100.0), 2) if net_sales > 0 else 0.0
        roi_percent = round((estimated_profit / total_expenses * 100.0), 2) if total_expenses > 0 else 0.0
        avg_commission_rate = round((comm_amount / net_sales * 100.0), 1) if net_sales > 0 else 20.0

        return ProfitBreakdownDto(
            gross_amount=round(gross, 2),
            discount_amount=round(discount, 2),
            net_sales_amount=round(net_sales, 2),
            product_purchase_cost=round(cogs, 2),
            trendyol_commission_amount=round(comm_amount, 2),
            trendyol_commission_rate_avg=avg_commission_rate,
            shipping_cost=round(shipping, 2),
            service_fee=round(service_fee, 2),
            tax_amount=round(tax, 2),
            packaging_and_handling=round(packaging, 2),
            total_expenses=round(total_expenses, 2),
            estimated_net_profit=round(estimated_profit, 2),
            realized_net_profit=round(realized_profit, 2) if realized_profit is not None else None,
            profit_margin_percent=margin_percent,
            return_on_cost_percent=roi_percent,
            is_profitable=estimated_profit > 0,
        )

    @classmethod
    def simulate_pricing(cls, req: ProfitSimulationRequest) -> ProfitSimulationResponse:
        """
        Simulates what-if pricing scenario for textile products on Trendyol.
        Calculates net profit, margin, breakeven sales price, and target price for 20% margin.
        """
        sale_price = float(req.sale_price)
        cost = float(req.purchase_cost)
        comm_pct = float(req.commission_rate_percent) / 100.0
        vat_pct = float(req.vat_rate_percent) / 100.0
        shipping = float(req.shipping_cost)
        packaging = float(req.packaging_cost)
        service_fee = float(req.service_fee)

        comm_amount = round(sale_price * comm_pct, 2)
        tax_amount = round(sale_price * vat_pct, 2)
        total_costs = round(cost + comm_amount + shipping + packaging + service_fee + tax_amount, 2)
        net_profit = round(sale_price - total_costs, 2)

        margin_pct = round((net_profit / sale_price * 100.0), 2) if sale_price > 0 else 0.0
        roi_pct = round((net_profit / total_costs * 100.0), 2) if total_costs > 0 else 0.0

        # Breakeven formula: Price = Fixed Costs / (1 - Variable Rate)
        # Variable Rate = Commission + Tax
        variable_rate = comm_pct + vat_pct
        fixed_costs = cost + shipping + packaging + service_fee

        if variable_rate < 0.95:
            breakeven_price = round(fixed_costs / (1.0 - variable_rate), 2)
            # Target for 20% net margin: Net = Price - Variable*Price - Fixed = 0.20 * Price
            # Price * (1 - Variable - 0.20) = Fixed
            target_denom = 1.0 - variable_rate - 0.20
            target_price_20 = round(fixed_costs / target_denom, 2) if target_denom > 0.1 else round(breakeven_price * 1.3, 2)
        else:
            breakeven_price = round(fixed_costs * 1.5, 2)
            target_price_20 = round(fixed_costs * 1.8, 2)

        is_profitable = net_profit > 0

        if margin_pct >= 25.0:
            recommendation = "Mükemmel Kârlılık: Yüksek kâr marjına sahip, reklam ve kampanyalarla satışı artırılabilir."
        elif margin_pct >= 15.0:
            recommendation = "İyi & Dengeli Kârlılık: Sürdürülebilir ve sağlıklı kâr marjı."
        elif margin_pct > 0.0:
            recommendation = "Düşük Kâr Marjı Uyarısı: İade veya kargo artışında zarara geçebilir. Fiyatın artırılması önerilir."
        else:
            recommendation = "ZARARINA SATIŞ TEHLİKESİ: Satış fiyatı komisyon ve kargo giderlerini karşılamıyor. Fiyat acilen güncellenmeli!"

        return ProfitSimulationResponse(
            sale_price=round(sale_price, 2),
            purchase_cost=round(cost, 2),
            commission_amount=comm_amount,
            shipping_cost=shipping,
            service_fee=service_fee,
            tax_amount=tax_amount,
            packaging_cost=packaging,
            total_cost=total_costs,
            net_profit=net_profit,
            profit_margin_percent=margin_pct,
            roi_percent=roi_pct,
            breakeven_price=breakeven_price,
            target_price_for_20_percent_margin=target_price_20,
            is_profitable=is_profitable,
            recommendation=recommendation,
        )
