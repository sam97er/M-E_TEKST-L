"""
Pydantic Schemas for Products, Variants, Barcodes, and Images.
"""

from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel, Field, ConfigDict


class VariantBase(BaseModel):
    color: str = Field(..., description="Renk (e.g. Siyah, Lacivert, Ekru)")
    size: str = Field(..., description="Beden (e.g. S, M, L, XL, 38, 40)")
    barcode: Optional[str] = Field(None, description="Benzersiz Barkod (EAN-13 veya Serbest)")
    sku: Optional[str] = Field(None, description="Stok Kodu (SKU)")
    purchase_cost: float = Field(default=0.0, ge=0.0, description="Alış Maliyeti (TL)")
    selling_price: float = Field(default=0.0, ge=0.0, description="Satış Fiyatı (TL)")
    initial_stock: int = Field(default=0, ge=0, description="Başlangıç Stok Adedi")
    min_stock_threshold: int = Field(default=5, ge=0, description="Kritik Stok Eşiği")
    is_active: bool = Field(default=True)


class ProductVariantCreate(VariantBase):
    pass


class ProductVariantUpdate(BaseModel):
    color: Optional[str] = None
    size: Optional[str] = None
    barcode: Optional[str] = None
    sku: Optional[str] = None
    purchase_cost: Optional[float] = Field(None, ge=0.0)
    selling_price: Optional[float] = Field(None, ge=0.0)
    is_active: Optional[bool] = None


class ProductVariantOut(BaseModel):
    id: int
    product_id: int
    barcode: str
    sku: Optional[str] = None
    color: str
    size: str
    purchase_cost: float
    selling_price: float
    is_active: bool
    total_stock: int = 0
    available_stock: int = 0
    created_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)


class ProductImageCreate(BaseModel):
    image_url: str
    is_primary: bool = False


class ProductImageOut(BaseModel):
    id: int
    product_id: int
    image_url: str
    is_primary: bool
    review_status: str
    quality_score: Optional[float] = None
    feedback: Optional[str] = None
    created_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)


class ProductBase(BaseModel):
    title: str = Field(..., min_length=2, max_length=255, description="Ürün Başlığı")
    description: Optional[str] = None
    category_name: Optional[str] = Field(None, description="Kategori (e.g. Elbise, Bluz, Kumaş)")
    brand: str = Field(default="M&E Tekstil", description="Marka")
    selling_price: float = Field(default=0.0, ge=0.0, description="Genel Satış Fiyatı")
    vat_rate: float = Field(default=10.0, ge=0.0, description="KDV Oranı (%)")
    status: str = Field(default="ACTIVE", description="ACTIVE, PASSIVE, ARCHIVED")
    trendyol_content_id: Optional[str] = None


class ProductCreate(ProductBase):
    product_code: Optional[str] = Field(None, description="Benzersiz Ürün Kodu. Boş bırakılırsa otomatik üretilir.")
    variants: List[ProductVariantCreate] = Field(default_factory=list, description="Ürün Varyantları")
    images: List[ProductImageCreate] = Field(default_factory=list, description="Ürün Görselleri")


class ProductUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    category_name: Optional[str] = None
    brand: Optional[str] = None
    selling_price: Optional[float] = Field(None, ge=0.0)
    vat_rate: Optional[float] = Field(None, ge=0.0)
    status: Optional[str] = None
    trendyol_content_id: Optional[str] = None


class ProductOut(ProductBase):
    id: int
    product_code: str
    total_stock: int = 0
    variants: List[ProductVariantOut] = []
    images: List[ProductImageOut] = []
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)


class BarcodeGenerateResponse(BaseModel):
    barcode: str
    format: str = "EAN-13"
