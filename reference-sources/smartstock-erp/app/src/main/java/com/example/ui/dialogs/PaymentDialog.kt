package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SaleOrder
import com.example.ui.theme.ErpNavyPrimary
import com.example.ui.theme.ErpStrings
import com.example.ui.theme.ErpSuccess
import com.example.ui.theme.ErpTealSecondary
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.delay

@Composable
fun PaymentDialog(
    cart: Map<Product, Int>,
    currency: AppCurrency,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmPayment: (customerName: String, customerPhone: String, method: PaymentMethod, cashier: String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CARD_MADA) }
    var isProcessing by remember { mutableStateOf(false) }
    var paymentStep by remember { mutableStateOf(1) } // 1: Select, 2: Terminal Simulation, 3: Completed

    val totalSar = cart.entries.sumOf { it.key.sellingPrice * it.value }
    val totalFormatted = currency.format(totalSar)

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = ErpNavyPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ErpStrings.get("checkout", language),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Box
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ErpStrings.get("total_payable", language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalFormatted,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ErpNavyPrimary
                        )
                        Text(
                            text = "${cart.values.sum()} ${if (language == AppLanguage.ARABIC) "قطعة في الفاتورة" else "items in invoice"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (paymentStep == 1) {
                    // Customer Details
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(ErpStrings.get("customer_name", language)) },
                        placeholder = { Text("مثال: فيصل العتيبي") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
                    )

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text(ErpStrings.get("customer_phone", language)) },
                        placeholder = { Text("+966 5X XXX XXXX") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_phone_input")
                    )

                    Text(
                        text = ErpStrings.get("select_payment_method", language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Payment Method Options
                    val methods = listOf(
                        PaymentMethod.CARD_MADA to Icons.Default.CreditCard,
                        PaymentMethod.ELECTRONIC_WALLET to Icons.Default.AccountBalanceWallet,
                        PaymentMethod.CREDIT_CARD to Icons.Default.CreditCard,
                        PaymentMethod.CASH to Icons.Default.LocalAtm,
                        PaymentMethod.SPLIT to Icons.Default.ReceiptLong
                    )

                    methods.forEach { (method, icon) ->
                        val isSelected = selectedMethod == method
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 2.dp) else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMethod = method }
                                .testTag("payment_method_${method.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) ErpNavyPrimary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) method.titleAr else method.titleEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (method == PaymentMethod.CARD_MADA) {
                                        Text(
                                            text = "دعم مدى NFC، Apple Pay، نقاط البيع السريعة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ErpSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (paymentStep == 2) {
                    // POS Payment Gateway Contactless Terminal Simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = ErpTealSecondary,
                                modifier = Modifier.size(54.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "جاري الاتصال بجهاز نقطة البيع (POS Terminal)...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مرر البطاقة أو استخدم الهاتف (NFC / Apple Pay) لإتمام العملية",
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LaunchedEffect(Unit) {
                        delay(1200)
                        isProcessing = false
                        onConfirmPayment(
                            customerName.ifBlank { "عميل محلي" },
                            customerPhone,
                            selectedMethod,
                            "كاشير النظام"
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (paymentStep == 1) {
                Button(
                    onClick = {
                        paymentStep = 2
                        isProcessing = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary),
                    modifier = Modifier.testTag("submit_payment_button")
                ) {
                    Text(ErpStrings.get("confirm", language))
                }
            }
        },
        dismissButton = {
            if (paymentStep == 1) {
                TextButton(onClick = onDismiss) {
                    Text(ErpStrings.get("cancel", language))
                }
            }
        }
    )
}

@Composable
fun OrderReceiptDialog(
    order: SaleOrder,
    currency: AppCurrency,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ErpSuccess,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فاتورة مبيعات إلكترونية معتمدة",
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "رقم الفاتورة: ${order.orderNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "العميل: ${order.customerName}",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "وسيلة الدفع: ${order.paymentMethod.titleAr}",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "الكاشير: ${order.cashierName}",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "الحالة: ${order.paymentStatus} (مدفوع)",
                            color = ErpSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المبلغ الإجمالي شامل الضريبة:",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currency.format(order.totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        color = ErpNavyPrimary,
                        fontSize = 18.sp
                    )
                }

                // Simulated ZATCA / Tax QR Code Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "████  ██  ████",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "رمز الاستجابة السريع المعتمد (QR Code)",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ErpNavyPrimary)
            ) {
                Text(ErpStrings.get("close", language))
            }
        }
    )
}
