package com.blackstack.beatbox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Ses kütüphanesi izinleri; Android 13+ bildirim iznini de kapsar. */
fun requiredAudioPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    // Android 9 ve öncesinde ID3 etiketi yazmak için yazma izni gerekir.
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ->
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/** Ses kütüphanesini okuma izni verilmiş mi? */
fun hasAudioPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Android 9 ve öncesinde etiket yazımı için gereken yazma iznini ister.
 *
 * Okuma izni zaten verilmiş kurulumlarda izin ekranı atlanacağı için bu etki
 * ayrıca çalışır. İzin reddedilirse uygulama çalışmaya devam eder; yalnızca
 * dosyaya etiket yazma atlanır.
 */
@Composable
fun RequestLegacyWritePermission(enabled: Boolean) {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
