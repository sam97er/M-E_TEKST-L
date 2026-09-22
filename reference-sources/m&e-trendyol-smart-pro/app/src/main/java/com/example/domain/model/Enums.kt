package com.example.domain.model

enum class OrderPrepStatus(val title: String) {
    WAITING("Bekliyor"),
    BEING_PREPARED("Hazırlanıyor"),
    READY("Hazır"),
    COMPLETED("Tamamlandı"),
    PROBLEM("Sorunlu")
}

enum class TrendyolOrderStatus(val title: String) {
    NEW("Yeni"),
    PREPARING("Hazırlanıyor"),
    SHIPPED("Kargoda"),
    DELIVERED("Teslim Edildi"),
    CANCELLED("İptal"),
    RETURNED("İade")
}

enum class AiTaskType(val title: String) {
    CUSTOMER_REPLY("Müşteri Yanıtı"),
    PROFIT_ANALYSIS("Kâr & Maliyet Analizi"),
    PRODUCT_IMPROVEMENT("Ürün Başlık & Açıklama"),
    QUALITY_CHECK("Ürün Kalite Denetimi"),
    PRICING_ADVICE("Fiyatlandırma Tavsiyesi"),
    RETURN_ANALYSIS("İade Nedenleri Analizi"),
    DAILY_BRIEF("Günlük İş Özeti")
}

enum class AiApprovalStatus(val title: String) {
    BEKLIYOR("Bekliyor"),
    INCELENIYOR("İnceleniyor"),
    ONAYLANDI("Onaylandı"),
    DUZENLENDI("Düzenlendi"),
    REDDEDILDI("Reddedildi"),
    UYGULANDI("Uygulandı"),
    HATA("Hata")
}

enum class ResponseStyle(val title: String, val description: String) {
    FORMAL("Resmi", "Kurumsal, saygılı ve profesyonel dil"),
    FRIENDLY("Samimi", "Sıcak, yardımsever ve nezaketli üslup"),
    SHORT("Kısa", "Net, dolaysız ve hızlı bilgilendirme"),
    DETAILED("Detaylı", "Kapsamlı açıklama ve kullanım rehberliği"),
    APOLOGY_SOLUTION("Özür ve Çözüm", "Hızlı telafi, özür ve net çözüm adımları"),
    INFORMATIONAL("Bilgilendirme", "Kumaş, kalıp, yıkama talimatı teknik detayları")
}

enum class AiSlotRole(val slotNumber: Int, val roleName: String, val description: String) {
    CUSTOMER_COMMUNICATION(1, "Müşteri İletişimi AI", "Müşteri soru yanıtlama ve destek"),
    BUSINESS_AND_PROFIT(2, "İş ve Kâr AI", "Satış, kâr, komisyon, kargo ve finansal analiz"),
    PRODUCT_AND_GROWTH(3, "Ürün ve Büyüme AI", "Başlık, SEO açıklama, kalite denetimi ve fiyat önerisi")
}

enum class StockRiskLevel(val title: String) {
    NORMAL("Normal"),
    LOW_STOCK("Kritik Düşük"),
    OUT_OF_STOCK("Tükendi"),
    SLOW_MOVING("Yavaş Hareket"),
    FAST_SELLING("Hızlı Satan")
}

enum class AutomationTrigger(val title: String) {
    NEW_ORDER("Yeni Sipariş Geldiğinde"),
    NEW_QUESTION("Yeni Müşteri Sorusu Geldiğinde"),
    LOW_STOCK("Stok Belirli Seviyenin Altına Düştüğünde"),
    SYNC_FAILURE("Senkronizasyon Hatası Oluştuğunda"),
    DAILY_SCHEDULE("Her Gün Belirlenen Saatte"),
    HIGH_RETURN_RATE("İade Oranı Yükseldiğinde")
}
