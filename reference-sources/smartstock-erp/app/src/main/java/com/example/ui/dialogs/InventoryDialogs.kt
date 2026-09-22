package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.data.model.TicketPriority
import com.example.data.model.Warehouse
import com.example.ui.theme.ErpNavyPrimary
import com.example.ui.theme.ErpStrings
import com.example.ui.theme.ErpTealSecondary

@Composable
fun AddProductDialog(
    warehouses: List<Warehouse>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ملابس وأزياء (Fashion)") }
    var selectedWarehouseId by remember { mutableLongStateOf(warehouses.firstOrNull()?.id ?: 1L) }
    var qtyString by remember { mutableStateOf("25") }
    var minAlertString by remember { mutableStateOf("10") }
    var costPriceString by remember { mutableStateOf("50.0") }
    var sellingPriceString by remember { mutableStateOf("99.0") }
    var trendyolBarcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = ErpNavyPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ErpStrings.get("add_product", language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المنتج بالعربية *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Product Name (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود العالمي") },
                        placeholder = { Text("62810...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_barcode_input")
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("رمز SKU") },
                        placeholder = { Text("SKU-101") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = trendyolBarcode,
                    onValueChange = { trendyolBarcode = it },
                    label = { Text("باركود متجر ترنديول (Trendyol Barcode)") },
                    placeholder = { Text("TY-PROD-...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyString,
                        onValueChange = { qtyString = it },
                        label = { Text("كمية المخزون") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minAlertString,
                        onValueChange = { minAlertString = it },
                        label = { Text("حد التنبيه") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceString,
                        onValueChange = { costPriceString = it },
                        label = { Text("التكلفة (ر.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceString,
                        onValueChange = { sellingPriceString = it },
                        label = { Text("سعر البيع (ر.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Warehouse selector
                Text(
                    text = "المستودع المخصص:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                warehouses.forEach { wh ->
                    val isSelected = selectedWarehouseId == wh.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedWarehouseId = wh.id }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = wh.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ErpNavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            nameEn,
                            barcode,
                            sku,
                            category,
                            selectedWarehouseId,
                            qtyString.toIntOrNull() ?: 10,
                            minAlertString.toIntOrNull() ?: 5,
                            costPriceString.toDoubleOrNull() ?: 0.0,
                            sellingPriceString.toDoubleOrNull() ?: 0.0,
                            trendyolBarcode
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(ErpStrings.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(ErpStrings.get("cancel", language))
            }
        }
    )
}

@Composable
fun AdjustStockDialog(
    product: Product,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (newStock: Int) -> Unit
) {
    var stockCount by remember { mutableIntStateOf(product.stockQuantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل كمية المخزون",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { if (stockCount > 0) stockCount-- },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "نقص")
                    }

                    Text(
                        text = "$stockCount",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ErpNavyPrimary
                    )

                    IconButton(
                        onClick = { stockCount++ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "زيادة")
                    }
                }

                Text(
                    text = "حد التنبيه الأدنى: ${product.minAlertStock} قطعة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(stockCount) },
                colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary)
            ) {
                Text(ErpStrings.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(ErpStrings.get("cancel", language))
            }
        }
    )
}

@Composable
fun LanguageCurrencyDialog(
    currentLanguage: AppLanguage,
    currentCurrency: AppCurrency,
    onDismiss: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (AppCurrency) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات اللغة والعملة (Localization)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = ErpNavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "اختر لغة الواجهة:", fontWeight = FontWeight.Bold)
                }

                AppLanguage.entries.forEach { lang ->
                    val isSelected = currentLanguage == lang
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLanguage(lang) }
                            .testTag("lang_${lang.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lang.displayName} (${lang.code.uppercase()})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ErpNavyPrimary)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = ErpNavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "اختر عملة الحسابات:", fontWeight = FontWeight.Bold)
                }

                AppCurrency.entries.forEach { curr ->
                    val isSelected = currentCurrency == curr
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCurrency(curr) }
                            .testTag("curr_${curr.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${curr.symbol} - ${curr.displayNameAr} (${curr.code})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ErpNavyPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary)
            ) {
                Text("تم")
            }
        }
    )
}

@Composable
fun NewTicketDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (name: String, contact: String, subject: String, msg: String, priority: TicketPriority) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TicketPriority.HIGH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = ErpNavyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ErpStrings.get("create_ticket", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل أو المتجر") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("رقم التواصل أو البريد") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("موضوع التذكرة") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("تفاصيل المشكلة أو الطلب") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "الأولوية:", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TicketPriority.entries.forEach { p ->
                        val isSelected = selectedPriority == p
                        Button(
                            onClick = { selectedPriority = p },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ErpNavyPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = p.titleAr, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && subject.isNotBlank()) {
                        onConfirm(name, contact, subject, message, selectedPriority)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary)
            ) {
                Text(ErpStrings.get("save", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(ErpStrings.get("cancel", language))
            }
        }
    )
}
