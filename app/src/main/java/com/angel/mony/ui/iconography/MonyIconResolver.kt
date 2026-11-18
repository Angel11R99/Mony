package com.angel.mony.ui.iconography

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.composables.icons.lucide.*
import com.angel.mony.ui.iconography.vendor.flatcolor.*
import com.angel.mony.ui.iconography.vendor.fluentemojiflat.*
import com.angel.mony.ui.iconography.vendor.mingcute.*

sealed interface MonyIconAsset {
    data class Tintable(
        val imageVector: ImageVector,
        val usesGlobalTint: Boolean = true,
    ) : MonyIconAsset
    data class FixedColor(val imageVector: ImageVector) : MonyIconAsset
}

object MonyIconResolver {
    fun resolve(icon: MonyIcon, pack: IconPack): MonyIconAsset =
        resolveOrNull(icon, pack) ?: MonyIconAsset.Tintable(
            imageVector = resolveMaterial(icon),
        )

    internal fun resolveOrNull(icon: MonyIcon, pack: IconPack): MonyIconAsset? = when (pack) {
        IconPack.MATERIAL -> MonyIconAsset.Tintable(resolveMaterial(icon))
        IconPack.LUCIDE -> resolveLucide(icon)?.let(MonyIconAsset::Tintable)
        IconPack.PHOSPHOR -> resolvePhosphor(icon)?.let(MonyIconAsset::Tintable)
        IconPack.MINGCUTE -> resolveMingCute(icon)?.let(MonyIconAsset::Tintable)
        IconPack.FLAT_COLOR -> resolveFlatColor(icon)?.let(MonyIconAsset::FixedColor)
        IconPack.FLUENT_EMOJI_FLAT -> FLUENT_EMOJI_FLAT_ICONS[icon]?.invoke()?.let(MonyIconAsset::FixedColor)
    }

    /** Selected Core Regular MingCute artwork. Unmapped semantic states keep the Material fallback. */
    private fun resolveMingCute(icon: MonyIcon): ImageVector? = MINGCUTE_ICONS[icon]?.invoke()

    /** The Icons8-derived flat pack keeps its original per-path colors and ignores global tint. */
    private fun resolveFlatColor(icon: MonyIcon): ImageVector? = FLAT_COLOR_ICONS[icon]?.invoke()

    private val MINGCUTE_ICONS: Map<MonyIcon, () -> ImageVector> = mapOf(
        MonyIcon.Home to ::mingCuteWallet4,
        MonyIcon.Fixed to ::mingCuteAlarm2,
        MonyIcon.Pending to ::mingCuteNotification,
        MonyIcon.Savings to ::mingCutePigMoney,
        MonyIcon.Shopping to ::mingCuteShoppingBag3,
        MonyIcon.Statistics to ::mingCuteChartBar,
        MonyIcon.Fortnight to ::mingCuteCalendar2,
        MonyIcon.Calendar to ::mingCuteCalendar2,
        MonyIcon.History to ::mingCuteHistory,
        MonyIcon.Settings to ::mingCuteSettings3,
        MonyIcon.Appearance to ::mingCutePalette,
        MonyIcon.Navigation to ::mingCuteFilter2,
        MonyIcon.Finance to ::mingCuteWallet,
        MonyIcon.Add to ::mingCuteAdd,
        MonyIcon.Edit to ::mingCuteEdit,
        MonyIcon.Delete to ::mingCuteDelete,
        MonyIcon.Search to ::mingCuteSearch,
        MonyIcon.More to ::mingCuteMore1,
        MonyIcon.Check to ::mingCuteCheck,
        MonyIcon.Close to ::mingCuteClose,
        MonyIcon.Warning to ::mingCuteWarning,
        MonyIcon.Info to ::mingCuteInformation,
        MonyIcon.Category to ::mingCuteTag,
        MonyIcon.Completed to ::mingCuteCheckCircle,
        MonyIcon.Expense to ::mingCuteBankCard,
        MonyIcon.Income to ::mingCuteHandCoins,
        MonyIcon.Food to ::mingCuteForkKnife,
        MonyIcon.Debt to ::mingCuteBankCard,
        MonyIcon.Entertainment to ::mingCuteGame2,
        MonyIcon.Family to ::mingCuteGroup,
        MonyIcon.Internet to ::mingCuteWifi,
        MonyIcon.AlertsDisabled to ::mingCuteNotificationOff,
        MonyIcon.Subscription to ::mingCuteRefresh3,
        MonyIcon.Transport to ::mingCuteCar2,
        MonyIcon.Housing to ::mingCuteHome3,
        MonyIcon.Lock to ::mingCuteLock,
        MonyIcon.Unlock to ::mingCuteUnlock,
        MonyIcon.Template to ::mingCutePackage2,
        MonyIcon.Save to ::mingCuteSave,
        MonyIcon.Filter to ::mingCuteFilter2,
        MonyIcon.Download to ::mingCuteDownload2,
        MonyIcon.Upload to ::mingCuteUpload2,
        MonyIcon.Share to ::mingCuteShare3,
        MonyIcon.Copy to ::mingCuteCopy,
        MonyIcon.Pin to ::mingCutePin,
        MonyIcon.Previous to ::mingCuteArrowLeft,
        MonyIcon.Next to ::mingCuteArrowRight,
        MonyIcon.Error to ::mingCuteWarning,
        MonyIcon.Time to ::mingCuteTime,
        MonyIcon.AlertsEnabled to ::mingCuteNotification,
        MonyIcon.Reopen to ::mingCuteRefresh3,
        MonyIcon.ScanBarcode to ::mingCuteScan,
        MonyIcon.Voice to ::mingCuteMic,
        MonyIcon.TrendUp to ::mingCuteArrowUp,
        MonyIcon.TrendDown to ::mingCuteArrowDown,
    )

