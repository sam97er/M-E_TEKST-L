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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AutomationRuleEntity
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.launch

@Composable
fun AutomationsScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val rules by repository.automationRules.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (rules.isEmpty()) {
            repository.initDefaultAutomationsIfEmpty()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Otomasyon Yöneticisi",
            subtitle = "${rules.size} Kural Tanımlı • Tetikleyici, Koşul & Eylem"
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = "Otomasyon", tint = TrendyolOrange, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Otomasyon Güvenlik Kuralları", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Otomasyonlar sadece bilgilendirme ve taslak üretimi yapar. Kritik fiyat ve stok değişiklikleri insan onayından geçer.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(rules, key = { it.id }) { rule ->
                AutomationRuleCard(
                    rule = rule,
                    onToggle = {
                        scope.launch {
                            repository.toggleAutomation(rule)
                            snackbarHostState.showSnackbar(
                                if (!rule.isEnabled) "'${rule.name}' aktif edildi." else "'${rule.name}' pasifleştirildi."
                            )
                        }
                    },
                    onTestRule = {
                        scope.launch {
                            val res = repository.testAutomationRule(rule.id)
                            snackbarHostState.showSnackbar(res.getOrDefault("Kural test edildi."))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AutomationRuleCard(
    rule: AutomationRuleEntity,
    onToggle: () -> Unit,
    onTestRule: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("automation_rule_${rule.id}"),
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
                Text(text = rule.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TrendyolOrange)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Koşul: ${rule.conditionDescription}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = "Tetikleyici: ${rule.triggerType}", colorType = StatusColorType.INFO)
                StatusPill(text = "Eylem: ${rule.actionType}", colorType = StatusColorType.NEUTRAL)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Çalıştırılma: ${rule.executionCount} kez", fontSize = 11.sp, color = Color.Gray)
                OutlinedButton(
                    onClick = onTestRule,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Test", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kuralı Test Et", fontSize = 11.sp)
                }
            }
        }
    }
}
