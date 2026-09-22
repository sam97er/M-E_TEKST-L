#!/bin/bash
# ==============================================================================
# M&E Tekstil AI Smart ERP - FastAPI Server Start Script
# ==============================================================================

export HOST=${HOST:-"0.0.0.0"}
export PORT=${PORT:-"8080"}
export APP_ENV=${APP_ENV:-"production"}

echo "🧵 M&E Tekstil AI Smart ERP Başlatılıyor..."
echo "📍 Dinleme Adresi: http://${HOST}:${PORT}"
echo "📚 API Dokümantasyonu: http://${HOST}:${PORT}/docs"
echo "=============================================================================="

exec uvicorn backend.app.main:app --host "$HOST" --port "$PORT" --access-log
