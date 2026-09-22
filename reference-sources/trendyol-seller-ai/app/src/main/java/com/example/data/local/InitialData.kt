package com.example.data.local

object InitialData {
    val sampleProducts = listOf(
        ProductEntity(
            id = 1,
            barcode = "TY-86801-OVR-01",
            title = "Oversize Unisex Pamuklu Sweatshirt Kapüşonlu Hoodie",
            brand = "Trendyol Curve",
            category = "Giyim / Moda",
            salePrice = 489.90,
            listPrice = 699.90,
            stockCount = 42,
            imageUrl = "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500&auto=format&fit=crop&q=60",
            buyboxWinner = true,
            competitorPrice = 510.0,
            isActive = true,
            description = "100% Pamuk 3 iplik şardonlu içi yumuşacık kumaş. Rahat kalıp, solma yapmayan organik boyama."
        ),
        ProductEntity(
            id = 2,
            barcode = "TY-99214-COF-02",
            title = "Közde Pişirme Özellikli Elektrikli Türk Kahvesi Makinesi",
            brand = "Arzum Okka",
            category = "Elektrikli Ev Aletleri",
            salePrice = 1349.00,
            listPrice = 1799.00,
            stockCount = 18,
            imageUrl = "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=500&auto=format&fit=crop&q=60",
            buyboxWinner = false,
            competitorPrice = 1319.00,
            isActive = true,
            description = "Taşmayı önleyici akıllı sensör sistemi, 4 fincan kapasiteli, ağır ateşte köz lezzetinde kahve pişirme."
        ),
        ProductEntity(
            id = 3,
            barcode = "TY-44120-DRS-03",
            title = "Kuşaklı Keten Görünümlü Tesettür Elbise Ferace Abaya",
            brand = "Moda Selvim",
            category = "Kadın Tesettür Giyim",
            salePrice = 679.50,
            listPrice = 899.00,
            stockCount = 7,
            imageUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=500&auto=format&fit=crop&q=60",
            buyboxWinner = true,
            competitorPrice = 720.0,
            isActive = true,
            description = "Nefes alabilen aero keten kumaş, boydan düğmeli, beli kuşaklı zarif ve şık tasarım."
        ),
        ProductEntity(
            id = 4,
            barcode = "TY-78119-SNK-04",
            title = "Erkek Ortopedik Taban Günlük Yürüyüş ve Koşu Spor Ayakkabısı",
            brand = "Lumberjack",
            category = "Ayakkabı & Çanta",
            salePrice = 899.90,
            listPrice = 1250.00,
            stockCount = 3, // Low stock warning
            imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=60",
            buyboxWinner = true,
            competitorPrice = 930.00,
            isActive = true,
            description = "Memory foam iç tabanlık, ultra hafif darbe emici dış taban, hava alan örgü saya."
        ),
        ProductEntity(
            id = 5,
            barcode = "TY-32001-SRM-05",
            title = "C Vitamini & Niasinamid Canlandırıcı Yüz Bakım Serumu 30ml",
            brand = "The Purest Solutions",
            category = "Kozmetik & Kişisel Bakım",
            salePrice = 299.00,
            listPrice = 450.00,
            stockCount = 85,
            imageUrl = "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500&auto=format&fit=crop&q=60",
            buyboxWinner = true,
            competitorPrice = 315.00,
            isActive = true,
            description = "Leke karşıtı formül, antioksidan koruma, cilt tonu eşitleyici ve aydınlatıcı etki."
        ),
        ProductEntity(
            id = 6,
            barcode = "TY-11928-ANC-06",
            title = "Kablosuz Aktif Gürültü Engelleyici Bluetooth Kulaklık ANC",
            brand = "Anker Soundcore",
            category = "Elektronik Aksesuar",
            salePrice = 1199.00,
            listPrice = 1599.00,
            stockCount = 0, // Out of stock
            imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=60",
            buyboxWinner = false,
            competitorPrice = 1180.00,
            isActive = false,
            description = "40 saat pil ömrü, hibrit aktif gürültü engelleme teknolojisi, Hi-Res ses sertifikası."
        )
    )

