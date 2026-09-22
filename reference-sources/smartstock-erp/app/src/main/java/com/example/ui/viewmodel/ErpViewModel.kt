package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.ErpDatabase
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.BackupLog
import com.example.data.model.Employee
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SaleOrder
import com.example.data.model.SupportTicket
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.data.model.TrendyolConfig
import com.example.data.model.TrendyolSyncLog
import com.example.data.model.Warehouse
import com.example.data.repository.ErpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ErpTab {
    DASHBOARD,
    INVENTORY,
    POS,
    TRENDYOL,
    SCANNER,
    STAFF,
    SUPPORT_BACKUP
}

class ErpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ErpDatabase.getDatabase(application, viewModelScope)
    private val repository = ErpRepository(
        productDao = database.productDao(),
        warehouseDao = database.warehouseDao(),
        saleOrderDao = database.saleOrderDao(),
        trendyolDao = database.trendyolDao(),
        employeeDao = database.employeeDao(),
        supportTicketDao = database.supportTicketDao(),
        backupDao = database.backupDao()
    )

    // Language & Currency
    private val _language = MutableStateFlow(AppLanguage.ARABIC)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currency = MutableStateFlow(AppCurrency.SAR)
    val currency: StateFlow<AppCurrency> = _currency.asStateFlow()

    private val _currentTab = MutableStateFlow(ErpTab.DASHBOARD)
    val currentTab: StateFlow<ErpTab> = _currentTab.asStateFlow()

    // Data Flows from Room
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warehouses: StateFlow<List<Warehouse>> = repository.allWarehouses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<SaleOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendyolLogs: StateFlow<List<TrendyolSyncLog>> = repository.trendyolLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickets: StateFlow<List<SupportTicket>> = repository.allTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backups: StateFlow<List<BackupLog>> = repository.allBackups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trendyol Config State
    private val _trendyolConfig = MutableStateFlow(TrendyolConfig())
    val trendyolConfig: StateFlow<TrendyolConfig> = _trendyolConfig.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // POS Cart State: Map of Product to Quantity
    private val _cart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val cart: StateFlow<Map<Product, Int>> = _cart.asStateFlow()

    // Barcode Scanning State
    private val _lastScannedProduct = MutableStateFlow<Product?>(null)
    val lastScannedProduct: StateFlow<Product?> = _lastScannedProduct.asStateFlow()

    private val _scannedCodeInput = MutableStateFlow("")
    val scannedCodeInput: StateFlow<String> = _scannedCodeInput.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Dialog Control
    var showPaymentDialog = MutableStateFlow(false)
    var showAddProductDialog = MutableStateFlow(false)
    var showLangCurrencyDialog = MutableStateFlow(false)
    var showNewTicketDialog = MutableStateFlow(false)
    var editingProductStock = MutableStateFlow<Product?>(null)
    var lastCompletedOrder = MutableStateFlow<SaleOrder?>(null)

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun setCurrency(curr: AppCurrency) {
        _currency.value = curr
    }

    fun setTab(tab: ErpTab) {
        _currentTab.value = tab
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    // POS Operations
    fun addToCart(product: Product, qty: Int = 1) {
        val currentCart = _cart.value.toMutableMap()
        val existingQty = currentCart[product] ?: 0
        val newQty = existingQty + qty

        if (newQty > product.stockQuantity) {
            showMessage("الكمية المطلوبة تتجاوز المخزون المتوفر (${product.stockQuantity})")
            return
        }
        currentCart[product] = newQty
        _cart.value = currentCart
    }

    fun removeFromCart(product: Product) {
        val currentCart = _cart.value.toMutableMap()
        currentCart.remove(product)
        _cart.value = currentCart
    }

    fun updateCartQty(product: Product, qty: Int) {
        if (qty <= 0) {
            removeFromCart(product)
            return
        }
        if (qty > product.stockQuantity) {
            showMessage("الكمية تتجاوز المخزون المتوفر (${product.stockQuantity})")
            return
        }
        val currentCart = _cart.value.toMutableMap()
        currentCart[product] = qty
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun processCheckout(
        customerName: String,
        customerPhone: String,
        paymentMethod: PaymentMethod,
        cashierName: String
    ) {
        viewModelScope.launch {
            val items = _cart.value.entries.map { it.key to it.value }
            val result = repository.createSaleOrder(
                customerName = customerName,
                customerPhone = customerPhone,
                cartItems = items,
                paymentMethod = paymentMethod,
                cashierName = cashierName
            )
            result.onSuccess { order ->
                clearCart()
                showPaymentDialog.value = false
                lastCompletedOrder.value = order
                showMessage("تم إنشاء الفاتورة رقم ${order.orderNumber} وحفظ العملية بنجاح!")
            }.onFailure { err ->
                showMessage("خطأ أثناء إتمام الدفع: ${err.localizedMessage}")
            }
        }
    }

    // Product & Stock Management
    fun addNewProduct(
        name: String,
        nameEn: String,
        barcode: String,
        sku: String,
        category: String,
        warehouseId: Long,
        qty: Int,
        minAlert: Int,
        costPrice: Double,
        sellingPrice: Double,
        trendyolBarcode: String
    ) {
        viewModelScope.launch {
            val product = Product(
                sku = sku.ifBlank { "SKU-" + (1000..9999).random() },
                barcode = barcode.ifBlank { "628" + (1000000000..9999999999).random() },
                name = name,
                nameEn = nameEn,
                category = category.ifBlank { "عام (General)" },
                warehouseId = warehouseId,
                stockQuantity = qty,
                minAlertStock = minAlert,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                trendyolBarcode = trendyolBarcode.ifBlank { "TY-" + barcode }
            )
            repository.insertProduct(product)
            showAddProductDialog.value = false
            showMessage("تمت إضافة المنتج ${product.name} إلى المخزون")
        }
    }

    fun updateProductStock(productId: Long, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(productId, newStock)
            editingProductStock.value = null
            showMessage("تم تحديث كمية المخزون إلى $newStock قطعة")
        }
    }

    // Barcode Scanning
    fun scanBarcode(code: String) {
        _scannedCodeInput.value = code
        viewModelScope.launch {
            val matched = repository.getProductByBarcode(code)
            _lastScannedProduct.value = matched
            if (matched != null) {
                showMessage("تم التعرف على المنتج: ${matched.name}")
            } else {
                showMessage("لم يتم العثور على صنف بالرمز: $code")
            }
        }
    }

    fun rapidStockCountIncrement(productId: Long) {
        viewModelScope.launch {
            repository.incrementStock(productId, 1)
            // Refresh scanned product
            _lastScannedProduct.value?.let { current ->
                if (current.id == productId) {
                    _lastScannedProduct.value = current.copy(stockQuantity = current.stockQuantity + 1)
                }
            }
            showMessage("تمت إضافة +1 للمخزون آلياً بالماسح")
        }
    }

    // Trendyol Marketplace Actions
    fun syncWithTrendyol() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncInventoryWithTrendyol(_trendyolConfig.value)
            _isSyncing.value = false
            result.onSuccess { msg ->
                _trendyolConfig.value = _trendyolConfig.value.copy(lastSyncTime = System.currentTimeMillis())
                showMessage(msg)
            }.onFailure { err ->
                showMessage("خطأ في المزامنة: ${err.localizedMessage}")
            }
        }
    }

    fun importTrendyolOrders() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.importTrendyolOrders()
            _isSyncing.value = false
            result.onSuccess { count ->
                showMessage("تم استيراد $count طلب جديد من سوق ترنديول بنجاح")
            }.onFailure { err ->
                showMessage("خطأ استيراد طلبات ترنديول: ${err.localizedMessage}")
            }
        }
    }

    fun updateTrendyolConfig(supplierId: String, apiKey: String, interval: Int) {
        _trendyolConfig.value = _trendyolConfig.value.copy(
            supplierId = supplierId,
            apiKey = apiKey,
            syncIntervalMinutes = interval
        )
        showMessage("تم حفظ إعدادات ربط ترنديول")
    }

    // Support Ticket
    fun createSupportTicket(name: String, contact: String, subject: String, msg: String, priority: TicketPriority) {
        viewModelScope.launch {
            val result = repository.createTicket(name, contact, subject, msg, priority)
            result.onSuccess {
                showNewTicketDialog.value = false
                showMessage("تم إرسال تذكرة الدعم بنجاح، جاري المتابعة 24/7")
            }.onFailure {
                showMessage("حدث خطأ أثناء إرسال التذكرة")
            }
        }
    }

    fun resolveTicket(ticket: SupportTicket) {
        viewModelScope.launch {
            repository.updateTicketStatus(ticket, TicketStatus.RESOLVED, "تم حل المشكلة وتأكيد العميل")
            showMessage("تم إغلاق التذكرة #${ticket.ticketCode} بنجاح")
        }
    }

    // Backup & Restore
    fun createEncryptedBackup() {
        viewModelScope.launch {
            val res = repository.performBackup()
            res.onSuccess { log ->
                showMessage("تم أخذ نسخة احتياطية مشفرة بنجاح (${log.backupName})")
            }.onFailure {
                showMessage("فشل النسخ الاحتياطي")
            }
        }
    }

    // Employee shift toggle
    fun toggleEmployeeShift(employee: Employee) {
        viewModelScope.launch {
            val updated = employee.copy(isClockedIn = !employee.isClockedIn)
            repository.updateEmployee(updated)
            val actionName = if (updated.isClockedIn) "تسجيل بدء الوردية" else "تسجيل إنهاء الوردية"
            showMessage("$actionName للموظف ${employee.fullName}")
        }
    }
}
