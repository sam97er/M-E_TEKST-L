package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", true),
    ENGLISH("en", "English", false),
    TURKISH("tr", "Türkçe", false);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ARABIC
    }
}

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val rateToSar: Double // Base currency SAR
) {
    SAR("SAR", "ر.س", "ريال سعودي", "Saudi Riyal", 1.0),
    USD("USD", "$", "دولار أمريكي", "US Dollar", 3.75),
    TRY("TRY", "₺", "ليرة تركية", "Turkish Lira", 0.11),
    EUR("EUR", "€", "يورو أوروبي", "Euro", 4.10);

    fun format(amountSar: Double): String {
        val converted = amountSar / rateToSar
        return String.format(java.util.Locale.US, "%.2f %s", converted, symbol)
    }

    fun convertFromSar(amountSar: Double): Double {
        return amountSar / rateToSar
    }

    fun convertToSar(amountInCurrency: Double): Double {
        return amountInCurrency * rateToSar
    }
}
