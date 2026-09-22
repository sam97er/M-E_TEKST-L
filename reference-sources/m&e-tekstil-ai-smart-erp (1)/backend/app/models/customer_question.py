"""
Customer Questions, AI Draft Responses, and Human Approval Workflow Models.
Rule: Zero automatic response sending. Human approval is mandatory.
"""

from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, Text, DateTime, Index
from sqlalchemy.orm import relationship
from backend.app.database.session import Base
from backend.app.models.base import TimestampMixin


class CustomerQuestion(Base, TimestampMixin):
    __tablename__ = "customer_questions"

    id = Column(Integer, primary_key=True, index=True)
    trendyol_question_id = Column(String(100), unique=True, nullable=False, index=True)
    product_id = Column(Integer, ForeignKey("products.id", ondelete="SET NULL"), nullable=True, index=True)
    customer_name = Column(String(100), default="Trendyol Müşterisi", nullable=False)
    question_text = Column(Text, nullable=False)
    
    # Status lifecycle: NEW, DRAFT_GENERATED, NEEDS_REVIEW, APPROVED, SENT, FAILED, REJECTED
    status = Column(String(50), default="NEW", nullable=False, index=True)
    trendyol_created_at = Column(DateTime, nullable=True)

    product = relationship("Product", back_populates="questions")
    drafts = relationship("AIDraft", back_populates="question", cascade="all, delete-orphan")


class AIDraft(Base, TimestampMixin):
    __tablename__ = "ai_drafts"

    id = Column(Integer, primary_key=True, index=True)
    question_id = Column(Integer, ForeignKey("customer_questions.id", ondelete="CASCADE"), nullable=False, index=True)
    provider_name = Column(String(50), nullable=False)  # gemini, openai, custom_llm
    model_used = Column(String(100), nullable=False)
    raw_prompt = Column(Text, nullable=True)
    generated_answer = Column(Text, nullable=False)  # AI Generated Turkish reply
    user_edited_answer = Column(Text, nullable=True)  # Human edited text if modified
    
    # Status: DRAFT, APPROVED, SENT, FAILED, REJECTED
    status = Column(String(50), default="DRAFT", nullable=False)
    approved_by = Column(String(100), nullable=True)  # Operator username/id
    approved_at = Column(DateTime, nullable=True)
    sent_at = Column(DateTime, nullable=True)
    error_message = Column(Text, nullable=True)

    question = relationship("CustomerQuestion", back_populates="drafts")

    @property
    def final_answer(self) -> str:
        return self.user_edited_answer or self.generated_answer
