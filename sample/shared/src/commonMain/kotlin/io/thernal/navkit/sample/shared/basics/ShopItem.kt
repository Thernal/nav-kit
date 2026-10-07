package io.thernal.navkit.sample.shared.basics

/** What the shop shows for a detail id. Presentation only — the route still carries just the id. */
internal data class ShopItem(
    val id: String,
    val emoji: String,
    val name: String,
    val blurb: String,
    val price: String,
    val material: String,
) {
    companion object {
        /** What the shop sells. */
        val all = listOf(
            ShopItem(
                id = "A",
                emoji = "☕",
                name = "Ceramic mug",
                blurb = "Hand-glazed stoneware, 350 ml",
                price = "€18",
                material = "Stoneware",
            ),
            ShopItem(
                id = "B",
                emoji = "👜",
                name = "Linen tote",
                blurb = "Natural linen, fits a 14\" laptop",
                price = "€24",
                material = "Linen",
            ),
        )
    }
}
