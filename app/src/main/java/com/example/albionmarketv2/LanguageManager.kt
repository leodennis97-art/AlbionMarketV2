package com.example.albionmarketv2

object LanguageManager {

    enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
        DE("DE", "Deutsch", "🇩🇪"),
        EN("EN", "English", "🇬🇧"),
        ES("ES", "Español", "🇪🇸"),
        FR("FR", "Français", "🇫🇷"),
        PT("PT", "Português", "🇵🇹"),
        RU("RU", "Русский", "🇷🇺"),
        ZH("ZH", "中文", "🇨🇳"),
        JA("JA", "日本語", "🇯🇵"),
        KO("KO", "한국어", "🇰🇷"),
        TR("TR", "Türkçe", "🇹🇷"),
        ID("ID", "Bahasa Indonesia", "🇮🇩"),
        PL("PL", "Polski", "🇵🇱")
    }

    private val deMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Echtzeit-Analyse & Trade Alerts",
        "tab_orders" to "📋 Aufträge & Historie",
        "tab_calculator" to "🧮 Rechner & Marge (Top 50)",
        "tab_catalog" to "📖 Markt-Katalog",
        "tab_builds" to "⚔️ KI Ausrüstung & Build-Sets",
        "tab_order_stats" to "📊 Order-Statistik (1T-1J)",
        "tab_events" to "🔥 Events & Boss Loot",
        "tab_gold" to "🪙 Goldmarkt",
        "tab_map" to "🗺️ Albion Weltkarte",
        "server_select" to "Server auswählen:",
        "silver_budget" to "Silber-Budget",
        "carry_capacity" to "Traglast (kg)",
        "min_margin" to "Mindest-Marge (%)",
        "premium_tax" to "Premium 4% Steuer",
        "standard_tax" to "Standard 8% Steuer",
        "avoid_danger" to "Gefährliche Zonen (Caerleon / Rot) meiden",
        "avoid_danger_desc" to "Schließt Caerleon & Full-Loot PvP Zonen aus",
        "top_opportunities" to "Lukrativste Handelschancen",
        "buy_in" to "Einkauf in",
        "sell_in" to "Verkauf in",
        "quantity" to "Menge",
        "weight" to "Gewicht",
        "investment" to "Investition",
        "profit_per_unit" to "Gewinn/Stk",
        "accept_order" to "Auftrag annehmen",
        "gold_buy" to "🪙 Gold Einkauf",
        "gold_sell" to "🪙 Gold Verkauf",
        "settings_title" to "⚙️ Einstellungen & Alerts",
        "bubble_toggle" to "Floating Bubble Overlay",
        "bubble_desc" to "Zeigt eine verschiebbare Bubble permanent über Albion Online an",
        "sys_notif_toggle" to "System Push-Benachrichtigungen",
        "sys_notif_desc" to "Zeigt Android-Alerts bei neuen Top-Deals in Echtzeit an",
        "gold_notif_toggle" to "Gold-Portfolio Benachrichtigungen",
        "gold_notif_desc" to "Zeigt automatische Live-Einnahmen Alerts bei Goldkurs-Updates an",
        "background_btn" to "🔋 App im Hintergrund unbegrenzt ausführen",
        "lang_select" to "Sprache / Language:",
        "save" to "Speichern",
        "close" to "Schließen",
        "roi" to "Marge",
        "no_price_data" to "Bisher kein Preis",
        
        "bubble_options" to "⚙️ Bubble Optionen",
        "bubble_lang" to "Sprache:",
        "bubble_category" to "Kategorie Filter:",
        "bubble_active_order" to "📦 Aktiver Auftrag",
        "bubble_top_margin" to "🔥 Handelschancen",
        "bubble_max_distance" to "Maximale Distanz (Zonen)",
        "bubble_min_stock" to "Max. Bestand",
        "bubble_max_stock" to "Max. Bestand",
        "bubble_book" to "Buchen",
        "bubble_cancel" to "Stornieren",
        "bubble_abort" to "Abbrechen",
        "bubble_save_book" to "Speichern & Buchen",
        "bubble_location" to "🏙️ Standpunkt:",
        "bubble_all_cities" to "Alle Städte",
        "bubble_all" to "Alle",
        "bubble_click_accept" to "Klicke zum Annehmen",
        "bubble_calculating" to "Berechne Top Marktchancen...",
        "bubble_no_opps" to "Keine passenden Marktchancen gefunden.",
        "bubble_search" to "Marktsuche...",
        "bubble_hold_move" to "Gedrückt halten zum Verschieben",
        "bubble_exit" to "Beenden",
        "bubble_kauf" to "Kauf:",
        "bubble_verkauf" to "Verkauf:",
        "bubble_buy_price" to "Kauf-Stückpreis (Silber)",
        "bubble_sell_price" to "Verkauf-Stückpreis (Silber)",
        "bubble_bought_amount" to "Gekaufte Stückzahl",
        "bubble_spent" to "Ausgegeben",
        "bubble_earned" to "Netto-Umsatz",
        "bubble_profit_loss" to "Gewinn/Verlust",
        "bubble_include_brecilien" to "Brecilien in Routen einschließen",
        "bubble_compact_mode" to "📱 Kompakter Bubble-Modus",
        "bubble_compact_desc" to "Verkleinert Fenster, Abstände und Schriftgrößen der Bubble",
        "bubble_opacity" to "👁️ Bubble-Transparenz / Deckkraft",
        "bubble_scale" to "🔍 Bubble-Skalierung (Größe)",
        "tab_crafting" to "🛠️ Crafting Guide",
        "tab_island" to "🏝️ Insel Guide",
        "tab_monsters" to "👹 Monster & Bosse",
        "crafting_search" to "Item zum Craften suchen...",
        "monster_search" to "Monster, Boss, Region oder Drop suchen...",
        "crafting_req_ingredients" to "Benötigte Ressourcen & günstigster Markt:",
        "cheapest_at" to "Günstigster Einkauf",

        "city_caerleon" to "Caerleon",
        "city_brecilien" to "Brecilien",
        "city_arthurs_rest" to "Arthurs Rast",
        "city_merlyns_rest" to "Merlyns Rast",
        "city_morganas_rest" to "Morganas Rast",
        "city_bridgewatch" to "Bridgewatch",
        "city_martlock" to "Martlock",
        "city_lymhurst" to "Lymhurst",
        "city_fort_sterling" to "Fort Sterling",
        "city_thetford" to "Thetford",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 ROT-ZONE",
        "black_zone" to "⚫ BLACK-ZONE"
    )

    private val enMap = mapOf(
        "app_title" to "Albion Online Market Resources",
        "app_subtitle" to "Real-time Analytics & Trade Alerts",
        "tab_orders" to "📋 Orders & History",
        "tab_calculator" to "🧮 Calculator & Margin (Top 50)",
        "tab_catalog" to "📖 Market Catalog",
        "tab_builds" to "⚔️ AI Gear & Build Sets",
        "tab_order_stats" to "📊 Order Statistics (1D-1Y)",
        "tab_events" to "🔥 Events & Boss Loot",
        "tab_gold" to "🪙 Gold Market",
        "tab_map" to "🗺️ Albion World Map",
        "server_select" to "Select Server:",
        "silver_budget" to "Silver Budget",
        "carry_capacity" to "Carry Capacity (kg)",
        "min_margin" to "Min Margin (%)",
        "premium_tax" to "Premium 4% Tax",
        "standard_tax" to "Standard 8% Tax",
        "avoid_danger" to "Avoid Dangerous Zones (Caerleon / Red)",
        "avoid_danger_desc" to "Excludes Caerleon & Full-Loot PvP Zones",
        "top_opportunities" to "Most Profitable Trade Deals",
        "buy_in" to "Buy in",
        "sell_in" to "Sell in",
        "quantity" to "Quantity",
        "weight" to "Weight",
        "investment" to "Investment",
        "profit_per_unit" to "Profit/Unit",
        "accept_order" to "Accept & Save Order",
        "gold_buy" to "🪙 Gold Buy",
        "gold_sell" to "🪙 Gold Sell",
        "settings_title" to "⚙️ Settings & Alerts",
        "bubble_toggle" to "Floating Bubble Overlay",
        "bubble_desc" to "Displays a draggable bubble permanently over Albion Online",
        "sys_notif_toggle" to "System Push Notifications",
        "sys_notif_desc" to "Displays Android alerts for top deals in real time",
        "gold_notif_toggle" to "Gold Portfolio Notifications",
        "gold_notif_desc" to "Sends automatic live income alerts on gold price updates",
        "background_btn" to "🔋 Run App Unrestricted in Background",
        "lang_select" to "Language / Sprache:",
        "save" to "Save",
        "close" to "Close",
        "roi" to "ROI",
        "no_price_data" to "No price yet",

        "bubble_options" to "⚙️ Bubble Settings",
        "bubble_lang" to "Language:",
        "bubble_category" to "Category Filter:",
        "bubble_active_order" to "📦 Active Order",
        "bubble_top_margin" to "🔥 Top 3 Margin",
        "bubble_book" to "Book",
        "bubble_cancel" to "Cancel",
        "bubble_abort" to "Abort",
        "bubble_save_book" to "Save & Book",
        "bubble_location" to "🏙️ Location:",
        "bubble_all_cities" to "All Cities",
        "bubble_all" to "All",
        "bubble_click_accept" to "Click to accept",
        "bubble_calculating" to "Calculating top opportunities...",
        "bubble_no_opps" to "No matching market opportunities found.",
        "bubble_search" to "Market search...",
        "bubble_hold_move" to "Hold to move",
        "bubble_exit" to "Exit",
        "bubble_kauf" to "Buy:",
        "bubble_verkauf" to "Sell:",
        "bubble_buy_price" to "Buy price (Silver)",
        "bubble_sell_price" to "Sell price (Silver)",
        "bubble_bought_amount" to "Bought quantity",
        "bubble_spent" to "Spent",
        "bubble_earned" to "Net Revenue",
        "bubble_profit_loss" to "Profit/Loss",
        "bubble_include_brecilien" to "Include Brecilien in routes",
        "bubble_compact_mode" to "📱 Compact Bubble Mode",
        "bubble_compact_desc" to "Reduces window size, padding, and font size of the bubble",
        "bubble_opacity" to "👁️ Bubble Opacity",
        "bubble_scale" to "🔍 Bubble Scale (Size)",
        "tab_crafting" to "🛠️ Crafting Guide",
        "tab_island" to "🏝️ Island Guide",
        "tab_monsters" to "👹 Monsters & Bosses",
        "crafting_search" to "Search item to craft...",
        "monster_search" to "Search monster, boss, region or drop...",
        "crafting_req_ingredients" to "Required resources & cheapest market:",
        "cheapest_at" to "Cheapest buy",

        "city_caerleon" to "Caerleon",
        "city_brecilien" to "Brecilien",
        "city_arthurs_rest" to "Arthur's Rest",
        "city_merlyns_rest" to "Merlyn's Rest",
        "city_morganas_rest" to "Morgana's Rest",
        "city_bridgewatch" to "Bridgewatch",
        "city_martlock" to "Martlock",
        "city_lymhurst" to "Lymhurst",
        "city_fort_sterling" to "Fort Sterling",
        "city_thetford" to "Thetford",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 RED ZONE",
        "black_zone" to "⚫ BLACK ZONE"
    )

    private val esMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Análisis en tiempo real",
        "tab_orders" to "📋 Pedidos e Historial",
        "tab_calculator" to "🧮 Calculadora y Margen",
        "tab_catalog" to "📖 Catálogo del Mercado",
        "tab_builds" to "⚔️ Equipo de IA",
        "tab_order_stats" to "📊 Estadísticas",
        "tab_events" to "🔥 Eventos y Jefes",
        "tab_gold" to "🪙 Mercado de Oro",
        "server_select" to "Seleccionar Servidor:",
        "silver_budget" to "Presupuesto de Plata",
        "carry_capacity" to "Capacidad (kg)",
        "min_margin" to "Margen Mínimo (%)",
        "settings_title" to "⚙️ Ajustes",
        "save" to "Guardar",
        "close" to "Cerrar",
        "lang_select" to "Idioma / Language:"
    )

    private val frMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Analyse en temps réel",
        "tab_orders" to "📋 Commandes et Historique",
        "tab_calculator" to "🧮 Calculateur et Marge",
        "tab_catalog" to "📖 Catalogue du Marché",
        "settings_title" to "⚙️ Paramètres",
        "save" to "Enregistrer",
        "close" to "Fermer",
        "lang_select" to "Langue / Language:"
    )

    private val ptMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Análise em tempo real",
        "tab_orders" to "📋 Pedidos e Histórico",
        "tab_calculator" to "🧮 Calculadora e Margem",
        "tab_catalog" to "📖 Catálogo do Mercado",
        "settings_title" to "⚙️ Configurações",
        "save" to "Salvar",
        "close" to "Fechar",
        "lang_select" to "Idioma / Language:"
    )

    private val ruMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Анализ рынка в реальном времени",
        "tab_orders" to "📋 Заказы и История",
        "tab_calculator" to "🧮 Калькулятор и Маржа",
        "tab_catalog" to "📖 Каталог рынка",
        "settings_title" to "⚙️ Настройки",
        "save" to "Сохранить",
        "close" to "Закрыть",
        "lang_select" to "Язык / Language:"
    )

    private val zhMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "实时市场分析",
        "tab_orders" to "📋 订单与历史",
        "tab_calculator" to "🧮 计算器与利润",
        "tab_catalog" to "📖 市场目录",
        "settings_title" to "⚙️ 设置",
        "save" to "保存",
        "close" to "关闭",
        "lang_select" to "语言 / Language:"
    )

    private val jaMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "リアルタイム市場分析",
        "tab_orders" to "📋 注文と履歴",
        "tab_calculator" to "🧮 計算機とマージン",
        "tab_catalog" to "📖 市場カタログ",
        "settings_title" to "⚙️ 設定",
        "save" to "保存",
        "close" to "閉じる",
        "lang_select" to "言語 / Language:"
    )

    private val koMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "실시간 시장 분석",
        "tab_orders" to "📋 주문 및 내역",
        "tab_calculator" to "🧮 계산기 및 마진",
        "tab_catalog" to "📖 시장 카탈로그",
        "settings_title" to "⚙️ 설정",
        "save" to "저장",
        "close" to "닫기",
        "lang_select" to "언어 / Language:"
    )

    private val trMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Anlık Pazar Analizi",
        "tab_orders" to "📋 Siparişler ve Geçmiş",
        "tab_calculator" to "🧮 Hesaplayıcı ve Kar",
        "tab_catalog" to "📖 Pazar Kataloğu",
        "settings_title" to "⚙️ Ayarlar",
        "save" to "Kaydet",
        "close" to "Kapat",
        "lang_select" to "Dil / Language:"
    )

    private val idMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Analisis Pasar Real-time",
        "tab_orders" to "📋 Pesanan & Riwayat",
        "tab_calculator" to "🧮 Kalkulator & Margin",
        "tab_catalog" to "📖 Katalog Pasar",
        "settings_title" to "⚙️ Pengaturan",
        "save" to "Simpan",
        "close" to "Tutup",
        "lang_select" to "Bahasa / Language:"
    )

    private val plMap = mapOf(
        "app_title" to "AlbionDataPro",
        "app_subtitle" to "Analiza rynku w czasie rzeczywistym",
        "tab_orders" to "📋 Zamówienia i Historia",
        "tab_calculator" to "🧮 Kalkulator i Marża",
        "tab_catalog" to "📖 Katalog Rynku",
        "settings_title" to "⚙️ Ustawienia",
        "save" to "Zapisz",
        "close" to "Zamknij",
        "lang_select" to "Język / Language:"
    )

    fun getString(key: String, langCode: String): String {
        val cleanLang = langCode.trim().uppercase()
        val baseDe = deMap[key] ?: key
        if (cleanLang == "DE") return baseDe

        val translated = when (cleanLang) {
            "EN" -> enMap[key]
            "ZH" -> zhMap[key]
            "ES" -> esMap[key]
            "FR" -> frMap[key]
            "PT" -> ptMap[key]
            "RU" -> ruMap[key]
            "JA" -> jaMap[key]
            "KO" -> koMap[key]
            "TR" -> trMap[key]
            "ID" -> idMap[key]
            "PL" -> plMap[key]
            else -> null
        }
        return translated ?: enMap[key] ?: baseDe
    }

    fun getCityTranslation(city: String, langCode: String): String {
        val key = "city_${city.lowercase().replace(" ", "_").replace("'", "")}"
        val trans = getString(key, langCode)
        return if (trans == key) city else trans
    }
}
