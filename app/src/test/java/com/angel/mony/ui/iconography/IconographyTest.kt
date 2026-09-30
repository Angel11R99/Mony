package com.angel.mony.ui.iconography

import androidx.compose.ui.graphics.Color
import com.angel.mony.ui.theme.AppAppearance
import com.angel.mony.ui.theme.monyColorPalette
import com.angel.mony.ui.theme.parseIconColorMode
import com.angel.mony.ui.theme.parseIconPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `every semantic icon resolves safely in every pack`() {
        MonyIcon.entries.forEach { icon ->
            IconPack.entries.forEach { pack ->
                val asset = MonyIconResolver.resolve(icon, pack)
                assertTrue(asset is MonyIconAsset.Tintable || asset is MonyIconAsset.Multicolor)
            }
        }
    }

    @Test
    fun `packs resolve different graphics without restarting`() {
        val material = MonyIconResolver.resolve(MonyIcon.Home, IconPack.MATERIAL)
        val lucide = MonyIconResolver.resolve(MonyIcon.Home, IconPack.LUCIDE)
        val phosphor = MonyIconResolver.resolve(MonyIcon.Home, IconPack.PHOSPHOR)
        val monyColor = MonyIconResolver.resolve(MonyIcon.Home, IconPack.MONY_COLOR)

        assertNotEquals(material.vector.name, lucide.vector.name)
        assertNotEquals(lucide.vector.name, phosphor.vector.name)
        assertTrue(monyColor is MonyIconAsset.Multicolor)
    }

    @Test
    fun `missing directional icon falls back to auto mirrored material`() {
        val material = MonyIconResolver.resolve(MonyIcon.Back, IconPack.MATERIAL)

        assertSame(material.vector, MonyIconResolver.resolve(MonyIcon.Back, IconPack.LUCIDE).vector)
        assertSame(material.vector, MonyIconResolver.resolve(MonyIcon.Back, IconPack.PHOSPHOR).vector)
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
        val blue = Color(monyColorPalette.single { it.id == "blue" }.argb)
        val config = IconographyConfig(colorMode = IconColorMode.CUSTOM, customColor = blue)

        assertEquals(blue, tint(config, background = Color.White))
        assertEquals(Color.Black, tint(config.copy(customColor = Color.White), background = Color.White))
        assertEquals(Color.White, tint(config.copy(customColor = Color.Black), local = Color.White, background = Color.Black))
    }

    @Test
    fun `Mony Color categories resolve to real multicolor assets`() {
        val categories = listOf(
            MonyIcon.Savings, MonyIcon.Food, MonyIcon.Shopping, MonyIcon.Debt,
            MonyIcon.Education, MonyIcon.Emergency, MonyIcon.Entertainment, MonyIcon.Family,
            MonyIcon.Internet, MonyIcon.Other, MonyIcon.Health, MonyIcon.Services,
            MonyIcon.Subscription, MonyIcon.Phone, MonyIcon.Transport, MonyIcon.Housing,
        )

        categories.forEach { icon ->
            assertTrue(MonyIconResolver.resolve(icon, IconPack.MONY_COLOR) is MonyIconAsset.Multicolor)
            assertTrue(MonyColorIconPainter.colorCount(icon) >= 3)
        }
    }

    @Test
    fun `Mony Color ignores monochrome tint while unsupported icons fall back safely`() {
        val savings = MonyIconResolver.resolve(MonyIcon.Savings, IconPack.MONY_COLOR)
        val fallback = MonyIconResolver.resolve(MonyIcon.Lock, IconPack.MONY_COLOR)

        assertTrue(savings is MonyIconAsset.Multicolor)
        assertFalse(savings is MonyIconAsset.Tintable)
        assertTrue(fallback is MonyIconAsset.Tintable)
        assertFalse((fallback as MonyIconAsset.Tintable).usesGlobalTint)
        assertSame(
            MonyIconResolver.resolve(MonyIcon.Lock, IconPack.MATERIAL).vector,
            fallback.vector,
        )
    }

    @Test
    fun `Mony Color covers main navigation and semantic states`() {
        val icons = listOf(
            MonyIcon.Home, MonyIcon.Fortnight, MonyIcon.Expense, MonyIcon.Income,
            MonyIcon.Savings, MonyIcon.Statistics, MonyIcon.Calendar, MonyIcon.Settings,
            MonyIcon.Warning, MonyIcon.Completed,
        )

        icons.forEach { icon ->
            assertTrue(MonyIconResolver.resolve(icon, IconPack.MONY_COLOR) is MonyIconAsset.Multicolor)
        }
        assertEquals(MonyColorPalette.Coral, MonyColorIconPainter.semanticAccent(MonyIcon.Delete))
        assertEquals(MonyColorPalette.Yellow, MonyColorIconPainter.semanticAccent(MonyIcon.Warning))
        assertEquals(MonyColorPalette.Mint, MonyColorIconPainter.semanticAccent(MonyIcon.Completed))
    }

    @Test
    fun `shared palette has stable unique options for every required color family`() {
        assertEquals(monyColorPalette.size, monyColorPalette.map { it.id }.distinct().size)
        assertEquals(monyColorPalette.size, monyColorPalette.map { it.argb }.distinct().size)
        assertEquals(
            setOf(
                "red", "pink", "purple", "violet", "blue", "cyan", "turquoise", "green",
                "lime", "yellow", "amber", "orange", "white", "gray", "black",
            ),
            monyColorPalette.map { it.id }.toSet(),
        )
    }

    @Test
    fun `custom purple is applied immediately by global tint resolution`() {
        val purple = Color(monyColorPalette.single { it.id == "purple" }.argb)

        assertEquals(
            purple,
            tint(IconographyConfig(IconPack.MATERIAL, IconColorMode.CUSTOM, purple)),
        )
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

    private val MonyIconAsset.vector
        get() = (this as MonyIconAsset.Tintable).imageVector
}
