package com.angel.mony.ui.iconography

import androidx.compose.ui.graphics.vector.ImageVector
import com.angel.mony.ui.iconography.vendor.iconpark.IconPark
import com.angel.mony.ui.iconography.vendor.iconpark.IconParkPalette
import com.angel.mony.ui.iconography.vendor.iconpark.addOne
import com.angel.mony.ui.iconography.vendor.iconpark.adjustment
import com.angel.mony.ui.iconography.vendor.iconpark.alarmClock
import com.angel.mony.ui.iconography.vendor.iconpark.bank
import com.angel.mony.ui.iconography.vendor.iconpark.bankCard
import com.angel.mony.ui.iconography.vendor.iconpark.bankTransfer
import com.angel.mony.ui.iconography.vendor.iconpark.bellRing
import com.angel.mony.ui.iconography.vendor.iconpark.calendar
import com.angel.mony.ui.iconography.vendor.iconpark.car
import com.angel.mony.ui.iconography.vendor.iconpark.chartPie
import com.angel.mony.ui.iconography.vendor.iconpark.config
import com.angel.mony.ui.iconography.vendor.iconpark.copy
import com.angel.mony.ui.iconography.vendor.iconpark.darkMode
import com.angel.mony.ui.iconography.vendor.iconpark.docDetail
import com.angel.mony.ui.iconography.vendor.iconpark.download
import com.angel.mony.ui.iconography.vendor.iconpark.edit
import com.angel.mony.ui.iconography.vendor.iconpark.filter
import com.angel.mony.ui.iconography.vendor.iconpark.health
import com.angel.mony.ui.iconography.vendor.iconpark.history
import com.angel.mony.ui.iconography.vendor.iconpark.home
import com.angel.mony.ui.iconography.vendor.iconpark.left
import com.angel.mony.ui.iconography.vendor.iconpark.lock
import com.angel.mony.ui.iconography.vendor.iconpark.minus
import com.angel.mony.ui.iconography.vendor.iconpark.moreTwo
import com.angel.mony.ui.iconography.vendor.iconpark.movie
import com.angel.mony.ui.iconography.vendor.iconpark.noodles
import com.angel.mony.ui.iconography.vendor.iconpark.notes
import com.angel.mony.ui.iconography.vendor.iconpark.order
import com.angel.mony.ui.iconography.vendor.iconpark.peoples
import com.angel.mony.ui.iconography.vendor.iconpark.phone
import com.angel.mony.ui.iconography.vendor.iconpark.pin
import com.angel.mony.ui.iconography.vendor.iconpark.redCross
import com.angel.mony.ui.iconography.vendor.iconpark.refreshOne
import com.angel.mony.ui.iconography.vendor.iconpark.right
import com.angel.mony.ui.iconography.vendor.iconpark.save
import com.angel.mony.ui.iconography.vendor.iconpark.school
import com.angel.mony.ui.iconography.vendor.iconpark.search
import com.angel.mony.ui.iconography.vendor.iconpark.shareOne
import com.angel.mony.ui.iconography.vendor.iconpark.shoppingCart
import com.angel.mony.ui.iconography.vendor.iconpark.strongbox
import com.angel.mony.ui.iconography.vendor.iconpark.tag
import com.angel.mony.ui.iconography.vendor.iconpark.time
import com.angel.mony.ui.iconography.vendor.iconpark.tool
import com.angel.mony.ui.iconography.vendor.iconpark.transaction
import com.angel.mony.ui.iconography.vendor.iconpark.undo
import com.angel.mony.ui.iconography.vendor.iconpark.unlock
import com.angel.mony.ui.iconography.vendor.iconpark.upload
import com.angel.mony.ui.iconography.vendor.iconpark.viewList
import com.angel.mony.ui.iconography.vendor.iconpark.wallet
import com.angel.mony.ui.iconography.vendor.iconpark.wifi

/** Builds a Mony Color vector for the outline colour the current theme asks for. */
typealias MonyColorVector = (IconParkPalette) -> ImageVector

/**
 * Mony Color artwork: real IconPark glyphs (IconPark by ByteDance, Apache-2.0) generated into
 * `ui/iconography/vendor/iconpark`.
 *
 * Only identity and action meanings are mapped. Semantic states and purely structural controls
 * are deliberately absent so they keep the Material fallback and stay tintable: error, warning,
 * success, selection and disabled colours belong to the color scheme, not to the artwork.
 */
internal val MONY_COLOR_VECTORS: Map<MonyIcon, MonyColorVector> = mapOf(
    // Navigation and destinations
    MonyIcon.Home to IconPark::wallet,
    MonyIcon.Fixed to IconPark::alarmClock,
    MonyIcon.Pending to IconPark::bellRing,
    MonyIcon.Savings to IconPark::strongbox,
    MonyIcon.Shopping to IconPark::shoppingCart,
    MonyIcon.Statistics to IconPark::chartPie,
    MonyIcon.Fortnight to IconPark::calendar,
    MonyIcon.Calendar to IconPark::calendar,
    MonyIcon.History to IconPark::history,
    MonyIcon.Settings to IconPark::config,
    MonyIcon.Appearance to IconPark::darkMode,
    MonyIcon.Navigation to IconPark::adjustment,
    MonyIcon.Finance to IconPark::bank,

    // Transaction identity
    MonyIcon.Expense to IconPark::transaction,
    MonyIcon.Income to IconPark::bankTransfer,

    // Categories
    MonyIcon.Food to IconPark::noodles,
    MonyIcon.Debt to IconPark::bankCard,
    MonyIcon.Education to IconPark::school,
    MonyIcon.Emergency to IconPark::redCross,
    MonyIcon.Entertainment to IconPark::movie,
    MonyIcon.Family to IconPark::peoples,
    MonyIcon.Internet to IconPark::wifi,
    MonyIcon.Health to IconPark::health,
    MonyIcon.Services to IconPark::tool,
    MonyIcon.Subscription to IconPark::refreshOne,
    MonyIcon.Phone to IconPark::phone,
    MonyIcon.Transport to IconPark::car,
    MonyIcon.Housing to IconPark::home,
    MonyIcon.More to IconPark::moreTwo,
    MonyIcon.Other to IconPark::moreTwo,

    // Actions
    MonyIcon.Add to IconPark::addOne,
    MonyIcon.Edit to IconPark::edit,
    MonyIcon.Search to IconPark::search,
    MonyIcon.Download to IconPark::download,
    MonyIcon.Upload to IconPark::upload,
    MonyIcon.Share to IconPark::shareOne,
    MonyIcon.Copy to IconPark::copy,
    MonyIcon.Undo to IconPark::undo,
    MonyIcon.Pin to IconPark::pin,
    MonyIcon.Filter to IconPark::filter,
    MonyIcon.Sort to IconPark::order,
    MonyIcon.Lock to IconPark::lock,
    MonyIcon.Unlock to IconPark::unlock,
    MonyIcon.Template to IconPark::docDetail,
    MonyIcon.Save to IconPark::save,
    MonyIcon.Time to IconPark::time,
    MonyIcon.Notes to IconPark::notes,
    MonyIcon.ListView to IconPark::viewList,
    MonyIcon.Category to IconPark::tag,
    MonyIcon.Previous to IconPark::left,
    MonyIcon.Next to IconPark::right,
    MonyIcon.Remove to IconPark::minus,
    MonyIcon.AlertsEnabled to IconPark::bellRing,
    MonyIcon.Reopen to IconPark::refreshOne,
)
