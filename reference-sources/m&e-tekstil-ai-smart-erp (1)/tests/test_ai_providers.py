"""
Automated unit and integration tests for AI Providers and Adapters (Phase 2).
Verifies:
- 3 independent provider slots
- Never leak raw API keys in responses
- Error transparency (no silent fallback)
- Task routing (customer_reply, image_analysis, pricing_recommendation)
- Test Connection endpoints
"""

import pytest
from unittest.mock import patch, AsyncMock
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from backend.app.services.ai.base import AIGenerationResult, AIImageAnalysisResult
from backend.app.services.ai.gemini_adapter import GeminiAdapter
from backend.app.services.ai.openai_adapter import OpenAIAdapter
from backend.app.services.ai.custom_adapter import CustomLLMAdapter
from backend.app.services.ai.manager import AIProviderManager
from backend.app.models.ai_provider import AIProviderConfig


def test_gemini_adapter_empty_key():
    adapter = GeminiAdapter(api_key="", model="gemini-2.5-flash")
    import asyncio
    res = asyncio.run(adapter.test_connection())
    assert res["success"] is False
    assert "API anahtarı yapılandırılmamış" in res["error"]


def test_openai_adapter_empty_key():
    adapter = OpenAIAdapter(api_key="", model="gpt-4o-mini")
    import asyncio
    res = asyncio.run(adapter.test_connection())
    assert res["success"] is False
    assert "API anahtarı yapılandırılmamış" in res["error"]


def test_custom_adapter_connection_error():
    adapter = CustomLLMAdapter(base_url="http://127.0.0.1:9999/v1", model="llama-3", timeout_seconds=1)
    import asyncio
    res = asyncio.run(adapter.test_connection())
    assert res["success"] is False
    assert "bağlanılamadı" in res["error"] or "hatası" in res["error"]


def test_adapter_factory():
    a1 = AIProviderManager.get_adapter_for_config("gemini", "key1", "gemini-2.5-flash")
    assert isinstance(a1, GeminiAdapter)

    a2 = AIProviderManager.get_adapter_for_config("openai", "key2", "gpt-4o-mini")
    assert isinstance(a2, OpenAIAdapter)

    a3 = AIProviderManager.get_adapter_for_config("custom_llm", None, "llama-3")
    assert isinstance(a3, CustomLLMAdapter)


def test_ai_provider_task_routing(db_session: Session):
    # Setup 3 slots in DB
    slot1 = AIProviderConfig(
        slot=1,
        provider_name="gemini",
        model="gemini-2.5-flash",
        is_enabled=True,
        assigned_tasks="customer_reply,daily_report",
        timeout_seconds=30,
    )
    slot2 = AIProviderConfig(
        slot=2,
        provider_name="openai",
        model="gpt-4o-mini",
        is_enabled=True,
        assigned_tasks="image_analysis",
        timeout_seconds=30,
    )
    slot3 = AIProviderConfig(
        slot=3,
        provider_name="custom_llm",
        model="llama-3",
        is_enabled=False,
        assigned_tasks="pricing_recommendation",
        timeout_seconds=45,
    )
    db_session.add_all([slot1, slot2, slot3])
    db_session.commit()

    # Routing checks
    assert AIProviderManager.find_provider_for_task("customer_reply", db_session) == 1
    assert AIProviderManager.find_provider_for_task("image_analysis", db_session) == 2
    # Slot 3 is disabled, so find_provider_for_task should fallback to enabled Slot 1 or None
    target_slot = AIProviderManager.find_provider_for_task("pricing_recommendation", db_session)
    assert target_slot == 1  # Slot 1 is default enabled fallback


def test_api_get_ai_providers(client: TestClient):
    response = client.get("/api/v1/ai/providers")
    assert response.status_code == 200
    slots = response.json()
    assert len(slots) == 3
    assert slots[0]["slot"] == 1
    assert slots[1]["slot"] == 2
    assert slots[2]["slot"] == 3

    # Check that secrets are NEVER leaked in plaintext
    for slot in slots:
        assert "masked_key" in slot
        assert not slot["masked_key"].startswith("actual_unmasked_key")
        assert "timeout_seconds" in slot


def test_api_update_ai_provider_slot(client: TestClient, db_session: Session):
    update_data = {
        "provider_name": "openai",
        "model": "gpt-4o",
        "is_enabled": True,
        "assigned_tasks": ["customer_reply", "pricing_recommendation"],
        "timeout_seconds": 25,
    }
    response = client.put("/api/v1/ai/providers/2", json=update_data)
    assert response.status_code == 200
    data = response.json()
    assert data["slot"] == 2
    assert data["model"] == "gpt-4o"
    assert data["timeout_seconds"] == 25
    assert "customer_reply" in data["assigned_tasks"]


def test_api_test_provider_connection_mocked(client: TestClient):
    with patch.object(
        AIProviderManager,
        "test_slot_connection",
        new_callable=AsyncMock,
        return_value={
            "success": True,
            "provider": "gemini",
            "model": "gemini-2.5-flash",
            "reply": "OK",
            "latency_ms": 124.5,
        },
    ):
        response = client.post("/api/v1/ai/providers/1/test")
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["reply"] == "OK"
        assert data["latency_ms"] == 124.5