    val sampleOrders = listOf(
        OrderEntity(
            id = 1,
            orderNumber = "TY-928174201",
            customerName = "Fatma Zehra Kaya",
            customerCity = "İstanbul / Kadıköy",
            orderDate = "اليوم، 10:24 ص",
            totalAmount = 979.80,
            status = "Created",
            cargoProvider = "Trendyol Express",
            trackingNumber = "TEX-928174201-TR",
            itemCount = 2,
            itemsSummary = "2x Oversize Pamuklu Sweatshirt (M, Siyah)"
        ),
        OrderEntity(
            id = 2,
            orderNumber = "TY-928169112",
            customerName = "Mehmet Demir",
            customerCity = "Ankara / Çankaya",
            orderDate = "اليوم، 09:12 ص",
            totalAmount = 1349.00,
            status = "Picking",
            cargoProvider = "Trendyol Express",
            trackingNumber = "TEX-928169112-TR",
            itemCount = 1,
            itemsSummary = "1x Arzum Okka Közde Pişirme Türk Kahvesi Makinesi"
        ),
        OrderEntity(
            id = 3,
            orderNumber = "TY-928054331",
            customerName = "Sultan Al-Otaibi",
            customerCity = "Riyadh / Suudi Arabistan (Trendyol Gulf)",
            orderDate = "أمس، 06:45 م",
            totalAmount = 1359.00,
            status = "Shipped",
            cargoProvider = "Yurtiçi Kargo (Global)",
            trackingNumber = "YK-8829104812",
            itemCount = 2,
            itemsSummary = "2x Tesettür Elbise Ferace Abaya (Beden 42)"
        ),
        OrderEntity(
            id = 4,
            orderNumber = "TY-927918442",
            customerName = "Ahmet Yılmaz",
            customerCity = "İzmir / Bornova",
            orderDate = "أمس، 02:15 م",
            totalAmount = 899.90,
            status = "Delivered",
            cargoProvider = "Trendyol Express",
            trackingNumber = "TEX-927918442-TR",
            itemCount = 1,
            itemsSummary = "1x Ortopedik Taban Spor Ayakkabı (No: 43)"
        ),
        OrderEntity(
            id = 5,
            orderNumber = "TY-927842109",
            customerName = "Ayşe Nur Şahin",
            customerCity = "Bursa / Nilüfer",
            orderDate = "منذ يومين",
            totalAmount = 598.00,
            status = "Returned",
            cargoProvider = "Aras Kargo",
            trackingNumber = "AR-339182049",
            itemCount = 2,
            itemsSummary = "2x C Vitamini & Niasinamid Canlandırıcı Serum"
        )
    )

    val sampleQuestions = listOf(
        QuestionEntity(
            id = 1,
            productBarcode = "TY-86801-OVR-01",
            productTitle = "Oversize Unisex Pamuklu Sweatshirt Kapüşonlu",
            customerName = "Zeynep K.",
            questionText = "Boyum 168 cm, kilom 58 kg. Hangi beden almalıyım? Bol dursun istiyorum ama çok aşırı dökümlü olmasın.",
            questionDate = "منذ ساعتين",
            replyText = null,
            isAnswered = false,
            aiSuggestedReply = "Merhaba Değerli Müşterimiz, 168 cm boy ve 58 kg için M beden hafif dökümlü ve tam istediğiniz modern oversize duruşu sağlayacaktır. Keyifli alışverişler dileriz!"
        ),
        QuestionEntity(
            id = 2,
            productBarcode = "TY-44120-DRS-03",
            productTitle = "Kuşaklı Keten Görünümlü Tesettür Elbise Ferace Abaya",
            customerName = "أم عبد الله (مشتري دولي)",
            questionText = "هل قماش العباءة يشف أو يحتاج بطانة داخلية؟ وما هو طول الفستان لمقاس 40؟",
            questionDate = "منذ 4 ساعات",
            replyText = null,
            isAnswered = false,
            aiSuggestedReply = "أهلاً بك عزيزتي العميله، قماش العباءة من الكتان المعالج عالي الكثافة (Aero Keten) غير شفاف تماماً ولا يحتاج إلى بطانة إضافية. طول العباءة لمقاس 40 هو 142 سم ويوفر ستراً وأناقة تامة. نسعد بخدمتك!"
        ),
        QuestionEntity(
            id = 3,
            productBarcode = "TY-99214-COF-02",
            productTitle = "Közde Pişirme Özellikli Elektrikli Türk Kahvesi Makinesi",
            customerName = "Kemal B.",
            questionText = "Cezve kısmı paslanmaz çelik mi yoksa teflon mu? Bulaşık makinesinde yıkanabilir mi?",
            questionDate = "أمس",
            replyText = "Merhaba, ürünümüzün haznesi gıda ile temasa uygun paslanmaz çelik tabanlıdır. Elektrikli aksam güvenliği için elde durulanması tavsiye edilmektedir.",
            isAnswered = true,
            aiSuggestedReply = null
        )
    )
}
