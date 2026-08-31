package com.angel.mony.presentation.background

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.angel.mony.R

@Immutable
private data class MedicalDecorationItem(
    @param:DrawableRes val resource: Int,
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float,
    val rotation: Float,
    val depthAlpha: Float,
)

private val medicalDecorationItems = listOf(
    MedicalDecorationItem(R.drawable.bg_medical_ambulance, 0.10f, 0.08f, 0.13f, -8f, 0.85f),
    MedicalDecorationItem(R.drawable.bg_medical_beaker, 0.34f, 0.06f, 0.10f, 12f, 0.75f),
    MedicalDecorationItem(R.drawable.bg_medical_electrocardiogram, 0.73f, 0.08f, 0.18f, 4f, 0.70f),
    MedicalDecorationItem(R.drawable.bg_medical_capsule, 0.93f, 0.16f, 0.11f, 22f, 0.95f),
    MedicalDecorationItem(R.drawable.bg_medical_clinic_building, 0.17f, 0.23f, 0.14f, 5f, 0.75f),
    MedicalDecorationItem(R.drawable.bg_medical_doctor, 0.50f, 0.19f, 0.11f, -5f, 0.85f),
    MedicalDecorationItem(R.drawable.bg_medical_folder, 0.79f, 0.27f, 0.10f, 10f, 0.75f),
    MedicalDecorationItem(R.drawable.bg_medical_infusion, 0.06f, 0.39f, 0.13f, -7f, 0.90f),
    MedicalDecorationItem(R.drawable.bg_medical_inject, 0.35f, 0.34f, 0.11f, -19f, 1f),
    MedicalDecorationItem(R.drawable.bg_medical_location, 0.64f, 0.40f, 0.09f, 7f, 0.75f),
    MedicalDecorationItem(R.drawable.bg_medical_medicine_bottle, 0.91f, 0.43f, 0.11f, 13f, 0.90f),
    MedicalDecorationItem(R.drawable.bg_medical_medicine_chest, 0.18f, 0.52f, 0.15f, -4f, 0.75f),
    MedicalDecorationItem(R.drawable.bg_medical_medicine_icon, 0.49f, 0.50f, 0.13f, 15f, 0.85f),
    MedicalDecorationItem(R.drawable.bg_medical_microscope, 0.78f, 0.55f, 0.15f, 6f, 0.80f),
    MedicalDecorationItem(R.drawable.bg_medical_nurse, 0.07f, 0.68f, 0.11f, -9f, 0.85f),
    MedicalDecorationItem(R.drawable.bg_medical_ointment, 0.35f, 0.65f, 0.10f, 11f, 0.80f),
    MedicalDecorationItem(R.drawable.bg_medical_stethoscope, 0.62f, 0.70f, 0.16f, -8f, 0.75f),
    MedicalDecorationItem(R.drawable.bg_medical_telephone, 0.92f, 0.73f, 0.10f, 9f, 0.85f),
    MedicalDecorationItem(R.drawable.bg_medical_test_tube, 0.20f, 0.87f, 0.10f, -14f, 0.95f),
    MedicalDecorationItem(R.drawable.bg_medical_wheelchair, 0.76f, 0.89f, 0.14f, 4f, 0.85f),
)

@Composable
internal fun MedicalBackground(alpha: Float) {
    if (alpha <= 0f) return

    BoxWithConstraints(Modifier.fillMaxSize().alpha(alpha)) {
        val shortSide = minOf(maxWidth, maxHeight)
        medicalDecorationItems.forEach { item ->
            val itemSize = (shortSide * item.sizeFraction).coerceIn(38.dp, 96.dp)
            Image(
                painter = painterResource(item.resource),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(
                        x = maxWidth * item.xFraction - itemSize / 2,
                        y = maxHeight * item.yFraction - itemSize / 2,
                    )
                    .size(itemSize)
                    .rotate(item.rotation)
                    .alpha(item.depthAlpha),
            )
        }
    }
}