    private val FLAT_COLOR_ICONS: Map<MonyIcon, () -> ImageVector> = mapOf(
        MonyIcon.Home to ::flatColorHome,
        MonyIcon.Fixed to ::flatColorAlarmClock,
        MonyIcon.Pending to ::flatColorAlarmClock,
        MonyIcon.Savings to ::flatColorSafe,
        MonyIcon.Shopping to ::flatColorShop,
        MonyIcon.Statistics to ::flatColorPieChart,
        MonyIcon.Fortnight to ::flatColorCalendar,
        MonyIcon.Calendar to ::flatColorCalendar,
        MonyIcon.History to ::flatColorClock,
        MonyIcon.Settings to ::flatColorSettings,
        MonyIcon.Appearance to ::flatColorSettings,
        MonyIcon.AlertsEnabled to ::flatColorAlarmClock,
        MonyIcon.Finance to ::flatColorMoneyTransfer,
        MonyIcon.Add to ::flatColorPlus,
        MonyIcon.Edit to ::flatColorEditImage,
        MonyIcon.Search to ::flatColorSearch,
        MonyIcon.Expense to ::flatColorMoneyTransfer,
        MonyIcon.Income to ::flatColorMoneyTransfer,
        MonyIcon.Education to ::flatColorReadingEbook,
        MonyIcon.Services to ::flatColorServices,
        MonyIcon.Phone to ::flatColorPhone,
        MonyIcon.Housing to ::flatColorHome,
        MonyIcon.Lock to ::flatColorLock,
        MonyIcon.Unlock to ::flatColorUnlock,
        MonyIcon.Template to ::flatColorBookmark,
        MonyIcon.Save to ::flatColorSafe,
        MonyIcon.Download to ::flatColorDownload,
        MonyIcon.Upload to ::flatColorUpload,
        MonyIcon.Share to ::flatColorShare,
        MonyIcon.Time to ::flatColorClock,
    )

