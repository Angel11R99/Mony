package com.angel.mony.ui.iconography

import androidx.compose.ui.graphics.Color
import com.angel.mony.ui.theme.AppAppearance
import com.angel.mony.ui.theme.monyColorPalette
import com.angel.mony.ui.theme.parseIconColorMode
import com.angel.mony.ui.theme.parseIconPack
import org.junit.Assert.*
import org.junit.Test

class IconographyTest {
    @Test fun `existing user receives material icons with automatic color`() {
        val appearance = AppAppearance()
        assertEquals(IconPack.MATERIAL, appearance.iconPack)
        assertEquals(IconColorMode.AUTOMATIC, appearance.iconColorMode)
        assertEquals(IconPack.MATERIAL, parseIconPack(null))
        assertEquals(IconPack.MATERIAL, parseIconPack("MONY_COLOR"))
        assertEquals(IconColorMode.AUTOMATIC, parseIconColorMode("unknown"))
    }

    @Test fun `stored pack names restore all supported packs`() {
        IconPack.entries.forEach { assertEquals(it, parseIconPack(it.name)) }
        IconColorMode.entries.forEach { assertEquals(it, parseIconColorMode(it.name)) }
    }

    @Test fun `every semantic icon resolves safely in every pack`() {
        MonyIcon.entries.forEach { icon -> IconPack.entries.forEach { pack ->
            val asset = MonyIconResolver.resolve(icon, pack)
            assertTrue(asset is MonyIconAsset.Tintable || asset is MonyIconAsset.FixedColor)
        } }
    }

    @Test fun `packs resolve different graphics without restarting`() {
        val material = MonyIconResolver.resolve(MonyIcon.Home, IconPack.MATERIAL) as MonyIconAsset.Tintable
        val lucide = MonyIconResolver.resolve(MonyIcon.Home, IconPack.LUCIDE) as MonyIconAsset.Tintable
        val phosphor = MonyIconResolver.resolve(MonyIcon.Home, IconPack.PHOSPHOR) as MonyIconAsset.Tintable
        val flat = MonyIconResolver.resolve(MonyIcon.Home, IconPack.FLAT_COLOR) as MonyIconAsset.FixedColor
        assertNotEquals(material.imageVector.name, lucide.imageVector.name)
        assertNotEquals(lucide.imageVector.name, phosphor.imageVector.name)
        assertNotEquals(phosphor.imageVector.name, flat.imageVector.name)
    }

    @Test fun `colorful packs retain fixed colors and expose colorful category icons`() {
        listOf(IconPack.FLAT_COLOR, IconPack.FLUENT_EMOJI_FLAT).forEach { pack ->
            listOf(MonyIcon.Savings, MonyIcon.Food, MonyIcon.Transport).forEach { icon ->
                assertTrue(MonyIconResolver.resolve(icon, pack) is MonyIconAsset.FixedColor)
            }
        }
    }

    @Test fun `missing directional icon falls back to auto mirrored material`() {
        val material = MonyIconResolver.resolve(MonyIcon.Back, IconPack.MATERIAL) as MonyIconAsset.Tintable
        assertSame(material.imageVector, (MonyIconResolver.resolve(MonyIcon.Back, IconPack.LUCIDE) as MonyIconAsset.Tintable).imageVector)
        assertSame(material.imageVector, (MonyIconResolver.resolve(MonyIcon.Back, IconPack.PHOSPHOR) as MonyIconAsset.Tintable).imageVector)
    }

    @Test fun `custom purple is applied immediately by global tint resolution`() {
        val purple = Color(monyColorPalette.single { it.id == "purple" }.argb)
        assertEquals(purple, tint(IconographyConfig(IconPack.MATERIAL, IconColorMode.CUSTOM, purple)))
    }

    @Test fun `state colors override global customization`() {
        val config = IconographyConfig(colorMode = IconColorMode.CUSTOM, customColor = Color.Blue)
        assertEquals(Color.Red, tint(config, role = MonyIconRole.STATE, local = Color.Red))
        assertEquals(Color.Yellow, tint(config, requested = Color.Yellow, local = Color.Red))
    }

    private fun tint(config: IconographyConfig, role: MonyIconRole = MonyIconRole.NORMAL,
        requested: Color = Color.Unspecified, local: Color = Color.Black,
        primary: Color = Color.Magenta, background: Color = Color.White) =
        resolveIconTint(config, role, requested, local, primary, background)
}
