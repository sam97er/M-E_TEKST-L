#!/usr/bin/env python3
"""
M&E Tekstil AI Smart ERP - Database Snapshot & Export CLI Utility.
Can be executed manually or via Cron/Task Scheduler.
"""

import sys
import json
import os
from datetime import datetime

# Add project root to path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from backend.app.database.session import SessionLocal
from backend.app.services.backup_service import BackupService


def main():
    print("📦 M&E Tekstil ERP Veritabanı Yedeği Alınıyor...")
    db = SessionLocal()
    try:
        backup = BackupService.export_full_json_backup(db)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_dir = os.path.join(os.getcwd(), "backups")
        os.makedirs(backup_dir, exist_ok=True)

        # JSON Dump
        json_path = os.path.join(backup_dir, f"metekstil_backup_{timestamp}.json")
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(backup, f, ensure_ascii=False, indent=2)

        # CSV Product Export
        csv_prod_path = os.path.join(backup_dir, f"products_{timestamp}.csv")
        with open(csv_prod_path, "w", encoding="utf-8") as f:
            f.write(BackupService.export_products_csv(db))

        # CSV Orders Export
        csv_ord_path = os.path.join(backup_dir, f"orders_{timestamp}.csv")
        with open(csv_ord_path, "w", encoding="utf-8") as f:
            f.write(BackupService.export_orders_csv(db))

        print("✅ Yedekleme Başarıyla Tamamlandı!")
        print(f"📁 JSON Yedeği: {json_path}")
        print(f"📁 Ürünler CSV: {csv_prod_path}")
        print(f"📁 Siparişler CSV: {csv_ord_path}")
        print(f"📊 Özet: {backup['metadata']['counts']}")

    except Exception as e:
        print(f"❌ Yedekleme sırasında hata oluştu: {e}")
        sys.exit(1)
    finally:
        db.close()


if __name__ == "__main__":
    main()
