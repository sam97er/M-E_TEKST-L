"""
Telegram Bot Notification Engine (Phase 7).
Handles:
- Event-driven Telegram alerts (New Orders, Low Stock, Customer Questions, System Errors, Daily Digest)
- Resilient queue with retry mechanism and exponential backoff tracking
- Config management stored in AppSettings (Database) with fallback to .env
- Comprehensive Audit Log recording for all outbound communications
- Simulation / Test mode for environments without live bot tokens
"""

import json
import logging
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple
import httpx
from sqlalchemy.orm import Session

from backend.app.config import settings
from backend.app.models.system import AppSetting, AuditLog, TelegramEvent

logger = logging.getLogger(__name__)


class TelegramNotifier:
    """Core Service for Telegram Alerts and Queue Management."""

    @staticmethod
    def get_config(db: Session) -> Dict[str, Any]:
        """Retrieves Telegram config from AppSetting table or fallback to settings."""
        def _get_val(key: str, default_val: Any) -> Any:
            row = db.query(AppSetting).filter(AppSetting.key == key).first()
            if row and row.value is not None:
                if isinstance(default_val, bool):
                    return row.value.lower() in ("true", "1", "yes")
                return row.value
            return default_val

        enabled = _get_val("telegram_enabled", settings.TELEGRAM_ENABLED)
        bot_token = _get_val("telegram_bot_token", settings.TELEGRAM_BOT_TOKEN)
        chat_id = _get_val("telegram_chat_id", settings.TELEGRAM_CHAT_ID)
        notify_new_order = _get_val("telegram_notify_new_order", True)
        notify_low_stock = _get_val("telegram_notify_low_stock", True)
        notify_new_question = _get_val("telegram_notify_new_question", True)
        notify_daily_digest = _get_val("telegram_notify_daily_digest", True)
        notify_system_error = _get_val("telegram_notify_system_error", True)

        masked_token = "Yapılandırılmamış"
        if bot_token and len(bot_token) > 6:
            masked_token = f"{bot_token[:4]}...{bot_token[-4:]}"

        return {
            "enabled": bool(enabled),
            "bot_token": bot_token,
            "bot_token_masked": masked_token,
            "chat_id": chat_id,
            "is_configured": bool(bot_token and chat_id),
            "notify_new_order": bool(notify_new_order),
            "notify_low_stock": bool(notify_low_stock),
            "notify_new_question": bool(notify_new_question),
            "notify_daily_digest": bool(notify_daily_digest),
            "notify_system_error": bool(notify_system_error),
        }

    @staticmethod
    def update_config(db: Session, update_data: Dict[str, Any]) -> Dict[str, Any]:
        """Updates Telegram configuration keys in AppSetting."""
        for k, v in update_data.items():
            if v is not None:
                setting_key = f"telegram_{k}" if not k.startswith("telegram_") else k
                row = db.query(AppSetting).filter(AppSetting.key == setting_key).first()
                str_val = str(v)
                if row:
                    row.value = str_val
                else:
                    db.add(AppSetting(
                        key=setting_key,
                        value=str_val,
                        description=f"Telegram notification config: {setting_key}"
                    ))
        db.commit()
        return TelegramNotifier.get_config(db)

    @classmethod
    def send_raw_telegram(
        cls,
        bot_token: str,
        chat_id: str,
        message: str,
        timeout: int = 15
    ) -> Tuple[bool, Optional[str]]:
        """Sends an HTTP POST message to Telegram Bot API."""
        if not bot_token or not chat_id:
            return False, "Bot Token veya Chat ID eksik"

        url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
        payload = {
            "chat_id": chat_id,
            "text": message,
            "parse_mode": "HTML",
            "disable_web_page_preview": True
        }

        try:
            with httpx.Client(timeout=timeout) as client:
                response = client.post(url, json=payload)
                if response.status_code == 200:
                    return True, None
                data = response.json()
                err_desc = data.get("description", response.text)
                return False, f"Telegram API Hatası ({response.status_code}): {err_desc}"
        except Exception as ex:
            logger.error(f"Telegram HTTP exception: {ex}")
            return False, f"Ağ / Bağlantı hatası: {str(ex)}"

    @classmethod
    def queue_event(
        cls,
        db: Session,
        event_type: str,
        message: str,
        auto_send: bool = True
    ) -> TelegramEvent:
        """Creates a TelegramEvent in the queue and optionally attempts sending it."""
        event = TelegramEvent(
            event_type=event_type,
            message=message,
            status="PENDING",
            retry_count=0
        )
        db.add(event)
        db.commit()
        db.refresh(event)

        # Audit log for event creation
        audit = AuditLog(
            event_type="TELEGRAM_QUEUED",
            entity_name="TelegramEvent",
            entity_id=str(event.id),
            action="CREATE",
            details=json.dumps({"event_type": event_type, "preview": message[:100]}),
            actor="SYSTEM"
        )
        db.add(audit)
        db.commit()

        if auto_send:
            cls.send_event(db, event)

        return event

    @classmethod
    def send_event(cls, db: Session, event: TelegramEvent) -> bool:
        """Attempts to transmit a single TelegramEvent."""
        config = cls.get_config(db)

        # Check category toggles
        type_toggle_map = {
            "NEW_ORDER": config.get("notify_new_order", True),
            "LOW_STOCK": config.get("notify_low_stock", True),
            "NEW_QUESTION": config.get("notify_new_question", True),
            "DAILY_DIGEST": config.get("notify_daily_digest", True),
            "SYSTEM_ERROR": config.get("notify_system_error", True),
            "SYSTEM_TEST": True,
        }

        if not config["enabled"] and event.event_type != "SYSTEM_TEST":
            event.status = "SKIPPED"
            event.error_message = "Telegram bildirimleri ayarlardan devre dışı bırakılmış"
            db.commit()
            return False

        if not type_toggle_map.get(event.event_type, True) and event.event_type != "SYSTEM_TEST":
            event.status = "SKIPPED"
            event.error_message = f"{event.event_type} bildirim kategorisi kapatılmış"
            db.commit()
            return False

        if not config["is_configured"]:
            event.status = "FAILED"
            event.retry_count += 1
            event.error_message = "Bot Token veya Chat ID tanımlı değil"
            db.commit()
            return False

        success, err = cls.send_raw_telegram(
            bot_token=config["bot_token"],
            chat_id=config["chat_id"],
            message=event.message
        )

        if success:
            event.status = "SENT"
            event.sent_at = datetime.utcnow()
            event.error_message = None
            db.commit()

            # Audit log for success
            db.add(AuditLog(
                event_type="TELEGRAM_SENT",
                entity_name="TelegramEvent",
                entity_id=str(event.id),
                action="SEND",
                details=json.dumps({"status": "SUCCESS", "event_type": event.event_type}),
                actor="TELEGRAM_SERVICE"
            ))
            db.commit()
            return True
        else:
            event.status = "FAILED"
            event.retry_count += 1
            event.error_message = err
            db.commit()

            # Audit log for failure
            db.add(AuditLog(
                event_type="TELEGRAM_FAILED",
                entity_name="TelegramEvent",
                entity_id=str(event.id),
                action="ERROR",
                details=json.dumps({"status": "FAILED", "error": err, "retry_count": event.retry_count}),
                actor="TELEGRAM_SERVICE"
            ))
            db.commit()
            return False

    @classmethod
    def retry_failed_events(cls, db: Session, max_retries: int = 5) -> Dict[str, int]:
        """Scans the queue for PENDING or FAILED events and re-attempts delivery."""
        pending_events = (
            db.query(TelegramEvent)
            .filter(
                TelegramEvent.status.in_(["PENDING", "FAILED"]),
                TelegramEvent.retry_count < max_retries
            )
            .order_by(TelegramEvent.created_at.asc())
            .all()
        )

        retried_count = len(pending_events)
        success_count = 0
        failed_count = 0

        for evt in pending_events:
            if cls.send_event(db, evt):
                success_count += 1
            else:
                failed_count += 1

        return {
            "retried_count": retried_count,
            "success_count": success_count,
            "failed_count": failed_count,
        }

    # =========================================================================
    # High-Level Formatted Event Dispatchers
    # =========================================================================

    @classmethod
    def notify_new_order(
        cls,
        db: Session,
        order_number: str,
        customer_name: str,
        city: Optional[str],
        total_price: float,
        estimated_profit: float,
        item_count: int,
        items_summary: Optional[str] = None
    ) -> TelegramEvent:
        """Formats and queues a new Trendyol order notification."""
        profit_emoji = "🟢" if estimated_profit >= 0 else "🔴"
        profit_text = f"+₺{estimated_profit:.2f}" if estimated_profit >= 0 else f"-₺{abs(estimated_profit):.2f}"

        msg = (
            f"🛒 <b>YENİ TRENDYOL SİPARİŞİ</b>\n\n"
            f"<b>Sipariş No:</b> <code>{order_number}</code>\n"
            f"<b>Müşteri:</b> {customer_name} ({city or 'Türkiye'})\n"
            f"<b>Ürün Kalemi:</b> {item_count} adet\n"
        )
        if items_summary:
            msg += f"<b>Detay:</b> {items_summary}\n"

        msg += (
            f"<b>Satış Tutarı:</b> ₺{total_price:.2f}\n"
            f"<b>Tahmini Net Kâr:</b> {profit_emoji} <b>{profit_text}</b>\n\n"
            f"<i>M&E Tekstil ERP üzerinden siparişi hazırlayabilirsiniz.</i>"
        )
        return cls.queue_event(db, "NEW_ORDER", msg)

    @classmethod
    def notify_low_stock(
        cls,
        db: Session,
        product_name: str,
        barcode: str,
        current_stock: int,
        threshold: int = 5,
        warehouse_name: str = "Ana Depo"
    ) -> TelegramEvent:
        """Formats and queues a low stock warning."""
        msg = (
            f"⚠️ <b>KRİTİK STOK UYARISI!</b>\n\n"
            f"<b>Ürün:</b> {product_name}\n"
            f"<b>Barkod:</b> <code>{barcode}</code>\n"
            f"<b>Depo:</b> {warehouse_name}\n"
            f"<b>Mevcut Stok:</b> <b>{current_stock} adet</b> (Kritik Eşik: {threshold})\n\n"
            f"🚨 <i>Stok tükenme riski! Lütfen tedarik veya Trendyol stok güncellemesi yapınız.</i>"
        )
        return cls.queue_event(db, "LOW_STOCK", msg)

    @classmethod
    def notify_new_question(
        cls,
        db: Session,
        question_id: int,
        customer_name: str,
        product_name: str,
        question_text: str,
        has_ai_draft: bool = False
    ) -> TelegramEvent:
        """Formats and queues a customer question notification."""
        msg = (
            f"💬 <b>YENİ MÜŞTERİ SORUSU GELDİ</b>\n\n"
            f"<b>Soru ID:</b> #{question_id}\n"
            f"<b>Müşteri:</b> {customer_name}\n"
            f"<b>İlgili Ürün:</b> {product_name}\n"
            f"<b>Soru:</b> <i>\"{question_text}\"</i>\n\n"
        )
        if has_ai_draft:
            msg += "🤖 <i>AI taslak yanıt hazırladı. ERP panelinden onaylayıp Trendyol'a iletebilirsiniz.</i>"
        else:
            msg += "<i>Lütfen ERP Sorular sekmesinden yanıtlayınız.</i>"

        return cls.queue_event(db, "NEW_QUESTION", msg)

    @classmethod
    def notify_system_error(
        cls,
        db: Session,
        service_name: str,
        error_title: str,
        error_detail: str
    ) -> TelegramEvent:
        """Formats and queues a system / integration error alert."""
        msg = (
            f"🚨 <b>SİSTEM / ENTEGRASYON HATASI</b>\n\n"
            f"<b>Servis:</b> {service_name}\n"
            f"<b>Hata Başlığı:</b> {error_title}\n"
            f"<b>Detay:</b> <code>{error_detail[:300]}</code>\n\n"
            f"<i>Lütfen ERP Ayarlar veya Entegrasyon sekmesini kontrol ediniz.</i>"
        )
        return cls.queue_event(db, "SYSTEM_ERROR", msg)

    @classmethod
    def notify_daily_digest(
        cls,
        db: Session,
        date_str: str,
        total_orders: int,
        gross_sales: float,
        net_profit: float,
        margin_percent: float,
        total_questions: int = 0
    ) -> TelegramEvent:
        """Formats and queues a daily financial digest."""
        profit_emoji = "🟢" if net_profit >= 0 else "🔴"
        msg = (
            f"📊 <b>GÜNLÜK SATIŞ & KÂR ÖZETİ ({date_str})</b>\n\n"
            f"📦 <b>Toplam Sipariş:</b> {total_orders} adet\n"
            f"💰 <b>Brüt Ciro:</b> ₺{gross_sales:.2f}\n"
            f"{profit_emoji} <b>Net Kâr:</b> ₺{net_profit:.2f} (%{margin_percent:.1f} Marj)\n"
            f"💬 <b>Gelen Sorular:</b> {total_questions} adet\n\n"
            f"<i>M&E Tekstil Akıllı ERP Günlük Raporu</i>"
        )
        return cls.queue_event(db, "DAILY_DIGEST", msg)
