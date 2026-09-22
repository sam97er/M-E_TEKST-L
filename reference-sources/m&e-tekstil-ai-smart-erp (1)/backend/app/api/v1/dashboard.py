"""
API Router for Consolidated Executive Dashboard (Phase 9).
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from backend.app.database.session import get_db
from backend.app.schemas.dashboard import DashboardSummaryResponse
from backend.app.services.dashboard_service import DashboardService

router = APIRouter(prefix="/dashboard", tags=["Executive Dashboard"])


@router.get("/summary", response_model=DashboardSummaryResponse)
def get_dashboard_summary(
    db: Session = Depends(get_db)
):
    """
    Tüm işletme sisteminin (Trendyol, 3 AI modelleri, Telegram bildirimleri,
    satış kârlılığı, soru onayları, stok riskleri ve canlı akış) konsolide özetini döner.
    """
    return DashboardService.get_dashboard_summary(db)
