package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ChatMessage
import com.example.model.InventoryItem
import com.example.model.PricingProduct
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ErpViewModel(application: Application) : AndroidViewModel(application) {

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "ai",
            text = "أهلاً بك يا مالك متجر M&E Tekstil. أنا العقل المدبر لنظام ERP الذكي. جاهز لإدارة المخزون، الرد على عملاء ترنديول، تحليل صندوق الشراء (Buybox)، وإرسال التقارير اليومية. 🌸"
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(listOf(
        InventoryItem("1", "Crop Top Modal", "M&E Pudra Pembesi", "M", 0, 249.90, true, "A1"),
        InventoryItem("2", "Crop Top Modal", "M&E Pudra Pembesi", "S", 12, 249.90, true, "A1"),
        InventoryItem("3", "Wide Leg Pantolon", "Siyah", "38", 45, 499.90, true, "A2"),
        InventoryItem("4", "Seamless Body", "Gül Kurusu", "S/M", 3, 329.90, true, "B1"),
        InventoryItem("5", "Wide Leg Pantolon", "Bej", "40", 28, 499.90, false, "A2"),
        InventoryItem("6", "Seamless Leggings", "Gül Kurusu", "M/L", 18, 379.90, true, "B2")
    ))
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _pricingProducts = MutableStateFlow<List<PricingProduct>>(listOf(
        PricingProduct("1", "Crop Top Modal", "M&E Pudra Pembesi", 90.0, 249.90),
        PricingProduct("2", "Wide Leg Pantolon", "Siyah", 180.0, 499.90),
        PricingProduct("3", "Seamless Body", "Gül Kurusu", 110.0, 329.90),
        PricingProduct("4", "Seamless Leggings", "Gül Kurusu", 130.0, 379.90)
    ))
    val pricingProducts: StateFlow<List<PricingProduct>> = _pricingProducts.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), sender = "user", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isGenerating.value = true
            delay(800) // simulate thinking
            
            val aiReplyText = generateSmartResponse(userText)
            val hasApproval = aiReplyText.contains("هل أعتمد") || aiReplyText.contains("هل أوافق") || aiReplyText.contains("هل أقوم") || aiReplyText.contains("أقترح")
            val aiMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = "ai",
                text = aiReplyText,
                hasApproval = hasApproval
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isGenerating.value = false
        }
    }

    private fun generateSmartResponse(input: String): String {
        return when {
            input.contains("مبيعات") || input.contains("طلب") || input.contains("Pudra Pembesi") -> """
                📊 ملخص المبيعات: 24 طلباً اليوم (أداء ممتاز).
                ⚠️ تنبيه المخزون: لون Pudra Pembesi (كروب توب مقاس M) نفد تماماً. أرجو تحديث ملف stock.xlsx وتجهيز طلبية جديدة من المصنع لتجنب خسارة ترتيب المنتج في ترنديول.
                💬 الرد المقترح للعميل (جاهز للنسخ):
                "Merhaba, Mankenimizin üzerindeki ürün S bedendir. Ürünlerimiz yüksek kaliteli ve %100 pamuklu içerikli olduğundan terletme yapmaz, gün boyu rahatlıkla kullanabilirsiniz. İlginiz için teşekkür eder, M&E Tekstil olarak keyifli alışverişler dileriz. 🌸"
            """.trimIndent()
            input.contains("جرد") || input.contains("رف") || input.contains("Gül Kurusu") -> """
                📦 تحديث الجرد: الرفوف A1 و A2 (بناطيل Wide Leg) في وضع آمن.
                💰 استراتيجية التسعير (Seamless - Gül Kurusu): بما أن الكمية انخفضت إلى 3 قطع فقط، أقترح رفع السعر بنسبة 10-15% فوراً لإبطاء وتيرة البيع زيادة هامش الربح حتى تصل الدفعة الجديدة من التصنيع.
                ❓ هل أعتمد هذا التعديل وأقوم بإنشاء كود بايثون لتعديل السعر عبر Trendyol API؟
            """.trimIndent()
            input.contains("سؤال") || input.contains("عميل") || input.contains("Mankenin") -> """
                💬 الرد المقترح للعميل (جاهز للنسخ):
                "Merhaba, Mankenimizin üzerindeki ürün S bedendir. Ürünlerimiz yüksek kaliteli ve %100 pamuklu içerikli olduğundan terletme yapmaz, gün boyu rahatlıkla kullanabilirsiniz. İlginiz için teşekkür eder, M&E Tekstil olarak keyifli alışverişler dileriz. 🌸"
            """.trimIndent()
            else -> """
                📊 تقرير المبيعات اليومي: الأرباح الصافية مستقر بمعدل 32% بعد خصم عمولة ترنديول.
                ⚠️ المخزون: الكروب توب Pudra Pembesi (مقاس M) بحاجة لإعادة طلب عاجلة.
                💬 النظام جاهز لمعالجة طلبات الجرد، أسئلة عملاء ترنديول، وتحليلات الـ Buybox. 🌸
            """.trimIndent()
        }
    }

    fun updateStock(itemId: String, newStock: Int) {
        _inventory.value = _inventory.value.map {
            if (it.id == itemId) it.copy(stockCount = newStock) else it
        }
    }

    fun approveAction(actionText: String) {
        val approvalMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "user",
            text = "تمت الموافقة بنجاح: $actionText ✅"
        )
        val aiConfirmation = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "ai",
            text = "تم تنفيذ الإجراء بنجاح وتحديث النظام عبر Trendyol API. تم إرسال الإشعار لفريق المستودع. 🚀"
        )
        _chatMessages.value = _chatMessages.value + approvalMsg + aiConfirmation
    }
}
