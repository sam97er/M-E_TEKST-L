"""
Customer Question & AI Reply Workflow Service (Phase 5).
Adheres strictly to the human approval rule:
Zero automated responses are transmitted to Trendyol.
Lifecycle: NEW -> DRAFT_GENERATED -> APPROVED (Human Verified) -> SENT
"""

import logging
from datetime import datetime
from typing import Optional, Dict, Any, List
from sqlalchemy.orm import Session

from backend.app.models.customer_question import CustomerQuestion, AIDraft
from backend.app.models.product import Product, ProductVariant
from backend.app.models.inventory import InventoryBalance
from backend.app.models.system import AuditLog
from backend.app.services.ai.manager import AIProviderManager
from backend.app.services.trendyol.trendyol_service import TrendyolService

logger = logging.getLogger("metekstil_ai")


class QuestionReplyService:
    """Manages AI draft generation, manual review/edit, approval, and transmission."""

    @classmethod
    def get_product_context(cls, question: CustomerQuestion, db: Session) -> str:
        """Constructs detailed textile context for the AI prompt."""
        if not question.product_id:
            return "Ürün bilgisi: Belirtilmemiş genel Trendyol mağaza sorusu."

        product = db.query(Product).filter(Product.id == question.product_id).first()
        if not product:
            return "Ürün bilgisi: Ürün veritabanında bulunamadı."

        variants = db.query(ProductVariant).filter(ProductVariant.product_id == product.id).all()
        variant_details = []
        for v in variants:
            # Check stock
            balance = db.query(InventoryBalance).filter(InventoryBalance.variant_id == v.id).first()
            stock_qty = balance.quantity if balance else 0
            variant_details.append(f"- Renk: {v.color}, Beden: {v.size}, Stok: {stock_qty} adet, Fiyat: ₺{v.selling_price}")

        variants_str = "\n".join(variant_details) if variant_details else "Varyant bilgisi bulunamadı."

        context = f"""
Ürün Kodu: {product.product_code}
Ürün Adı: {product.title}
Kategori: {product.category_name or 'Tekstil / Giyim'}
Açıklama: {product.description or 'Belirtilmemiş'}
KDV Oranı: %{product.vat_rate}
Varyantlar ve Stok Durumu:
{variants_str}
"""
        return context.strip()

    @classmethod
    def build_system_instruction(cls, tone: str = "KURUMSAL") -> str:
        """Constructs Turkish prompt instruction based on selected tone."""
        tone_instruction = {
            "KURUMSAL": (
                "Son derece profesyonel, saygılı, kurumsal ve güven verici bir Türkçe müşteri hizmetleri üslubu kullan. "
                "Cümlelerine 'Değerli Müşterimiz,' veya 'Merhaba Sayın Müşterimiz,' gibi resmi ve şık bir hitapla başla. "
                "Sonunda 'M&E Tekstil olarak keyifli alışverişler dileriz.' gibi kurumsal bir kapanış yap."
            ),
            "SAMIMI": (
                "Sıcak, samimi, yardımsever ve enerjik bir üslup kullan. "
                "'Merhaba!' veya 'Merhaba [İsim] Hanım/Bey,' şeklinde başla. "
                "Müşterinin karar vermesini kolaylaştıracak tavsiyeler ver. "
                "'Güzel günlerde giymeniz dileğiyle!' şeklinde samimi bitir."
            ),
            "KISA_VE_OZ": (
                "Doğrudan soruya odaklanan, gereksiz uzatmalardan kaçınan, kısa ve net bilgi veren bir üslup kullan. "
                "Maksimum 2-3 cümlede müşterinin sorduğu beden, kumaş veya kargo sorusunu net bir şekilde yanıtla."
            )
        }.get(tone.upper(), "Profesyonel, kibar ve çözüm odaklı Türkçe bir müşteri hizmetleri dili kullan.")

        return f"""
Sen Türkiye'nin önde gelen tekstil üreticisi ve e-ticaret markası olan 'M&E Tekstil'in Trendyol mağazasında görevli yapay zekâ müşteri temsilcisisin.
Görevin, müşterilerin ürünler hakkında sorduğu sorulara (beden tavsiyesi, kumaş özellikleri, çekme/solma, yıkama talimatı, kargo süresi vb.) eksiksiz, doğru ve nazik yanıtlar hazırlamaktır.

Üslup Rehberi:
{tone_instruction}

Katı Kurallar ve Bilgiler:
1. M&E Tekstil ürünleri yüksek kaliteli penye, pamuk ve modal kumaşlardan üretilir. Yıkama talimatlarına uyulduğunda (30°C tersten yıkama) çekme veya renk atması yapmaz.
2. Müşteri boy ve kilo vererek beden sorduysa:
   - Standart/Regular kalıplarda genel oranları dikkate alarak uygun bedeni öner.
   - Eğer müşteri dökümlü veya oversize giymeyi seviyorsa bir beden büyük tercih edebileceğini nazikçe belirt.
3. Kargo sorularında: Siparişlerin aynı gün veya en geç 24 saat içinde özenle paketlenip Trendyol Express veya anlaşmalı kargo firmalarına teslim edildiğini belirt.
4. Asla teyit edilmemiş hayali vaatlerde bulunma. Stokta olmayan varyantlar için mağazayı takipte kalmalarını ve 'Gelince Haber Ver' butonuna tıklayabileceklerini hatırlat.
5. Asla başka rakip pazaryerlerinin adını (Hepsiburada, Amazon vb.) veya şirket dışı telefon/IBAN gibi bilgileri yanıta ekleme. Sadece Trendyol içi iletişim kurallarına uy.
""".strip()

    @classmethod
    async def generate_draft_for_question(
        cls,
        question_id: int,
        provider_slot: Optional[int] = None,
        tone: str = "KURUMSAL",
        additional_instructions: Optional[str] = None,
        db: Optional[Session] = None,
    ) -> AIDraft:
        """
        Generates an AI response draft for a given customer question.
        Enforces: Status becomes DRAFT. Never automatically sent to marketplace.
        """
        if db is None:
            raise ValueError("Database session is required")

        question = db.query(CustomerQuestion).filter(CustomerQuestion.id == question_id).first()
        if not question:
            raise ValueError(f"Soru ID bulunamadı: {question_id}")

        product_context = cls.get_product_context(question, db)
        system_instruction = cls.build_system_instruction(tone)

        prompt = f"""
{product_context}

Müşteri Adı: {question.customer_name}
Müşterinin Sorusu: "{question.question_text}"
{f'Ek Operatör Talimatı: {additional_instructions}' if additional_instructions else ''}

Lütfen bu soruya yukarıdaki ürün bilgilerini ve belirlenen üslubu dikkate alarak eksiksiz, samimi ve satışa dönüştürücü Türkçe bir müşteri yanıtı hazırla.
""".strip()

        # Route to requested provider slot or task-assigned provider
        slot = provider_slot if provider_slot else AIProviderManager.find_provider_for_task("customer_questions", db)
        if slot is None:
            slot = 1

        cfg = AIProviderManager.get_slot_config(slot, db)
        provider_name = cfg.get("provider_name", "gemini")
        model_name = cfg.get("model", "gemini-2.5-flash")

        # Execute text generation
        try:
            adapter = AIProviderManager.get_adapter_for_slot(slot, db)
            res = await adapter.generate_text(prompt, system_instruction=system_instruction)
            if res.success and res.text.strip():
                generated_text = res.text.strip()
            else:
                logger.warning(f"AI generation failed ({res.error_message}), using intelligent fallback template.")
                generated_text = cls._generate_contextual_fallback(question, tone)
        except Exception as e:
            logger.exception("AI generation call threw exception")
            generated_text = cls._generate_contextual_fallback(question, tone)

        # Create Draft in DB
        draft = AIDraft(
            question_id=question.id,
            provider_name=provider_name,
            model_used=model_name,
            raw_prompt=prompt,
            generated_answer=generated_text,
            user_edited_answer=None,
            status="DRAFT",
        )
        db.add(draft)
        question.status = "DRAFT_GENERATED"

        # Record AuditLog
        audit = AuditLog(
            event_type="AI_EVENT",
            entity_name="AIDraft",
            entity_id=str(question.id),
            action="GENERATE_DRAFT",
            details=f"Yapay zekâ yanıt taslağı oluşturuldu. Sağlayıcı: {provider_name}, Model: {model_name}, Ton: {tone}.",
            actor="SYSTEM_AI",
        )
        db.add(audit)
        db.commit()
        db.refresh(draft)
        db.refresh(question)

        return draft

    @classmethod
    def _generate_contextual_fallback(cls, question: CustomerQuestion, tone: str) -> str:
        """Creates a high quality rule-based fallback response if the LLM is temporarily unreachable."""
        q_lower = question.question_text.lower()
        if "beden" in q_lower or "boy" in q_lower or "kilo" in q_lower:
            return (
                f"Merhaba Sayın {question.customer_name}, M&E Tekstil'e gösterdiğiniz ilgi için teşekkür ederiz. "
                "Ürünümüz standart rahat kalıptır. Boy ve kilo ölçülerinize göre tam oturması için normal bedeninizi, "
                "daha dökümlü ve rahat bir duruş isterseniz bir beden büyüğünü tercih edebilirsiniz. "
                "Keyifli alışverişler dileriz."
            )
        elif "kumaş" in q_lower or "pamuk" in q_lower or "çekme" in q_lower:
            return (
                f"Merhaba Sayın {question.customer_name}, ürünümüz 1. sınıf yüksek kaliteli pamuklu kumaştan üretilmiştir. "
                "30 derecede tersten yıkandığı sürece çekme ya da renk solması yapmaz. Güvenle tercih edebilirsiniz. "
                "M&E Tekstil ailesi olarak sağlıklı günler dileriz."
            )
        elif "kargo" in q_lower or "ne zaman" in q_lower or "gönder" in q_lower:
            return (
                f"Merhaba Sayın {question.customer_name}, siparişleriniz aynı gün veya en geç 24 saat içerisinde "
                "özenle hazırlanarak Trendyol Express ile kargoya teslim edilmektedir. Kargonuz yola çıktığında SMS ve "
                "bildirim ile bilgilendirileceksiniz. Şimdiden iyi günlerde kullanmanızı dileriz."
            )
        else:
            return (
                f"Merhaba Sayın {question.customer_name}, M&E Tekstil'e gösterdiğiniz ilgi için çok teşekkür ederiz. "
                "Sorunuz ilgili departmanımız tarafından incelenmiştir. Ürünümüz yüksek üretim standartlarında hazırlanmış olup "
                "gönül rahatlığıyla sipariş verebilirsiniz. Başka bir konuda yardımcı olabilirsek her zaman buradayız."
            )

    @classmethod
    def edit_draft(cls, draft_id: int, edited_answer: str, db: Session) -> AIDraft:
        """Allows human operator to edit the AI generated response before approval."""
        draft = db.query(AIDraft).filter(AIDraft.id == draft_id).first()
        if not draft:
            raise ValueError(f"Taslak bulunamadı: {draft_id}")

        draft.user_edited_answer = edited_answer.strip()
        db.commit()
        db.refresh(draft)
        return draft

    @classmethod
    def approve_draft(cls, draft_id: int, operator_name: str, db: Session) -> AIDraft:
        """Marks draft as human-verified and APPROVED."""
        draft = db.query(AIDraft).filter(AIDraft.id == draft_id).first()
        if not draft:
            raise ValueError(f"Taslak bulunamadı: {draft_id}")

        draft.status = "APPROVED"
        draft.approved_by = operator_name
        draft.approved_at = datetime.utcnow()

        question = db.query(CustomerQuestion).filter(CustomerQuestion.id == draft.question_id).first()
        if question:
            question.status = "APPROVED"

        audit = AuditLog(
            event_type="OPERATOR_EVENT",
            entity_name="AIDraft",
            entity_id=str(draft.id),
            action="APPROVE_DRAFT",
            details=f"Operatör {operator_name} tarafından onaylandı. Soru ID: {draft.question_id}",
            actor=operator_name,
        )
        db.add(audit)
        db.commit()
        db.refresh(draft)
        return draft

    @classmethod
    def reject_draft(cls, draft_id: int, reason: Optional[str], db: Session) -> AIDraft:
        """Rejects draft and sets question status back to NEW or REJECTED."""
        draft = db.query(AIDraft).filter(AIDraft.id == draft_id).first()
        if not draft:
            raise ValueError(f"Taslak bulunamadı: {draft_id}")

        draft.status = "REJECTED"
        draft.error_message = reason

        question = db.query(CustomerQuestion).filter(CustomerQuestion.id == draft.question_id).first()
        if question:
            question.status = "REJECTED"

        audit = AuditLog(
            event_type="OPERATOR_EVENT",
            entity_name="AIDraft",
            entity_id=str(draft.id),
            action="REJECT_DRAFT",
            details=f"Taslak reddedildi. Sebep: {reason or 'Belirtilmedi'}",
            actor="OPERATOR",
        )
        db.add(audit)
        db.commit()
        db.refresh(draft)
        return draft

    @classmethod
    def send_reply_to_trendyol(
        cls,
        question_id: int,
        draft_id: Optional[int],
        operator_name: str,
        db: Session
    ) -> Dict[str, Any]:
        """
        Strict Human-in-the-Loop check:
        Verifies draft is APPROVED before sending.
        Transmits text to Trendyol Partner API.
        """
        question = db.query(CustomerQuestion).filter(CustomerQuestion.id == question_id).first()
        if not question:
            raise ValueError(f"Soru bulunamadı: {question_id}")

        # Find the target draft
        if draft_id:
            draft = db.query(AIDraft).filter(AIDraft.id == draft_id, AIDraft.question_id == question_id).first()
        else:
            # Pick latest approved draft
            draft = (
                db.query(AIDraft)
                .filter(AIDraft.question_id == question_id, AIDraft.status == "APPROVED")
                .order_by(AIDraft.id.desc())
                .first()
            )

        if not draft:
            raise ValueError("Gönderilecek onaylı (APPROVED) bir taslak bulunamadı! Lütfen önce taslağı onaylayınız.")

        # CRITICAL RULE: Human Approval Enforced
        if draft.status != "APPROVED":
            raise ValueError(
                f"Güvenlik Kuralı İhlali: Bu taslak onaylanmamış (Mevcut Durum: {draft.status}). "
                "Trendyol'a sadece operatör tarafından incelenip onaylanan yanıtlar gönderilebilir."
            )

        final_answer = draft.final_answer

        # Call Trendyol API service
        api_res = TrendyolService.send_question_answer(
            trendyol_question_id=question.trendyol_question_id,
            answer_text=final_answer,
            db=db,
        )

        if api_res.get("success"):
            draft.status = "SENT"
            draft.sent_at = datetime.utcnow()
            question.status = "SENT"

            audit = AuditLog(
                event_type="MARKETPLACE_EVENT",
                entity_name="CustomerQuestion",
                entity_id=str(question.id),
                action="REPLY_SENT",
                details=f"Trendyol sorusu yanıtlandı. Soru ID: {question.trendyol_question_id}, Onaylayan: {draft.approved_by}, Gönderen: {operator_name}.",
                actor=operator_name,
            )
            db.add(audit)
            db.commit()

            return {
                "success": True,
                "status": "SENT",
                "message": "Cevap Trendyol'a başarıyla iletildi.",
                "question_id": question.id,
                "draft_id": draft.id,
                "sent_at": draft.sent_at.isoformat(),
                "details": api_res,
            }
        else:
            draft.status = "FAILED"
            draft.error_message = api_res.get("message")
            question.status = "FAILED"
            db.commit()

            return {
                "success": False,
                "status": "FAILED",
                "message": f"Trendyol'a gönderim başarısız: {api_res.get('message')}",
                "question_id": question.id,
                "draft_id": draft.id,
                "details": api_res,
            }
