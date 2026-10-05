package com.example.albionmarketv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object AlbionMarketApi {

    private const val CITIES = "Bridgewatch,Caerleon,FortSterling,Lymhurst,Martlock,Thetford,Brecilien,BlackMarket,ArthursRest,MerlynsRest,MorganasRest"

    fun isUnrealisticPrice(itemId: String, price: Int): Boolean {
        if (price < 5 || price > 500_000_000) return true
        val tier = when {
            itemId.startsWith("T8") -> 8
            itemId.startsWith("T7") -> 7
            itemId.startsWith("T6") -> 6
            itemId.startsWith("T5") -> 5
            itemId.startsWith("T4") -> 4
            itemId.startsWith("T3") -> 3
            itemId.startsWith("T2") -> 2
            else -> 1
        }
        val maxReasonablePrice = when (tier) {
            1, 2 -> 100_000
            3 -> 500_000
            4 -> 3_000_000
            5 -> 10_000_000
            6 -> 35_000_000
            7 -> 120_000_000
            8 -> 500_000_000
            else -> 100_000_000
        }
        return price > maxReasonablePrice
    }

    suspend fun fetchDynamicItemsFromAlbionBuilds(): List<AlbionResource> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AlbionResource>()
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://www.albiononlinebuilds.com/api/market/items")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connectTimeout = 5000 // Fast 5s timeout to prevent ANR freeze
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                if (responseText.trim().startsWith("[")) {
                    val jsonArray = JSONArray(responseText)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
                        val id = obj.optString("id", "")
                        val nameEn = obj.optString("name", id)
                        val nameDe = translateAlbionTermsToGerman(nameEn)
                        val categoryStr = obj.optString("category", "").lowercase()

                        val category = when {
                            categoryStr.contains("wood") || categoryStr.contains("ore") || categoryStr.contains("rock") || categoryStr.contains("stone") || categoryStr.contains("hide") || categoryStr.contains("leather") || categoryStr.contains("fiber") || categoryStr.contains("cloth") -> ResourceCategory.RESOURCES
                            categoryStr.contains("farm") || categoryStr.contains("food") -> ResourceCategory.FOOD
                            categoryStr.contains("potion") -> ResourceCategory.POTIONS
                            categoryStr.contains("planks") || categoryStr.contains("metalbar") || categoryStr.contains("refined") -> ResourceCategory.REFINED
                            categoryStr.contains("mount") -> ResourceCategory.MOUNTS
                            categoryStr.contains("weapon") || categoryStr.contains("mainhand") -> ResourceCategory.WEAPONS
                            categoryStr.contains("offhand") || categoryStr.contains("shield") -> ResourceCategory.OFFHAND
                            categoryStr.contains("helmet") || categoryStr.contains("head") -> ResourceCategory.HELMETS
                            categoryStr.contains("armor") -> ResourceCategory.ARMOR
                            categoryStr.contains("shoes") || categoryStr.contains("boots") -> ResourceCategory.SHOES
                            categoryStr.contains("bag") -> ResourceCategory.BAG
                            categoryStr.contains("cape") -> ResourceCategory.CAPE
                            categoryStr.contains("artifact") -> ResourceCategory.ARTIFACTS
                            else -> ResourceCategory.ALL
                        }

                        val tiers = obj.optJSONArray("tiers")
                        val tierList = mutableListOf<Int>()
                        if (tiers != null) {
                            for (t in 0 until tiers.length()) {
                                tierList.add(tiers.getInt(t))
                            }
                        } else {
                            tierList.add(4)
                        }

                        for (tier in tierList) {
                            val fullId = if (tier > 0 && !id.startsWith("T")) "T${tier}_$id" else id
                            val finalEnchantments = when (category) {
                                ResourceCategory.WEAPONS, ResourceCategory.HELMETS, ResourceCategory.ARMOR, ResourceCategory.SHOES, ResourceCategory.OFFHAND, ResourceCategory.REFINED -> if (tier >= 4) (0..4).toList() else listOf(0)
                                ResourceCategory.BAG, ResourceCategory.CAPE -> if (tier >= 4) (0..3).toList() else listOf(0)
                                ResourceCategory.FOOD, ResourceCategory.POTIONS -> (0..3).toList()
                                ResourceCategory.RESOURCES -> if (tier >= 4) (0..4).toList() else listOf(0)
                                else -> listOf(0)
                            }
                            
                            for (ench in finalEnchantments) {
                                list.add(
                                    AlbionResource(
                                        id = fullId,
                                        nameDe = nameDe,
                                        nameEn = nameEn,
                                        tier = if (tier in 1..8) tier else 4,
                                        category = category,
                                        enchantment = ench,
                                        description = "Live Item via albiononlinebuilds.com"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fail silently and use local static resource repository to prevent app freezing
        } finally {
            connection?.disconnect()
        }
        list
    }

    suspend fun fetchPrices(
        server: AlbionServer,
        itemIds: List<String>
    ): List<MarketPrice> = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<MarketPrice>()
        val chunks = itemIds.chunked(80)

        for (chunk in chunks) {
            val itemParam = chunk.joinToString(",")
            val primaryUrl = "${server.baseUrl}${itemParam}.json?locations=$CITIES&qualities=1,2,3,4,5"

            val chunkResults = tryFetchFromUrl(primaryUrl)
            results.addAll(chunkResults)
        }

        results
    }

    private fun tryFetchFromUrl(urlString: String): List<MarketPrice> {
        val list = mutableListOf<MarketPrice>()
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 // Fast 5s timeout to prevent ANR freeze
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                if (responseText.trim().startsWith("[")) {
                    val jsonArray = JSONArray(responseText)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
                        val itemId = obj.optString("item_id", obj.optString("ItemId", ""))
                        val city = obj.optString("city", obj.optString("City", ""))
                        val quality = obj.optInt("quality", obj.optInt("Quality", 1))
                        val sellPriceMin = obj.optInt("sell_price_min", obj.optInt("SellPriceMin", 0))
                        val sellPriceMinDate = obj.optString("sell_price_min_date", obj.optString("SellPriceMinDate", ""))
                        val sellPriceMinAmount = obj.optInt("sell_price_min_amount", obj.optInt("SellPriceMinAmount", 0))
                        val buyPriceMax = obj.optInt("buy_price_max", obj.optInt("BuyPriceMax", 0))
                        val buyPriceMaxDate = obj.optString("buy_price_max_date", obj.optString("BuyPriceMaxDate", ""))

                        if (itemId.isNotEmpty() && city.isNotEmpty()) {
                            if (sellPriceMin > 0 || buyPriceMax > 0) {
                                list.add(
                                    MarketPrice(
                                        itemId = itemId,
                                        city = city,
                                        quality = quality,
                                        sellPriceMin = sellPriceMin,
                                        sellPriceMinDate = sellPriceMinDate,
                                        buyPriceMax = buyPriceMax,
                                        buyPriceMaxDate = buyPriceMaxDate,
                                        sellPriceMinAmount = sellPriceMinAmount
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fail silently to prevent app freezing
        } finally {
            connection?.disconnect()
        }
        return list
    }

    fun getFallbackMarketPrices(): Map<String, List<MarketPrice>> {
        val map = mutableMapOf<String, List<MarketPrice>>()
        val cities = listOf("Bridgewatch", "Caerleon", "Fort Sterling", "Lymhurst", "Martlock", "Thetford", "Brecilien")

        for (res in AlbionResourceRepository.resources) {
            for (enc in 0..4) {
                val enchantedRes = if (enc > 0) res.copy(enchantment = enc) else res
                val basePrice = when (enchantedRes.tier) {
                    1 -> 50
                    2 -> 120
                    3 -> 350
                    4 -> 1200
                    5 -> 3800
                    6 -> 12500
                    7 -> 45000
                    8 -> 180000
                    else -> 1000
                }

                val encMult = when (enc) {
                    1 -> 2.2
                    2 -> 4.5
                    3 -> 9.0
                    4 -> 18.0
                    else -> 1.0
                }

                val list = mutableListOf<MarketPrice>()
                cities.forEachIndexed { _, city ->
                    val cityMult = when (city) {
                        "Caerleon" -> 1.35
                        "Bridgewatch" -> if (enchantedRes.category == ResourceCategory.RESOURCES) 0.85 else 1.0
                        "Lymhurst" -> if (enchantedRes.category == ResourceCategory.RESOURCES) 0.85 else 1.05
                        "Fort Sterling" -> if (enchantedRes.category == ResourceCategory.RESOURCES) 0.85 else 1.02
                        "Martlock" -> if (enchantedRes.category == ResourceCategory.RESOURCES) 0.85 else 1.03
                        "Thetford" -> if (enchantedRes.category == ResourceCategory.RESOURCES) 0.85 else 1.04
                        else -> 1.10
                    }
                    val sellPrice = (basePrice * encMult * cityMult).toInt()
                    list.add(
                        MarketPrice(
                            itemId = enchantedRes.fullId,
                            city = city,
                            quality = 1,
                            sellPriceMin = sellPrice,
                            sellPriceMinDate = "2026-03-28T04:00:00",
                            buyPriceMax = (sellPrice * 0.85).toInt(),
                            buyPriceMaxDate = "2026-03-28T04:00:00"
                        )
                    )
                }
                map[enchantedRes.fullId] = list
            }
        }
        return map
    }

    fun translateAlbionTermsToGerman(text: String): String {
        val foundByEn = AlbionResourceRepository.resources.find { it.nameEn.equals(text, ignoreCase = true) || it.id.equals(text, ignoreCase = true) }
        if (foundByEn != null) return foundByEn.nameDe

        return text
            .replace("Scholar Cowl", "Gelehrtenhaube", ignoreCase = true)
            .replace("Scholar Robe", "Gelehrtenrobe", ignoreCase = true)
            .replace("Scholar Sandals", "Gelehrtensandalen", ignoreCase = true)
            .replace("Mercenary Hood", "Söldnerkapuze", ignoreCase = true)
            .replace("Mercenary Jacket", "Söldnerjacke", ignoreCase = true)
            .replace("Mercenary Shoes", "Söldnerschuhe", ignoreCase = true)
            .replace("Soldier Helmet", "Soldatenhelm", ignoreCase = true)
            .replace("Soldier Armor", "Soldatenrüstung", ignoreCase = true)
            .replace("Soldier Boots", "Soldatenstiefel", ignoreCase = true)
            .replace("Cleric Cowl", "Klerikerhaube", ignoreCase = true)
            .replace("Cleric Robe", "Klerikerrobe", ignoreCase = true)
            .replace("Cleric Sandals", "Klerikersandalen", ignoreCase = true)
            .replace("Mage Cowl", "Magierhaube", ignoreCase = true)
            .replace("Mage Robe", "Magierrobe", ignoreCase = true)
            .replace("Mage Sandals", "Magiersandalen", ignoreCase = true)
            .replace("Hunter Hood", "Jägerkapuze", ignoreCase = true)
            .replace("Hunter Jacket", "Jägerjacke", ignoreCase = true)
            .replace("Hunter Shoes", "Jägerschuhe", ignoreCase = true)
            .replace("Assassin Hood", "Assassinenkapuze", ignoreCase = true)
            .replace("Assassin Jacket", "Assassinenjacke", ignoreCase = true)
            .replace("Assassin Shoes", "Assassinenschuhe", ignoreCase = true)
            .replace("Knight Helmet", "Ritterhelm", ignoreCase = true)
            .replace("Knight Armor", "Ritterrüstung", ignoreCase = true)
            .replace("Knight Boots", "Ritterstiefel", ignoreCase = true)
            .replace("Guardian Helmet", "Wächterhelm", ignoreCase = true)
            .replace("Guardian Armor", "Wächterrüstung", ignoreCase = true)
            .replace("Guardian Boots", "Wächterstiefel", ignoreCase = true)
            .replace("Broadsword", "Breitschwert", ignoreCase = true)
            .replace("Claymore", "Großschwert", ignoreCase = true)
            .replace("Battleaxe", "Kriegsaxt", ignoreCase = true)
            .replace("Crossbow", "Armbrust", ignoreCase = true)
            .replace("Fire Staff", "Feuerstab", ignoreCase = true)
            .replace("Holy Staff", "Heiliger Stab", ignoreCase = true)
            .replace("Nature Staff", "Naturstab", ignoreCase = true)
            .replace("Pine Planks", "Kiefernplanken", ignoreCase = true)
            .replace("Cedar Planks", "Zedernplanken", ignoreCase = true)
            .replace("Bloodoak Planks", "Bluteichenplanken", ignoreCase = true)
            .replace("Iron Bar", "Eisenbarren", ignoreCase = true)
            .replace("Titanium Bar", "Titanbarren", ignoreCase = true)
            .replace("Runite Bar", "Runenbarren", ignoreCase = true)
            .replace("Planks", "Planken", ignoreCase = true)
            .replace("Metal Bar", "Metallbarren", ignoreCase = true)
            .replace("Leather", "Leder", ignoreCase = true)
            .replace("Cloth", "Stoff", ignoreCase = true)
            .replace("Logs", "Stamm", ignoreCase = true)
            .replace("Ore", "Erz", ignoreCase = true)
            .replace("Hide", "Haut", ignoreCase = true)
            .replace("Fiber", "Faser", ignoreCase = true)
            .replace("Stone", "Stein", ignoreCase = true)
            .replace("Bag", "Tasche", ignoreCase = true)
            .replace("Cape", "Umhang", ignoreCase = true)
            .replace("Horse", "Pferd", ignoreCase = true)
            .replace("Ox", "Ochs", ignoreCase = true)
            .replace("Stew", "Eintopf", ignoreCase = true)
            .replace("Pie", "Pastete", ignoreCase = true)
            .replace("Omelette", "Omelett", ignoreCase = true)
            .replace("Salad", "Salat", ignoreCase = true)
            .replace("Soup", "Suppe", ignoreCase = true)
            .replace("Sandwich", "Sandwich", ignoreCase = true)
            .replace("Potion", "Trank", ignoreCase = true)
            .replace("Harvester Garb", "Ernter-Gewand", ignoreCase = true)
            .replace("Harvester Cap", "Ernter-Mütze", ignoreCase = true)
            .replace("Harvester Workboots", "Ernter-Arbeitsstiefel", ignoreCase = true)
            .replace("Harvester Backpack", "Ernter-Rucksack", ignoreCase = true)
            .replace("Miner Garb", "Bergmann-Gewand", ignoreCase = true)
            .replace("Miner Cap", "Bergmann-Mütze", ignoreCase = true)
            .replace("Miner Workboots", "Bergmann-Arbeitsstiefel", ignoreCase = true)
            .replace("Miner Backpack", "Bergmann-Rucksack", ignoreCase = true)
            .replace("Skinner Garb", "Häuter-Gewand", ignoreCase = true)
            .replace("Skinner Cap", "Häuter-Mütze", ignoreCase = true)
            .replace("Skinner Workboots", "Häuter-Arbeitsstiefel", ignoreCase = true)
            .replace("Skinner Backpack", "Häuter-Rucksack", ignoreCase = true)
            .replace("Quarryman Garb", "Steinmetz-Gewand", ignoreCase = true)
            .replace("Quarryman Cap", "Steinmetz-Mütze", ignoreCase = true)
            .replace("Quarryman Workboots", "Steinmetz-Arbeitsstiefel", ignoreCase = true)
            .replace("Quarryman Backpack", "Steinmetz-Rucksack", ignoreCase = true)
            .replace("Lumberjack Garb", "Holzfäller-Gewand", ignoreCase = true)
            .replace("Lumberjack Cap", "Holzfäller-Mütze", ignoreCase = true)
            .replace("Lumberjack Workboots", "Holzfäller-Arbeitsstiefel", ignoreCase = true)
            .replace("Lumberjack Backpack", "Holzfäller-Rucksack", ignoreCase = true)
            .replace("Fisherman Garb", "Fischer-Gewand", ignoreCase = true)
            .replace("Fisherman Cap", "Fischer-Mütze", ignoreCase = true)
            .replace("Fisherman Boots", "Fischer-Stiefel", ignoreCase = true)
            .replace("Fisherman Backpack", "Fischer-Rucksack", ignoreCase = true)
            .replace("Gatherer Garb", "Sammler-Gewand", ignoreCase = true)
            .replace("Gatherer Cap", "Sammler-Mütze", ignoreCase = true)
            .replace("Gatherer Boots", "Sammler-Stiefel", ignoreCase = true)
            .replace("Garb", "Gewand", ignoreCase = true)
            .replace("Workboots", "Arbeitsstiefel", ignoreCase = true)
    }
}
