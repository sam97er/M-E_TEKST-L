"""
OpenAI & OpenAI-compatible Adapter implementation using direct REST API via httpx.
Supports GPT-4o, GPT-4o mini, and compatible endpoints.
"""

import time
import httpx
from typing import Optional, Dict, Any
from backend.app.services.ai.base import (
    BaseAIAdapter,
    AIGenerationResult,
    AIImageAnalysisResult,
)


class OpenAIAdapter(BaseAIAdapter):
    DEFAULT_BASE_URL = "https://api.openai.com/v1"

    def __init__(
        self,
        api_key: Optional[str],
        model: str = "gpt-4o-mini",
        base_url: Optional[str] = None,
        timeout_seconds: int = 30,
    ):
        super().__init__(api_key, model, base_url or self.DEFAULT_BASE_URL, timeout_seconds)

    async def test_connection(self) -> Dict[str, Any]:
        if not self.api_key:
            return {
                "success": False,
                "provider": "openai",
                "model": self.model,
                "error": "API anahtarı yapılandırılmamış (Empty API key)",
                "latency_ms": 0.0,
            }

        url = f"{self.base_url.rstrip('/')}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": "M&E Tekstil ERP test connection. Reply with 'OK'."}],
            "max_tokens": 5,
        }

        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                response = await client.post(url, headers=headers, json=payload)
                latency = round((time.perf_counter() - start) * 1000, 2)

                if response.status_code == 200:
                    data = response.json()
                    choices = data.get("choices", [])
                    reply = choices[0].get("message", {}).get("content", "").strip() if choices else "OK"
                    return {
                        "success": True,
                        "provider": "openai",
                        "model": self.model,
                        "reply": reply,
                        "latency_ms": latency,
                    }
                else:
                    error_msg = response.json().get("error", {}).get("message", response.text)
                    return {
                        "success": False,
                        "provider": "openai",
                        "model": self.model,
                        "error": f"OpenAI API Hatası ({response.status_code}): {error_msg}",
                        "latency_ms": latency,
                    }
        except httpx.TimeoutException:
            return {
                "success": False,
                "provider": "openai",
                "model": self.model,
                "error": f"Bağlantı zaman aşımı ({self.timeout_seconds}s)",
                "latency_ms": round((time.perf_counter() - start) * 1000, 2),
            }
        except Exception as exc:
            return {
                "success": False,
                "provider": "openai",
                "model": self.model,
                "error": f"Bağlantı hatası: {str(exc)}",
                "latency_ms": round((time.perf_counter() - start) * 1000, 2),
            }

    async def generate_text(
        self,
        prompt: str,
        system_instruction: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1000,
    ) -> AIGenerationResult:
        if not self.api_key:
            return AIGenerationResult(
                success=False,
                provider_name="openai",
                model=self.model,
                error_message="OpenAI API anahtarı yapılandırılmamış.",
            )

        url = f"{self.base_url.rstrip('/')}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        messages = []
        if system_instruction:
            messages.append({"role": "system", "content": system_instruction})
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                response = await client.post(url, headers=headers, json=payload)
                latency = round((time.perf_counter() - start) * 1000, 2)

                if response.status_code == 200:
                    data = response.json()
                    choices = data.get("choices", [])
                    content = choices[0].get("message", {}).get("content", "").strip() if choices else ""
                    usage = data.get("usage", {})
                    tokens = usage.get("total_tokens")

                    return AIGenerationResult(
                        success=True,
                        text=content,
                        provider_name="openai",
                        model=self.model,
                        latency_ms=latency,
                        tokens_used=tokens,
                    )
                else:
                    err = response.json().get("error", {}).get("message", response.text)
                    return AIGenerationResult(
                        success=False,
                        provider_name="openai",
                        model=self.model,
                        latency_ms=latency,
                        error_message=f"OpenAI Hatası: {err}",
                    )
        except Exception as exc:
            return AIGenerationResult(
                success=False,
                provider_name="openai",
                model=self.model,
                latency_ms=round((time.perf_counter() - start) * 1000, 2),
                error_message=str(exc),
            )

    async def analyze_image(
        self,
        image_url_or_b64: str,
        prompt: str,
    ) -> AIImageAnalysisResult:
        if not self.api_key:
            return AIImageAnalysisResult(
                success=False,
                provider_name="openai",
                model=self.model,
                error_message="OpenAI API anahtarı eksik.",
            )

        url = f"{self.base_url.rstrip('/')}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {"type": "image_url", "image_url": {"url": image_url_or_b64}},
                ],
            }
        ]

        payload = {
            "model": self.model if "gpt-4" in self.model else "gpt-4o-mini",
            "messages": messages,
            "max_tokens": 500,
        }

        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                response = await client.post(url, headers=headers, json=payload)
                latency = round((time.perf_counter() - start) * 1000, 2)

                if response.status_code == 200:
                    data = response.json()
                    choices = data.get("choices", [])
                    content = choices[0].get("message", {}).get("content", "").strip() if choices else ""

                    return AIImageAnalysisResult(
                        success=True,
                        score=8.0,
                        review_status="APPROVED",
                        feedback=content,
                        provider_name="openai",
                        model=self.model,
                        latency_ms=latency,
                    )
                else:
                    err = response.json().get("error", {}).get("message", response.text)
                    return AIImageAnalysisResult(
                        success=False,
                        provider_name="openai",
                        model=self.model,
                        latency_ms=latency,
                        error_message=err,
                    )
        except Exception as exc:
            return AIImageAnalysisResult(
                success=False,
                provider_name="openai",
                model=self.model,
                latency_ms=round((time.perf_counter() - start) * 1000, 2),
                error_message=str(exc),
            )
