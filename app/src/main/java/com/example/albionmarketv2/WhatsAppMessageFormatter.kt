package com.example.albionmarketv2

import java.text.NumberFormat
import java.util.Locale

object WhatsAppMessageFormatter {

    private val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)

    fun formatOpportunityMessage(
        opp: TradeOpportunity,
        serverName: String,
        silverBudget: Long,
        carryCapacityKg: Double
    ): String {
        return """
⚔️ *ALBION ONLINE MARKT-ALERT* ⚔️
🌐 Server: *$serverName*

📦 *Gegenstand:* [${opp.resource.tierText}] ${opp.resource.nameDe} (${opp.resource.fullId})
📍 *Route:* *${opp.buyCity}* ➔ *${opp.sellCity}*

🛒 *Einkauf:* *${fmt.format(opp.buyPrice)} Silber* (${opp.buyCity})
🏷️ *Verkauf:* *${fmt.format(opp.sellPrice)} Silber* (${opp.sellCity})

📊 *Eingestellte Parameter:*
• Silber-Budget: *${fmt.format(silverBudget)} Silber*
• Tragkapazität: *${fmt.format(carryCapacityKg.toLong())} kg*

🚚 *Berechnete Transportmenge:*
• Menge: *${fmt.format(opp.tradeUnits)} Stk.*
• Gewicht: *${"%.1f".format(opp.totalWeightKg)} / ${fmt.format(carryCapacityKg.toLong())} kg*
• Gesamt-Investition: *${fmt.format(opp.totalInvestment)} Silber*

💰 *GESAMTGEWINN:*
➕ *+${fmt.format(opp.totalNetProfit)} Silber*
📈 ROI: *+${"%.1f".format(opp.roiPercent)}%*

_Generiert mit AlbionMarketV2 Echtzeit-Bot_
""".trimIndent()
    }
}
