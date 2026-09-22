"""
M&E Tekstil AI Smart ERP - System & Maintenance API Router.
Provides database backup downloads, CSV data exports, and runtime diagnostics.
"""

from fastapi import APIRouter, Depends, Response
from fastapi.responses import PlainTextResponse
from sqlalchemy.orm import Session

from backend.app.database.session import get_db
from backend.app.services.backup_service import BackupService

router = APIRouter(prefix="/system", tags=["System & Maintenance"])


@router.get("/backup/json")
def get_full_database_backup(db: Session = Depends(get_db)):
    """Returns a full structured JSON backup snapshot of all system tables."""
    return BackupService.export_full_json_backup(db)


@router.get("/export/products-csv", response_class=PlainTextResponse)
def export_products_csv(db: Session = Depends(get_db)):
    """Generates and returns products and variants as CSV data."""
    csv_content = BackupService.export_products_csv(db)
    return Response(
        content=csv_content,
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=metekstil_products.csv"}
    )


@router.get("/export/orders-csv", response_class=PlainTextResponse)
def export_orders_csv(db: Session = Depends(get_db)):
    """Generates and returns orders and profitability metrics as CSV data."""
    csv_content = BackupService.export_orders_csv(db)
    return Response(
        content=csv_content,
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=metekstil_orders.csv"}
    )


@router.get("/diagnostics")
def get_system_diagnostics(db: Session = Depends(get_db)):
    """Runs database PRAGMA checks and returns health statistics."""
    return BackupService.run_system_diagnostics(db)
