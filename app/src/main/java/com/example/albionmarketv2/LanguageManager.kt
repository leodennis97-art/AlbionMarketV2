package com.example.albionmarketv2

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

val LocalAppLanguage = compositionLocalOf { "DE" }

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
        return translated ?: enMap[key] ?: translateUI(baseDe, cleanLang)
    }

    fun updateAppLocale(context: Context, langCode: String) {
        try {
            val locale = Locale(langCode.lowercase())
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (_: Exception) {}
    }

    fun translateUI(text: String, langCode: String): String {
        if (text.isBlank()) return text
        val cleanLang = langCode.trim().uppercase()
        if (cleanLang == "DE") return text

        return when (cleanLang) {
            "EN" -> translateToEn(text)
            "ES" -> translateToEs(text)
            "FR" -> translateToFr(text)
            "PT" -> translateToPt(text)
            "RU" -> translateToRu(text)
            "ZH" -> translateToZh(text)
            "JA" -> translateToJa(text)
            "KO" -> translateToKo(text)
            "TR" -> translateToTr(text)
            "ID" -> translateToId(text)
            "PL" -> translateToPl(text)
            else -> translateToEn(text)
        }
    }

    private fun translateToEn(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Server Login & Authentication", ignoreCase = true)
            .replace("Bitte Server-Zugangsdaten eingeben", "Please enter server credentials", ignoreCase = true)
            .replace("Benutzername", "Username", ignoreCase = true)
            .replace("Passwort", "Password", ignoreCase = true)
            .replace("Einloggen", "Log In", ignoreCase = true)
            .replace("Anmelden", "Sign In", ignoreCase = true)
            .replace("Registrieren", "Register", ignoreCase = true)
            .replace("Overlay Starten", "Start Overlay", ignoreCase = true)
            .replace("Overlay Beenden", "Stop Overlay", ignoreCase = true)
            .replace("App Minimieren", "Minimize App", ignoreCase = true)
            .replace("Abmelden / Sperren", "Log Out / Lock", ignoreCase = true)
            .replace("Abmelden / Logout", "Log Out", ignoreCase = true)
            .replace("Auf Update prüfen", "Check for Updates", ignoreCase = true)
            .replace("Jetzt aktualisieren", "Update Now", ignoreCase = true)
            .replace("Marge", "Margin", ignoreCase = true)
            .replace("Neueste", "Newest", ignoreCase = true)
            .replace("Wenigster Bestand", "Fewest Stock", ignoreCase = true)
            .replace("Zugangsdaten merken", "Remember Credentials", ignoreCase = true)
            .replace("Server auswählen:", "Select Server:", ignoreCase = true)
            .replace("Silber-Budget", "Silver Budget", ignoreCase = true)
            .replace("Traglast (kg)", "Carry Capacity (kg)", ignoreCase = true)
            .replace("Mindest-Marge (%)", "Min Margin (%)", ignoreCase = true)
            .replace("Handelschancen", "Trade Deals", ignoreCase = true)
            .replace("Auftrag annehmen", "Accept Order", ignoreCase = true)
            .replace("Kaufen in", "Buy in", ignoreCase = true)
            .replace("Verkaufen in", "Sell in", ignoreCase = true)
            .replace("Kaufpreis", "Buy Price", ignoreCase = true)
            .replace("Verkaufspreis", "Sell Price", ignoreCase = true)
            .replace("Gewinn/Stk", "Profit/Unit", ignoreCase = true)
            .replace("Investition", "Investment", ignoreCase = true)
            .replace("Gewinn", "Profit", ignoreCase = true)
            .replace("Netto-Umsatz", "Net Revenue", ignoreCase = true)
            .replace("Ausgegeben", "Spent", ignoreCase = true)
            .replace("Menge", "Quantity", ignoreCase = true)
            .replace("Buchen", "Book", ignoreCase = true)
            .replace("Abbrechen", "Cancel", ignoreCase = true)
            .replace("Speichern", "Save", ignoreCase = true)
            .replace("Schließen", "Close", ignoreCase = true)
            .replace("Einstellungen", "Settings", ignoreCase = true)
            .replace("Standpunkt:", "Location:", ignoreCase = true)
            .replace("Gefährliche Zonen meiden", "Avoid dangerous zones", ignoreCase = true)
    }

    private fun translateToEs(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Inicio de sesión y autenticación", ignoreCase = true)
            .replace("Benutzername", "Usuario", ignoreCase = true)
            .replace("Passwort", "Contraseña", ignoreCase = true)
            .replace("Einloggen", "Iniciar sesión", ignoreCase = true)
            .replace("Anmelden", "Ingresar", ignoreCase = true)
            .replace("Registrieren", "Registrarse", ignoreCase = true)
            .replace("Overlay Starten", "Iniciar Superposición", ignoreCase = true)
            .replace("Overlay Beenden", "Detener Superposición", ignoreCase = true)
            .replace("App Minimieren", "Minimizar App", ignoreCase = true)
            .replace("Abmelden / Sperren", "Cerrar sesión / Bloquear", ignoreCase = true)
            .replace("Auf Update prüfen", "Buscar actualizaciones", ignoreCase = true)
            .replace("Marge", "Margen", ignoreCase = true)
            .replace("Neueste", "Más reciente", ignoreCase = true)
            .replace("Silber-Budget", "Presupuesto de plata", ignoreCase = true)
            .replace("Kaufpreis", "Precio de compra", ignoreCase = true)
            .replace("Verkaufspreis", "Precio de venta", ignoreCase = true)
            .replace("Gewinn", "Ganancia", ignoreCase = true)
            .replace("Menge", "Cantidad", ignoreCase = true)
            .replace("Buchen", "Reservar", ignoreCase = true)
            .replace("Abbrechen", "Cancelar", ignoreCase = true)
            .replace("Speichern", "Guardar", ignoreCase = true)
            .replace("Schließen", "Cerrar", ignoreCase = true)
            .replace("Einstellungen", "Ajustes", ignoreCase = true)
    }

    private fun translateToFr(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Connexion et authentification", ignoreCase = true)
            .replace("Benutzername", "Nom d'utilisateur", ignoreCase = true)
            .replace("Passwort", "Mot de passe", ignoreCase = true)
            .replace("Einloggen", "Se connecter", ignoreCase = true)
            .replace("Anmelden", "Connexion", ignoreCase = true)
            .replace("Registrieren", "S'inscrire", ignoreCase = true)
            .replace("Overlay Starten", "Lancer Overlay", ignoreCase = true)
            .replace("Overlay Beenden", "Arrêter Overlay", ignoreCase = true)
            .replace("App Minimieren", "Réduire l'application", ignoreCase = true)
            .replace("Abmelden / Sperren", "Déconnexion / Verrouiller", ignoreCase = true)
            .replace("Silber-Budget", "Budget argent", ignoreCase = true)
            .replace("Kaufpreis", "Prix d'achat", ignoreCase = true)
            .replace("Verkaufspreis", "Prix de vente", ignoreCase = true)
            .replace("Gewinn", "Bénéfice", ignoreCase = true)
            .replace("Menge", "Quantité", ignoreCase = true)
            .replace("Speichern", "Enregistrer", ignoreCase = true)
            .replace("Schließen", "Fermer", ignoreCase = true)
            .replace("Einstellungen", "Paramètres", ignoreCase = true)
    }

    private fun translateToPt(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Login e Autenticação", ignoreCase = true)
            .replace("Benutzername", "Nome de usuário", ignoreCase = true)
            .replace("Passwort", "Senha", ignoreCase = true)
            .replace("Einloggen", "Entrar", ignoreCase = true)
            .replace("Anmelden", "Entrar", ignoreCase = true)
            .replace("Registrieren", "Registrar", ignoreCase = true)
            .replace("Overlay Starten", "Iniciar Overlay", ignoreCase = true)
            .replace("Overlay Beenden", "Parar Overlay", ignoreCase = true)
            .replace("App Minimieren", "Minimizar App", ignoreCase = true)
            .replace("Abmelden / Sperren", "Sair / Bloquear", ignoreCase = true)
            .replace("Silber-Budget", "Orçamento de prata", ignoreCase = true)
            .replace("Kaufpreis", "Preço de compra", ignoreCase = true)
            .replace("Verkaufspreis", "Preço de venda", ignoreCase = true)
            .replace("Gewinn", "Lucro", ignoreCase = true)
            .replace("Menge", "Quantidade", ignoreCase = true)
            .replace("Speichern", "Salvar", ignoreCase = true)
            .replace("Schließen", "Fechar", ignoreCase = true)
            .replace("Einstellungen", "Configurações", ignoreCase = true)
    }

    private fun translateToRu(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Вход и Аутентификация", ignoreCase = true)
            .replace("Benutzername", "Имя пользователя", ignoreCase = true)
            .replace("Passwort", "Пароль", ignoreCase = true)
            .replace("Einloggen", "Войти", ignoreCase = true)
            .replace("Anmelden", "Вход", ignoreCase = true)
            .replace("Registrieren", "Регистрация", ignoreCase = true)
            .replace("Overlay Starten", "Запустить оверлей", ignoreCase = true)
            .replace("Overlay Beenden", "Остановить оверлей", ignoreCase = true)
            .replace("App Minimieren", "Свернуть прилож.", ignoreCase = true)
            .replace("Abmelden / Sperren", "Выйти / Заблокировать", ignoreCase = true)
            .replace("Silber-Budget", "Бюджет серебра", ignoreCase = true)
            .replace("Kaufpreis", "Цена покупки", ignoreCase = true)
            .replace("Verkaufspreis", "Цена продажи", ignoreCase = true)
            .replace("Gewinn", "Прибыль", ignoreCase = true)
            .replace("Menge", "Количество", ignoreCase = true)
            .replace("Speichern", "Сохранить", ignoreCase = true)
            .replace("Schließen", "Закрыть", ignoreCase = true)
            .replace("Einstellungen", "Настройки", ignoreCase = true)
    }

    private fun translateToZh(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "服务器登录与身份验证", ignoreCase = true)
            .replace("Benutzername", "用户名", ignoreCase = true)
            .replace("Passwort", "密码", ignoreCase = true)
            .replace("Einloggen", "登录", ignoreCase = true)
            .replace("Anmelden", "登录", ignoreCase = true)
            .replace("Registrieren", "注册", ignoreCase = true)
            .replace("Overlay Starten", "启动悬浮窗", ignoreCase = true)
            .replace("Overlay Beenden", "关闭悬浮窗", ignoreCase = true)
            .replace("App Minimieren", "最小化应用", ignoreCase = true)
            .replace("Abmelden / Sperren", "退出 / 锁定", ignoreCase = true)
            .replace("Silber-Budget", "银币预算", ignoreCase = true)
            .replace("Kaufpreis", "买入价", ignoreCase = true)
            .replace("Verkaufspreis", "卖出价", ignoreCase = true)
            .replace("Gewinn", "利润", ignoreCase = true)
            .replace("Menge", "数量", ignoreCase = true)
            .replace("Speichern", "保存", ignoreCase = true)
            .replace("Schließen", "关闭", ignoreCase = true)
            .replace("Einstellungen", "设置", ignoreCase = true)
    }

    private fun translateToJa(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "サーバーログインと認証", ignoreCase = true)
            .replace("Benutzername", "ユーザー名", ignoreCase = true)
            .replace("Passwort", "パスワード", ignoreCase = true)
            .replace("Einloggen", "ログイン", ignoreCase = true)
            .replace("Anmelden", "サインイン", ignoreCase = true)
            .replace("Registrieren", "登録", ignoreCase = true)
            .replace("Silber-Budget", "シルバー予算", ignoreCase = true)
            .replace("Kaufpreis", "購入価格", ignoreCase = true)
            .replace("Verkaufspreis", "売却価格", ignoreCase = true)
            .replace("Gewinn", "利益", ignoreCase = true)
            .replace("Menge", "数量", ignoreCase = true)
            .replace("Speichern", "保存", ignoreCase = true)
            .replace("Schließen", "閉じる", ignoreCase = true)
            .replace("Einstellungen", "設定", ignoreCase = true)
    }

    private fun translateToKo(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "서버 로그인 및 인증", ignoreCase = true)
            .replace("Benutzername", "사용자 이름", ignoreCase = true)
            .replace("Passwort", "비밀번호", ignoreCase = true)
            .replace("Einloggen", "로그인", ignoreCase = true)
            .replace("Anmelden", "로그인", ignoreCase = true)
            .replace("Registrieren", "회원가입", ignoreCase = true)
            .replace("Silber-Budget", "실버 예산", ignoreCase = true)
            .replace("Kaufpreis", "구매가", ignoreCase = true)
            .replace("Verkaufspreis", "판매가", ignoreCase = true)
            .replace("Gewinn", "이익", ignoreCase = true)
            .replace("Menge", "수량", ignoreCase = true)
            .replace("Speichern", "저장", ignoreCase = true)
            .replace("Schließen", "닫기", ignoreCase = true)
            .replace("Einstellungen", "설정", ignoreCase = true)
    }

    private fun translateToTr(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Giriş ve Doğrulama", ignoreCase = true)
            .replace("Benutzername", "Kullanıcı Adı", ignoreCase = true)
            .replace("Passwort", "Şifre", ignoreCase = true)
            .replace("Einloggen", "Giriş Yap", ignoreCase = true)
            .replace("Anmelden", "Giriş", ignoreCase = true)
            .replace("Registrieren", "Kayıt Ol", ignoreCase = true)
            .replace("Silber-Budget", "Gümüş Bütçesi", ignoreCase = true)
            .replace("Kaufpreis", "Alış Fiyatı", ignoreCase = true)
            .replace("Verkaufspreis", "Satış Fiyatı", ignoreCase = true)
            .replace("Gewinn", "Kar", ignoreCase = true)
            .replace("Menge", "Miktar", ignoreCase = true)
            .replace("Speichern", "Kaydet", ignoreCase = true)
            .replace("Schließen", "Kapat", ignoreCase = true)
            .replace("Einstellungen", "Ayarlar", ignoreCase = true)
    }

    private fun translateToId(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Login & Autentikasi", ignoreCase = true)
            .replace("Benutzername", "Nama Pengguna", ignoreCase = true)
            .replace("Passwort", "Kata Sandi", ignoreCase = true)
            .replace("Einloggen", "Masuk", ignoreCase = true)
            .replace("Anmelden", "Masuk", ignoreCase = true)
            .replace("Registrieren", "Daftar", ignoreCase = true)
            .replace("Silber-Budget", "Anggaran Perak", ignoreCase = true)
            .replace("Kaufpreis", "Harga Beli", ignoreCase = true)
            .replace("Verkaufspreis", "Harga Jual", ignoreCase = true)
            .replace("Gewinn", "Keuntungan", ignoreCase = true)
            .replace("Menge", "Jumlah", ignoreCase = true)
            .replace("Speichern", "Simpan", ignoreCase = true)
            .replace("Schließen", "Tutup", ignoreCase = true)
            .replace("Einstellungen", "Pengaturan", ignoreCase = true)
    }

    private fun translateToPl(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Logowanie i Uwierzytelnianie", ignoreCase = true)
            .replace("Benutzername", "Nazwa użytkownika", ignoreCase = true)
            .replace("Passwort", "Hasło", ignoreCase = true)
            .replace("Einloggen", "Zaloguj się", ignoreCase = true)
            .replace("Anmelden", "Zaloguj", ignoreCase = true)
            .replace("Registrieren", "Zarejestruj się", ignoreCase = true)
            .replace("Silber-Budget", "Budżet srebra", ignoreCase = true)
            .replace("Kaufpreis", "Cena zakupu", ignoreCase = true)
            .replace("Verkaufspreis", "Cena sprzedaży", ignoreCase = true)
            .replace("Gewinn", "Zysk", ignoreCase = true)
            .replace("Menge", "Ilość", ignoreCase = true)
            .replace("Speichern", "Zapisz", ignoreCase = true)
            .replace("Schließen", "Zamknij", ignoreCase = true)
            .replace("Einstellungen", "Ustawienia", ignoreCase = true)
    }

    fun getCityTranslation(city: String, langCode: String): String {
        val key = "city_${city.lowercase().replace(" ", "_").replace("'", "")}"
        val trans = getString(key, langCode)
        return if (trans == key) city else trans
    }
}
