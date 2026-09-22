package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BackupDao
import com.example.data.dao.EmployeeDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleOrderDao
import com.example.data.dao.SupportTicketDao
import com.example.data.dao.TrendyolDao
import com.example.data.dao.WarehouseDao
import com.example.data.model.BackupLog
import com.example.data.model.Employee
import com.example.data.model.EmployeeRole
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SaleOrder
import com.example.data.model.SaleOrderItem
import com.example.data.model.SupportTicket
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.data.model.TrendyolSyncLog
import com.example.data.model.Warehouse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Warehouse::class,
        SaleOrder::class,
        SaleOrderItem::class,
        TrendyolSyncLog::class,
        Employee::class,
        SupportTicket::class,
        BackupLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ErpConverters::class)
abstract class ErpDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun saleOrderDao(): SaleOrderDao
    abstract fun trendyolDao(): TrendyolDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: ErpDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ErpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ErpDatabase::class.java,
                    "smarterp_enterprise.db"
                )
                    .addCallback(ErpDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class ErpDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: ErpDatabase) {
            // 1. Warehouses
            val w1 = Warehouse(
                id = 1,
                name = "المستودع الرئيسي - الرياض",
                code = "WH-RUH-01",
                location = "المنطقة الصناعية الثانية، الرياض",
                managerName = "م. خالد العتيبي",
                phone = "+966501234567",
                maxCapacity = 10000
            )
            val w2 = Warehouse(
                id = 2,
                name = "مستودع التوزيع السريع - جدة",
                code = "WH-JED-02",
                location = "ميناء جدة الإسلامي",
                managerName = "أ. عمر باوزير",
                phone = "+966507654321",
                maxCapacity = 6000
            )
            val w3 = Warehouse(
                id = 3,
                name = "مركز شحن ترنديول المعتمد",
                code = "WH-TY-HUB",
                location = "المنطقة اللوجستية، مطار الملك خالد",
                managerName = "سارة الشمري",
                phone = "+966509988776",
                maxCapacity = 15000
            )
            db.warehouseDao().insertWarehouses(listOf(w1, w2, w3))

            // 2. Initial Products with Barcodes & Trendyol sync tags
            val products = listOf(
                Product(
                    id = 1,
                    sku = "APP-HD-001",
                    barcode = "6281001000101",
                    name = "هودي صوف شتوي ترنديول بريميوم - أسود",
                    nameEn = "Trendyol Premium Winter Fleece Hoodie - Black",
                    category = "ملابس وأزياء (Fashion)",
                    warehouseId = 1,
                    stockQuantity = 4, // LOW STOCK TRIGGER!
                    minAlertStock = 12,
                    costPrice = 85.0,
                    sellingPrice = 169.0,
                    trendyolBarcode = "TY-HOOD-BLK-XL",
                    isTrendyolSynced = true
                ),
                Product(
                    id = 2,
                    sku = "TECH-TW-002",
                    barcode = "6281001000200",
                    name = "سماعات بلوتوث لاسلكية عازلة للضوضاء ANC",
                    nameEn = "Wireless ANC Noise Cancelling Earbuds",
                    category = "إلكترونيات (Electronics)",
                    warehouseId = 1,
                    stockQuantity = 85,
                    minAlertStock = 15,
                    costPrice = 110.0,
                    sellingPrice = 249.0,
                    trendyolBarcode = "TY-TECH-ANC-PRO",
                    isTrendyolSynced = true
                ),
                Product(
                    id = 3,
                    sku = "HOME-CM-003",
                    barcode = "6281001000309",
                    name = "صانعة قهوة إسبريسو إيطالية مدمجة",
                    nameEn = "Compact Italian Espresso Coffee Maker",
                    category = "أجهزة منزلية (Home Appliances)",
                    warehouseId = 2,
                    stockQuantity = 6, // LOW STOCK TRIGGER!
                    minAlertStock = 10,
                    costPrice = 290.0,
                    sellingPrice = 480.0,
                    trendyolBarcode = "TY-HOME-ESP-01",
                    isTrendyolSynced = true
                ),
                Product(
                    id = 4,
                    sku = "FSH-SH-004",
                    barcode = "6281001000408",
                    name = "حذاء رياضي جري ألترا كومفورت - أبيض",
                    nameEn = "Ultra Comfort Running Shoes - White",
                    category = "أحذية ورياضة (Sports & Shoes)",
                    warehouseId = 3,
                    stockQuantity = 42,
                    minAlertStock = 15,
                    costPrice = 130.0,
                    sellingPrice = 279.0,
                    trendyolBarcode = "TY-SHOE-RUN-43",
                    isTrendyolSynced = true
                ),
                Product(
                    id = 5,
                    sku = "BEAUTY-SR-005",
                    barcode = "6281001000507",
                    name = "سيروم فيتامين سي نقي لنضارة البشرة 50ml",
                    nameEn = "Pure Vitamin C Radiant Serum 50ml",
                    category = "عناية وجمال (Cosmetics)",
                    warehouseId = 1,
                    stockQuantity = 120,
                    minAlertStock = 20,
                    costPrice = 45.0,
                    sellingPrice = 115.0,
                    trendyolBarcode = "TY-BEAUTY-VITC-50",
                    isTrendyolSynced = true
                ),
                Product(
                    id = 6,
                    sku = "BAG-LTH-006",
                    barcode = "6281001000606",
                    name = "حقيبة يد جلد طبيعي فاخرة للعمل واللابتوب",
                    nameEn = "Genuine Leather Business Laptop Bag",
                    category = "إكسسوارات وحقائب (Bags)",
                    warehouseId = 2,
                    stockQuantity = 2, // CRITICAL LOW STOCK!
                    minAlertStock = 8,
                    costPrice = 210.0,
                    sellingPrice = 399.0,
                    trendyolBarcode = "TY-BAG-LTH-BRN",
                    isTrendyolSynced = false
                ),
                Product(
                    id = 7,
                    sku = "TECH-SW-007",
                    barcode = "6281001000705",
                    name = "ساعة ذكية مقاومة للماء مع مراقبة نبضات القلب",
                    nameEn = "Waterproof Smartwatch with Health Tracking",
                    category = "إلكترونيات (Electronics)",
                    warehouseId = 3,
                    stockQuantity = 64,
                    minAlertStock = 10,
                    costPrice = 140.0,
                    sellingPrice = 319.0,
                    trendyolBarcode = "TY-WATCH-SM-V2",
                    isTrendyolSynced = true
                ),
                Product(
                    id = 8,
                    sku = "HOME-AP-008",
                    barcode = "6281001000804",
                    name = "جهاز تنقية هواء ذكي بفلتر HEPA فائق",
                    nameEn = "Smart Air Purifier with Ultra HEPA Filter",
                    category = "أجهزة منزلية (Home Appliances)",
                    warehouseId = 1,
                    stockQuantity = 19,
                    minAlertStock = 10,
                    costPrice = 320.0,
                    sellingPrice = 549.0,
                    trendyolBarcode = "TY-AIR-PUR-01",
                    isTrendyolSynced = true
                )
            )
            db.productDao().insertProducts(products)

            // 3. Initial Orders
            val now = System.currentTimeMillis()
            val o1 = SaleOrder(
                id = 1,
                orderNumber = "ORD-2026-901",
                customerName = "عبدالرحمن الشامسي",
                customerPhone = "+966551122334",
                totalAmount = 568.0,
                paymentMethod = PaymentMethod.CARD_MADA,
                paymentStatus = "PAID",
                orderStatus = OrderStatus.COMPLETED,
                cashierName = "سالم القرني (كاشير 1)",
                source = "DIRECT_POS",
                timestamp = now - 1000 * 60 * 45,
                notes = "فاتورة نقاط البيع المباشرة - مدفوع مدى"
            )
            val o2 = SaleOrder(
                id = 2,
                orderNumber = "TY-ORD-88231",
                customerName = "منى الحربي (عبر ترنديول)",
                customerPhone = "+966567788990",
                totalAmount = 338.0,
                paymentMethod = PaymentMethod.TRENDYOL_GATEWAY,
                paymentStatus = "PAID",
                orderStatus = OrderStatus.SHIPPED,
                cashierName = "مزامنة ترنديول الآلية",
                source = "TRENDYOL",
                timestamp = now - 1000 * 60 * 120,
                notes = "تمت المزامنة والشحن من مستودع ترنديول"
            )
            val o3 = SaleOrder(
                id = 3,
                orderNumber = "ORD-2026-902",
                customerName = "فيصل الدوسري",
                customerPhone = "+966504455667",
                totalAmount = 798.0,
                paymentMethod = PaymentMethod.ELECTRONIC_WALLET,
                paymentStatus = "PAID",
                orderStatus = OrderStatus.COMPLETED,
                cashierName = "سالم القرني (كاشير 1)",
                source = "DIRECT_POS",
                timestamp = now - 1000 * 60 * 20,
                notes = "دفع عن طريق Apple Pay"
            )
            db.saleOrderDao().insertOrder(o1)
            db.saleOrderDao().insertOrder(o2)
            db.saleOrderDao().insertOrder(o3)

            val items = listOf(
                SaleOrderItem(orderId = 1, productId = 2, productName = "سماعات بلوتوث لاسلكية", productBarcode = "6281001000200", quantity = 2, unitPrice = 249.0, subtotal = 498.0),
                SaleOrderItem(orderId = 2, productId = 1, productName = "هودي صوف شتوي ترنديول", productBarcode = "6281001000101", quantity = 2, unitPrice = 169.0, subtotal = 338.0),
                SaleOrderItem(orderId = 3, productId = 6, productName = "حقيبة يد جلد طبيعي فاخرة", productBarcode = "6281001000606", quantity = 2, unitPrice = 399.0, subtotal = 798.0)
            )
            db.saleOrderDao().insertOrderItems(items)

            // 4. Trendyol Sync Logs
            val logs = listOf(
                TrendyolSyncLog(
                    actionType = "INVENTORY_SYNC",
                    status = "SUCCESS",
                    message = "تم تحديث كميات 8 منتجات بنجاح في متجر ترنديول الرسمي",
                    itemsProcessed = 8,
                    timestamp = now - 1000 * 60 * 12
                ),
                TrendyolSyncLog(
                    actionType = "ORDER_IMPORT",
                    status = "SUCCESS",
                    message = "تم استيراد الطلب #TY-ORD-88231 وخصم الكميات من المخزون آلياً",
                    itemsProcessed = 1,
                    timestamp = now - 1000 * 60 * 120
                ),
                TrendyolSyncLog(
                    actionType = "CATALOG_UPDATE",
                    status = "SUCCESS",
                    message = "تمت مطابقة أرقام الباركود مع كتالوج Trendyol Marketplace",
                    itemsProcessed = 7,
                    timestamp = now - 1000 * 60 * 360
                )
            )
            db.trendyolDao().insertLogs(logs)

            // 5. Employees
            val employees = listOf(
                Employee(
                    id = 1,
                    employeeCode = "EMP-101",
                    fullName = "أحمد السبيعي",
                    role = EmployeeRole.MANAGER,
                    currentShift = "الوردية الإدارية الشاملة",
                    dailyTargetSales = 10000.0,
                    currentSales = 8450.0,
                    scannedItemsCount = 450,
                    efficiencyScore = 98
                ),
                Employee(
                    id = 2,
                    employeeCode = "EMP-102",
                    fullName = "سالم القرني",
                    role = EmployeeRole.CASHIER,
                    currentShift = "وردية المبيعات والكاشير 1",
                    dailyTargetSales = 4000.0,
                    currentSales = 3280.0,
                    scannedItemsCount = 210,
                    efficiencyScore = 95
                ),
                Employee(
                    id = 3,
                    employeeCode = "EMP-103",
                    fullName = "فهد المطيري",
                    role = EmployeeRole.WAREHOUSE_SUPERVISOR,
                    currentShift = "وردية الجرد ومستودع الرياض",
                    dailyTargetSales = 2000.0,
                    currentSales = 1800.0,
                    scannedItemsCount = 380,
                    efficiencyScore = 94
                ),
                Employee(
                    id = 4,
                    employeeCode = "EMP-104",
                    fullName = "نورة القحطاني",
                    role = EmployeeRole.INVENTORY_SPECIALIST,
                    currentShift = "وردية تدقيق وتجهيز ترنديول",
                    dailyTargetSales = 3000.0,
                    currentSales = 2950.0,
                    scannedItemsCount = 320,
                    efficiencyScore = 97
                )
            )
            db.employeeDao().insertEmployees(employees)

            // 6. Support Tickets
            val tickets = listOf(
                SupportTicket(
                    id = 1,
                    ticketCode = "TCK-8801",
                    customerName = "شركة النخبة للتجارة",
                    customerContact = "+966540011223",
                    subject = "استفسار بخصوص تسليم شحنة ترنديول السريعة",
                    message = "العميل يرغب في تأكيد وصول البضاعة إلى مستودع جدة برقم بوليصة الشحن",
                    priority = TicketPriority.HIGH,
                    status = TicketStatus.IN_PROGRESS,
                    createdAt = now - 1000 * 60 * 90,
                    responseNotes = "تم إرسال إشعار التتبع اللوجستي للشاحنة"
                ),
                SupportTicket(
                    id = 2,
                    ticketCode = "TCK-8802",
                    customerName = "مؤسسة الأفق التقني",
                    customerContact = "+966556677889",
                    subject = "طلب إعادة ضبط حد التنبيه لمنتج الهودي الشتوي",
                    message = "يرجى تعديل حد التنبيه الأدنى ليصل إلى 15 قطعة بدلاً من 12",
                    priority = TicketPriority.MEDIUM,
                    status = TicketStatus.RESOLVED,
                    createdAt = now - 1000 * 60 * 240,
                    responseNotes = "تم التعديل في النظام وإرسال تقرير المتابعة"
                )
            )
            db.supportTicketDao().insertTickets(tickets)

            // 7. Backup Log
            val backups = listOf(
                BackupLog(
                    id = 1,
                    backupName = "Backup_Daily_Enterprise_Auto.json",
                    recordCount = 1420,
                    sizeKb = 428,
                    timestamp = now - 1000 * 60 * 300,
                    status = "SUCCESS",
                    encryptionType = "AES-256 GCM"
                )
            )
            db.backupDao().insertBackupLogs(backups)
        }
    }
}
