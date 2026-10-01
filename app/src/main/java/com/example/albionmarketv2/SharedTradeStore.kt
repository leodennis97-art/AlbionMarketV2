package com.example.albionmarketv2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SharedTradeStore {
    var latestOpportunities by mutableStateOf<List<TradeOpportunity>>(emptyList())
}
