package com.example.albionmarketv2

object AlbionResourceRepository {
    private val _baseResources = mutableListOf<AlbionResource>().apply {
        add(AlbionResource("T4_ORE", "Eisenstein (T4)", "Iron Ore (T4)", 4, ResourceCategory.RESOURCES))
        add(AlbionResource("T5_ORE", "Titanerz (T5)", "Titanium Ore (T5)", 5, ResourceCategory.RESOURCES))
        add(AlbionResource("T6_ORE", "Runenerz (T6)", "Runite Ore (T6)", 6, ResourceCategory.RESOURCES))
        add(AlbionResource("T7_ORE", "Seelenerz (T7)", "Meteorite Ore (T7)", 7, ResourceCategory.RESOURCES))
        add(AlbionResource("T8_ORE", "Sternenerz (T8)", "Adamantium Ore (T8)", 8, ResourceCategory.RESOURCES))

        add(AlbionResource("T4_ROCK", "Kalkstein (T4)", "Limestone (T4)", 4, ResourceCategory.RESOURCES))
        add(AlbionResource("T5_ROCK", "Sandstein (T5)", "Sandstone (T5)", 5, ResourceCategory.RESOURCES))
        add(AlbionResource("T6_ROCK", "Granit (T6)", "Granite (T6)", 6, ResourceCategory.RESOURCES))
        add(AlbionResource("T7_ROCK", "Schiefer (T7)", "Slate (T7)", 7, ResourceCategory.RESOURCES))
        add(AlbionResource("T8_ROCK", "Basalt (T8)", "Basalt (T8)", 8, ResourceCategory.RESOURCES))

        add(AlbionResource("T4_WOOD", "Kastanienholz (T4)", "Chestnut Wood (T4)", 4, ResourceCategory.RESOURCES))
        add(AlbionResource("T5_WOOD", "Kiefernwald (T5)", "Pine Wood (T5)", 5, ResourceCategory.RESOURCES))
        add(AlbionResource("T6_WOOD", "Zedernholz (T6)", "Cedar Wood (T6)", 6, ResourceCategory.RESOURCES))
        add(AlbionResource("T7_WOOD", "Mahlholz (T7)", "Bloodwood (T7)", 7, ResourceCategory.RESOURCES))
        add(AlbionResource("T8_WOOD", "Eschenholz (T8)", "Ashenwood (T8)", 8, ResourceCategory.RESOURCES))

        add(AlbionResource("T4_FIBER", "Flachs (T4)", "Flax (T4)", 4, ResourceCategory.RESOURCES))
        add(AlbionResource("T5_FIBER", "Baumwolle (T5)", "Cotton (T5)", 5, ResourceCategory.RESOURCES))
        add(AlbionResource("T6_FIBER", "Hanf (T6)", "Hemp (T6)", 6, ResourceCategory.RESOURCES))
        add(AlbionResource("T7_FIBER", "Teufelskraut (T7)", "Duskleaf (T7)", 7, ResourceCategory.RESOURCES))
        add(AlbionResource("T8_FIBER", "Glanzkraut (T8)", "Wispflax (T8)", 8, ResourceCategory.RESOURCES))

        add(AlbionResource("T4_HIDE", "Gegerbtes Leder (T4)", "Cured Leather (T4)", 4, ResourceCategory.RESOURCES))
        add(AlbionResource("T5_HIDE", "Dunkles Leder (T5)", "Stiff Leather (T5)", 5, ResourceCategory.RESOURCES))
        add(AlbionResource("T6_HIDE", "Getarntes Leder (T6)", "Thick Leather (T6)", 6, ResourceCategory.RESOURCES))
        add(AlbionResource("T7_HIDE", "Gepanzertes Leder (T7)", "Worked Leather (T7)", 7, ResourceCategory.RESOURCES))
        add(AlbionResource("T8_HIDE", "Drachenleder (T8)", "Venerable Leather (T8)", 8, ResourceCategory.RESOURCES))
    }

    private val _dynamicResources = mutableListOf<AlbionResource>()

    val resources: List<AlbionResource>
        get() = (_baseResources + _dynamicResources).distinctBy { it.fullId }

    fun addDynamicResources(newItems: List<AlbionResource>) {
        for (item in newItems) {
            if (_baseResources.none { it.fullId == item.fullId } && _dynamicResources.none { it.fullId == item.fullId }) {
                _dynamicResources.add(item)
            }
        }
    }

    fun filterResources(
        category: ResourceCategory? = null,
        tier: Int = 0,
        enchantment: Int = -1,
        query: String = ""
    ): List<AlbionResource> {
        return resources.filter { res ->
            val matchCategory = category == null || category == ResourceCategory.ALL || res.category == category
            val matchTier = tier <= 0 || res.tier == tier
            val matchEnchantment = enchantment < 0 || res.enchantment == enchantment
            val matchQuery = query.isBlank() || res.nameDe.contains(query, true) || res.nameEn.contains(query, true) || res.id.contains(query, true)
            matchCategory && matchTier && matchEnchantment && matchQuery
        }
    }

    fun getFallbackMarketPrices(vararg args: Any?): List<MarketPrice> = emptyList()
    fun getCheapestMarketDetails(vararg args: Any?): MarketPrice? = null
    fun getHighestSellMarketDetails(vararg args: Any?): MarketPrice? = null
    fun getPriceInCity(vararg args: Any?): Int = 100
    fun fetchDynamicItemsFromAlbionBuilds(vararg args: Any?) {}
    fun generateLiveEvents(vararg args: Any?): List<LiveEventItem> = emptyList()
}
