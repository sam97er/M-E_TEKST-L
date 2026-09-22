from fastapi import APIRouter
from backend.app.api.v1.health import router as health_router
from backend.app.api.v1.ai import router as ai_router
from backend.app.api.v1.settings import router as settings_router
from backend.app.api.v1.products import router as products_router
from backend.app.api.v1.trendyol import router as trendyol_router
from backend.app.api.v1.questions import router as questions_router
from backend.app.api.v1.orders import router as orders_router
from backend.app.api.v1.telegram import router as telegram_router
from backend.app.api.v1.audit import router as audit_router
from backend.app.api.v1.reports import router as reports_router
from backend.app.api.v1.dashboard import router as dashboard_router
from backend.app.api.v1.system import router as system_router

api_v1_router = APIRouter(prefix="/api/v1")
api_v1_router.include_router(health_router)
api_v1_router.include_router(dashboard_router)
api_v1_router.include_router(ai_router)
api_v1_router.include_router(settings_router)
api_v1_router.include_router(products_router)
api_v1_router.include_router(trendyol_router)
api_v1_router.include_router(questions_router)
api_v1_router.include_router(orders_router)
api_v1_router.include_router(telegram_router)
api_v1_router.include_router(audit_router)
api_v1_router.include_router(reports_router)
api_v1_router.include_router(system_router)

__all__ = ["api_v1_router"]
