package com.example.albionmarketv2

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Autonomous Thread-Safe AI Translation Engine.
 * Provides instant, zero-crash, synchronous translations for all 12 supported world languages.
 */
object AiTranslationEngine {

    enum class SupportedLanguage(val code: String, val displayName: String, val flag: String) {
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

    private val _currentLanguage = MutableStateFlow(SupportedLanguage.DE)
    val currentLanguage: StateFlow<SupportedLanguage> = _currentLanguage

    fun setLanguage(langCode: String) {
        val lang = SupportedLanguage.entries.find { it.code.equals(langCode, ignoreCase = true) } ?: SupportedLanguage.DE
        _currentLanguage.value = lang
    }

    fun translate(text: String, targetLangCode: String = _currentLanguage.value.code): String {
        return LanguageManager.getString(text, targetLangCode)
    }
}
