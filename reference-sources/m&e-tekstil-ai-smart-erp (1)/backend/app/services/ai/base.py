"""
Abstract Base Adapter and Data Transfer Objects for AI Providers.
Enforces separation of text generation from visual image analysis.
"""

from abc import ABC, abstractmethod
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field


class AIGenerationResult(BaseModel):
    success: bool
    text: str = ""
    provider_name: str
    model: str
    latency_ms: float = 0.0
    tokens_used: Optional[int] = None
    error_message: Optional[str] = None


class AIImageAnalysisResult(BaseModel):
    success: bool
    score: Optional[float] = None  # 0.0 to 10.0
    review_status: str = "PENDING"  # APPROVED, NEEDS_REVIEW, REJECTED
    feedback: str = ""
    suggestions: List[str] = Field(default_factory=list)
    provider_name: str
    model: str
    latency_ms: float = 0.0
    error_message: Optional[str] = None


class BaseAIAdapter(ABC):
    """Abstract Interface for any AI Provider (Gemini, OpenAI, Anthropic, Local LLM)."""

    def __init__(
        self,
        api_key: Optional[str],
        model: str,
        base_url: Optional[str] = None,
        timeout_seconds: int = 30,
    ):
        self.api_key = api_key
        self.model = model
        self.base_url = base_url
        self.timeout_seconds = timeout_seconds

    @abstractmethod
    async def test_connection(self) -> Dict[str, Any]:
        """Validates API credentials and model availability."""
        pass

    @abstractmethod
    async def generate_text(
        self,
        prompt: str,
        system_instruction: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1000,
    ) -> AIGenerationResult:
        """Generates textual answers (e.g. Trendyol customer reply or report)."""
        pass

    @abstractmethod
    async def analyze_image(
        self,
        image_url_or_b64: str,
        prompt: str,
    ) -> AIImageAnalysisResult:
        """Analyzes product garment images for marketplace compliance and quality."""
        pass
