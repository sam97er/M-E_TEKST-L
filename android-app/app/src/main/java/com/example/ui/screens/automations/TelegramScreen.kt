package com.example.ui.screens.automations

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
import com.example.security.TelegramConfiguration
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.launch

@Composable
fun TelegramScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val initialConfig = remember { repository.getTelegramConfig() }
    val scope = rememberCoroutineScope()

    var isEnabled by remember { mutableStateOf(initialConfig.isEnabled) }
    var botToken by remember { mutableStateOf(initialConfig.botToken) }
    var chatId by remember { mutableStateOf(initialConfig.chatId) }

    var notifyOrders by remember { mutableStateOf(initialConfig.notifyOrders) }
    var notifyQuestions by remember { mutableStateOf(initialConfig.notifyQuestions) }
    var notifyLowStock by remember { mutableStateOf(initialConfig.notifyLowStock) }
    var notifyReturns by remember { mutableStateOf(initialConfig.notifyReturns) }
    var notifyDailyBrief by remember { mutableStateOf(initialConfig.notifyDailyBrief) }
    var quietHoursEnabled by remember { mutableStateOf(initialConfig.quietHoursEnabled) }

    var isTesting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Telegram Bildirim Merkezi",
            subtitle = "Sipariş, Soru, Stok ve Rapor Bildirimleri"
        )

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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Telegram Bildirimlerini Etkinleştir", fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = TrendyolOrange)
                            )
                        }

                        OutlinedTextField(
                            value = botToken,
                            onValueChange = { botToken = it },
                            label = { Text("Telegram Bot Token (örn: 123456:ABC-DEF...)") },
                            modifier = Modifier.fillMaxWidth().testTag("telegram_bot_token_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = chatId,
                            onValueChange = { chatId = it },
                            label = { Text("Telegram Chat ID / Grup ID") },
                            modifier = Modifier.fillMaxWidth().testTag("telegram_chat_id_input"),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bildirim Kategorileri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        NotificationCheckboxRow(label = "Yeni Sipariş Bildirimi", checked = notifyOrders, onChecked = { notifyOrders = it })
                        NotificationCheckboxRow(label = "Yeni Müşteri Sorusu", checked = notifyQuestions, onChecked = { notifyQuestions = it })
                        NotificationCheckboxRow(label = "Kritik Düşük Stok (< 5 adet)", checked = notifyLowStock, onChecked = { notifyLowStock = it })
                        NotificationCheckboxRow(label = "Yeni İade Talebi", checked = notifyReturns, onChecked = { notifyReturns = it })
                        NotificationCheckboxRow(label = "Günlük Yönetici Özeti (Akşam)", checked = notifyDailyBrief, onChecked = { notifyDailyBrief = it })
                        NotificationCheckboxRow(label = "Gece Sessiz Saatler (23:00 - 07:00)", checked = quietHoursEnabled, onChecked = { quietHoursEnabled = it })
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val newCfg = TelegramConfiguration(
                                isEnabled = isEnabled,
                                botToken = botToken,
                                chatId = chatId,
                                notifyOrders = notifyOrders,
                                notifyQuestions = notifyQuestions,
                                notifyLowStock = notifyLowStock,
                                notifyReturns = notifyReturns,
                                notifyDailyBrief = notifyDailyBrief,
                                quietHoursEnabled = quietHoursEnabled
                            )
                            repository.saveTelegramConfig(newCfg)
                            scope.launch {
                                snackbarHostState.showSnackbar("Telegram ayarları kaydedildi.")
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_telegram_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                    ) {
                        Text("Ayarları Kaydet", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                val res = repository.sendTelegramTest("🔔 M&E Tekstil Trendyol bot bağlantı testi başarılı!")
                                isTesting = false
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Test mesajı Telegram'a başarıyla iletildi.")
                                } else {
                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Gönderilemedi.")
                                }
                            }
                        },
                        enabled = !isTesting && isEnabled && botToken.isNotBlank() && chatId.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("test_telegram_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text("Test Gönder", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = TrendyolOrange)
        )
        Text(text = label, fontSize = 13.sp)
    }
}
