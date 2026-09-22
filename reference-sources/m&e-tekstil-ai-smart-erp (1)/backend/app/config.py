"""
M&E Tekstil AI Smart ERP - Configuration Module
Manages application settings, database connection URLs, API keys, and environment toggles.
Adheres strictly to zero-hardcoded secret principles.
"""

from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


class Settings(BaseSettings):
    # App General Settings
    APP_NAME: str = "M&E Tekstil AI Smart ERP"
    APP_ENV: str = "development"
    APP_DEBUG: bool = True
    LOG_LEVEL: str = "INFO"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    SECRET_KEY: str = "default_dev_secret_key_change_in_production"

    # SQLite Database
    DATABASE_URL: str = "sqlite:///./me_tekstil_erp.db"

    # Trendyol Integration Config
    TRENDYOL_SUPPLIER_ID: Optional[str] = None
    TRENDYOL_API_KEY: Optional[str] = None
    TRENDYOL_API_SECRET: Optional[str] = None
    TRENDYOL_BASE_URL: str = "https://api.trendyol.com/sapigw/suppliers/"

    # AI Provider 1 (Primary - e.g. Gemini)
    AI_PROVIDER_1_ENABLED: bool = True
    AI_PROVIDER_1_NAME: str = "gemini"
    AI_PROVIDER_1_KEY: Optional[str] = None
    AI_PROVIDER_1_MODEL: str = "gemini-2.5-flash"
    AI_PROVIDER_1_BASE_URL: Optional[str] = None
    AI_PROVIDER_1_TASKS: str = "customer_reply,daily_report"
    AI_PROVIDER_1_TIMEOUT: int = 30

    # AI Provider 2 (Secondary / Vision - e.g. OpenAI)
    AI_PROVIDER_2_ENABLED: bool = False
    AI_PROVIDER_2_NAME: str = "openai"
    AI_PROVIDER_2_KEY: Optional[str] = None
    AI_PROVIDER_2_MODEL: str = "gpt-4o-mini"
    AI_PROVIDER_2_BASE_URL: Optional[str] = None
    AI_PROVIDER_2_TASKS: str = "image_analysis,stock_audit"
    AI_PROVIDER_2_TIMEOUT: int = 30

    # AI Provider 3 (Fallback / Local LLM)
    AI_PROVIDER_3_ENABLED: bool = False
    AI_PROVIDER_3_NAME: str = "custom_llm"
    AI_PROVIDER_3_KEY: Optional[str] = None
    AI_PROVIDER_3_MODEL: str = "llama-3"
    AI_PROVIDER_3_BASE_URL: Optional[str] = "http://localhost:11434/v1"
    AI_PROVIDER_3_TASKS: str = "pricing_recommendation"
    AI_PROVIDER_3_TIMEOUT: int = 45

    # Telegram Bot
    TELEGRAM_ENABLED: bool = False
    TELEGRAM_BOT_TOKEN: Optional[str] = None
    TELEGRAM_CHAT_ID: Optional[str] = None

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    def mask_key(self, key: Optional[str]) -> str:
        """Returns masked API key for safe UI inspection (e.g. 'sk-...9abc')."""
        if not key or len(key) < 6:
            return "Yapılandırılmamış" if not key else "******"
        return f"{key[:3]}...{key[-4:]}"


settings = Settings()
