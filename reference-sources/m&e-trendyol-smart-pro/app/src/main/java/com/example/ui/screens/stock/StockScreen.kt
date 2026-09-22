package com.example.ui.screens.stock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.local.TrendyolProductEntity
import com.example.data.local.WarehouseEntity
import com.example.data.repository.MAndETekstilRepository
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
fun StockScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val products by repository.products.collectAsState(initial = emptyList())
    val lowStockProducts by repository.lowStockProducts.collectAsState(initial = emptyList())
    val movements by repository.stockMovements.collectAsState(initial = emptyList())
    val warehouses by repository.warehouses.collectAsState(initial = emptyList())
    val transfers by repository.warehouseTransfers.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Ürünler & Varyantlar, 1: Depo & Transfer, 2: AI Risk, 3: Hareketler
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, LOW, OUT, DEAD

    // Modals state
    var productToEdit by remember { mutableStateOf<TrendyolProductEntity?>(null) }
    var editQuantityText by remember { mutableStateOf("") }
    var editNoteText by remember { mutableStateOf("") }

    var showAddProductModal by remember { mutableStateOf(false) }
    var showCsvModal by remember { mutableStateOf(false) }
    var showTransferModal by remember { mutableStateOf(false) }

    val filteredProducts = products.filter { p ->
        val matchesSearch = searchQuery.isBlank() ||
                p.productCode.contains(searchQuery, ignoreCase = true) ||
                p.title.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true) ||
                p.color.contains(searchQuery, ignoreCase = true) ||
                p.size.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (filterType) {
            "LOW" -> p.stockQuantity in 1..4
            "OUT" -> p.stockQuantity <= 0
            "DEAD" -> p.totalSold == 0
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "M&E Stok & Varyant ERP",
            subtitle = "${products.size} Varyant • Kritik: ${lowStockProducts.size} • Depo: ${warehouses.size}"
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = TrendyolOrange
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Varyantlar", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Depo & Transfer", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("AI Tedarik", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Hareketler", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Products & Variants Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search & Action row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).testTag("stock_search_input"),
                            placeholder = { Text("Kod, Renk, Beden, Barkod...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedButton(
                            onClick = { showCsvModal = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "CSV", tint = TrendyolOrange)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 12.sp, color = TrendyolOrange)
                        }

                        Button(
                            onClick = { showAddProductModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekle", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Filter chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text("Tümü (${products.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TrendyolOrange.copy(alpha = 0.15f),
                                selectedLabelColor = TrendyolOrange
                            )
                        )
                        FilterChip(
                            selected = filterType == "LOW",
                            onClick = { filterType = "LOW" },
                            label = { Text("Kritik (<5)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRed.copy(alpha = 0.15f),
                                selectedLabelColor = DangerRed
                            )
                        )
                        FilterChip(
                            selected = filterType == "OUT",
                            onClick = { filterType = "OUT" },
                            label = { Text("Tükendi (0)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterType == "DEAD",
                            onClick = { filterType = "DEAD" },
                            label = { Text("Hareketsiz", fontSize = 11.sp) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts, key = { it.barcode }) { product ->
                            StockProductHierarchyCard(
                                product = product,
                                onQuickStockChange = {
                                    productToEdit = product
                                    editQuantityText = product.stockQuantity.toString()
                                    editNoteText = "Depo sayım düzeltmesi"
                                },
                                onToggleActive = {
                                    scope.launch {
                                        repository.toggleProductActive(product.barcode)
                                        snackbarHostState.showSnackbar("Durum güncellendi.")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Multi-Warehouse & Inter-Depot Transfers
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warehouse, contentDescription = null, tint = TrendyolOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kayıtlı Depo & Mağazalar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Button(
                                        onClick = { showTransferModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Transfer Yap", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                warehouses.forEach { wh ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(wh.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("ID: ${wh.warehouseId} • ${wh.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            StatusPill(text = if (wh.isDefault) "Varsayılan" else "Ek Depo", colorType = StatusColorType.INFO)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Depolar Arası Transfer Geçmişi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    if (transfers.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Henüz depo transfer kaydı bulunmuyor.", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(transfers) { trf ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${trf.productTitle} [${trf.color} - ${trf.size}]", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${trf.fromWarehouseId} ➔ ${trf.toWarehouseId} • Adet: ${trf.quantity}", fontSize = 12.sp, color = TrendyolOrange)
                                        Text("Not: ${trf.note} • Yapan: ${trf.performedBy}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // AI Risk & Tedarik
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = TrendyolOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Stok Risk & Tedarik Öngörüsü", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Mevcut satış hızı ve kalan kumaş/stok durumuna göre M&E Tekstil için otomatik kesim ve tedarik tavsiyeleri.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(lowStockProducts) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${p.productCode} • ${p.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    StatusPill(text = "${p.stockQuantity} Adet Kaldı", colorType = StatusColorType.DANGER)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Varyant: ${p.color} (${p.size}) • Barkod: ${p.barcode}", fontSize = 12.sp)
                                Text("Tavsiye: Kumaş (${p.fabricDetails}) için acil 20 adet ilave üretim kesim emri önerilir.", fontSize = 12.sp, color = DangerRed)
                            }
                        }
                    }
                }
            }

            3 -> {
                // Stock Movement Ledger
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (movements.isEmpty()) {
                        item {
                            Text("Henüz stok hareket kaydı bulunmuyor.", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(movements) { mov ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = mov.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "İşlem: ${mov.movementType} • ${mov.note}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val timeStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(mov.timestamp))
                                        Text(text = "$timeStr • Depo: ${mov.warehouseId} • Yapan: ${mov.performedBy}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (mov.quantityChange > 0) "+${mov.quantityChange}" else "${mov.quantityChange}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = if (mov.quantityChange >= 0) SuccessGreen else DangerRed
                                        )
                                        Text(text = "Kalan: ${mov.remainingStock}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Quick Stock Edit (with negative stock prevention)
    if (productToEdit != null) {
        val prod = productToEdit!!
        var hasError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { productToEdit = null },
            title = { Text("Stok Miktarını Düzelt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${prod.productCode} ➔ ${prod.title}\nVaryant: ${prod.color} - ${prod.size}", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = editQuantityText,
                        onValueChange = {
                            editQuantityText = it
                            hasError = null
                        },
                        label = { Text("Yeni Stok Adedi (Negatif Girilemez)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNoteText,
                        onValueChange = { editNoteText = it },
                        label = { Text("Düzeltme Nedeni / Sayım Notu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (hasError != null) {
                        Text(hasError!!, color = DangerRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = editQuantityText.toIntOrNull()
                        if (newQty == null) {
                            hasError = "Geçerli bir sayı giriniz."
                            return@Button
                        }
                        if (newQty < 0) {
                            hasError = "M&E ERP Kuralı: Stok adedi negatif olamaz!"
                            return@Button
                        }
                        scope.launch {
                            val res = repository.adjustStockWithAudit(prod.barcode, newQty, editNoteText)
                            if (res.isSuccess) {
                                productToEdit = null
                                snackbarHostState.showSnackbar("Stok $newQty adet olarak güncellendi.")
                            } else {
                                hasError = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToEdit = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Modal: Inter-Depot Transfer
    if (showTransferModal) {
        var barcodeInput by remember { mutableStateOf(products.firstOrNull()?.barcode ?: "") }
        var fromWh by remember { mutableStateOf("W-MAIN") }
        var toWh by remember { mutableStateOf("W-WORKSHOP") }
        var transferQtyText by remember { mutableStateOf("5") }
        var transferNote by remember { mutableStateOf("Atölye tamir ve dikim transferi") }
        var transferError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showTransferModal = false },
            title = { Text("Depolar Arası Transfer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kaynak Depo: $fromWh ➔ Hedef Depo: $toWh", fontSize = 12.sp, color = TrendyolOrange)

                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        label = { Text("Ürün Barkodu") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferQtyText,
                        onValueChange = { transferQtyText = it },
                        label = { Text("Transfer Adedi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferNote,
                        onValueChange = { transferNote = it },
                        label = { Text("Transfer Nedeni") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (transferError != null) {
                        Text(transferError!!, color = DangerRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = transferQtyText.toIntOrNull() ?: 0
                        scope.launch {
                            val res = repository.executeWarehouseTransfer(
                                barcode = barcodeInput,
                                fromWarehouseId = fromWh,
                                toWarehouseId = toWh,
                                quantity = qty,
                                note = transferNote
                            )
                            if (res.isSuccess) {
                                showTransferModal = false
                                snackbarHostState.showSnackbar("Depo transferi tamamlandı.")
                            } else {
                                transferError = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Transferi Onayla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferModal = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Modal: CSV Export & Import
    if (showCsvModal) {
        var csvImportInput by remember { mutableStateOf("") }
        var importResultText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCsvModal = false },
            title = { Text("Excel / CSV Veri Yönetimi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tüm ürün ve varyantları dışa aktarabilir veya toplu olarak içe aktarabilirsiniz.", fontSize = 12.sp)

                    Button(
                        onClick = {
                            val csv = repository.exportProductsCsv(products)
                            csvImportInput = csv
                            scope.launch {
                                snackbarHostState.showSnackbar("Ürün CSV metni oluşturuldu (${products.size} ürün).")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV Olarak Dışa Aktar", color = Color.White)
                    }

                    OutlinedTextField(
                        value = csvImportInput,
                        onValueChange = { csvImportInput = it },
                        label = { Text("CSV Metni (İçe Aktarmak İçin Yapıştır)") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("Barkod,UrunKodu,UrunAdi,Kategori,Renk,Beden,Stok...") }
                    )

                    if (importResultText != null) {
                        Text(importResultText!!, fontSize = 12.sp, color = TrendyolOrange)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = repository.importProductsFromCsv(csvImportInput)
                            if (res.isSuccess) {
                                val (count, errors) = res.getOrThrow()
                                importResultText = "$count ürün başarıyla aktarıldı. Hatalı: ${errors.size}"
                                snackbarHostState.showSnackbar("$count ürün içe aktarıldı.")
                            } else {
                                importResultText = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("İçe Aktar (Import)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvModal = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Modal: Add New Variant
    if (showAddProductModal) {
        var code by remember { mutableStateOf("ME-TSH-03") }
        var barcode by remember { mutableStateOf("868000100999") }
        var title by remember { mutableStateOf("Pamuklu Polo Yaka Tişört") }
        var color by remember { mutableStateOf("Lacivert") }
        var size by remember { mutableStateOf("M") }
        var stock by remember { mutableStateOf("25") }
        var buyPrice by remember { mutableStateOf("115.0") }
        var salePrice by remember { mutableStateOf("329.90") }
        var commission by remember { mutableStateOf("18.5") }
        var warehouse by remember { mutableStateOf("W-MAIN") }
        var fabric by remember { mutableStateOf("%100 Merserize Pamuk") }
        var addError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddProductModal = false },
            title = { Text("Yeni Ürün / Varyant Ekle") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Ürün Kodu") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barkod") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Ürün Adı") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Renk") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("Beden") }, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stok Adedi") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = it }, label = { Text("Alış (₺)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = salePrice, onValueChange = { salePrice = it }, label = { Text("Satış (₺)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(value = commission, onValueChange = { commission = it }, label = { Text("Komisyon (%)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                    item {
                        OutlinedTextField(value = fabric, onValueChange = { fabric = it }, label = { Text("Kumaş Detayı") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (addError != null) {
                        item {
                            Text(addError!!, color = DangerRed, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val product = TrendyolProductEntity(
                            barcode = barcode.trim(),
                            productCode = code.trim(),
                            title = title.trim(),
                            categoryName = "Tekstil",
                            color = color.trim(),
                            size = size.trim(),
                            stockQuantity = stock.toIntOrNull() ?: 0,
                            salePrice = salePrice.toDoubleOrNull() ?: 299.90,
                            buyingPrice = buyPrice.toDoubleOrNull() ?: 110.0,
                            commissionRate = commission.toDoubleOrNull() ?: 18.5,
                            warehouseId = warehouse,
                            fabricDetails = fabric.trim(),
                            isActive = true
                        )
                        scope.launch {
                            val res = repository.saveProductVariant(product)
                            if (res.isSuccess) {
                                showAddProductModal = false
                                snackbarHostState.showSnackbar("Varyant kaydedildi.")
                            } else {
                                addError = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductModal = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

/**
 * Strict Hierarchy Card:
 * [Ürün Kodu] ➔ [Ürün Adı] ➔ [Renk] ➔ [Beden] ➔ [Adet]
 */
@Composable
fun StockProductHierarchyCard(
    product: TrendyolProductEntity,
    onQuickStockChange: () -> Unit,
    onToggleActive: () -> Unit
) {
    val isLowStock = product.stockQuantity < 5

    Card(
        modifier = Modifier.fillMaxWidth().testTag("stock_item_${product.barcode}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) DangerRed.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Strict hierarchy bar: [Product Code] -> [Title]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavyDark
                    ) {
                        Text(
                            text = product.productCode,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                Switch(
                    checked = product.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.size(36.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SuccessGreen,
                        checkedTrackColor = SuccessGreen.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-hierarchy: [Renk] ➔ [Beden] ➔ [Depo] ➔ [Adet]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(text = product.color, colorType = StatusColorType.NEUTRAL)
                    StatusPill(text = "Beden: ${product.size}", colorType = StatusColorType.INFO)
                    StatusPill(text = product.warehouseId, colorType = StatusColorType.NEUTRAL)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${product.stockQuantity} Adet",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isLowStock) DangerRed else SuccessGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onQuickStockChange,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Düzelt", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Satış: ${String.format(Locale.US, "%.2f", product.salePrice)} ₺ • Alış: ${product.buyingPrice} ₺ • Komisyon: %${product.commissionRate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Satılan: ${product.totalSold} • İade: ${product.returnCount}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