    private val FLUENT_EMOJI_FLAT_ICONS: Map<MonyIcon, () -> ImageVector> = mapOf(
        MonyIcon.Home to ::fluentFlatHouse,
        MonyIcon.Fixed to ::fluentFlatAlarmClock,
        MonyIcon.Pending to ::fluentFlatBell,
        MonyIcon.Savings to ::fluentFlatMoneyBag,
        MonyIcon.Shopping to ::fluentFlatShoppingCart,
        MonyIcon.Statistics to ::fluentFlatBarChart,
        MonyIcon.Fortnight to ::fluentFlatCalendar,
        MonyIcon.Calendar to ::fluentFlatCalendar,
        MonyIcon.Settings to ::fluentFlatGear,
        MonyIcon.Appearance to ::fluentFlatArtistPalette,
        MonyIcon.Navigation to ::fluentFlatControlKnobs,
        MonyIcon.Finance to ::fluentFlatBank,
        MonyIcon.Edit to ::fluentFlatPencil,
        MonyIcon.Delete to ::fluentFlatWastebasket,
        MonyIcon.Search to ::fluentFlatMagnifyingGlassTiltedLeft,
        MonyIcon.Income to ::fluentFlatDollarBanknote,
        MonyIcon.Expense to ::fluentFlatCreditCard,
        MonyIcon.Food to ::fluentFlatForkAndKnife,
        MonyIcon.Debt to ::fluentFlatCreditCard,
        MonyIcon.Education to ::fluentFlatSchool,
        MonyIcon.Emergency to ::fluentFlatHospital,
        MonyIcon.Entertainment to ::fluentFlatGameDie,
        MonyIcon.Internet to ::fluentFlatGlobeWithMeridians,
        MonyIcon.Health to ::fluentFlatHospital,
        MonyIcon.Services to ::fluentFlatToolbox,
        MonyIcon.Subscription to ::fluentFlatRepeatButton,
        MonyIcon.Phone to ::fluentFlatTelephone,
        MonyIcon.Transport to ::fluentFlatAutomobile,
        MonyIcon.Housing to ::fluentFlatHouse,
        MonyIcon.Lock to ::fluentFlatLocked,
        MonyIcon.Unlock to ::fluentFlatUnlocked,
        MonyIcon.Save to ::fluentFlatFloppyDisk,
        MonyIcon.AlertsEnabled to ::fluentFlatBell,
        MonyIcon.AlertsDisabled to ::fluentFlatBellWithSlash,
        MonyIcon.TrendUp to ::fluentFlatChartIncreasing,
        MonyIcon.TrendDown to ::fluentFlatChartDecreasing,
    )

