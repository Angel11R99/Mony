package com.angel.mony.ui.iconography

import androidx.compose.ui.graphics.Color
import com.angel.mony.ui.theme.AppAppearance
import com.angel.mony.ui.theme.parseIconColorMode
import com.angel.mony.ui.theme.parseIconPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IconographyTest {
    @Test
    fun `existing user receives material icons with automatic color`() {
        val appearance = AppAppearance()

        assertEquals(IconPack.MATERIAL, appearance.iconPack)
        assertEquals(IconColorMode.AUTOMATIC, appearance.iconColorMode)
        assertEquals(IconPack.MATERIAL, parseIconPack(null))
        assertEquals(IconColorMode.AUTOMATIC, parseIconColorMode("unknown"))
    }

    @Test
    fun `stored pack names restore all supported packs`() {
        IconPack.entries.forEach { pack -> assertEquals(pack, parseIconPack(pack.name)) }
        IconColorMode.entries.forEach { mode -> assertEquals(mode, parseIconColorMode(mode.name)) }
    }

    @Test
    fun `packs resolve different graphics without restarting`() {
        val material = MonyIconResolver.resolve(MonyIcon.Home, IconPack.MATERIAL)
        val lucide = MonyIconResolver.resolve(MonyIcon.Home, IconPack.LUCIDE)
        val phosphor = MonyIconResolver.resolve(MonyIcon.Home, IconPack.PHOSPHOR)

        assertNotEquals(material.name, lucide.name)
        assertNotEquals(lucide.name, phosphor.name)
    }

    @Test
    fun `missing directional icon falls back to auto mirrored material`() {
        val material = MonyIconResolver.resolve(MonyIcon.Back, IconPack.MATERIAL)

        assertSame(material, MonyIconResolver.resolve(MonyIcon.Back, IconPack.LUCIDE))
        assertSame(material, MonyIconResolver.resolve(MonyIcon.Back, IconPack.PHOSPHOR))
    }

    @Test
    fun `every default category has a graphic in every pack`() {
        val categories = mapOf(
            "restaurant" to MonyIcon.Food,
            "directions_car" to MonyIcon.Transport,
            "home" to MonyIcon.Housing,
            "receipt_long" to MonyIcon.Services,
            "wifi" to MonyIcon.Internet,
            "phone_android" to MonyIcon.Phone,
            "medical_services" to MonyIcon.Health,
            "school" to MonyIcon.Education,
            "movie" to MonyIcon.Entertainment,
            "shopping_cart" to MonyIcon.Shopping,
            "credit_card" to MonyIcon.Debt,
            "subscriptions" to MonyIcon.Subscription,
            "family_restroom" to MonyIcon.Family,
            "savings" to MonyIcon.Savings,
            "emergency" to MonyIcon.Emergency,
            "more_horiz" to MonyIcon.Other,
        )

        categories.forEach { (key, expected) ->
            val semantic = semanticIconForCategory(key)
            assertEquals(expected, semantic)
            IconPack.entries.forEach { pack -> MonyIconResolver.resolve(semantic, pack) }
        }
    }

    @Test
    fun `automatic color follows light and dark context`() {
        val config = IconographyConfig(colorMode = IconColorMode.AUTOMATIC)

        assertEquals(Color.Black, tint(config, local = Color.Black, background = Color.White))
        assertEquals(Color.White, tint(config, local = Color.White, background = Color.Black))
    }

    @Test
    fun `primary mode follows current primary color`() {
        val config = IconographyConfig(colorMode = IconColorMode.PRIMARY)

        assertEquals(Color.Blue, tint(config, primary = Color.Blue))
        assertEquals(Color.Green, tint(config, primary = Color.Green))
    }

    @Test
    fun `custom color persists conceptually and falls back when contrast is insufficient`() {
        val custom = Color(0xFF0066CC)
        val config = IconographyConfig(colorMode = IconColorMode.CUSTOM, customColor = custom)

        assertEquals(custom, tint(config, background = Color.White))
        assertEquals(Color.Black, tint(config.copy(customColor = Color.White), background = Color.White))
    }

    @Test
    fun `state colors override global customization`() {
        val config = IconographyConfig(colorMode = IconColorMode.CUSTOM, customColor = Color.Blue)

        assertEquals(
            Color.Red,
            tint(config, role = MonyIconRole.STATE, local = Color.Red),
        )
        assertEquals(
            Color.Yellow,
            tint(config, requested = Color.Yellow, local = Color.Red),
        )
    }

    private fun tint(
        config: IconographyConfig,
        role: MonyIconRole = MonyIconRole.NORMAL,
        requested: Color = Color.Unspecified,
        local: Color = Color.Black,
        primary: Color = Color.Magenta,
        background: Color = Color.White,
    ) = resolveIconTint(config, role, requested, local, primary, background)
}
