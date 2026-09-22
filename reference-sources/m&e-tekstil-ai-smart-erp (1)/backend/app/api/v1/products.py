"""
API Router for Products, Variants, Barcodes, and Categories.
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import func

from backend.app.database.session import get_db
from backend.app.models.product import Product
from backend.app.services.product_service import ProductService
from backend.app.schemas.product import (
    ProductCreate,
    ProductUpdate,
    ProductOut,
    ProductVariantCreate,
    ProductVariantUpdate,
    ProductVariantOut,
    ProductImageCreate,
    ProductImageOut,
    BarcodeGenerateResponse,
)

router = APIRouter(prefix="/products", tags=["Products"])


@router.get("", response_model=List[ProductOut], summary="Ürünleri Listele ve Filtrele")
def get_products(
    search: Optional[str] = Query(None, description="Ürün kodu, başlık veya barkod araması"),
    category: Optional[str] = Query(None, description="Kategori filtresi"),
    status: Optional[str] = Query(None, description="ACTIVE, PASSIVE, ARCHIVED"),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=500),
    db: Session = Depends(get_db)
):
    return ProductService.list_products(
        db=db,
        search=search,
        category=category,
        status_filter=status,
        skip=skip,
        limit=limit
    )


@router.post("", response_model=ProductOut, status_code=status.HTTP_201_CREATED, summary="Yeni Ürün ve Varyantları Oluştur")
def create_product(
    product_in: ProductCreate,
    db: Session = Depends(get_db)
):
    return ProductService.create_product_with_variants(db=db, product_in=product_in)


@router.get("/categories", response_model=List[str], summary="Mevcut Kategorileri Listele")
def get_categories(db: Session = Depends(get_db)):
    categories = (
        db.query(Product.category_name)
        .filter(Product.category_name.isnot(None), Product.category_name != "")
        .distinct()
        .all()
    )
    result = [c[0] for c in categories if c[0]]
    default_categories = ["Elbise", "Bluz & Gömlek", "Pantolon", "Etek", "Takım", "Trikotaj", "Kumaş"]
    for dc in default_categories:
        if dc not in result:
            result.append(dc)
    return sorted(result)


@router.get("/generate-barcode", response_model=BarcodeGenerateResponse, summary="Otomatik Geçerli EAN-13 Barkodu Üret")
def generate_barcode(
    prefix: str = Query("868", description="GS1 Barkod Öneki"),
    db: Session = Depends(get_db)
):
    code = ProductService.generate_unique_barcode(db=db, prefix=prefix)
    return BarcodeGenerateResponse(barcode=code, format="EAN-13")


@router.get("/{product_id}", response_model=ProductOut, summary="Ürün Detayını Getir")
def get_product(
    product_id: int,
    db: Session = Depends(get_db)
):
    return ProductService.get_product_by_id(db=db, product_id=product_id)


@router.put("/{product_id}", response_model=ProductOut, summary="Ürün Bilgilerini Güncelle")
def update_product(
    product_id: int,
    product_in: ProductUpdate,
    db: Session = Depends(get_db)
):
    return ProductService.update_product(db=db, product_id=product_id, product_in=product_in)


@router.delete("/{product_id}", status_code=status.HTTP_200_OK, summary="Ürünü Sil")
def delete_product(
    product_id: int,
    db: Session = Depends(get_db)
):
    ProductService.delete_product(db=db, product_id=product_id)
    return {"message": "Ürün başarıyla silindi", "product_id": product_id}


@router.post("/{product_id}/variants", response_model=ProductVariantOut, status_code=status.HTTP_201_CREATED, summary="Ürüne Yeni Varyant Ekle")
def add_variant(
    product_id: int,
    variant_in: ProductVariantCreate,
    db: Session = Depends(get_db)
):
    return ProductService.add_variant(db=db, product_id=product_id, variant_in=variant_in)


@router.put("/variants/{variant_id}", response_model=ProductVariantOut, summary="Varyant Bilgilerini Güncelle")
def update_variant(
    variant_id: int,
    variant_in: ProductVariantUpdate,
    db: Session = Depends(get_db)
):
    return ProductService.update_variant(db=db, variant_id=variant_id, variant_in=variant_in)


@router.delete("/variants/{variant_id}", status_code=status.HTTP_200_OK, summary="Varyantı Sil")
def delete_variant(
    variant_id: int,
    db: Session = Depends(get_db)
):
    ProductService.delete_variant(db=db, variant_id=variant_id)
    return {"message": "Varyant başarıyla silindi", "variant_id": variant_id}


@router.post("/{product_id}/images", response_model=ProductImageOut, status_code=status.HTTP_201_CREATED, summary="Ürüne Görsel Ekle")
def add_image(
    product_id: int,
    image_in: ProductImageCreate,
    db: Session = Depends(get_db)
):
    return ProductService.add_image(db=db, product_id=product_id, image_in=image_in)