    private fun resolveMaterial(icon: MonyIcon): ImageVector = when (icon) {
        MonyIcon.Home -> Icons.Outlined.AccountBalanceWallet
        MonyIcon.Fixed -> Icons.Outlined.Repeat
        MonyIcon.Pending -> Icons.Outlined.Notifications
        MonyIcon.Savings -> Icons.Outlined.Savings
        MonyIcon.Shopping -> Icons.Outlined.ShoppingCart
        MonyIcon.Statistics -> Icons.Outlined.Insights
        MonyIcon.Fortnight, MonyIcon.Calendar -> Icons.Outlined.CalendarMonth
        MonyIcon.History -> Icons.Outlined.History
        MonyIcon.Settings -> Icons.Outlined.Settings
        MonyIcon.Appearance -> Icons.Outlined.Palette
        MonyIcon.Navigation -> Icons.Outlined.Tune
        MonyIcon.Finance -> Icons.Outlined.AccountBalance
        MonyIcon.Add -> Icons.Outlined.Add
        MonyIcon.Edit -> Icons.Outlined.Edit
        MonyIcon.Delete -> Icons.Outlined.Delete
        // Back always points toward the physical left on app bars, even when the
        // Android system layout direction is RTL.
        MonyIcon.Back -> Icons.Outlined.ArrowBack
        MonyIcon.Search -> Icons.Outlined.Search
        MonyIcon.More, MonyIcon.Other -> Icons.Outlined.MoreHoriz
        MonyIcon.Check -> Icons.Outlined.Check
        MonyIcon.Close -> Icons.Outlined.Close
        MonyIcon.Warning -> Icons.Outlined.WarningAmber
        MonyIcon.Info -> Icons.Outlined.Info
        MonyIcon.Category -> Icons.AutoMirrored.Outlined.Label
        MonyIcon.Completed -> Icons.Outlined.CheckCircle
        MonyIcon.Expense -> Icons.AutoMirrored.Outlined.ReceiptLong
        MonyIcon.Income -> Icons.Outlined.Payments
        MonyIcon.Food -> Icons.Outlined.Restaurant
        MonyIcon.Debt -> Icons.Outlined.CreditCard
        MonyIcon.Education -> Icons.Outlined.School
        MonyIcon.Emergency -> Icons.Outlined.Emergency
        MonyIcon.Entertainment -> Icons.Outlined.Movie
        MonyIcon.Family -> Icons.Outlined.FamilyRestroom
        MonyIcon.Internet -> Icons.Outlined.Wifi
        MonyIcon.Health -> Icons.Outlined.MedicalServices
        MonyIcon.Services -> Icons.Outlined.Receipt
        MonyIcon.Subscription -> Icons.Outlined.Subscriptions
        MonyIcon.Phone -> Icons.Outlined.PhoneAndroid
        MonyIcon.Transport -> Icons.Outlined.DirectionsCar
        MonyIcon.Housing -> Icons.Outlined.Home
        MonyIcon.Lock -> Icons.Outlined.Lock
        MonyIcon.Unlock -> Icons.Outlined.LockOpen
        MonyIcon.Template -> Icons.Outlined.Inventory2
        MonyIcon.Save -> Icons.Outlined.Save
        MonyIcon.Filter -> Icons.Outlined.FilterList
        MonyIcon.Download -> Icons.Outlined.FileDownload
        MonyIcon.Upload -> Icons.Outlined.FileUpload
        MonyIcon.Share -> Icons.Outlined.Share
        MonyIcon.Copy -> Icons.Outlined.ContentCopy
        MonyIcon.Undo -> Icons.AutoMirrored.Outlined.Undo
        MonyIcon.Pin -> Icons.Outlined.PushPin
        MonyIcon.Previous -> Icons.Outlined.ChevronLeft
        MonyIcon.Next -> Icons.Outlined.ChevronRight
        MonyIcon.Error -> Icons.Outlined.ErrorOutline
        MonyIcon.Dropdown -> Icons.Outlined.ArrowDropDown
        MonyIcon.ExpandMore -> Icons.Outlined.ExpandMore
        MonyIcon.ExpandLess -> Icons.Outlined.ExpandLess
        MonyIcon.Remove -> Icons.Outlined.Remove
        MonyIcon.Sort -> Icons.Outlined.SwapVert
        MonyIcon.Time -> Icons.Outlined.AccessTime
        MonyIcon.Notes -> Icons.AutoMirrored.Outlined.Notes
        MonyIcon.ListView -> Icons.Outlined.ViewAgenda
        MonyIcon.AlertsEnabled -> Icons.Outlined.NotificationsActive
        MonyIcon.AlertsDisabled -> Icons.Outlined.NotificationsOff
        MonyIcon.Reopen -> Icons.Outlined.Refresh
        MonyIcon.Restore -> Icons.Outlined.Restore
        MonyIcon.ScanBarcode -> Icons.Outlined.QrCodeScanner
        MonyIcon.ScanDocument -> Icons.Outlined.DocumentScanner
        MonyIcon.ScanPrice -> Icons.Outlined.PriceCheck
        MonyIcon.TrendUp -> Icons.Outlined.ArrowDropUp
        MonyIcon.TrendDown -> Icons.Outlined.ArrowDropDown
        MonyIcon.TrendFlat -> Icons.Outlined.Remove
        MonyIcon.Voice -> Icons.Outlined.Mic
        MonyIcon.Sparkles -> Icons.Outlined.AutoAwesome
    }

