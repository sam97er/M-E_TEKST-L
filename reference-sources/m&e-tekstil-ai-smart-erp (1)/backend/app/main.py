"""
M&E Tekstil AI Smart ERP - FastAPI Main Application
Provides RESTful APIs for Android Client, Termux Backend, and External Automation.
"""

import time
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from backend.app.config import settings
from backend.app.database.session import init_db, SessionLocal
from backend.app.api import api_v1_router
from backend.app.models.inventory import Warehouse

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("metekstil_erp")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown life-cycle."""
    logger.info("Initializing M&E Tekstil AI Smart ERP Database...")
    init_db()

    # Seed default warehouse if not exists
    db = SessionLocal()
    try:
        default_wh = db.query(Warehouse).filter(Warehouse.is_default == True).first()
        if not default_wh:
            wh = Warehouse(
                name="Ana Depo (Merkez)",
                code="WH-MAIN",
                is_default=True,
                address="M&E Tekstil İstanbul Merkez Depo"
            )
            db.add(wh)
            db.commit()
            logger.info("Created default warehouse: WH-MAIN")
    except Exception as e:
        logger.error(f"Error seeding default warehouse: {e}")
        db.rollback()
    finally:
        db.close()

    logger.info(f"{settings.APP_NAME} started successfully on {settings.HOST}:{settings.PORT}")
    yield
    logger.info(f"{settings.APP_NAME} shutting down...")


app = FastAPI(
    title=settings.APP_NAME,
    description="M&E Tekstil için Trendyol entegrasyonlu, 3 AI modelleri destekli Akıllı ERP Backend Servisi",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS Middleware for Android Emulators, Localhost, and Termux
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def add_process_time_and_logging(request: Request, call_next):
    start_time = time.perf_counter()
    try:
        response = await call_next(request)
        process_time = time.perf_counter() - start_time
        response.headers["X-Process-Time"] = f"{process_time:.4f}s"
        return response
    except Exception as exc:
        process_time = time.perf_counter() - start_time
        logger.error(f"Unhandled Request Exception on {request.url.path}: {exc}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"detail": "Sunucu hatası oluştu", "error": str(exc)},
            headers={"X-Process-Time": f"{process_time:.4f}s"},
        )


app.include_router(api_v1_router)


@app.get("/")
def root():
    return {
        "app": settings.APP_NAME,
        "version": "1.0.0",
        "status": "online",
        "docs": "/docs",
        "health": "/api/v1/health",
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.APP_DEBUG)
