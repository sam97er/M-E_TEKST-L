#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# M&E Tekstil AI Smart ERP - Termux On-Device Deployment Script
# Otomatik Kurulum ve Çalıştırma Betiği
# ==============================================================================

set -e

echo "🚀 [1/5] Termux Ortamı Güncelleniyor..."
pkg update -y && pkg upgrade -y

echo "📦 [2/5] Temel Paketler (Python, SQLite, Git, Clang) Kuruluyor..."
pkg install -y python python-pip git sqlite clang libffi openssl

echo "⚡ [3/5] Termux Wake-Lock Aktif Ediliyor (Arka Planda Kapanmayı Önler)..."
if command -v termux-wake-lock &> /dev/null; then
    termux-wake-lock
    echo "✅ Wake-lock başarıyla devrede."
else
    echo "⚠️ termux-wake-lock komutu bulunamadı. Lütfen Termux:API eklentisini kontrol edin."
fi

echo "📥 [4/5] Python Bağımlılıkları Yükleniyor..."
pip install --upgrade pip
pip install fastapi uvicorn pydantic python-dotenv sqlalchemy httpx requests jinja2

echo "🎉 [5/5] Kurulum Tamamlandı!"
echo "=============================================================================="
echo "Sunucuyu Başlatmak İçin:"
echo "  bash scripts/start_server.sh"
echo "=============================================================================="
