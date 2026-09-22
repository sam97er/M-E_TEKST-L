package com.example.domain

import com.example.security.TelegramConfiguration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramNotifierTest {

    @Test
    fun testTelegramTokenValidation() {
        val validConfig = TelegramConfiguration(
            botToken = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz1234567",
            chatId = "-1001234567890",
            isEnabled = true
        )
        // Check format
        val tokenRegex = Regex("^\\d{8,12}:[A-Za-z0-9_-]{30,50}$")
        assertTrue(tokenRegex.matches(validConfig.botToken))
    }

    @Test
    fun testInvalidTelegramTokens() {
        val tokenRegex = Regex("^\\d{8,12}:[A-Za-z0-9_-]{30,50}$")
        assertFalse(tokenRegex.matches(""))
        assertFalse(tokenRegex.matches("invalid_token_without_colon"))
        assertFalse(tokenRegex.matches("123:short"))
    }

    @Test
    fun testChatIdFormat() {
        val chatIdRegex = Regex("^-?\\d{6,16}$")
        assertTrue(chatIdRegex.matches("-1001234567890"))
        assertTrue(chatIdRegex.matches("123456789"))
        assertFalse(chatIdRegex.matches("not_a_number"))
        assertFalse(chatIdRegex.matches(""))
    }

    @Test
    fun testCategoryFilteringLogic() {
        val config = TelegramConfiguration(
            botToken = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz1234567",
            chatId = "-1001234567890",
            isEnabled = true,
            notifyOrders = true,
            notifyLowStock = false
        )

        fun shouldNotify(category: String, cfg: TelegramConfiguration): Boolean {
            if (!cfg.isEnabled) return false
            return when (category.uppercase()) {
                "SİPARİŞ" -> cfg.notifyOrders
                "KRİTİK_STOK" -> cfg.notifyLowStock
                else -> true
            }
        }

        assertTrue(shouldNotify("SİPARİŞ", config))
        assertFalse(shouldNotify("KRİTİK_STOK", config))
        assertTrue(shouldNotify("GÜNLÜK_RAPOR", config))
    }
}
