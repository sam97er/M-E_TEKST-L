package com.example.ui.theme

import com.example.data.model.AppLanguage

object ErpStrings {
    fun get(key: String, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ARABIC -> arabicMap[key] ?: key
            AppLanguage.ENGLISH -> englishMap[key] ?: key
            AppLanguage.TURKISH -> turkishMap[key] ?: key
        }
    }

    private val arabicMap = mapOf(
        "app_title" to "Smart ERP للمخازن والمبيعات",
        "tab_dashboard" to "لوحة الأداء",
        "tab_inventory" to "المخزون",
        "tab_pos" to "نقطة البيع",
        "tab_trendyol" to "ترنديول",
        "tab_scanner" to "الباركود",
        "tab_staff" to "الموظفين",
        "tab_support_backup" to "الدعم والنسخ",

        // Dashboard
        "daily_overview" to "ملخص الأداء اليومي",
        "today_sales" to "مبيعات اليوم",
        "total_orders" to "إجمالي الطلبات",
        "low_stock_alert_title" to "تنبيهات انخفاض المخزون",
        "active_warehouses" to "المستودعات الفعالة",
        "top_selling_products" to "أعلى المنتجات طلباً ومبيعاً",
        "sales_channels" to "قنوات البيع والمبيعات",
        "direct_pos" to "نقاط البيع المباشرة",
        "trendyol_orders" to "طلبات متجر ترنديول",
        "auto_sync_status" to "المزامنة الآلية نشطة",

        // Inventory
        "inventory_management" to "إدارة المستودعات والأصناف",
        "search_product_hint" to "ابحث بالاسم، الباركود، أو SKU...",
        "add_product" to "إضافة صنف جديد",
        "all_categories" to "جميع التصنيفات",
        "stock_qty" to "الكمية المتوفرة",
        "min_alert" to "حد التنبيه الأدنى",
        "cost_price" to "سعر التكلفة",
        "selling_price" to "سعر البيع",
        "edit_stock" to "تعديل الكمية",
        "stock_low" to "مخزون منخفض!",
        "stock_good" to "متوفر",
        "stock_empty" to "نفذ من المخزن!",

        // POS
        "point_of_sale" to "نقطة البيع السريعة (POS)",
        "cart_summary" to "سلة الفاتورة",
        "checkout" to "إتمام الدفع الإلكتروني",
        "empty_cart" to "السلة فارغة، اختر منتجات أو امسح الباركود",
        "customer_name" to "اسم العميل",
        "customer_phone" to "رقم جوال العميل",
        "total_payable" to "الإجمالي المطلوب سداده",
        "clear_cart" to "تفريغ السلة",
        "select_payment_method" to "اختر وسيلة الدفع الإلكتروني",
        "cash" to "نقداً (Cash)",
        "card_mada" to "بطاقة مدى / POS",
        "credit_card" to "فيزا / ماستركارد",
        "ewallet" to "محفظة رقمية (Apple Pay / STC)",
        "split_payment" to "دفع مجزأ",
        "order_success" to "تم إصدار الفاتورة وحفظ البيع وخصم المخزون بنجاح!",

        // Trendyol
        "trendyol_integration" to "الربط الآلي مع متجر ترنديول (Trendyol)",
        "sync_now" to "مزامنة المخزون الفورية",
        "import_orders" to "استيراد طلبات ترنديول",
        "api_credentials" to "بيانات الربط والاعتماد البرمجي",
        "supplier_id" to "معرّف المورّد (Supplier ID)",
        "api_key" to "مفتاح الربط (API Key)",
        "sync_interval" to "تكرار المزامنة الآلية",
        "recent_sync_logs" to "سجل عمليات المزامنة الأخيرة",

        // Barcode
        "barcode_scanner" to "قارئ الباركود للأجهزة المحمولة",
        "barcode_hardware_ready" to "جاهز لمسح الباركود السلكي واللاسلكي (HID/USB)",
        "scan_item_hint" to "وجّه الباركود داخل الإطار أو استخدم الماسح المحمول",
        "rapid_stock_count" to "وضع الجرد السريع بالمستودع",
        "scanned_code" to "الباركود المقروء",
        "increment_qty" to "زيادة الكمية بمقدار 1",

        // Staff
        "staff_dashboard" to "لوحة تحكم وإنتاجية الموظفين",
        "active_employee" to "الموظف المناوب",
        "daily_target" to "الهدف البيعي اليومي",
        "achievement_rate" to "نسبة الإنجاز",
        "shift_status" to "حالة الوردية",
        "clock_in" to "تسجيل الحضور بالوردية",
        "clock_out" to "إنهاء الوردية",

        // Support & Backup
        "support_and_backup" to "الدعم الفني والنسخ الاحتياطي",
        "create_ticket" to "فتح تذكرة دعم فني جديدة",
        "create_backup" to "إنشاء نسخة احتياطية آمنة (AES-256)",
        "periodic_backup" to "النسخ الاحتياطي التلقائي الدوري",
        "backup_history" to "سجل النسخ الاحتياطية السابقة",
        "support_tickets" to "تذاكر الدعم الفني للعملاء",

        // Actions
        "save" to "حفظ",
        "cancel" to "إلغاء",
        "close" to "إغلاق",
        "confirm" to "تأكيد",
        "currency" to "العملة",
        "language" to "اللغة"
    )

    private val englishMap = mapOf(
        "app_title" to "Smart ERP Warehouse & Sales",
        "tab_dashboard" to "Dashboard",
        "tab_inventory" to "Inventory",
        "tab_pos" to "POS Sales",
        "tab_trendyol" to "Trendyol",
        "tab_scanner" to "Barcode",
        "tab_staff" to "Staff",
        "tab_support_backup" to "Support & Backup",

        "daily_overview" to "Daily Performance Overview",
        "today_sales" to "Today's Sales",
        "total_orders" to "Total Orders",
        "low_stock_alert_title" to "Low Stock Alerts",
        "active_warehouses" to "Active Warehouses",
        "top_selling_products" to "Top Selling Products",
        "sales_channels" to "Sales Channels",
        "direct_pos" to "Direct POS Counter",
        "trendyol_orders" to "Trendyol Marketplace",
        "auto_sync_status" to "Auto-Sync Active",

        "inventory_management" to "Warehouse & Inventory",
        "search_product_hint" to "Search by name, barcode, or SKU...",
        "add_product" to "Add New Product",
        "all_categories" to "All Categories",
        "stock_qty" to "Available Stock",
        "min_alert" to "Low Stock Alert Level",
        "cost_price" to "Cost Price",
        "selling_price" to "Selling Price",
        "edit_stock" to "Adjust Stock",
        "stock_low" to "Low Stock!",
        "stock_good" to "In Stock",
        "stock_empty" to "Out of Stock!",

        "point_of_sale" to "Point of Sale (POS)",
        "cart_summary" to "Order Cart",
        "checkout" to "Electronic Checkout",
        "empty_cart" to "Cart is empty, scan or select products",
        "customer_name" to "Customer Name",
        "customer_phone" to "Customer Phone",
        "total_payable" to "Total Payable",
        "clear_cart" to "Clear Cart",
        "select_payment_method" to "Select Electronic Payment Method",
        "cash" to "Cash",
        "card_mada" to "Mada / Debit Card",
        "credit_card" to "Credit Card",
        "ewallet" to "E-Wallet (Apple / STC Pay)",
        "split_payment" to "Split Payment",
        "order_success" to "Order invoice created and inventory deducted successfully!",

        "trendyol_integration" to "Trendyol Marketplace Integration",
        "sync_now" to "Sync Inventory Now",
        "import_orders" to "Import Trendyol Orders",
        "api_credentials" to "API Integration Credentials",
        "supplier_id" to "Supplier ID",
        "api_key" to "API Key",
        "sync_interval" to "Sync Interval",
        "recent_sync_logs" to "Recent Sync Operations Log",

        "barcode_scanner" to "Barcode Scanner (Handheld Ready)",
        "barcode_hardware_ready" to "Ready for USB / Bluetooth Handheld Scanners",
        "scan_item_hint" to "Align barcode within viewfinder or trigger handheld scanner",
        "rapid_stock_count" to "Warehouse Rapid Stock Count",
        "scanned_code" to "Scanned Code",
        "increment_qty" to "Add +1 to Stock",

        "staff_dashboard" to "Staff Productivity & Shifts",
        "active_employee" to "Active Staff Member",
        "daily_target" to "Daily Target",
        "achievement_rate" to "Achievement",
        "shift_status" to "Shift Status",
        "clock_in" to "Clock In",
        "clock_out" to "Clock Out",

        "support_and_backup" to "Customer Support & Data Backup",
        "create_ticket" to "New Support Ticket",
        "create_backup" to "Create Encrypted Backup (AES-256)",
        "periodic_backup" to "Periodic Auto-Backup",
        "backup_history" to "Backup History",
        "support_tickets" to "Customer Support Tickets",

        "save" to "Save",
        "cancel" to "Cancel",
        "close" to "Close",
        "confirm" to "Confirm",
        "currency" to "Currency",
        "language" to "Language"
    )

    private val turkishMap = mapOf(
        "app_title" to "Smart ERP Depo ve Satış",
        "tab_dashboard" to "Panel",
        "tab_inventory" to "Stok",
        "tab_pos" to "Kasa Satış",
        "tab_trendyol" to "Trendyol",
        "tab_scanner" to "Barkod",
        "tab_staff" to "Personel",
        "tab_support_backup" to "Destek & Yedek",

        "daily_overview" to "Günlük Performans Özeti",
        "today_sales" to "Günün Satışları",
        "total_orders" to "Toplam Sipariş",
        "low_stock_alert_title" to "Kritik Stok Uyarıları",
        "active_warehouses" to "Aktif Depolar",
        "top_selling_products" to "En Çok Satan Ürünler",
        "sales_channels" to "Satış Kanalları",
        "direct_pos" to "Doğrudan Kasa",
        "trendyol_orders" to "Trendyol Pazaryeri",
        "auto_sync_status" to "Otomatik Senkronizasyon Aktif",

        "inventory_management" to "Depo ve Stok Yönetimi",
        "search_product_hint" to "Ürün adı, barkod veya SKU ile ara...",
        "add_product" to "Yeni Ürün Ekle",
        "all_categories" to "Tüm Kategoriler",
        "stock_qty" to "Mevcut Stok",
        "min_alert" to "Kritik Stok Sınırı",
        "cost_price" to "Maliyet Fiyatı",
        "selling_price" to "Satış Fiyatı",
        "edit_stock" to "Stok Düzenle",
        "stock_low" to "Kritik Stok!",
        "stock_good" to "Mevcut",
        "stock_empty" to "Tükendi!",

        "point_of_sale" to "Hızlı Kasa Satış (POS)",
        "cart_summary" to "Sipariş Sepeti",
        "checkout" to "Elektronik Ödeme",
        "empty_cart" to "Sepet boş, ürün seçin veya barkod okutun",
        "customer_name" to "Müşteri Adı",
        "customer_phone" to "Müşteri Telefonu",
        "total_payable" to "Ödenecek Tutar",
        "clear_cart" to "Sepeti Temizle",
        "select_payment_method" to "Elektronik Ödeme Yöntemi Seçin",
        "cash" to "Nakit (Cash)",
        "card_mada" to "Banka Kartı / POS",
        "credit_card" to "Kredi Kartı",
        "ewallet" to "E-Cüzdan (Apple / STC Pay)",
        "split_payment" to "Parçalı Ödeme",
        "order_success" to "Fatura oluşturuldu ve stok otomatik düşüldü!",

        "trendyol_integration" to "Trendyol Entegrasyonu",
        "sync_now" to "Stokları Hemen Senkronize Et",
        "import_orders" to "Trendyol Siparişlerini Çek",
        "api_credentials" to "API Bağlantı Bilgileri",
        "supplier_id" to "Satıcı ID (Supplier ID)",
        "api_key" to "API Anahtarı",
        "sync_interval" to "Senkronizasyon Sıklığı",
        "recent_sync_logs" to "Son Entegrasyon İşlem Günlüğü",

        "barcode_scanner" to "Barkod Okuyucu (El Terminali)",
        "barcode_hardware_ready" to "USB / Bluetooth El Terminallerine Uygun",
        "scan_item_hint" to "Barkodu çerçeve içine getirin ya da okutun",
        "rapid_stock_count" to "Hızlı Depo Sayım Modu",
        "scanned_code" to "Okunan Barkod",
        "increment_qty" to "Stoka +1 Ekle",

        "staff_dashboard" to "Personel Verimlilik Paneli",
        "active_employee" to "Aktif Çalışan",
        "daily_target" to "Günlük Hedef",
        "achievement_rate" to "Hedef Başarısı",
        "shift_status" to "Vardiya Durumu",
        "clock_in" to "Vardiya Başlat",
        "clock_out" to "Vardiya Bitir",

        "support_and_backup" to "Müşteri Destek & Veri Yedekleme",
        "create_ticket" to "Yeni Destek Talebi",
        "create_backup" to "Şifreli Yedek Al (AES-256)",
        "periodic_backup" to "Otomatik Periyodik Yedekleme",
        "backup_history" to "Yedekleme Geçmişi",
        "support_tickets" to "Müşteri Destek Talepleri",

        "save" to "Kaydet",
        "cancel" to "İptal",
        "close" to "Kapat",
        "confirm" to "Onayla",
        "currency" to "Para Birimi",
        "language" to "Dil"
    )
}
