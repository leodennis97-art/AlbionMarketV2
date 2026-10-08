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
        "bubble_max_distance" to "Max Distance (Zones)",
        "bubble_min_stock" to "Min Stock",
        "bubble_max_stock" to "Max Stock",
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
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Análisis en tiempo real y alertas",
        "tab_orders" to "📋 Pedidos e Historial",
        "tab_calculator" to "🧮 Calculadora y Margen (Top 50)",
        "tab_catalog" to "📖 Catálogo de Mercado",
        "tab_builds" to "⚔️ Equipamiento IA y Sets",
        "tab_order_stats" to "📊 Estadísticas (1D-1A)",
        "tab_events" to "🔥 Eventos y Botín de Jefes",
        "tab_gold" to "🪙 Mercado de Oro",
        "tab_map" to "🗺️ Mapa Mundial de Albion",
        "server_select" to "Seleccionar Servidor:",
        "silver_budget" to "Presupuesto de Plata",
        "carry_capacity" to "Capacidad de Carga (kg)",
        "min_margin" to "Margen Mínimo (%)",
        "premium_tax" to "Impuesto Premium 4%",
        "standard_tax" to "Impuesto Estándar 8%",
        "avoid_danger" to "Evitar Zonas Peligrosas (Caerleon / Roja)",
        "avoid_danger_desc" to "Excluye Caerleon y Zonas PvP",
        "top_opportunities" to "Ofertas Más Rentables",
        "buy_in" to "Comprar en",
        "sell_in" to "Vender en",
        "quantity" to "Cantidad",
        "weight" to "Peso",
        "investment" to "Inversión",
        "profit_per_unit" to "Ganancia/Unidad",
        "accept_order" to "Aceptar Pedido",
        "gold_buy" to "🪙 Compra de Oro",
        "gold_sell" to "🪙 Venta de Oro",
        "settings_title" to "⚙️ Ajustes y Alertas",
        "bubble_toggle" to "Superposición de Burbuja",
        "bubble_desc" to "Burbuja flotante sobre Albion Online",
        "sys_notif_toggle" to "Notificaciones Push",
        "sys_notif_desc" to "Alertas Android en tiempo real",
        "gold_notif_toggle" to "Notificaciones de Oro",
        "gold_notif_desc" to "Alertas automáticas del precio del oro",
        "background_btn" to "🔋 Ejecutar en Segundo Plano",
        "lang_select" to "Idioma / Language:",
        "save" to "Guardar",
        "close" to "Cerrar",
        "roi" to "Margen",
        "no_price_data" to "Sin precio aún",
        "bubble_options" to "⚙️ Ajustes de Burbuja",
        "bubble_lang" to "Idioma:",
        "bubble_category" to "Filtro de Categoría:",
        "bubble_active_order" to "📦 Pedido Activo",
        "bubble_top_margin" to "🔥 Margen Top 3",
        "bubble_max_distance" to "Distancia Máxima",
        "bubble_min_stock" to "Stock Mínimo",
        "bubble_max_stock" to "Stock Máximo",
        "bubble_book" to "Reservar",
        "bubble_cancel" to "Cancelar",
        "bubble_abort" to "Abortar",
        "bubble_save_book" to "Guardar y Reservar",
        "bubble_location" to "🏙️ Ubicación:",
        "bubble_all_cities" to "Todas las Ciudades",
        "bubble_all" to "Todo",
        "bubble_click_accept" to "Clic para aceptar",
        "bubble_calculating" to "Calculando ofertas...",
        "bubble_no_opps" to "No se encontraron ofertas.",
        "bubble_search" to "Buscar mercado...",
        "bubble_hold_move" to "Mantener para mover",
        "bubble_exit" to "Salir",
        "bubble_kauf" to "Compra:",
        "bubble_verkauf" to "Venta:",
        "bubble_buy_price" to "Precio compra (Plata)",
        "bubble_sell_price" to "Precio venta (Plata)",
        "bubble_bought_amount" to "Cantidad comprada",
        "bubble_spent" to "Gastado",
        "bubble_earned" to "Ingreso Neto",
        "bubble_profit_loss" to "Ganancia/Pérdida",
        "bubble_include_brecilien" to "Incluir Brecilien",
        "bubble_compact_mode" to "📱 Modo Compacto",
        "bubble_compact_desc" to "Reduce el tamaño de la burbuja",
        "bubble_opacity" to "👁️ Opacidad de Burbuja",
        "bubble_scale" to "🔍 Escala de Burbuja",
        "tab_crafting" to "🛠️ Guía de Fabricación",
        "tab_island" to "🏝️ Guía de Isla",
        "tab_monsters" to "👹 Monstruos y Jefes",
        "crafting_search" to "Buscar objeto a fabricar...",
        "monster_search" to "Buscar monstruo o botín...",
        "crafting_req_ingredients" to "Recursos y mercado más barato:",
        "cheapest_at" to "Compra más barata",
        "city_caerleon" to "Caerleon",
        "city_brecilien" to "Brecilien",
        "city_arthurs_rest" to "Reposo de Arthur",
        "city_merlyns_rest" to "Reposo de Merlyn",
        "city_morganas_rest" to "Reposo de Morgana",
        "city_bridgewatch" to "Bridgewatch",
        "city_martlock" to "Martlock",
        "city_lymhurst" to "Lymhurst",
        "city_fort_sterling" to "Fort Sterling",
        "city_thetford" to "Thetford",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 ZONA ROJA",
        "black_zone" to "⚫ ZONA NEGRA"
    )

    private val frMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Analyse en temps réel et alertes",
        "tab_orders" to "📋 Commandes et Historique",
        "tab_calculator" to "🧮 Calculateur et Marge (Top 50)",
        "tab_catalog" to "📖 Catalogue du Marché",
        "tab_builds" to "⚔️ Équipement IA et Builds",
        "tab_order_stats" to "📊 Statistiques (1J-1A)",
        "tab_events" to "🔥 Événements et Boss",
        "tab_gold" to "🪙 Marché de l'Or",
        "tab_map" to "🗺️ Carte du Monde d'Albion",
        "server_select" to "Sélectionner le Serveur :",
        "silver_budget" to "Budget Argent",
        "carry_capacity" to "Capacité de Charge (kg)",
        "min_margin" to "Marge Minimale (%)",
        "premium_tax" to "Taxe Premium 4%",
        "standard_tax" to "Taxe Standard 8%",
        "avoid_danger" to "Éviter les Zones Dangereuses",
        "avoid_danger_desc" to "Exclut Caerleon et Zones PvP",
        "top_opportunities" to "Meilleures Offres",
        "buy_in" to "Acheter à",
        "sell_in" to "Vendre à",
        "quantity" to "Quantité",
        "weight" to "Poids",
        "investment" to "Investissement",
        "profit_per_unit" to "Profit/Unité",
        "accept_order" to "Accepter la commande",
        "gold_buy" to "🪙 Achat d'Or",
        "gold_sell" to "🪙 Vente d'Or",
        "settings_title" to "⚙️ Paramètres et Alertes",
        "bubble_toggle" to "Bulle Flottante en Jeu",
        "bubble_desc" to "Affiche une bulle sur Albion Online",
        "sys_notif_toggle" to "Notifications Push",
        "sys_notif_desc" to "Alertes Android en temps réel",
        "gold_notif_toggle" to "Notifications d'Or",
        "gold_notif_desc" to "Alertes de prix de l'or",
        "background_btn" to "🔋 Exécuter en Arrière-plan",
        "lang_select" to "Langue / Language :",
        "save" to "Enregistrer",
        "close" to "Fermer",
        "roi" to "Marge",
        "no_price_data" to "Pas de prix",
        "bubble_options" to "⚙️ Options de Bulle",
        "bubble_lang" to "Langue :",
        "bubble_category" to "Filtre Catégorie :",
        "bubble_active_order" to "📦 Commande Active",
        "bubble_top_margin" to "🔥 Marge Top 3",
        "bubble_max_distance" to "Distance Max",
        "bubble_min_stock" to "Stock Min",
        "bubble_max_stock" to "Stock Max",
        "bubble_book" to "Réserver",
        "bubble_cancel" to "Annuler",
        "bubble_abort" to "Abandonner",
        "bubble_save_book" to "Enregistrer & Réserver",
        "bubble_location" to "🏙️ Emplacement :",
        "bubble_all_cities" to "Toutes les Villes",
        "bubble_all" to "Tout",
        "bubble_click_accept" to "Cliquer pour accepter",
        "bubble_calculating" to "Calcul en cours...",
        "bubble_no_opps" to "Aucune offre trouvée.",
        "bubble_search" to "Recherche marché...",
        "bubble_hold_move" to "Maintenir pour déplacer",
        "bubble_exit" to "Quitter",
        "bubble_kauf" to "Achat :",
        "bubble_verkauf" to "Vente :",
        "bubble_buy_price" to "Prix d'achat (Argent)",
        "bubble_sell_price" to "Prix de vente (Argent)",
        "bubble_bought_amount" to "Quantité achetée",
        "bubble_spent" to "Dépensé",
        "bubble_earned" to "Revenu Net",
        "bubble_profit_loss" to "Profit/Perte",
        "bubble_include_brecilien" to "Inclure Brecilien",
        "bubble_compact_mode" to "📱 Mode Compact",
        "bubble_compact_desc" to "Réduit la taille de la bulle",
        "bubble_opacity" to "👁️ Opacité de la Bulle",
        "bubble_scale" to "🔍 Taille de la Bulle",
        "tab_crafting" to "🛠️ Guide d'Artisanat",
        "tab_island" to "🏝️ Guide d'Île",
        "tab_monsters" to "👹 Monstres & Boss",
        "crafting_search" to "Rechercher un objet...",
        "monster_search" to "Rechercher monstre...",
        "crafting_req_ingredients" to "Ressources requises :",
        "cheapest_at" to "Achat moins cher",
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
        "red_zone" to "🔴 ZONE ROUGE",
        "black_zone" to "⚫ ZONE NOIRE"
    )

    private val ptMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Análise em tempo real e alertas",
        "tab_orders" to "📋 Pedidos e Histórico",
        "tab_calculator" to "🧮 Calculadora e Margem (Top 50)",
        "tab_catalog" to "📖 Catálogo do Mercado",
        "tab_builds" to "⚔️ Equipamento IA e Builds",
        "tab_order_stats" to "📊 Estatísticas (1D-1A)",
        "tab_events" to "🔥 Eventos e Jefes",
        "tab_gold" to "🪙 Mercado de Ouro",
        "tab_map" to "🗺️ Mapa Mundial de Albion",
        "server_select" to "Selecionar Servidor:",
        "silver_budget" to "Orçamento de Prata",
        "carry_capacity" to "Capacidade de Carga (kg)",
        "min_margin" to "Margem Mínima (%)",
        "premium_tax" to "Imposto Premium 4%",
        "standard_tax" to "Imposto Padrão 8%",
        "avoid_danger" to "Evitar Zonas Perigosas",
        "avoid_danger_desc" to "Exclui Caerleon e Zonas PvP",
        "top_opportunities" to "Ofertas Mais Lucrativas",
        "buy_in" to "Comprar em",
        "sell_in" to "Vender em",
        "quantity" to "Quantidade",
        "weight" to "Peso",
        "investment" to "Investimento",
        "profit_per_unit" to "Lucro/Unidade",
        "accept_order" to "Aceitar Pedido",
        "gold_buy" to "🪙 Compra de Ouro",
        "gold_sell" to "🪙 Venda de Ouro",
        "settings_title" to "⚙️ Configurações e Alertas",
        "bubble_toggle" to "Overlay de Bolha Flutuante",
        "bubble_desc" to "Exibe uma bolha sobre o Albion Online",
        "sys_notif_toggle" to "Notificações Push",
        "sys_notif_desc" to "Alertas Android em tempo real",
        "gold_notif_toggle" to "Notificações de Ouro",
        "gold_notif_desc" to "Alertas do preço do ouro",
        "background_btn" to "🔋 Executar em Segundo Plano",
        "lang_select" to "Idioma / Language:",
        "save" to "Salvar",
        "close" to "Fechar",
        "roi" to "Margem",
        "no_price_data" to "Sem preço ainda",
        "bubble_options" to "⚙️ Opções de Bolha",
        "bubble_lang" to "Idioma:",
        "bubble_category" to "Filtro de Categoria:",
        "bubble_active_order" to "📦 Pedido Ativo",
        "bubble_top_margin" to "🔥 Margem Top 3",
        "bubble_max_distance" to "Distância Máxima",
        "bubble_min_stock" to "Estoque Mínimo",
        "bubble_max_stock" to "Estoque Máximo",
        "bubble_book" to "Reservar",
        "bubble_cancel" to "Cancelar",
        "bubble_abort" to "Abortar",
        "bubble_save_book" to "Salvar e Reservar",
        "bubble_location" to "🏙️ Localização:",
        "bubble_all_cities" to "Todas as Cidades",
        "bubble_all" to "Tudo",
        "bubble_click_accept" to "Clique para aceitar",
        "bubble_calculating" to "Calculando ofertas...",
        "bubble_no_opps" to "Nenhuma oferta encontrada.",
        "bubble_search" to "Buscar mercado...",
        "bubble_hold_move" to "Segure para mover",
        "bubble_exit" to "Sair",
        "bubble_kauf" to "Compra:",
        "bubble_verkauf" to "Venda:",
        "bubble_buy_price" to "Preço de compra (Prata)",
        "bubble_sell_price" to "Preço de venda (Prata)",
        "bubble_bought_amount" to "Quantidade comprada",
        "bubble_spent" to "Gasto",
        "bubble_earned" to "Receita Líquida",
        "bubble_profit_loss" to "Lucro/Prejuízo",
        "bubble_include_brecilien" to "Incluir Brecilien",
        "bubble_compact_mode" to "📱 Modo Compacto",
        "bubble_compact_desc" to "Reduz o tamanho da bolha",
        "bubble_opacity" to "👁️ Opacidade da Bolha",
        "bubble_scale" to "🔍 Escala da Bolha",
        "tab_crafting" to "🛠️ Guia de Fabricação",
        "tab_island" to "🏝️ Guia de Ilha",
        "tab_monsters" to "👹 Monstros e Chefes",
        "crafting_search" to "Buscar item...",
        "monster_search" to "Buscar monstro...",
        "crafting_req_ingredients" to "Recursos necessários:",
        "cheapest_at" to "Compra mais barata",
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
        "red_zone" to "🔴 ZONA VERMELHA",
        "black_zone" to "⚫ ZONA PRETA"
    )

    private val ruMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Анализ рынка в реальном времени",
        "tab_orders" to "📋 Заказы и История",
        "tab_calculator" to "🧮 Калькулятор и Маржа (Топ 50)",
        "tab_catalog" to "📖 Каталог рынка",
        "tab_builds" to "⚔️ ИИ Снаряжение и Билды",
        "tab_order_stats" to "📊 Статистика (1Д-1Г)",
        "tab_events" to "🔥 События и Боссы",
        "tab_gold" to "🪙 Рынок золота",
        "tab_map" to "🗺️ Карта мира Альбиона",
        "server_select" to "Выберите сервер:",
        "silver_budget" to "Бюджет серебра",
        "carry_capacity" to "Грузоподъемность (кг)",
        "min_margin" to "Мин. маржа (%)",
        "premium_tax" to "Премиум налог 4%",
        "standard_tax" to "Стандартный налог 8%",
        "avoid_danger" to "Избегать опасных зон",
        "avoid_danger_desc" to "Исключает Карлеон и PvP зоны",
        "top_opportunities" to "Самые прибыльные сделки",
        "buy_in" to "Покупка в",
        "sell_in" to "Продажа в",
        "quantity" to "Количество",
        "weight" to "Вес",
        "investment" to "Инвестиции",
        "profit_per_unit" to "Прибыль/Шт",
        "accept_order" to "Принять заказ",
        "gold_buy" to "🪙 Покупка золота",
        "gold_sell" to "🪙 Продажа золота",
        "settings_title" to "⚙️ Настройки и Оповещения",
        "bubble_toggle" to "Плавающий оверлей",
        "bubble_desc" to "Отображает баббл поверх Albion Online",
        "sys_notif_toggle" to "Push-уведомления",
        "sys_notif_desc" to "Alerts в реальном времени",
        "gold_notif_toggle" to "Уведомления о золоте",
        "gold_notif_desc" to "Оповещения о курсе золота",
        "background_btn" to "🔋 Работать в фоновом режиме",
        "lang_select" to "Язык / Language:",
        "save" to "Сохранить",
        "close" to "Закрыть",
        "roi" to "Маржа",
        "no_price_data" to "Нет цены",
        "bubble_options" to "⚙️ Настройки баббла",
        "bubble_lang" to "Язык:",
        "bubble_category" to "Фильтр категорий:",
        "bubble_active_order" to "📦 Активный заказ",
        "bubble_top_margin" to "🔥 Топ 3 маржа",
        "bubble_max_distance" to "Макс. расстояние",
        "bubble_min_stock" to "Мин. запас",
        "bubble_max_stock" to "Макс. запас",
        "bubble_book" to "Забронировать",
        "bubble_cancel" to "Отмена",
        "bubble_abort" to "Прервать",
        "bubble_save_book" to "Сохранить",
        "bubble_location" to "🏙️ Локация:",
        "bubble_all_cities" to "Все города",
        "bubble_all" to "Все",
        "bubble_click_accept" to "Нажмите для принятия",
        "bubble_calculating" to "Расчет сделок...",
        "bubble_no_opps" to "Сделок не найдено.",
        "bubble_search" to "Поиск по рынку...",
        "bubble_hold_move" to "Удерживайте для перемещения",
        "bubble_exit" to "Выход",
        "bubble_kauf" to "Покупка:",
        "bubble_verkauf" to "Продажа:",
        "bubble_buy_price" to "Цена покупки (Серебро)",
        "bubble_sell_price" to "Цена продажи (Серебро)",
        "bubble_bought_amount" to "Куплено штук",
        "bubble_spent" to "Потрачено",
        "bubble_earned" to "Чистый доход",
        "bubble_profit_loss" to "Прибыль/Убыток",
        "bubble_include_brecilien" to "Включить Бресилиэн",
        "bubble_compact_mode" to "📱 Компактный режим",
        "bubble_compact_desc" to "Уменьшает размер баббла",
        "bubble_opacity" to "👁️ Прозрачность",
        "bubble_scale" to "🔍 Масштаб",
        "tab_crafting" to "🛠️ Руководство по крафту",
        "tab_island" to "🏝️ Руководство по острову",
        "tab_monsters" to "👹 Монстры и Боссы",
        "crafting_search" to "Поиск предмета...",
        "monster_search" to "Поиск монстра...",
        "crafting_req_ingredients" to "Необходимые ресурсы:",
        "cheapest_at" to "Дешевле всего в",
        "city_caerleon" to "Карлеон",
        "city_brecilien" to "Бресилиэн",
        "city_arthurs_rest" to "Артурс Рест",
        "city_merlyns_rest" to "Merlyn's Rest",
        "city_morganas_rest" to "Morgana's Rest",
        "city_bridgewatch" to "Бриджвоч",
        "city_martlock" to "Мартлок",
        "city_lymhurst" to "Лимхурст",
        "city_fort_sterling" to "Форт Стерлинг",
        "city_thetford" to "Тетфорд",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 КРАСНАЯ ЗОНА",
        "black_zone" to "⚫ ЧЕРНАЯ ЗОНА"
    )

    private val zhMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "实时市场分析与交易预警",
        "tab_orders" to "📋 订单与历史",
        "tab_calculator" to "🧮 计算器与利润 (Top 50)",
        "tab_catalog" to "📖 市场目录",
        "tab_builds" to "⚔️ AI 装备与 Build 配装",
        "tab_order_stats" to "📊 挂单统计 (1天-1年)",
        "tab_events" to "🔥 活动与 Boss 掉落",
        "tab_gold" to "🪙 黄金市场",
        "tab_map" to "🗺️ 阿尔比恩世界地图",
        "server_select" to "选择服务器：",
        "silver_budget" to "银币预算",
        "carry_capacity" to "负重能力 (kg)",
        "min_margin" to "最低利润率 (%)",
        "premium_tax" to "VIP 4% 税率",
        "standard_tax" to "普通 8% 税率",
        "avoid_danger" to "避开危险区域 (Caerleon / 红区)",
        "avoid_danger_desc" to "排除 Caerleon 与全掉落 PvP 区域",
        "top_opportunities" to "最具吸引力的交易机会",
        "buy_in" to "买入城市",
        "sell_in" to "卖出城市",
        "quantity" to "数量",
        "weight" to "重量",
        "investment" to "投资额",
        "profit_per_unit" to "单件利润",
        "accept_order" to "接受并保存订单",
        "gold_buy" to "🪙 黄金买入",
        "gold_sell" to "🪙 黄金卖出",
        "settings_title" to "⚙️ 设置与预警",
        "bubble_toggle" to "游戏内悬浮窗 Overlay",
        "bubble_desc" to "在 Albion Online 上方显示悬浮窗",
        "sys_notif_toggle" to "系统 Push 推送通知",
        "sys_notif_desc" to "实时推送 Android 热门交易预警",
        "gold_notif_toggle" to "金币投资组合通知",
        "gold_notif_desc" to "金价更新时自动发送收益预警",
        "background_btn" to "🔋 在后台无限制运行",
        "lang_select" to "语言 / Language:",
        "save" to "保存",
        "close" to "关闭",
        "roi" to "利润率",
        "no_price_data" to "暂无价格",
        "bubble_options" to "⚙️ 悬浮窗设置",
        "bubble_lang" to "语言：",
        "bubble_category" to "分类筛选：",
        "bubble_active_order" to "📦 当前订单",
        "bubble_top_margin" to "🔥 前 3 利润率",
        "bubble_max_distance" to "最大距离 (区域)",
        "bubble_min_stock" to "最小库存",
        "bubble_max_stock" to "最大库存",
        "bubble_book" to "预订",
        "bubble_cancel" to "取消",
        "bubble_abort" to "中止",
        "bubble_save_book" to "保存并预订",
        "bubble_location" to "🏙️ 当前位置：",
        "bubble_all_cities" to "所有城市",
        "bubble_all" to "全部",
        "bubble_click_accept" to "点击接受",
        "bubble_calculating" to "正在计算最佳市场机会...",
        "bubble_no_opps" to "未找到匹配的市场机会。",
        "bubble_search" to "搜索市场...",
        "bubble_hold_move" to "按住移动",
        "bubble_exit" to "退出",
        "bubble_kauf" to "买入：",
        "bubble_verkauf" to "卖出：",
        "bubble_buy_price" to "买入单价 (银币)",
        "bubble_sell_price" to "卖出单价 (银币)",
        "bubble_bought_amount" to "已购数量",
        "bubble_spent" to "总支出",
        "bubble_earned" to "净收入",
        "bubble_profit_loss" to "盈亏",
        "bubble_include_brecilien" to "路由包含 Brecilien",
        "bubble_compact_mode" to "📱 紧凑悬浮窗模式",
        "bubble_compact_desc" to "缩小悬浮窗尺寸与字号",
        "bubble_opacity" to "👁️ 悬浮窗不透明度",
        "bubble_scale" to "🔍 悬浮窗缩放比例",
        "tab_crafting" to "🛠️ 制造指南",
        "tab_island" to "🏝️ 岛屿指南",
        "tab_monsters" to "👹 怪物与 Boss",
        "crafting_search" to "搜索要制造的物品...",
        "monster_search" to "搜索怪物、Boss 或掉落物...",
        "crafting_req_ingredients" to "所需资源与最便宜市场：",
        "cheapest_at" to "最便宜买入地",
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
        "red_zone" to "🔴 红区",
        "black_zone" to "⚫ 黑区"
    )

    private val jaMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "リアルタイム市場分析＆取引アラート",
        "tab_orders" to "📋 注文と履歴",
        "tab_calculator" to "🧮 計算機とマージン (Top 50)",
        "tab_catalog" to "📖 市場カタログ",
        "tab_builds" to "⚔️ AI 装備＆ビルドセット",
        "tab_order_stats" to "📊 注文統計 (1日-1年)",
        "tab_events" to "🔥 イベント＆ボスドロップ",
        "tab_gold" to "🪙 ゴールド市場",
        "tab_map" to "🗺️ アルビオン世界マップ",
        "server_select" to "サーバーを選択:",
        "silver_budget" to "シルバー予算",
        "carry_capacity" to "所持重量 (kg)",
        "min_margin" to "最小マージン (%)",
        "premium_tax" to "プレミアム 4% 税",
        "standard_tax" to "ノーマル 8% 税",
        "avoid_danger" to "危険ゾーンを回避 (Caerleon / 赤)",
        "avoid_danger_desc" to "Caerleon と Full-Loot PvP ゾーンを除外",
        "top_opportunities" to "最高益取引の機会",
        "buy_in" to "購入都市",
        "sell_in" to "売却都市",
        "quantity" to "数量",
        "weight" to "重量",
        "investment" to "投資額",
        "profit_per_unit" to "1個あたり利益",
        "accept_order" to "注文を承認",
        "gold_buy" to "🪙 ゴールド購入",
        "gold_sell" to "🪙 ゴールド売却",
        "settings_title" to "⚙️ 設定＆アラート",
        "bubble_toggle" to "ゲーム内バブルオーバーレイ",
        "bubble_desc" to "Albion Online 上に移動可能なバブルを表示",
        "sys_notif_toggle" to "システムプッシュ通知",
        "sys_notif_desc" to "トップ取引をリアルタイムで通知",
        "gold_notif_toggle" to "ゴールド資産通知",
        "gold_notif_desc" to "金相場更新時に自動通知を送信",
        "background_btn" to "🔋 バックグラウンドで無制限実行",
        "lang_select" to "言語 / Language:",
        "save" to "保存",
        "close" to "閉じる",
        "roi" to "マージン",
        "no_price_data" to "価格なし",
        "bubble_options" to "⚙️ バブル設定",
        "bubble_lang" to "言語:",
        "bubble_category" to "カテゴリフィルター:",
        "bubble_active_order" to "📦 アクティブな注文",
        "bubble_top_margin" to "🔥 トップ 3 マージン",
        "bubble_max_distance" to "最大距離 (ゾーン)",
        "bubble_min_stock" to "最小在庫",
        "bubble_max_stock" to "最大在庫",
        "bubble_book" to "予約",
        "bubble_cancel" to "キャンセル",
        "bubble_abort" to "中止",
        "bubble_save_book" to "保存＆予約",
        "bubble_location" to "🏙️ 現在地:",
        "bubble_all_cities" to "全都市",
        "bubble_all" to "すべて",
        "bubble_click_accept" to "タップして承認",
        "bubble_calculating" to "最適機会を計算中...",
        "bubble_no_opps" to "条件に合う取引が見つかりません。",
        "bubble_search" to "市場検索...",
        "bubble_hold_move" to "長押しで移動",
        "bubble_exit" to "終了",
        "bubble_kauf" to "購入:",
        "bubble_verkauf" to "売却:",
        "bubble_buy_price" to "購入単価 (シルバー)",
        "bubble_sell_price" to "売却単価 (シルバー)",
        "bubble_bought_amount" to "購入数量",
        "bubble_spent" to "支出",
        "bubble_earned" to "純売上",
        "bubble_profit_loss" to "損益",
        "bubble_include_brecilien" to "ルートに Brecilien を含める",
        "bubble_compact_mode" to "📱 コンパクトバブルモード",
        "bubble_compact_desc" to "バブルのサイズと文字を縮小",
        "bubble_opacity" to "👁️ バブルの不透明度",
        "bubble_scale" to "🔍 バブルのスケール",
        "tab_crafting" to "🛠️ クラフトガイド",
        "tab_island" to "🏝️ 島ガイド",
        "tab_monsters" to "👹 モンスター＆ボス",
        "crafting_search" to "クラフト品を検索...",
        "monster_search" to "モンスターやボスを検索...",
        "crafting_req_ingredients" to "必要リソース＆最安市場:",
        "cheapest_at" to "最安購入地",
        "city_caerleon" to "カーレオン",
        "city_brecilien" to "ブレシリアン",
        "city_arthurs_rest" to "アーサーズ・レスト",
        "city_merlyns_rest" to "マーリンズ・レスト",
        "city_morganas_rest" to "モルガナズ・レスト",
        "city_bridgewatch" to "ブリッジウォッチ",
        "city_martlock" to "マートロック",
        "city_lymhurst" to "リムハースト",
        "city_fort_sterling" to "フォート・スターリング",
        "city_thetford" to "テットフォード",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 レッドゾーン",
        "black_zone" to "⚫ ブラックゾーン"
    )

    private val koMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "실시간 시장 분석 및 거래 알림",
        "tab_orders" to "📋 주문 및 내역",
        "tab_calculator" to "🧮 계산기 및 마진 (Top 50)",
        "tab_catalog" to "📖 시장 카탈로그",
        "tab_builds" to "⚔️ AI 장비 및 빌드 세트",
        "tab_order_stats" to "📊 주문 통계 (1일-1년)",
        "tab_events" to "🔥 이벤트 및 보스 전리품",
        "tab_gold" to "🪙 골드 시장",
        "tab_map" to "🗺️ 알비온 세계 지도",
        "server_select" to "서버 선택:",
        "silver_budget" to "실버 예산",
        "carry_capacity" to "운반 용량 (kg)",
        "min_margin" to "최소 마진 (%)",
        "premium_tax" to "프리미엄 4% 세율",
        "standard_tax" to "일반 8% 세율",
        "avoid_danger" to "위험 지역 (Caerleon / 레드) 피하기",
        "avoid_danger_desc" to "Caerleon 및 Full-Loot PvP 지역 제외",
        "top_opportunities" to "최고 수익 거래 기회",
        "buy_in" to "구매 도시",
        "sell_in" to "판매 도시",
        "quantity" to "수량",
        "weight" to "무게",
        "investment" to "투자금",
        "profit_per_unit" to "개당 이익",
        "accept_order" to "주문 수락",
        "gold_buy" to "🪙 골드 구매",
        "gold_sell" to "🪙 골드 판매",
        "settings_title" to "⚙️ 설정 및 알림",
        "bubble_toggle" to "플로팅 버블 오버레이",
        "bubble_desc" to "Albion Online 위에 버블을 상시 표시",
        "sys_notif_toggle" to "시스템 푸시 알림",
        "sys_notif_desc" to "인기 거래 실시간 알림",
        "gold_notif_toggle" to "골드 포트폴리오 알림",
        "gold_notif_desc" to "골드 시세 업데이트 알림",
        "background_btn" to "🔋 백그라운드 제한 없이 실행",
        "lang_select" to "언어 / Language:",
        "save" to "저장",
        "close" to "닫기",
        "roi" to "마진",
        "no_price_data" to "가격 없음",
        "bubble_options" to "⚙️ 버블 설정",
        "bubble_lang" to "언어:",
        "bubble_category" to "카테고리 필터:",
        "bubble_active_order" to "📦 활성 주문",
        "bubble_top_margin" to "🔥 상위 3개 마진",
        "bubble_max_distance" to "최대 거리 (지역)",
        "bubble_min_stock" to "최소 재고",
        "bubble_max_stock" to "최대 재고",
        "bubble_book" to "예약",
        "bubble_cancel" to "취소",
        "bubble_abort" to "중단",
        "bubble_save_book" to "저장 및 예약",
        "bubble_location" to "🏙️ 현재 위치:",
        "bubble_all_cities" to "모든 도시",
        "bubble_all" to "전체",
        "bubble_click_accept" to "클릭하여 수락",
        "bubble_calculating" to "최적 거래 계산 중...",
        "bubble_no_opps" to "조건에 맞는 거래 기회가 없습니다.",
        "bubble_search" to "시장 검색...",
        "bubble_hold_move" to "길게 눌러 이동",
        "bubble_exit" to "종료",
        "bubble_kauf" to "구매:",
        "bubble_verkauf" to "판매:",
        "bubble_buy_price" to "구매 단가 (실버)",
        "bubble_sell_price" to "판매 단가 (실버)",
        "bubble_bought_amount" to "구매 수량",
        "bubble_spent" to "지출",
        "bubble_earned" to "순매출",
        "bubble_profit_loss" to "손익",
        "bubble_include_brecilien" to "Brecilien 경로 포함",
        "bubble_compact_mode" to "📱 컴팩트 버블 모드",
        "bubble_compact_desc" to "버블 크기 및 폰트 줄이기",
        "bubble_opacity" to "👁️ 버블 투명도",
        "bubble_scale" to "🔍 버블 크기 비율",
        "tab_crafting" to "🛠️ 제작 가이드",
        "tab_island" to "🏝️ 섬 가이드",
        "tab_monsters" to "👹 몬스터 및 보스",
        "crafting_search" to "제작할 아이템 검색...",
        "monster_search" to "몬스터 또는 전리품 검색...",
        "crafting_req_ingredients" to "필요 재료 및 최저가 시장:",
        "cheapest_at" to "최저가 구매지",
        "city_caerleon" to "카얼레온",
        "city_brecilien" to "브레실리엔",
        "city_arthurs_rest" to "아서스 레스트",
        "city_merlyns_rest" to "멀린스 레스트",
        "city_morganas_rest" to "모르가나스 레스트",
        "city_bridgewatch" to "브릿지워치",
        "city_martlock" to "마트록",
        "city_lymhurst" to "림허스트",
        "city_fort_sterling" to "포트 스털링",
        "city_thetford" to "뎃포드",
        "city_cairn_drain" to "Cairn Drain",
        "red_zone" to "🔴 레드 존",
        "black_zone" to "⚫ 블랙 존"
    )

    private val trMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Anlık Pazar Analizi ve Ticaret Bildirimleri",
        "tab_orders" to "📋 Siparişler ve Geçmiş",
        "tab_calculator" to "🧮 Hesaplayıcı ve Kar (Top 50)",
        "tab_catalog" to "📖 Pazar Kataloğu",
        "tab_builds" to "⚔️ YAZ Ekipman ve Build Setleri",
        "tab_order_stats" to "📊 Emir İstatistikleri (1G-1Y)",
        "tab_events" to "🔥 Etkinlikler ve Boss Ganimetleri",
        "tab_gold" to "🪙 Altın Pazarı",
        "tab_map" to "🗺️ Albion Dünya Haritası",
        "server_select" to "Sunucu Seçin:",
        "silver_budget" to "Gümüş Bütçesi",
        "carry_capacity" to "Taşıma Kapasitesi (kg)",
        "min_margin" to "Min. Kar Oranı (%)",
        "premium_tax" to "Premium %4 Vergi",
        "standard_tax" to "Standart %8 Vergi",
        "avoid_danger" to "Tehlikeli Bölgelerden Kaçın (Caerleon / Kırmızı)",
        "avoid_danger_desc" to "Caerleon ve Full-Loot PvP Bölgelerini Hariç Tut",
        "top_opportunities" to "En Karlı Ticaret Fırsatları",
        "buy_in" to "Alış Şehri",
        "sell_in" to "Satış Şehri",
        "quantity" to "Miktar",
        "weight" to "Ağırlık",
        "investment" to "Yatırım",
        "profit_per_unit" to "Kar/Birim",
        "accept_order" to "Siparişi Kabul Et",
        "gold_buy" to "🪙 Altın Alış",
        "gold_sell" to "🪙 Altın Satış",
        "settings_title" to "⚙️ Ayarlar ve Bildirimler",
        "bubble_toggle" to "Oyun İçi Baloncuk Overlay",
        "bubble_desc" to "Albion Online üzerinde baloncuk gösterir",
        "sys_notif_toggle" to "Sistem Anlık Bildirimleri",
        "sys_notif_desc" to "En iyi fırsatlar için gerçek zamanlı bildirimler",
        "gold_notif_toggle" to "Altın Portföy Bildirimleri",
        "gold_notif_desc" to "Altın fiyat güncellemelerinde otomatik bildirim",
        "background_btn" to "🔋 Arka Planda Sınırsız Çalıştır",
        "lang_select" to "Dil / Language:",
        "save" to "Kaydet",
        "close" to "Kapat",
        "roi" to "Kar Oranı",
        "no_price_data" to "Henüz fiyat yok",
        "bubble_options" to "⚙️ Baloncuk Ayarları",
        "bubble_lang" to "Dil:",
        "bubble_category" to "Kategori Filtresi:",
        "bubble_active_order" to "📦 Aktif Sipariş",
        "bubble_top_margin" to "🔥 En Yüksek 3 Kar",
        "bubble_max_distance" to "Maksimum Mesafe (Bölge)",
        "bubble_min_stock" to "Min. Stok",
        "bubble_max_stock" to "Maks. Stok",
        "bubble_book" to "Rezerve Et",
        "bubble_cancel" to "İptal Et",
        "bubble_abort" to "Durdur",
        "bubble_save_book" to "Kaydet ve Rezerve Et",
        "bubble_location" to "🏙️ Konum:",
        "bubble_all_cities" to "Tüm Şehirler",
        "bubble_all" to "Tümü",
        "bubble_click_accept" to "Kabul etmek için tıklayın",
        "bubble_calculating" to "En iyi fırsatlar hesaplanıyor...",
        "bubble_no_opps" to "Uygun ticaret fırsatı bulunamadı.",
        "bubble_search" to "Pazarda ara...",
        "bubble_hold_move" to "Taşımak için basılı tutun",
        "bubble_exit" to "Çıkış",
        "bubble_kauf" to "Alış:",
        "bubble_verkauf" to "Satış:",
        "bubble_buy_price" to "Birim alış fiyatı (Gümüş)",
        "bubble_sell_price" to "Birim satış fiyatı (Gümüş)",
        "bubble_bought_amount" to "Satın alınan miktar",
        "bubble_spent" to "Harcanan",
        "bubble_earned" to "Net Gelir",
        "bubble_profit_loss" to "Kar/Zarar",
        "bubble_include_brecilien" to "Brecilien'i rotalara dahil et",
        "bubble_compact_mode" to "📱 Kompakt Baloncuk Modu",
        "bubble_compact_desc" to "Baloncuk boyutunu ve yazı tipini küçültür",
        "bubble_opacity" to "👁️ Baloncuk Saydamlığı",
        "bubble_scale" to "🔍 Baloncuk Boyutu",
        "tab_crafting" to "🛠️ Üretim Rehberi",
        "tab_island" to "🏝️ Ada Rehberi",
        "tab_monsters" to "👹 Canavarlar ve Bosslar",
        "crafting_search" to "Üretilecek eşyayı ara...",
        "monster_search" to "Canavar veya ganimet ara...",
        "crafting_req_ingredients" to "Gerekli kaynaklar ve en ucuz pazar:",
        "cheapest_at" to "En ucuz alış yeri",
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
        "red_zone" to "🔴 KIRMIZI BÖLGE",
        "black_zone" to "⚫ SİYAH BÖLGE"
    )

    private val idMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Analisis Pasar & Peringatan Real-time",
        "tab_orders" to "📋 Pesanan & Riwayat",
        "tab_calculator" to "🧮 Kalkulator & Margin (Top 50)",
        "tab_catalog" to "📖 Katalog Pasar",
        "tab_builds" to "⚔️ Perengkapan AI & Set Build",
        "tab_order_stats" to "📊 Statistik Pesanan (1H-1Th)",
        "tab_events" to "🔥 Acara & Loot Boss",
        "tab_gold" to "🪙 Pasar Emas",
        "tab_map" to "🗺️ Peta Dunia Albion",
        "server_select" to "Pilih Server:",
        "silver_budget" to "Anggaran Perak",
        "carry_capacity" to "Kapasitas Bawa (kg)",
        "min_margin" to "Margin Min (%)",
        "premium_tax" to "Pajak Premium 4%",
        "standard_tax" to "Pajak Standar 8%",
        "avoid_danger" to "Hindari Zona Berbahaya (Caerleon / Merah)",
        "avoid_danger_desc" to "Pengecualian Caerleon & Zona PvP Full-Loot",
        "top_opportunities" to "Peluang Dagang Paling Menguntungkan",
        "buy_in" to "Beli di",
        "sell_in" to "Jual di",
        "quantity" to "Jumlah",
        "weight" to "Berat",
        "investment" to "Investasi",
        "profit_per_unit" to "Keuntungan/Unit",
        "accept_order" to "Terima & Simpan Pesanan",
        "gold_buy" to "🪙 Beli Emas",
        "gold_sell" to "🪙 Jual Emas",
        "settings_title" to "⚙️ Pengaturan & Peringatan",
        "bubble_toggle" to "Overlay Gelembung Melayang",
        "bubble_desc" to "Tampilkan gelembung di atas Albion Online",
        "sys_notif_toggle" to "Notifikasi Push Sistem",
        "sys_notif_desc" to "Peringatan penawaran terbaik secara real-time",
        "gold_notif_toggle" to "Notifikasi Portofolio Emas",
        "gold_notif_desc" to "Peringatan otomatis harga emas",
        "background_btn" to "🔋 Jalankan Tanpa Batas di Latar Belakang",
        "lang_select" to "Bahasa / Language:",
        "save" to "Simpan",
        "close" to "Tutup",
        "roi" to "Margin",
        "no_price_data" to "Belum ada harga",
        "bubble_options" to "⚙️ Pengaturan Gelembung",
        "bubble_lang" to "Bahasa:",
        "bubble_category" to "Filter Kategori:",
        "bubble_active_order" to "📦 Pesanan Aktif",
        "bubble_top_margin" to "🔥 Margin Top 3",
        "bubble_max_distance" to "Jarak Maksimal (Zona)",
        "bubble_min_stock" to "Stok Min",
        "bubble_max_stock" to "Stok Maks",
        "bubble_book" to "Pesan",
        "bubble_cancel" to "Batal",
        "bubble_abort" to "Gagalkan",
        "bubble_save_book" to "Simpan & Pesan",
        "bubble_location" to "🏙️ Lokasi Saat Ini:",
        "bubble_all_cities" to "Semua Kota",
        "bubble_all" to "Semua",
        "bubble_click_accept" to "Klik untuk menerima",
        "bubble_calculating" to "Menghitung peluang terbaik...",
        "bubble_no_opps" to "Tidak ada peluang dagang yang cocok.",
        "bubble_search" to "Cari di pasar...",
        "bubble_hold_move" to "Tahan untuk memindahkan",
        "bubble_exit" to "Keluar",
        "bubble_kauf" to "Beli:",
        "bubble_verkauf" to "Jual:",
        "bubble_buy_price" to "Harga beli per unit (Perak)",
        "bubble_sell_price" to "Harga jual per unit (Perak)",
        "bubble_bought_amount" to "Jumlah yang dibeli",
        "bubble_spent" to "Pengeluaran",
        "bubble_earned" to "Pendapatan Bersih",
        "bubble_profit_loss" to "Laba/Rugi",
        "bubble_include_brecilien" to "Sertakan Brecilien dalam rute",
        "bubble_compact_mode" to "📱 Mode Gelembung Kompak",
        "bubble_compact_desc" to "Mengecilkan ukuran gelembung dan teks",
        "bubble_opacity" to "👁️ Transparansi Gelembung",
        "bubble_scale" to "🔍 Skala Ukuran Gelembung",
        "tab_crafting" to "🛠️ Panduan Crafting",
        "tab_island" to "🏝️ Panduan Pulau",
        "tab_monsters" to "👹 Monster & Boss",
        "crafting_search" to "Cari item untuk dibuat...",
        "monster_search" to "Cari monster atau loot...",
        "crafting_req_ingredients" to "Bahan yang dibutuhkan & pasar termurah:",
        "cheapest_at" to "Pembelian termurah di",
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
        "red_zone" to "🔴 ZONA MERAH",
        "black_zone" to "⚫ ZONA HITAM"
    )

    private val plMap = mapOf(
        "app_title" to "DataPro - Market Companion (Unofficial)",
        "app_subtitle" to "Analiza Rynku w Czasie Rzeczywistym",
        "tab_orders" to "📋 Zamówienia i Historia",
        "tab_calculator" to "🧮 Kalkulator i Marża (Top 50)",
        "tab_catalog" to "📖 Katalog Rynku",
        "tab_builds" to "⚔️ Ekwipunek AI i Builds",
        "tab_order_stats" to "📊 Statystyki Zleceń (1D-1R)",
        "tab_events" to "🔥 Wydarzenia i Bossowie",
        "tab_gold" to "🪙 Rynek Złota",
        "tab_map" to "🗺️ Mapa Świata Albion",
        "server_select" to "Wybierz Serwer:",
        "silver_budget" to "Budżet Srebra",
        "carry_capacity" to "Udźwig (kg)",
        "min_margin" to "Minimalna Marża (%)",
        "premium_tax" to "Podatek Premium 4%",
        "standard_tax" to "Podatek Standardowy 8%",
        "avoid_danger" to "Unikaj Niebezpiecznych Stref (Caerleon / Czerwona)",
        "avoid_danger_desc" to "Wyklucza Caerleon i Strefy PvP",
        "top_opportunities" to "Najbardziej Opłacalne Okazje",
        "buy_in" to "Zakup w",
        "sell_in" to "Sprzedaż w",
        "quantity" to "Ilość",
        "weight" to "Waga",
        "investment" to "Inwestycja",
        "profit_per_unit" to "Zysk/Sztuka",
        "accept_order" to "Zaakceptuj i Zapisz Zlecenie",
        "gold_buy" to "🪙 Zakup Złota",
        "gold_sell" to "🪙 Sprzedaż Złota",
        "settings_title" to "⚙️ Ustawienia i Powiadomienia",
        "bubble_toggle" to "Pływająca Nakładka Bąbelkowa",
        "bubble_desc" to "Wyświetla bąbel nad aplikacją Albion Online",
        "sys_notif_toggle" to "Powiadomienia Systemowe Push",
        "sys_notif_desc" to "Alerts Android w czasie rzeczywistym",
        "gold_notif_toggle" to "Powiadomienia Portfolio Złota",
        "gold_notif_desc" to "Powiadomienia o zmianach kursu złota",
        "background_btn" to "🔋 Uruchamiaj w Tle Bez Ograniczeń",
        "lang_select" to "Język / Language:",
        "save" to "Zapisz",
        "close" to "Zamknij",
        "roi" to "Marża",
        "no_price_data" to "Brak ceny",
        "bubble_options" to "⚙️ Opcje Bąbla",
        "bubble_lang" to "Język:",
        "bubble_category" to "Filtr Kategorii:",
        "bubble_active_order" to "📦 Aktywne Zlecenie",
        "bubble_top_margin" to "🔥 Marża Top 3",
        "bubble_max_distance" to "Maksymalna Odległość",
        "bubble_min_stock" to "Min. Zapasy",
        "bubble_max_stock" to "Maks. Zapasy",
        "bubble_book" to "Zarezerwuj",
        "bubble_cancel" to "Anuluj",
        "bubble_abort" to "Przerwij",
        "bubble_save_book" to "Zapisz i Zarezerwuj",
        "bubble_location" to "🏙️ Lokalizacja:",
        "bubble_all_cities" to "Wszystkie Miasta",
        "bubble_all" to "Wszystko",
        "bubble_click_accept" to "Kliknij, aby zaakceptować",
        "bubble_calculating" to "Obliczanie okazji rynkowych...",
        "bubble_no_opps" to "Brak pasujących okazji rynkowych.",
        "bubble_search" to "Szukaj na rynku...",
        "bubble_hold_move" to "Przytrzymaj, aby przesunąć",
        "bubble_exit" to "Wyjście",
        "bubble_kauf" to "Zakup:",
        "bubble_verkauf" to "Sprzedaż:",
        "bubble_buy_price" to "Cena zakupu (Srebro)",
        "bubble_sell_price" to "Cena sprzedaży (Srebro)",
        "bubble_bought_amount" to "Kupiona ilość",
        "bubble_spent" to "Wydano",
        "bubble_earned" to "Przychód Netto",
        "bubble_profit_loss" to "Zysk/Strata",
        "bubble_include_brecilien" to "Uwzględnij Brecilien",
        "bubble_compact_mode" to "📱 Tryb Kompaktowy Bąbla",
        "bubble_compact_desc" to "Zmniejsza rozmiar bąbla i czcionkę",
        "bubble_opacity" to "👁️ Przezroczystość Bąbla",
        "bubble_scale" to "🔍 Skala Bąbla",
        "tab_crafting" to "🛠️ Przewodnik Wytwarzania",
        "tab_island" to "🏝️ Przewodnik Wyspy",
        "tab_monsters" to "👹 Potwory i Bossowie",
        "crafting_search" to "Szukaj przedmiotu...",
        "monster_search" to "Szukaj potwora lub lootu...",
        "crafting_req_ingredients" to "Wymagane zasoby i najtańszy rynek:",
        "cheapest_at" to "Najtańszy zakup",
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
        "red_zone" to "🔴 CZERWONA STREFA",
        "black_zone" to "⚫ CZARNA STREFA"
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

    @Suppress("DEPRECATION")
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
            .replace("Bitte Server-Zugangsdaten eingeben", "Ingrese credenciales", ignoreCase = true)
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
            .replace("Silber-Budget", "Budżet srebra", ignoreCase = true)
            .replace("Kaufpreis", "Cena zakupu", ignoreCase = true)
            .replace("Verkaufspreis", "Cena sprzedaży", ignoreCase = true)
            .replace("Gewinn", "Zysk", ignoreCase = true)
            .replace("Menge", "Ilość", ignoreCase = true)
            .replace("Speichern", "Zapisz", ignoreCase = true)
            .replace("Schließen", "Zamknij", ignoreCase = true)
            .replace("Einstellungen", "Ustawienia", ignoreCase = true)
            .replace("Standpunkt:", "Lokalizacja:", ignoreCase = true)
            .replace("Auftrag annehmen", "Zaakceptuj zlecenie", ignoreCase = true)
    }

    fun getCityTranslation(city: String, langCode: String): String {
        if (langCode == "DE") {
            val lower = city.lowercase(Locale.ROOT)
            if (lower.contains("smuggler") || lower.contains("schmuggler")) {
                if (lower.contains("gravemound") && lower.contains("gnoll")) {
                    return "Gravemound-Gnoll-Schmugglernetzwerk"
                }
                if (lower.contains("black market") || lower.contains("blackmarket") || lower.contains("schwarzmarkt")) {
                    return "Schwarzmarkt"
                }
                return "Schmugglernetzwerk"
            }
        }
        val key = "city_${city.lowercase().replace(" ", "_").replace("'", "")}"
        val trans = getString(key, langCode)
        return if (trans == key) city else trans
    }
}
