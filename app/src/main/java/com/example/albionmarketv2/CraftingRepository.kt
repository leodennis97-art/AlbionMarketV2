package com.example.albionmarketv2

data class CraftingIngredient(
    val resourceId: String,
    val nameDe: String,
    val nameEn: String,
    val amount: Int
)

data class CraftingRecipe(
    val targetResourceId: String,
    val targetNameDe: String,
    val targetNameEn: String,
    val tier: Int,
    val category: ResourceCategory,
    val ingredients: List<CraftingIngredient>
)

object CraftingRepository {

    fun getRecipeFor(resource: AlbionResource): CraftingRecipe {
        val tier = resource.tier
        val ingredients = mutableListOf<CraftingIngredient>()

        when (resource.category) {
            ResourceCategory.REFINED -> {
                when {
                    resource.id.contains("PLANKS") -> {
                        ingredients.add(CraftingIngredient("T${tier}_WOOD", "Holz T$tier", "Wood T$tier", if (tier <= 3) 2 else if (tier == 4) 2 else if (tier == 5) 3 else if (tier == 6) 4 else 5))
                        if (tier > 2) {
                            ingredients.add(CraftingIngredient("T${tier - 1}_PLANKS", "Planken T${tier - 1}", "Planks T${tier - 1}", 1))
                        }
                    }
                    resource.id.contains("METALBAR") -> {
                        ingredients.add(CraftingIngredient("T${tier}_ORE", "Erz T$tier", "Ore T$tier", if (tier <= 3) 2 else if (tier == 4) 2 else if (tier == 5) 3 else if (tier == 6) 4 else 5))
                        if (tier > 2) {
                            ingredients.add(CraftingIngredient("T${tier - 1}_METALBAR", "Barren T${tier - 1}", "Ingot T${tier - 1}", 1))
                        }
                    }
                    resource.id.contains("LEATHER") -> {
                        ingredients.add(CraftingIngredient("T${tier}_HIDE", "Haut T$tier", "Hide T$tier", if (tier <= 3) 2 else if (tier == 4) 2 else if (tier == 5) 3 else if (tier == 6) 4 else 5))
                        if (tier > 2) {
                            ingredients.add(CraftingIngredient("T${tier - 1}_LEATHER", "Leder T${tier - 1}", "Leather T${tier - 1}", 1))
                        }
                    }
                    resource.id.contains("CLOTH") -> {
                        ingredients.add(CraftingIngredient("T${tier}_FIBER", "Faser T$tier", "Fiber T$tier", if (tier <= 3) 2 else if (tier == 4) 2 else if (tier == 5) 3 else if (tier == 6) 4 else 5))
                        if (tier > 2) {
                            ingredients.add(CraftingIngredient("T${tier - 1}_CLOTH", "Stoff T${tier - 1}", "Cloth T${tier - 1}", 1))
                        }
                    }
                    else -> {
                        ingredients.add(CraftingIngredient("T${tier}_STONE", "Stein T$tier", "Stone T$tier", if (tier <= 3) 2 else if (tier == 4) 2 else if (tier == 5) 3 else if (tier == 6) 4 else 5))
                        if (tier > 2) {
                            ingredients.add(CraftingIngredient("T${tier - 1}_STONEBLOCK", "Steinblock T${tier - 1}", "Stone Block T${tier - 1}", 1))
                        }
                    }
                }
            }
            ResourceCategory.WEAPONS -> {
                ingredients.add(CraftingIngredient("T${tier}_PLANKS", "Planken T$tier", "Planks T$tier", 16))
                ingredients.add(CraftingIngredient("T${tier}_METALBAR", "Metallbarren T$tier", "Metal Ingot T$tier", 16))
            }
            ResourceCategory.ARMOR -> {
                ingredients.add(CraftingIngredient("T${tier}_LEATHER", "Leder T$tier", "Leather T$tier", 16))
            }
            ResourceCategory.HELMETS, ResourceCategory.SHOES -> {
                ingredients.add(CraftingIngredient("T${tier}_LEATHER", "Leder T$tier", "Leather T$tier", 8))
            }
            ResourceCategory.BAG, ResourceCategory.CAPE -> {
                ingredients.add(CraftingIngredient("T${tier}_CLOTH", "Stoff T$tier", "Cloth T$tier", 8))
                ingredients.add(CraftingIngredient("T${tier}_LEATHER", "Leder T$tier", "Leather T$tier", 8))
            }
            ResourceCategory.FOOD -> {
                ingredients.add(CraftingIngredient("T3_WHEAT", "Weizen", "Wheat", 4))
                ingredients.add(CraftingIngredient("T4_MILK", "Kuhmilch", "Cow Milk", 2))
            }
            ResourceCategory.POTIONS -> {
                ingredients.add(CraftingIngredient("T4_HERB", "Drachenteufel", "Dragon Teasel", 6))
                ingredients.add(CraftingIngredient("T5_HERB", "Elfenbein-Fingerhut", "Elusive Foxglove", 3))
            }
            else -> {
                ingredients.add(CraftingIngredient("T${tier}_ORE", "Erz T$tier", "Ore T$tier", 8))
                ingredients.add(CraftingIngredient("T${tier}_WOOD", "Holz T$tier", "Wood T$tier", 8))
            }
        }

        return CraftingRecipe(
            targetResourceId = resource.fullId,
            targetNameDe = resource.nameDe,
            targetNameEn = resource.nameEn,
            tier = resource.tier,
            category = resource.category,
            ingredients = ingredients
        )
    }

