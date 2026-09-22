"""
Unit and Integration Tests for Phase 5:
Customer Questions & AI Reply Engine with Mandatory Human Approval.
"""

import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.database.session import get_db, SessionLocal
from backend.app.models.customer_question import CustomerQuestion, AIDraft
from backend.app.models.product import Product
from backend.app.models.system import AuditLog

client = TestClient(app)


@pytest.fixture(autouse=True)
def clean_questions_data():
    """Ensures test database is prepared with a fresh sample question and product."""
    db = SessionLocal()
    try:
        # Create a test product if not exists
        prod = db.query(Product).filter(Product.product_code == "TEST-SW-501").first()
        if not prod:
            prod = Product(
                product_code="TEST-SW-501",
                title="Oversize Baskılı Kapüşonlu Sweatshirt",
                description="%100 Pamuklu 3 iplik şardonlu kumaş. Rahat kalıp.",
                category_name="Sweatshirt",
                brand="M&E Tekstil",
                selling_price=499.90,
                status="ACTIVE",
            )
            db.add(prod)
            db.commit()
            db.refresh(prod)

        # Seed sample question
        q = db.query(CustomerQuestion).filter(CustomerQuestion.trendyol_question_id == "TY-TEST-Q-999").first()
        if not q:
            q = CustomerQuestion(
                trendyol_question_id="TY-TEST-Q-999",
                product_id=prod.id,
                customer_name="Test Müşteri",
                question_text="Boyum 1.82, kilom 78. Hangi bedeni önerirsiniz? Yıkamada çekme yapar mı?",
                status="NEW",
            )
            db.add(q)
        else:
            q.status = "NEW"
            # remove past test drafts
            db.query(AIDraft).filter(AIDraft.question_id == q.id).delete()
        db.commit()
    finally:
        db.close()


def test_list_and_summary_questions():
    """Verifies listing questions and retrieving KPI summary."""
    res_list = client.get("/api/v1/questions")
    assert res_list.status_code == 200
    questions = res_list.json()
    assert isinstance(questions, list)
    assert len(questions) > 0

    res_summary = client.get("/api/v1/questions/summary")
    assert res_summary.status_code == 200
    summary = res_summary.json()
    assert summary["total_count"] >= 1
    assert "new_count" in summary
    assert "approved_count" in summary


def test_sync_questions_from_trendyol():
    """Verifies that questions can be synced from Trendyol."""
    res = client.post("/api/v1/questions/sync")
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True


def test_full_ai_draft_and_human_approval_workflow():
    """
    End-to-end test of the strict human-in-the-loop lifecycle:
    1. Question starts as NEW.
    2. AI draft is generated -> status becomes DRAFT_GENERATED, draft is DRAFT.
    3. Human operator edits draft text.
    4. ATTEMPT TO SEND UNAPPROVED DRAFT MUST FAIL (Zero unapproved send rule).
    5. Human operator explicitly approves draft -> status becomes APPROVED.
    6. Approved draft is transmitted to Trendyol -> status becomes SENT.
    """
    db = SessionLocal()
    question = db.query(CustomerQuestion).filter(CustomerQuestion.trendyol_question_id == "TY-TEST-Q-999").first()
    qid = question.id
    db.close()

    # Step 2: Generate AI draft
    gen_res = client.post(
        f"/api/v1/questions/{qid}/draft",
        json={"tone": "KURUMSAL", "additional_instructions": "L beden tavsiye et."},
    )
    assert gen_res.status_code == 200
    draft_data = gen_res.json()
    draft_id = draft_data["id"]
    assert draft_data["status"] == "DRAFT"
    assert len(draft_data["generated_answer"]) > 20
    assert "M&E Tekstil" in draft_data["generated_answer"] or "Merhaba" in draft_data["generated_answer"]

    # Verify question status updated
    q_check = client.get(f"/api/v1/questions/{qid}").json()
    assert q_check["status"] == "DRAFT_GENERATED"

    # Step 3: Human edits draft
    custom_edited_text = "Merhaba Sayın Müşterimiz, 1.82 boy ve 78 kilo için L beden rahat oturacaktır. Yıkamada çekme yapmaz."
    edit_res = client.put(
        f"/api/v1/questions/drafts/{draft_id}",
        json={"edited_answer": custom_edited_text},
    )
    assert edit_res.status_code == 200
    assert edit_res.json()["user_edited_answer"] == custom_edited_text
    assert edit_res.json()["final_answer"] == custom_edited_text

    # Step 4: ZERO UNAPPROVED SEND RULE: Must reject sending while status is DRAFT
    blocked_send = client.post(
        f"/api/v1/questions/{qid}/send",
        json={"draft_id": draft_id, "operator_name": "Ahmet_Operator"},
    )
    assert blocked_send.status_code == 400
    assert "onay" in blocked_send.json()["detail"].lower()

    # Step 5: Human operator approves draft
    app_res = client.post(
        f"/api/v1/questions/drafts/{draft_id}/approve",
        json={"operator_name": "Ahmet_Operator"},
    )
    assert app_res.status_code == 200
    assert app_res.json()["status"] == "APPROVED"
    assert app_res.json()["approved_by"] == "Ahmet_Operator"

    # Step 6: Send approved reply to Trendyol
    send_res = client.post(
        f"/api/v1/questions/{qid}/send",
        json={"draft_id": draft_id, "operator_name": "Ahmet_Operator"},
    )
    assert send_res.status_code == 200
    send_data = send_res.json()
    assert send_data["success"] is True
    assert send_data["status"] == "SENT"

    # Verify in DB and AuditLog
    db = SessionLocal()
    q_final = db.query(CustomerQuestion).filter(CustomerQuestion.id == qid).first()
    assert q_final.status == "SENT"

    audits = db.query(AuditLog).filter(AuditLog.entity_name == "CustomerQuestion", AuditLog.entity_id == str(qid)).all()
    assert any(a.action == "REPLY_SENT" for a in audits)
    db.close()


def test_reject_draft_flow():
    """Verifies that an operator can reject an AI draft."""
    import time
    db = SessionLocal()
    unique_qid = f"TY-TEST-REJ-{int(time.time() * 1000)}"
    q = CustomerQuestion(
        trendyol_question_id=unique_qid,
        customer_name="Reddedilen Soru",
        question_text="Bu ürün su geçirir mi?",
        status="NEW",
    )
    db.add(q)
    db.commit()
    db.refresh(q)
    qid = q.id
    db.close()

    # Generate draft
    gen = client.post(f"/api/v1/questions/{qid}/draft", json={"tone": "SAMIMI"}).json()
    draft_id = gen["id"]

    # Reject
    rej_res = client.post(
        f"/api/v1/questions/drafts/{draft_id}/reject",
        json={"reason": "Soru anlamsız veya hatalı bilgi içeriyor."},
    )
    assert rej_res.status_code == 200
    assert rej_res.json()["status"] == "REJECTED"

    # Question status should now be REJECTED
    q_check = client.get(f"/api/v1/questions/{qid}").json()
    assert q_check["status"] == "REJECTED"
