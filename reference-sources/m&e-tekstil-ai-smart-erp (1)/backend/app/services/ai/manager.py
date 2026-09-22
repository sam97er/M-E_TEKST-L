"""
AI Provider Manager.
Coordinates the 3 independent AI provider slots, handles dynamic task routing,
and enforces strict security (no raw API keys logged, explicit error reporting).
"""

import logging
from typing import Dict, Any, Optional, List
from sqlalchemy.orm import Session

from backend.app.config import settings
from backend.app.models.ai_provider import AIProviderConfig
from backend.app.services.ai.base import (
    BaseAIAdapter,
    AIGenerationResult,
    AIImageAnalysisResult,
)
from backend.app.services.ai.gemini_adapter import GeminiAdapter
from backend.app.services.ai.openai_adapter import OpenAIAdapter
from backend.app.services.ai.custom_adapter import CustomLLMAdapter

logger = logging.getLogger("metekstil_ai")


class AIProviderManager:
    """Manages the 3 AI Provider slots and orchestrates task assignment."""

    @staticmethod
    def get_adapter_for_config(
        provider_name: str,
        api_key: Optional[str],
        model: str,
        base_url: Optional[str] = None,
        timeout_seconds: int = 30,
    ) -> BaseAIAdapter:
        name_lower = provider_name.lower().strip()
        if "gemini" in name_lower or "google" in name_lower:
            return GeminiAdapter(api_key=api_key, model=model, base_url=base_url, timeout_seconds=timeout_seconds)
        elif "openai" in name_lower or "chatgpt" in name_lower or "gpt" in name_lower:
            return OpenAIAdapter(api_key=api_key, model=model, base_url=base_url, timeout_seconds=timeout_seconds)
        else:
            return CustomLLMAdapter(api_key=api_key, model=model, base_url=base_url, timeout_seconds=timeout_seconds)

    @classmethod
    def get_slot_config(cls, slot: int, db: Optional[Session] = None) -> Dict[str, Any]:
        """Fetches active config for slot 1, 2, or 3 from Database or fallback settings."""
        if db:
            db_config = db.query(AIProviderConfig).filter(AIProviderConfig.slot == slot).first()
            if db_config:
                return {
                    "slot": slot,
                    "provider_name": db_config.provider_name,
                    "is_enabled": db_config.is_enabled,
                    "model": db_config.model,
                    "base_url": db_config.base_url,
                    "assigned_tasks": [t.strip() for t in db_config.assigned_tasks.split(",") if t.strip()],
                    "timeout_seconds": db_config.timeout_seconds,
                    "api_key": cls._get_env_key_for_slot(slot),
                    "last_health_status": db_config.last_health_status,
                }

        # Fallback to config settings
        if slot == 1:
            return {
                "slot": 1,
                "provider_name": settings.AI_PROVIDER_1_NAME,
                "is_enabled": settings.AI_PROVIDER_1_ENABLED,
                "model": settings.AI_PROVIDER_1_MODEL,
                "base_url": settings.AI_PROVIDER_1_BASE_URL,
                "assigned_tasks": [t.strip() for t in settings.AI_PROVIDER_1_TASKS.split(",") if t.strip()],
                "timeout_seconds": settings.AI_PROVIDER_1_TIMEOUT,
                "api_key": settings.AI_PROVIDER_1_KEY,
                "last_health_status": "UNCHECKED",
            }
        elif slot == 2:
            return {
                "slot": 2,
                "provider_name": settings.AI_PROVIDER_2_NAME,
                "is_enabled": settings.AI_PROVIDER_2_ENABLED,
                "model": settings.AI_PROVIDER_2_MODEL,
                "base_url": settings.AI_PROVIDER_2_BASE_URL,
                "assigned_tasks": [t.strip() for t in settings.AI_PROVIDER_2_TASKS.split(",") if t.strip()],
                "timeout_seconds": settings.AI_PROVIDER_2_TIMEOUT,
                "api_key": settings.AI_PROVIDER_2_KEY,
                "last_health_status": "UNCHECKED",
            }
        else:
            return {
                "slot": 3,
                "provider_name": settings.AI_PROVIDER_3_NAME,
                "is_enabled": settings.AI_PROVIDER_3_ENABLED,
                "model": settings.AI_PROVIDER_3_MODEL,
                "base_url": settings.AI_PROVIDER_3_BASE_URL,
                "assigned_tasks": [t.strip() for t in settings.AI_PROVIDER_3_TASKS.split(",") if t.strip()],
                "timeout_seconds": settings.AI_PROVIDER_3_TIMEOUT,
                "api_key": settings.AI_PROVIDER_3_KEY,
                "last_health_status": "UNCHECKED",
            }

    @classmethod
    def _get_env_key_for_slot(cls, slot: int) -> Optional[str]:
        if slot == 1:
            return settings.AI_PROVIDER_1_KEY
        elif slot == 2:
            return settings.AI_PROVIDER_2_KEY
        else:
            return settings.AI_PROVIDER_3_KEY

    @classmethod
    def get_adapter_for_slot(cls, slot: int, db: Optional[Session] = None) -> BaseAIAdapter:
        cfg = cls.get_slot_config(slot, db)
        return cls.get_adapter_for_config(
            provider_name=cfg["provider_name"],
            api_key=cfg.get("api_key"),
            model=cfg["model"],
            base_url=cfg.get("base_url"),
            timeout_seconds=cfg["timeout_seconds"],
        )

    @classmethod
    async def test_slot_connection(cls, slot: int, db: Optional[Session] = None) -> Dict[str, Any]:
        """Runs a real live ping test to the configured provider for this slot."""
        logger.info(f"Initiating AI test connection for Slot {slot}...")
        cfg = cls.get_slot_config(slot, db)
        adapter = cls.get_adapter_for_slot(slot, db)
        result = await adapter.test_connection()

        # Update health status in DB if available
        if db:
            db_record = db.query(AIProviderConfig).filter(AIProviderConfig.slot == slot).first()
            if db_record:
                import datetime
                db_record.last_health_status = "HEALTHY" if result.get("success") else "ERROR"
                db_record.last_health_check = datetime.datetime.utcnow()
                db_record.last_error_message = result.get("error") if not result.get("success") else None
                db.commit()

        return result

    @classmethod
    def find_provider_for_task(cls, task_name: str, db: Optional[Session] = None) -> Optional[int]:
        """
        Locates which enabled slot (1, 2, or 3) is explicitly assigned to this task.
        No silent switching occurs if none is found.
        """
        for slot in [1, 2, 3]:
            cfg = cls.get_slot_config(slot, db)
            if cfg.get("is_enabled") and task_name in cfg.get("assigned_tasks", []):
                return slot

        # Fallback to Slot 1 if Slot 1 is enabled
        cfg1 = cls.get_slot_config(1, db)
        if cfg1.get("is_enabled"):
            return 1

        return None

    @classmethod
    async def execute_task_text(
        cls,
        task_name: str,
        prompt: str,
        system_instruction: Optional[str] = None,
        db: Optional[Session] = None,
    ) -> AIGenerationResult:
        slot = cls.find_provider_for_task(task_name, db)
        if slot is None:
            return AIGenerationResult(
                success=False,
                provider_name="none",
                model="none",
                error_message=f"'{task_name}' görevi için aktif bir AI Sağlayıcı atanmamış. Lütfen Ayarlar sayfasından bir sağlayıcıyı etkinleştirin.",
            )

        adapter = cls.get_adapter_for_slot(slot, db)
        logger.info(f"Routing task '{task_name}' to Slot {slot} ({adapter.__class__.__name__})...")
        return await adapter.generate_text(prompt, system_instruction=system_instruction)

    @classmethod
    async def execute_image_analysis(
        cls,
        image_url_or_b64: str,
        prompt: str,
        db: Optional[Session] = None,
    ) -> AIImageAnalysisResult:
        slot = cls.find_provider_for_task("image_analysis", db)
        if slot is None:
            # Check slot 2 (vision by default) or slot 1
            cfg2 = cls.get_slot_config(2, db)
            slot = 2 if cfg2.get("is_enabled") else 1

        adapter = cls.get_adapter_for_slot(slot, db)
        logger.info(f"Routing visual analysis to Slot {slot} ({adapter.__class__.__name__})...")
        return await adapter.analyze_image(image_url_or_b64, prompt)
