package com.example.albionmarketv2

import android.content.Context
import android.os.Build
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
        "app_title" to "DataPro - Market Companion (Unofficial)",
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
        
        "city_bridgewatch" to "Bridgewatch",
        "city_caerleon" to "Caerleon",
        "city_black_market" to "Schwarzmarkt",
        "city_brecilien" to "Brecilien",
        "city_lymhurst" to "Lymhurst",
        "city_martlock" to "Martlock",
        "city_fort_sterling" to "Fort Sterling",
        "city_thetford" to "Thetford",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 CZERWONA STREFA",
        "black_zone" to "⚫ CZARNA STREFA"
    )

    fun getString(key: String, langCode: String): String {
        val cleanLang = langCode.trim().uppercase()
        val baseDe = deMap[key] ?: key
        if (cleanLang == "DE") return baseDe

        return translateUI(baseDe, cleanLang)
    }

    fun getCityTranslation(cityDe: String, langCode: String): String {
        val cleanLang = langCode.trim().uppercase()
        if (cleanLang == "DE") return cityDe
        return when (cityDe.trim().lowercase()) {
            "schwarzmarkt", "black market" -> when (cleanLang) {
                "ES" -> "Mercado Negro"
                "FR" -> "Marché Noir"
                "PT" -> "Mercado Negro"
                "RU" -> "Черный рынок"
                "ZH" -> "黑市"
                "JA" -> "ブラックマーケット"
                "KO" -> "암시장"
                "TR" -> "Karaborsa"
                "ID" -> "Pasar Gelap"
                "PL" -> "Czarny Rynek"
                else -> "Black Market"
            }
            else -> cityDe
        }
    }

    @Suppress("DEPRECATION")
    fun updateAppLocale(context: Context, langCode: String) {
        try {
            val locale = Locale(langCode.lowercase())
            Locale.setDefault(locale)
            val config = context.resources.configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale)
            } else {
                config.locale = locale
            }
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
            .replace("Konto erstellen & Einloggen", "Create Account & Sign In", ignoreCase = true)
            .replace("In Konto Anmelden", "Sign In to Account", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Unlock Account / Buy License", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Pay with PayPal / Unlock Account", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "PayPal Instant Payment & Unlock", ignoreCase = true)
            .replace("Konto / Lizenz-Schlüssel:", "Account / License Key:", ignoreCase = true)
            .replace("Verbleibende Lizenzdauer:", "Remaining License Duration:", ignoreCase = true)
            .replace("Neueste Enterprise-Updates", "Latest Enterprise Updates", ignoreCase = true)
            .replace("Offizielle Website", "Official Website", ignoreCase = true)
            .replace("Offizielle Webseite", "Official Website", ignoreCase = true)
            .replace("Echtzeit Markt-Analyses & In-Game Overlay Bot", "Real-Time Market Analytics & In-Game Overlay Bot", ignoreCase = true)
            .replace("Willkommen bei DataPro", "Welcome to DataPro", ignoreCase = true)
            .replace("Overlay Starten", "Start Overlay", ignoreCase = true)
            .replace("Overlay Beenden", "Stop Overlay", ignoreCase = true)
            .replace("App Minimieren", "Minimize App", ignoreCase = true)
            .replace("Abmelden / Sperren", "Log Out / Lock", ignoreCase = true)
            .replace("Abmelden / Logout", "Log Out", ignoreCase = true)
            .replace("Auf Update prüfen", "Check for Updates", ignoreCase = true)
            .replace("Jetzt aktualisieren", "Update Now", ignoreCase = true)
            .replace("Aufträge & Historie", "Orders & History", ignoreCase = true)
            .replace("Rechner & Marge", "Calculator & Margin", ignoreCase = true)
            .replace("Markt-Katalog", "Market Catalog", ignoreCase = true)
            .replace("KI Ausrüstung & Build-Sets", "AI Equipment & Build Sets", ignoreCase = true)
            .replace("Events & Boss Loot", "Events & Boss Loot", ignoreCase = true)
            .replace("Goldmarkt", "Gold Market", ignoreCase = true)
            .replace("Handwerks-Guide", "Crafting Guide", ignoreCase = true)
            .replace("Insel-Guide", "Island Guide", ignoreCase = true)
            .replace("Schmuggler-Guide", "Smuggler Guide", ignoreCase = true)
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
            .replace("Bitte Server-Zugangsdaten eingeben", "Ingrese credenciales", ignoreCase = true)
            .replace("Benutzername", "Usuario", ignoreCase = true)
            .replace("Passwort", "Contraseña", ignoreCase = true)
            .replace("Einloggen", "Iniciar sesión", ignoreCase = true)
            .replace("Anmelden", "Ingresar", ignoreCase = true)
            .replace("Registrieren", "Registrarse", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Crear cuenta e ingresar", ignoreCase = true)
            .replace("In Konto Anmelden", "Iniciar sesión en cuenta", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Desbloquear cuenta / Comprar licencias", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Pagar con PayPal / Desbloquear cuenta", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "Pago instantáneo con PayPal y desbloqueo", ignoreCase = true)
            .replace("Konto / Lizenz-Schlüssel:", "Cuenta / Clave de licencia:", ignoreCase = true)
            .replace("Verbleibende Lizenzdauer:", "Duración restante de la licencia:", ignoreCase = true)
            .replace("Offizielle Website", "Sitio web oficial", ignoreCase = true)
            .replace("Offizielle Webseite", "Sitio web oficial", ignoreCase = true)
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
            .replace("Standpunkt:", "Ubicación:", ignoreCase = true)
            .replace("Auftrag annehmen", "Aceptar Pedido", ignoreCase = true)
            .replace("Gefährliche Zonen meiden", "Evitar zonas peligrosas", ignoreCase = true)
    }

    private fun translateToFr(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Connexion et authentification", ignoreCase = true)
            .replace("Benutzername", "Nom d'utilisateur", ignoreCase = true)
            .replace("Passwort", "Mot de passe", ignoreCase = true)
            .replace("Einloggen", "Se connecter", ignoreCase = true)
            .replace("Anmelden", "Connexion", ignoreCase = true)
            .replace("Registrieren", "S'inscrire", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Créer un compte et se connecter", ignoreCase = true)
            .replace("In Konto Anmelden", "Se connecter au compte", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Débloquer le compte / Acheter des licences", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Payer avec PayPal / Débloquer le compte", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "Paiement instantané PayPal et déblocage", ignoreCase = true)
            .replace("Konto / Lizenz-Schlüssel:", "Compte / Clé de licence :", ignoreCase = true)
            .replace("Verbleibende Lizenzdauer:", "Durée restante de la licence :", ignoreCase = true)
            .replace("Offizielle Website", "Site officiel", ignoreCase = true)
            .replace("Offizielle Webseite", "Site officiel", ignoreCase = true)
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
            .replace("Standpunkt:", "Emplacement :", ignoreCase = true)
            .replace("Auftrag annehmen", "Accepter la commande", ignoreCase = true)
    }

    private fun translateToPt(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Login e Autenticação", ignoreCase = true)
            .replace("Benutzername", "Nome de usuário", ignoreCase = true)
            .replace("Passwort", "Senha", ignoreCase = true)
            .replace("Einloggen", "Entrar", ignoreCase = true)
            .replace("Anmelden", "Entrar", ignoreCase = true)
            .replace("Registrieren", "Registrar", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Criar conta e entrar", ignoreCase = true)
            .replace("In Konto Anmelden", "Entrar na conta", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Desbloquear conta / Comprar licenças", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Pagar com PayPal / Desbloquear conta", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "Pagamento instantâneo PayPal e desbloqueio", ignoreCase = true)
            .replace("Konto / Lizenz-Schlüssel:", "Conta / Chave de licença:", ignoreCase = true)
            .replace("Verbleibende Lizenzdauer:", "Duração restante da licença:", ignoreCase = true)
            .replace("Offizielle Website", "Website oficial", ignoreCase = true)
            .replace("Offizielle Webseite", "Website oficial", ignoreCase = true)
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
            .replace("Standpunkt:", "Localização:", ignoreCase = true)
            .replace("Auftrag annehmen", "Aceitar Pedido", ignoreCase = true)
    }

    private fun translateToRu(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Вход и Аутентификация", ignoreCase = true)
            .replace("Benutzername", "Имя пользователя", ignoreCase = true)
            .replace("Passwort", "Пароль", ignoreCase = true)
            .replace("Einloggen", "Войти", ignoreCase = true)
            .replace("Anmelden", "Вход", ignoreCase = true)
            .replace("Registrieren", "Регистрация", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Создать аккаунт и войти", ignoreCase = true)
            .replace("In Konto Anmelden", "Войти в аккаунт", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Разблокировать аккаунт / Купить лицензии", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Оплатить PayPal / Разблокировать аккаунт", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "Мгновенная оплата PayPal и разблокировка", ignoreCase = true)
            .replace("Konto / Lizenz-Schlüssel:", "Аккаунт / Ключ лицензии:", ignoreCase = true)
            .replace("Verbleibende Lizenzdauer:", "Оставшийся срок лицензии:", ignoreCase = true)
            .replace("Offizielle Website", "Официальный сайт", ignoreCase = true)
            .replace("Offizielle Webseite", "Официальный сайт", ignoreCase = true)
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
            .replace("Standpunkt:", "Локация:", ignoreCase = true)
            .replace("Auftrag annehmen", "Принять заказ", ignoreCase = true)
    }

    private fun translateToZh(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "服务器登录与身份验证", ignoreCase = true)
            .replace("Benutzername", "用户名", ignoreCase = true)
            .replace("Passwort", "密码", ignoreCase = true)
            .replace("Einloggen", "登录", ignoreCase = true)
            .replace("Anmelden", "登录", ignoreCase = true)
            .replace("Registrieren", "注册", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "创建账户并登录", ignoreCase = true)
            .replace("In Konto Anmelden", "登录账户", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "解锁账户 / 购买许可证", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "PayPal 支付 / 解锁账户", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "PayPal 即时支付与解锁", ignoreCase = true)
            .replace("Konto / Lizenz-Schlüssel:", "账户 / 许可证密钥：", ignoreCase = true)
            .replace("Verbleibende Lizenzdauer:", "剩余许可证时长：", ignoreCase = true)
            .replace("Offizielle Website", "官方网站", ignoreCase = true)
            .replace("Offizielle Webseite", "官方网站", ignoreCase = true)
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
            .replace("Standpunkt:", "当前位置：", ignoreCase = true)
            .replace("Auftrag annehmen", "接受订单", ignoreCase = true)
    }

    private fun translateToJa(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "サーバーログインと認証", ignoreCase = true)
            .replace("Benutzername", "ユーザー名", ignoreCase = true)
            .replace("Passwort", "パスワード", ignoreCase = true)
            .replace("Einloggen", "ログイン", ignoreCase = true)
            .replace("Anmelden", "サインイン", ignoreCase = true)
            .replace("Registrieren", "登録", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "アカウント作成＆ログイン", ignoreCase = true)
            .replace("In Konto Anmelden", "アカウントにサインイン", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "アカウント解除 / ライセンス購入", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "PayPalで支払う / アカウント解除", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "PayPal即時決済＆アカウント解除", ignoreCase = true)
            .replace("Offizielle Website", "公式ウェブサイト", ignoreCase = true)
            .replace("Offizielle Webseite", "公式ウェブサイト", ignoreCase = true)
            .replace("Silber-Budget", "シルバー予算", ignoreCase = true)
            .replace("Kaufpreis", "購入価格", ignoreCase = true)
            .replace("Verkaufspreis", "売却価格", ignoreCase = true)
            .replace("Gewinn", "利益", ignoreCase = true)
            .replace("Menge", "数量", ignoreCase = true)
            .replace("Speichern", "保存", ignoreCase = true)
            .replace("Schließen", "閉じる", ignoreCase = true)
            .replace("Einstellungen", "設定", ignoreCase = true)
            .replace("Standpunkt:", "現在地:", ignoreCase = true)
            .replace("Auftrag annehmen", "注文を承認", ignoreCase = true)
    }

    private fun translateToKo(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "서버 로그인 및 인증", ignoreCase = true)
            .replace("Benutzername", "사용자 이름", ignoreCase = true)
            .replace("Passwort", "비밀번호", ignoreCase = true)
            .replace("Einloggen", "로그인", ignoreCase = true)
            .replace("Anmelden", "로그인", ignoreCase = true)
            .replace("Registrieren", "회원가입", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "계정 생성 및 로그인", ignoreCase = true)
            .replace("In Konto Anmelden", "계정 로그인", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "계정 잠금 해제 / 라이선스 구매", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "PayPal 결제 / 계정 잠금 해제", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "PayPal 즉시 결제 및 잠금 해제", ignoreCase = true)
            .replace("Offizielle Website", "공식 웹사이트", ignoreCase = true)
            .replace("Offizielle Webseite", "공식 웹사이트", ignoreCase = true)
            .replace("Silber-Budget", "실버 예산", ignoreCase = true)
            .replace("Kaufpreis", "구매가", ignoreCase = true)
            .replace("Verkaufspreis", "판매가", ignoreCase = true)
            .replace("Gewinn", "이익", ignoreCase = true)
            .replace("Menge", "수량", ignoreCase = true)
            .replace("Speichern", "저장", ignoreCase = true)
            .replace("Schließen", "닫기", ignoreCase = true)
            .replace("Einstellungen", "설정", ignoreCase = true)
            .replace("Standpunkt:", "현재 위치:", ignoreCase = true)
            .replace("Auftrag annehmen", "주문 수락", ignoreCase = true)
    }

    private fun translateToTr(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Giriş ve Doğrulama", ignoreCase = true)
            .replace("Benutzername", "Kullanıcı Adı", ignoreCase = true)
            .replace("Passwort", "Şifre", ignoreCase = true)
            .replace("Einloggen", "Giriş Yap", ignoreCase = true)
            .replace("Anmelden", "Giriş", ignoreCase = true)
            .replace("Registrieren", "Kayıt Ol", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Hesap Oluştur ve Giriş Yap", ignoreCase = true)
            .replace("In Konto Anmelden", "Hesaba Giriş Yap", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Hesabı Aç / Lisans Satın Al", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "PayPal ile Öde / Hesabı Aç", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "PayPal Anında Ödeme ve Aktivasyon", ignoreCase = true)
            .replace("Offizielle Website", "Resmi Web Sitesi", ignoreCase = true)
            .replace("Offizielle Webseite", "Resmi Web Sitesi", ignoreCase = true)
            .replace("Silber-Budget", "Gümüş Bütçesi", ignoreCase = true)
            .replace("Kaufpreis", "Alış Fiyatı", ignoreCase = true)
            .replace("Verkaufspreis", "Satış Fiyatı", ignoreCase = true)
            .replace("Gewinn", "Kar", ignoreCase = true)
            .replace("Menge", "Miktar", ignoreCase = true)
            .replace("Speichern", "Kaydet", ignoreCase = true)
            .replace("Schließen", "Kapat", ignoreCase = true)
            .replace("Einstellungen", "Ayarlar", ignoreCase = true)
            .replace("Standpunkt:", "Konum:", ignoreCase = true)
            .replace("Auftrag annehmen", "Siparişi Kabul Et", ignoreCase = true)
    }

    private fun translateToId(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Login & Autentikasi", ignoreCase = true)
            .replace("Benutzername", "Nama Pengguna", ignoreCase = true)
            .replace("Passwort", "Kata Sandi", ignoreCase = true)
            .replace("Einloggen", "Masuk", ignoreCase = true)
            .replace("Anmelden", "Masuk", ignoreCase = true)
            .replace("Registrieren", "Daftar", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Buat Akun & Masuk", ignoreCase = true)
            .replace("In Konto Anmelden", "Masuk ke Akun", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Buka Akun / Beli Lisensi", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Bayar dengan PayPal / Buka Akun", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "Pembayaran Instan PayPal & Aktivasi", ignoreCase = true)
            .replace("Offizielle Website", "Situs Resmi", ignoreCase = true)
            .replace("Offizielle Webseite", "Situs Resmi", ignoreCase = true)
            .replace("Silber-Budget", "Anggaran Perak", ignoreCase = true)
            .replace("Kaufpreis", "Harga Beli", ignoreCase = true)
            .replace("Verkaufspreis", "Harga Jual", ignoreCase = true)
            .replace("Gewinn", "Keuntungan", ignoreCase = true)
            .replace("Menge", "Jumlah", ignoreCase = true)
            .replace("Speichern", "Simpan", ignoreCase = true)
            .replace("Schließen", "Tutup", ignoreCase = true)
            .replace("Einstellungen", "Pengaturan", ignoreCase = true)
            .replace("Standpunkt:", "Lokasi:", ignoreCase = true)
            .replace("Auftrag annehmen", "Terima Pesanan", ignoreCase = true)
    }

    private fun translateToPl(text: String): String {
        return text
            .replace("Server Login & Authentifizierung", "Logowanie i Uwierzytelnianie", ignoreCase = true)
            .replace("Benutzername", "Nazwa użytkownika", ignoreCase = true)
            .replace("Passwort", "Hasło", ignoreCase = true)
            .replace("Einloggen", "Zaloguj się", ignoreCase = true)
            .replace("Anmelden", "Zaloguj", ignoreCase = true)
            .replace("Registrieren", "Zarejestruj się", ignoreCase = true)
            .replace("Konto erstellen & Einloggen", "Utwórz konto i zaloguj się", ignoreCase = true)
            .replace("In Konto Anmelden", "Zaloguj się do konta", ignoreCase = true)
            .replace("Konto Freischalten / Lizenzen kaufen", "Odblokuj konto / Kup licencje", ignoreCase = true)
            .replace("PayPal Bezahlen / Konto Freischalten", "Zapłać przez PayPal / Odblokuj konto", ignoreCase = true)
            .replace("PayPal Sofort-Bezahlung & Freischaltung", "Natychmiastowa płatność PayPal i odblokowanie", ignoreCase = true)
            .replace("Offizielle Website", "Oficjalna strona", ignoreCase = true)
            .replace("Offizielle Webseite", "Oficjalna strona", ignoreCase = true)
            .replace("Silber-Budget", "Budżet srebra", ignoreCase = true)
            .replace("Kaufpreis", "Cena zakupu", ignoreCase = true)
            .replace("Verkaufspreis", "Cena sprzedaży", ignoreCase = true)
            .replace("Gewinn", "Zysk", ignoreCase = true)
            .replace("Menge", "Ilość", ignoreCase = true)
            .replace("Speichern", "Zapisz", ignoreCase = true)
            .replace("Schließen", "Zamknij", ignoreCase = true)
            .replace("Einstellungen", "Ustawienia", ignoreCase = true)
            .replace("Standpunkt:", "Lokalizacja:", ignoreCase = true)
            .replace("Auftrag annehmen", "Zaakceptuj zamówienie", ignoreCase = true)
    }
}