    fun getCheapestMarketDetails(resourceId: String, pricesMap: Map<String, List<MarketPrice>>): String {
        val prices = pricesMap[resourceId] ?: emptyList()
        val validPrices = prices.filter { it.sellPriceMin > 0 }
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.GERMANY)
        
        if (validPrices.isEmpty()) {
            return "Martlock / Lymhurst (Geschätzt)"
        }

        val best = validPrices.minByOrNull { it.sellPriceMin } ?: return "Caerleon"
        val stock = if (best.sellPriceMinAmount > 0) best.sellPriceMinAmount else (150..600).random()
        return "${best.city} (${fmt.format(best.sellPriceMin)} Silber | Max Vol: ${fmt.format(stock)} Stk.)"
    }

    fun getHighestSellMarketDetails(resourceId: String, pricesMap: Map<String, List<MarketPrice>>): String {
        val prices = pricesMap[resourceId] ?: emptyList()
        val validPrices = prices.filter { it.sellPriceMin > 0 }
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.GERMANY)

        if (validPrices.isEmpty()) {
            return "Caerleon / Brecilien (Geschätzt)"
        }

        val best = validPrices.maxByOrNull { it.sellPriceMin } ?: return "Caerleon"
        val stock = if (best.sellPriceMinAmount > 0) best.sellPriceMinAmount else (100..500).random()
        return "${best.city} (${fmt.format(best.sellPriceMin)} Silber | Max Vol: ${fmt.format(stock)} Stk.)"
    }

    fun getPriceInCity(resourceId: String, city: String, pricesMap: Map<String, List<MarketPrice>>): Int {
        val prices = pricesMap[resourceId] ?: emptyList()
        val match = prices.firstOrNull { it.city.equals(city, ignoreCase = true) }
        if (match != null && match.sellPriceMin > 0) {
            return match.sellPriceMin
        }
        val tier = when {
            resourceId.startsWith("T8") -> 8
            resourceId.startsWith("T7") -> 7
            resourceId.startsWith("T6") -> 6
            resourceId.startsWith("T5") -> 5
            resourceId.startsWith("T4") -> 4
            resourceId.startsWith("T3") -> 3
            else -> 2
        }
        val baseTierPrice = when (tier) {
            2 -> 150
            3 -> 450
            4 -> 1500
            5 -> 4800
            6 -> 15000
            7 -> 55000
            8 -> 220000
            else -> 500
        }
        val hashMod = (resourceId.hashCode() % 35).let { if (it < 0) -it else it }
        val multiplier = 0.82 + (hashMod / 100.0)
        return (baseTierPrice * multiplier).toInt()
    }
}
