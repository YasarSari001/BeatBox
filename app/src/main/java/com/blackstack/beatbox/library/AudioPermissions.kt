package com.blackstack.beatbox.library
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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

    // Android 10-12: Scoped Storage ile okuma.
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    // Android 9 ve öncesinde ID3 etiketi yazmak için yazma izni gerekir.
    else -> arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}

/** Ses kütüphanesini okuma izni verilmiş mi? */
fun hasAudioPermission(context: Context): Boolean =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

        else -> ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

/** Tüm dosyalara erişim izni (Android 11+). Scoped Storage dışındaki dosya işlemleri için. */
fun hasManageExternalStoragePermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
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

/**
 * Android 11+ için "Tüm dosyalara erişim" iznini ister.
 * Scoped Storage kapsamı dışındaki dosya işlemleri (ID3 yazma, kapak kaydetme vb.)
 * için gereklidir. İzin reddedilirse uygulama scoped storage sınırları içinde
 * çalışmaya devam eder.
 */
@Composable
fun RequestManageExternalStoragePermission(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(enabled) {
        if (!enabled || hasManageExternalStoragePermission(context)) return@LaunchedEffect
        val appSettings = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(Uri.parse("package:${context.packageName}"))
        val launched = runCatching { launcher.launch(appSettings) }.isSuccess
        if (!launched) {
            runCatching {
                launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}