    private fun resolveLucide(icon: MonyIcon): ImageVector? = when (icon) {
        MonyIcon.Back -> null // Keep the fixed left-pointing Material app-bar arrow.
        MonyIcon.Home -> Lucide.Wallet
        MonyIcon.Fixed -> Lucide.Repeat2
        MonyIcon.Pending -> Lucide.Bell
        MonyIcon.Savings -> Lucide.PiggyBank
        MonyIcon.Shopping -> Lucide.ShoppingCart
        MonyIcon.Statistics -> Lucide.ChartNoAxesColumn
        MonyIcon.Fortnight, MonyIcon.Calendar -> Lucide.CalendarDays
        MonyIcon.History -> Lucide.History
        MonyIcon.Settings -> Lucide.Settings
        MonyIcon.Appearance -> Lucide.Palette
        MonyIcon.Navigation -> Lucide.SlidersHorizontal
        MonyIcon.Finance -> Lucide.Wallet
        MonyIcon.Add -> Lucide.Plus
        MonyIcon.Edit -> Lucide.Pencil
        MonyIcon.Delete -> Lucide.Trash2
        MonyIcon.Search -> Lucide.Search
        MonyIcon.More, MonyIcon.Other -> Lucide.Ellipsis
        MonyIcon.Check -> Lucide.Check
        MonyIcon.Close -> Lucide.X
        MonyIcon.Warning -> Lucide.TriangleAlert
        MonyIcon.Info -> Lucide.Info
        MonyIcon.Category -> Lucide.Tag
        MonyIcon.Completed -> Lucide.CircleCheck
        MonyIcon.Expense -> Lucide.ReceiptText
        MonyIcon.Income -> Lucide.HandCoins
        MonyIcon.Food -> Lucide.UtensilsCrossed
        MonyIcon.Debt -> Lucide.CreditCard
        MonyIcon.Education -> Lucide.GraduationCap
        MonyIcon.Emergency -> Lucide.Siren
        MonyIcon.Entertainment -> Lucide.Gamepad2
        MonyIcon.Family -> Lucide.Users
        MonyIcon.Internet -> Lucide.Wifi
        MonyIcon.Health -> Lucide.Stethoscope
        MonyIcon.Services -> Lucide.Wrench
        MonyIcon.Subscription -> Lucide.RefreshCw
        MonyIcon.Phone -> Lucide.Smartphone
        MonyIcon.Transport -> Lucide.Car
        MonyIcon.Housing -> Lucide.House
        MonyIcon.Lock -> Lucide.Lock
        MonyIcon.Unlock -> Lucide.LockOpen
        MonyIcon.Template -> Lucide.Package
        MonyIcon.Save -> Lucide.Save
        MonyIcon.Filter -> Lucide.ListFilter
        MonyIcon.Download -> Lucide.Download
        MonyIcon.Upload -> Lucide.Upload
        MonyIcon.Share -> Lucide.Share2
        MonyIcon.Copy -> Lucide.Copy
        MonyIcon.Undo -> Lucide.Undo2
        MonyIcon.Pin -> Lucide.Pin
        MonyIcon.Previous -> Lucide.ChevronLeft
        MonyIcon.Next -> Lucide.ChevronRight
        MonyIcon.Error -> Lucide.TriangleAlert
        MonyIcon.Dropdown, MonyIcon.ExpandMore, MonyIcon.ExpandLess, MonyIcon.Remove,
        MonyIcon.Sort, MonyIcon.Time, MonyIcon.Notes, MonyIcon.ListView,
        MonyIcon.AlertsDisabled, MonyIcon.Restore, MonyIcon.ScanBarcode,
        MonyIcon.ScanDocument, MonyIcon.ScanPrice, MonyIcon.TrendUp,
        MonyIcon.TrendDown, MonyIcon.TrendFlat -> null
        MonyIcon.AlertsEnabled -> Lucide.Bell
        MonyIcon.Reopen -> Lucide.RefreshCw
        MonyIcon.Voice, MonyIcon.Sparkles -> null // Material fallback.
    }

