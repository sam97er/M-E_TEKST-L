"""
Custom & Local LLM Adapter (e.g. Ollama, vLLM, LocalAI, Termux hosted model).
Provides full autonomy without dependence on external commercial clouds.
"""

import time
import httpx
from typing import Optional, Dict, Any
from backend.app.services.ai.base import (
    BaseAIAdapter,
    AIGenerationResult,
    AIImageAnalysisResult,
)


class CustomLLMAdapter(BaseAIAdapter):
    DEFAULT_BASE_URL = "http://localhost:11434/v1"

    def __init__(
        self,
        api_key: Optional[str] = None,
        model: str = "llama-3",
        base_url: Optional[str] = None,
        timeout_seconds: int = 45,
    ):
        super().__init__(api_key, model, base_url or self.DEFAULT_BASE_URL, timeout_seconds)

    async def test_connection(self) -> Dict[str, Any]:
        url = f"{self.base_url.rstrip('/')}/models"
        headers = {}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                response = await client.get(url, headers=headers)
                latency = round((time.perf_counter() - start) * 1000, 2)

                if response.status_code == 200:
                    models_data = response.json()
                    available_models = [m.get("id") for m in models_data.get("data", [])]
                    return {
                        "success": True,
                        "provider": "custom_llm",
                        "model": self.model,
                        "available_models": available_models[:5],
                        "reply": f"Yerel LLM sunucusu aktif ({len(available_models)} model mevcut)",
                        "latency_ms": latency,
                    }
                else:
                    return {
                        "success": False,
                        "provider": "custom_llm",
                        "model": self.model,
                        "error": f"Yerel LLM sunucu yanıtı: HTTP {response.status_code}",
                        "latency_ms": latency,
                    }
        except httpx.ConnectError:
            return {
                "success": False,
                "provider": "custom_llm",
                "model": self.model,
                "error": f"Yerel LLM sunucusuna bağlanılamadı ({self.base_url}). Sunucunun çalıştığından emin olun.",
                "latency_ms": round((time.perf_counter() - start) * 1000, 2),
            }
        except Exception as exc:
            return {
                "success": False,
                "provider": "custom_llm",
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
        url = f"{self.base_url.rstrip('/')}/chat/completions"
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

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

                    return AIGenerationResult(
                        success=True,
                        text=content,
                        provider_name="custom_llm",
                        model=self.model,
                        latency_ms=latency,
                    )
                else:
                    return AIGenerationResult(
                        success=False,
                        provider_name="custom_llm",
                        model=self.model,
                        latency_ms=latency,
                        error_message=f"Yerel LLM Hatası: HTTP {response.status_code}",
                    )
        except Exception as exc:
            return AIGenerationResult(
                success=False,
                provider_name="custom_llm",
                model=self.model,
                latency_ms=round((time.perf_counter() - start) * 1000, 2),
                error_message=str(exc),
            )

    async def analyze_image(
        self,
        image_url_or_b64: str,
        prompt: str,
    ) -> AIImageAnalysisResult:
        # Fallback or pass to vision-capable local model (e.g. llava)
        return AIImageAnalysisResult(
            success=False,
            provider_name="custom_llm",
            model=self.model,
            error_message="Yerel modelde görsel analiz desteği yapılandırılmadı (Llava veya multimodal model gereklidir).",
        )
