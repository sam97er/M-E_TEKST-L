package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ProductEntity
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TrendyolBlue
import com.example.ui.theme.TrendyolErrorRed
import com.example.ui.theme.TrendyolNavy
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeDark
import com.example.ui.theme.TrendyolOrangeLight
import com.example.ui.theme.TrendyolSuccessGreen
import com.example.ui.theme.TrendyolWarningYellow

@Composable
fun ProductsScreen(
    products: List<ProductEntity>,
    searchQuery: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStockChange: (Long, Int) -> Unit,
    onToggleActive: (Long) -> Unit,
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onAnalyzeBuybox: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("الكل", "Giyim / Moda", "Elektrikli Ev Aletleri", "Kozmetik", "Ayakkabı", "Elektronik")

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("products_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input"),
                placeholder = { Text("بحث بالاسم، الباركود، أو الماركة...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate400) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح", tint = Slate400)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrendyolOrange,
                    unfocusedBorderColor = Slate200
                )
            )

            // Category Filter row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(text = cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TrendyolOrange,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = Slate400, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "لم يتم العثور على منتجات", style = MaterialTheme.typography.titleMedium, color = Slate500)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_item_${product.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Product Image
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Slate200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = product.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Product Core Details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.brand,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TrendyolOrange
                                        )
                                        Text(
                                            text = product.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "باركود: ${product.barcode}",
                                            fontSize = 10.sp,
                                            color = Slate400
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        // Price
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${String.format("%.2f", product.salePrice)} ₺",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TrendyolOrangeDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${String.format("%.2f", product.listPrice)} ₺",
                                                fontSize = 11.sp,
                                                color = Slate400,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Buybox status banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (product.buyboxWinner) TrendyolSuccessGreen.copy(alpha = 0.12f)
                                            else TrendyolErrorRed.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (product.buyboxWinner) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = if (product.buyboxWinner) TrendyolSuccessGreen else TrendyolErrorRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (product.buyboxWinner) "🏆 كسب الباي بوكس (Buybox Kazananı)"
                                                else "⚠️ خاسر الباي بوكس (سعر المنافس: ${product.competitorPrice} ₺)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (product.buyboxWinner) TrendyolSuccessGreen else TrendyolErrorRed
                                            )
                                        }

                                        // AI Analysis button
                                        IconButton(
                                            onClick = { onAnalyzeBuybox(product) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "تحليل الباي بوكس",
                                                tint = TrendyolOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Stock Stepper & Store Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Stock stepper
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onStockChange(product.id, -1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص المخزون", modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = "المخزون: ${product.stockCount}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                product.stockCount == 0 -> TrendyolErrorRed
                                                product.stockCount < 5 -> TrendyolWarningYellow
                                                else -> TrendyolSuccessGreen
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { onStockChange(product.id, 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة المخزون", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Active Switch & Controls
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (product.isActive) "معروض" else "متوقف",
                                            fontSize = 11.sp,
                                            color = if (product.isActive) TrendyolSuccessGreen else Slate400,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = product.isActive,
                                            onCheckedChange = { onToggleActive(product.id) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = TrendyolSuccessGreen
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { onEditProduct(product) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = Slate500, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(
                                            onClick = { onDeleteProduct(product) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = TrendyolErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