    private fun resolvePhosphor(icon: MonyIcon): ImageVector? = when (icon) {
        MonyIcon.Back -> null // Keep the fixed left-pointing Material app-bar arrow.
        MonyIcon.Home, MonyIcon.Finance -> PhosphorIcons.Regular.Wallet
        MonyIcon.Fixed -> PhosphorIcons.Regular.Repeat
        MonyIcon.Pending -> PhosphorIcons.Regular.Bell
        MonyIcon.Savings -> PhosphorIcons.Regular.PiggyBank
        MonyIcon.Shopping -> PhosphorIcons.Regular.ShoppingCart
        MonyIcon.Statistics -> PhosphorIcons.Regular.ChartBar
        MonyIcon.Fortnight, MonyIcon.Calendar -> PhosphorIcons.Regular.CalendarDots
        MonyIcon.History -> PhosphorIcons.Regular.ClockCounterClockwise
        MonyIcon.Settings -> PhosphorIcons.Regular.Gear
        MonyIcon.Appearance -> PhosphorIcons.Regular.Palette
        MonyIcon.Navigation -> PhosphorIcons.Regular.SlidersHorizontal
        MonyIcon.Add -> PhosphorIcons.Regular.Plus
        MonyIcon.Edit -> PhosphorIcons.Regular.Pencil
        MonyIcon.Delete -> PhosphorIcons.Regular.Trash
        MonyIcon.Search -> PhosphorIcons.Regular.MagnifyingGlass
        MonyIcon.More, MonyIcon.Other -> PhosphorIcons.Regular.DotsThree
        MonyIcon.Check -> PhosphorIcons.Regular.Check
        MonyIcon.Close -> PhosphorIcons.Regular.X
        MonyIcon.Warning -> PhosphorIcons.Regular.Warning
        MonyIcon.Info -> PhosphorIcons.Regular.Info
        MonyIcon.Category -> PhosphorIcons.Regular.Tag
        MonyIcon.Completed -> PhosphorIcons.Regular.CheckCircle
        MonyIcon.Expense -> PhosphorIcons.Regular.Receipt
        MonyIcon.Income -> PhosphorIcons.Regular.HandCoins
        MonyIcon.Food -> PhosphorIcons.Regular.ForkKnife
        MonyIcon.Debt -> PhosphorIcons.Regular.CreditCard
        MonyIcon.Education -> PhosphorIcons.Regular.GraduationCap
        MonyIcon.Emergency -> PhosphorIcons.Regular.Siren
        MonyIcon.Entertainment -> PhosphorIcons.Regular.GameController
        MonyIcon.Family -> PhosphorIcons.Regular.Users
        MonyIcon.Internet -> PhosphorIcons.Regular.WifiHigh
        MonyIcon.Health -> PhosphorIcons.Regular.Stethoscope
        MonyIcon.Services -> PhosphorIcons.Regular.Wrench
        MonyIcon.Subscription -> PhosphorIcons.Regular.ArrowsClockwise
        MonyIcon.Phone -> PhosphorIcons.Regular.DeviceMobile
        MonyIcon.Transport -> PhosphorIcons.Regular.Car
        MonyIcon.Housing -> PhosphorIcons.Regular.House
        MonyIcon.Lock -> PhosphorIcons.Regular.Lock
        MonyIcon.Unlock -> PhosphorIcons.Regular.LockOpen
        MonyIcon.Template -> PhosphorIcons.Regular.Package
        MonyIcon.Save -> PhosphorIcons.Regular.FloppyDisk
        MonyIcon.Filter -> PhosphorIcons.Regular.Funnel
        MonyIcon.Download -> PhosphorIcons.Regular.Download
        MonyIcon.Upload -> PhosphorIcons.Regular.Upload
        MonyIcon.Share -> PhosphorIcons.Regular.ShareNetwork
        MonyIcon.Copy -> PhosphorIcons.Regular.Copy
        MonyIcon.Undo -> PhosphorIcons.Regular.ArrowCounterClockwise
        MonyIcon.Pin -> PhosphorIcons.Regular.PushPin
        MonyIcon.Previous -> PhosphorIcons.Regular.CaretLeft
        MonyIcon.Next -> PhosphorIcons.Regular.CaretRight
        MonyIcon.Error -> PhosphorIcons.Regular.Warning
        MonyIcon.Dropdown, MonyIcon.ExpandMore, MonyIcon.ExpandLess, MonyIcon.Remove,
        MonyIcon.Sort, MonyIcon.Time, MonyIcon.Notes, MonyIcon.ListView,
        MonyIcon.AlertsDisabled, MonyIcon.Restore, MonyIcon.ScanBarcode,
        MonyIcon.ScanDocument, MonyIcon.ScanPrice, MonyIcon.TrendUp,
        MonyIcon.TrendDown, MonyIcon.TrendFlat -> null
        MonyIcon.AlertsEnabled -> PhosphorIcons.Regular.Bell
        MonyIcon.Reopen -> PhosphorIcons.Regular.ArrowsClockwise
        MonyIcon.Voice, MonyIcon.Sparkles -> null // Material fallback.
    }
}

