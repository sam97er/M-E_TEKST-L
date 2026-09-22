package com.example.ui.screens.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrendyolOrderEntity
import com.example.data.repository.MAndETekstilRepository
import com.example.domain.model.OrderPrepStatus
import com.example.domain.model.TrendyolOrderStatus
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoSky
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val orders by repository.orders.collectAsState(initial = emptyList())
    val products by repository.products.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<TrendyolOrderStatus?>(null) }
    var selectedPrepFilter by remember { mutableStateOf<OrderPrepStatus?>(null) }

    // Dialog states
    var selectedOrderForDetail by remember { mutableStateOf<TrendyolOrderEntity?>(null) }
    var pendingStatusChange by remember { mutableStateOf<Pair<TrendyolOrderEntity, OrderPrepStatus>?>(null) }
    var showCreateSaleModal by remember { mutableStateOf(false) }

    val filteredOrders = orders.filter { order ->
        val matchesSearch = searchQuery.isBlank() ||
                order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                order.productCode.contains(searchQuery, ignoreCase = true)

        val matchesStatus = selectedStatusFilter == null || order.status == selectedStatusFilter
        val matchesPrep = selectedPrepFilter == null || order.prepStatus == selectedPrepFilter

        matchesSearch && matchesStatus && matchesPrep
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Akıllı Sipariş & Satış",
            subtitle = "${orders.size} Kayıtlı Satış/Sipariş • Canlı ERP",
            onRefreshClick = {
                scope.launch {
                    val result = repository.syncTrendyol()
                    snackbarHostState.showSnackbar(result.getOrDefault("Siparişler tazelendi."))
                }
            }
        )

        // Action and Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).testTag("order_search_input"),
                placeholder = { Text("Sipariş No, Müşteri, Ürün...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Temizle",
                            modifier = Modifier.clickable { searchQuery = "" }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedButton(
                onClick = {
                    val csv = repository.exportFinancialReportCsv(orders)
                    scope.launch {
                        snackbarHostState.showSnackbar("Mali sipariş CSV raporu hazırlandı (${orders.size} satır).")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = TrendyolOrange)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rapor", fontSize = 12.sp, color = TrendyolOrange)
            }

            Button(
                onClick = { showCreateSaleModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Satış", fontSize = 12.sp, color = Color.White)
            }
        }

        // Horizontal Status Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedStatusFilter == null && selectedPrepFilter == null,
                    onClick = {
                        selectedStatusFilter = null
                        selectedPrepFilter = null
                    },
                    label = { Text("Tümü (${orders.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TrendyolOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(OrderPrepStatus.values()) { prep ->
                FilterChip(
                    selected = selectedPrepFilter == prep,
                    onClick = {
                        selectedPrepFilter = if (selectedPrepFilter == prep) null else prep
                    },
                    label = { Text(prep.title, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TrendyolOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(TrendyolOrderStatus.values()) { status ->
                FilterChip(
                    selected = selectedStatusFilter == status,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == status) null else status
                    },
                    label = { Text(status.title, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyDark,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Orders List
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Empty",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aramanızla eşleşen sipariş bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.orderNumber }) { order ->
                    OrderCardItem(
                        order = order,
                        onCardClick = { selectedOrderForDetail = order },
                        onPrepStatusChange = { newStatus ->
                            pendingStatusChange = Pair(order, newStatus)
                        }
                    )
                }
            }
        }
    }

    // Modal: Create Manual Sale (Atomic Transaction)
    if (showCreateSaleModal) {
        val defaultBarcode = products.firstOrNull()?.barcode ?: "868000100101"
        var custName by remember { mutableStateOf("Ahmet Kaya") }
        var barcodeInput by remember { mutableStateOf(defaultBarcode) }
        var qtyInput by remember { mutableStateOf("1") }
        var unitPriceInput by remember { mutableStateOf("299.90") }
        var cityInput by remember { mutableStateOf("İstanbul") }
        var noteInput by remember { mutableStateOf("Doğrudan Mağaza/Trendyol Satışı") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        val selectedProduct = products.find { it.barcode == barcodeInput }

        AlertDialog(
            onDismissRequest = { showCreateSaleModal = false },
            title = { Text("Yeni Satış İşlemi Oluştur") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Satış onaylandığında stok otomatik düşülür, kâr hesaplanır ve hareket kaydı oluşturulur.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = custName,
                            onValueChange = { custName = it },
                            label = { Text("Müşteri Adı") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { barcodeInput = it },
                            label = { Text("Ürün Barkodu") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (selectedProduct != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${selectedProduct.productCode} (${selectedProduct.color} - ${selectedProduct.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Mevcut: ${selectedProduct.stockQuantity} adet", fontSize = 12.sp, color = SuccessGreen)
                                }
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qtyInput,
                                onValueChange = { qtyInput = it },
                                label = { Text("Adet") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = unitPriceInput,
                                onValueChange = { unitPriceInput = it },
                                label = { Text("Birim Fiyat (₺)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("Şehir") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("Not") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (errorMsg != null) {
                        item {
                            Text(errorMsg!!, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyInput.toIntOrNull() ?: 1
                        val price = unitPriceInput.toDoubleOrNull() ?: 299.90
                        val orderNum = "TY-MAN-${System.currentTimeMillis().toString().takeLast(6)}"

                        scope.launch {
                            val res = repository.executeSaleTransaction(
                                orderNumber = orderNum,
                                customerName = custName,
                                barcode = barcodeInput,
                                quantity = qty,
                                unitPrice = price,
                                city = cityInput,
                                note = noteInput
                            )
                            if (res.isSuccess) {
                                showCreateSaleModal = false
                                snackbarHostState.showSnackbar("Satış başarıyla kaydedildi ($orderNum).")
                            } else {
                                errorMsg = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Satışı Tamamla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSaleModal = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Status Change Confirmation Dialog (Mandatory User Review Rule)
    if (pendingStatusChange != null) {
        val (order, nextStatus) = pendingStatusChange!!
        AlertDialog(
            onDismissRequest = { pendingStatusChange = null },
            title = { Text("Hazırlık Durumu Güncelleme") },
            text = {
                Column {
                    Text("Sipariş No: ${order.orderNumber}")
                    Text("Müşteri: ${order.customerName}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Durum '${order.prepStatus.title}' ➔ '${nextStatus.title}' olarak güncellenecek. Onaylıyor musunuz?",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateOrderPrepStatus(order.orderNumber, nextStatus)
                            pendingStatusChange = null
                            snackbarHostState.showSnackbar("Sipariş durumu '${nextStatus.title}' yapıldı.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Onayla ve Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatusChange = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Order Detail Modal Dialog
    if (selectedOrderForDetail != null) {
        val order = selectedOrderForDetail!!
        AlertDialog(
            onDismissRequest = { selectedOrderForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sipariş & Maliyet Detayı", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    StatusPill(text = order.status.title, colorType = StatusColorType.INFO)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sipariş No: ${order.orderNumber}", fontWeight = FontWeight.Bold)
                    Text("Müşteri: ${order.customerName} (${order.city})")
                    Text("Ürün: ${order.itemsSummary}")
                    Text("Kod: ${order.productCode} • Barkod: ${order.barcode}")
                    Text("Varyant: Renk: ${order.color} • Beden: ${order.size} • Adet: ${order.quantity}")

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Toplam Satış: ${String.format(Locale.US, "%.2f", order.totalPrice)} ₺", fontWeight = FontWeight.Bold)
                            Text("Ürün Maliyeti: ${order.buyingCostTotal} ₺", fontSize = 12.sp)
                            Text("Komisyon Tutarı: ${order.commissionAmount} ₺", fontSize = 12.sp)
                            Text("Kargo Ücreti: ${order.shippingCost} ₺", fontSize = 12.sp)
                            Text("Net Kâr: ${String.format(Locale.US, "%.2f", order.netProfit)} ₺", fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }

                    Text("Hazırlık: ${order.prepStatus.title}", fontWeight = FontWeight.SemiBold)
                    if (order.returnStatus.isNotBlank()) {
                        Text("İade Durumu: ${order.returnStatus}", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedOrderForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
fun OrderCardItem(
    order: TrendyolOrderEntity,
    onCardClick: () -> Unit,
    onPrepStatusChange: (OrderPrepStatus) -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(order.orderDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("order_item_${order.orderNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = order.prepStatus.title,
                        colorType = when (order.prepStatus) {
                            OrderPrepStatus.WAITING -> StatusColorType.WARNING
                            OrderPrepStatus.BEING_PREPARED -> StatusColorType.INFO
                            OrderPrepStatus.READY -> StatusColorType.SUCCESS
                            OrderPrepStatus.COMPLETED -> StatusColorType.SUCCESS
                            else -> StatusColorType.DANGER
                        }
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.2f", order.totalPrice)} ₺",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TrendyolOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.customerName} • ${order.city}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = "${order.productCode} ➔ ${order.itemsSummary}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (order.netProfit > 0) {
                Text(
                    text = "Tahmini Net Kâr: ${String.format(Locale.US, "%.2f", order.netProfit)} ₺",
                    fontSize = 11.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row for order flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (order.prepStatus == OrderPrepStatus.WAITING) {
                        OutlinedButton(
                            onClick = { onPrepStatusChange(OrderPrepStatus.BEING_PREPARED) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hazırla", fontSize = 11.sp)
                        }
                    } else if (order.prepStatus == OrderPrepStatus.BEING_PREPARED) {
                        Button(
                            onClick = { onPrepStatusChange(OrderPrepStatus.READY) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kargoya Ver", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
