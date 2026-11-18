package com.angel.mony.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.angel.mony.presentation.components.FinanceCard

@Composable
fun VoiceRecognitionSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var service by remember(context) { mutableStateOf(activeVoiceRecognitionService(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) service = activeVoiceRecognitionService(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val currentService = service

    Column(Modifier.fillMaxSize()) {
        SettingsModuleHeader(title = "Voz y dictado", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Mony usa el reconocedor de voz instalado en tu dispositivo. Si no reconoce español, configúralo aquí y vuelve a probar el micrófono.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Servicio activo", style = MaterialTheme.typography.titleMedium)
                        Text(
                            currentService?.label ?: "No se pudo identificar el servicio predeterminado.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "El menú y los idiomas disponibles dependen de Android, del fabricante y del servicio instalado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                FinanceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Pasos para habilitar español", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "1. En los ajustes del teclado, abre el teclado que usas y busca Dictado por voz o Entrada de voz.\n\n" +
                                "2. En los ajustes del servicio activo, busca Reconocimiento de voz sin conexión, Idiomas o Modelos de voz.\n\n" +
                                "3. Descarga una variante de Español. Si no aparece, actualiza el servicio desde la tienda del dispositivo y vuelve a revisar.\n\n" +
                                "4. Regresa a Mony. Si el dispositivo ofrece reconocimiento convencional, puedes usarlo con Internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { openDeviceSettings(context, Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abrir ajustes del teclado")
                }
            }
            if (currentService != null) {
                item {
                    OutlinedButton(
                        onClick = { openServiceDetails(context, currentService.packageName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Abrir información del servicio")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { openServiceStore(context, currentService.packageName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Buscar actualización del servicio")
                    }
                }
            }
        }
    }
}

private data class VoiceServiceInfo(val packageName: String, val label: String)

private fun activeVoiceRecognitionService(context: Context): VoiceServiceInfo? {
    val component = Settings.Secure.getString(
        context.contentResolver,
        VOICE_RECOGNITION_SERVICE_SETTING,
    ) ?: return null
    val packageName = component.substringBefore('/').takeIf(String::isNotBlank) ?: return null
    val appInfo = try {
        context.packageManager.getApplicationInfo(packageName, 0)
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
        return null
    }
    val label = context.packageManager.getApplicationLabel(appInfo).toString()
    return VoiceServiceInfo(packageName, label)
}

private const val VOICE_RECOGNITION_SERVICE_SETTING = "voice_recognition_service"

private fun openDeviceSettings(context: Context, preferred: Intent) {
    if (!launchSettings(context, preferred) && !launchSettings(context, Intent(Settings.ACTION_SETTINGS))) {
        Toast.makeText(context, "No se pudieron abrir los ajustes del dispositivo.", Toast.LENGTH_LONG).show()
    }
}

private fun openServiceDetails(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    if (!launchSettings(context, intent)) {
        Toast.makeText(context, "No se encontró la ficha del servicio de voz.", Toast.LENGTH_LONG).show()
    }
}

private fun openServiceStore(context: Context, packageName: String) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    if (launchSettings(context, marketIntent)) return
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
    )
    if (!launchSettings(context, webIntent)) {
        Toast.makeText(context, "No se encontró una tienda para actualizar el servicio.", Toast.LENGTH_LONG).show()
    }
}

private fun launchSettings(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
