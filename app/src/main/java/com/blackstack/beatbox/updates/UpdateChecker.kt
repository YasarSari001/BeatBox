/*
 * BeatBox Project (C) 2026
 *
 * GitHub üzerindeki tek bir JSON dosyasından (update.json) sürüm bilgisini
 * okur. Yeni sürüm yayınlamak için o dosyadaki versionCode/apkUrl alanlarını
 * güncellemek yeterlidir; uygulama açılışta dosyayı okuyup kullanıcıya
 * güncelleme teklif eder.
 */

package com.blackstack.beatbox.updates

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.blackstack.beatbox.BuildConfig
import com.blackstack.beatbox.i18n.appLanguageOf
import com.blackstack.beatbox.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** `update.json` dosyasının uygulamadaki karşılığı. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
)

object UpdateChecker {
    /**
     * Sürüm dosyası. Yayınlamak için bu dosyayı repoda güncellemek yeterli:
     * https://github.com/YasarSari001/BeatBox/blob/main/update.json
     */
    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/YasarSari001/BeatBox/main/update.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** JSON gövdesini ayrıştırır; zorunlu alanlar yoksa `null` döner. */
    fun parse(body: String): UpdateInfo? = try {
        val json = JSONObject(body)
        val code = json.optInt("versionCode", 0)
        val apk = json.optString("apkUrl").trim()
        if (code <= 0 || apk.isBlank()) {
            null
        } else {
            UpdateInfo(
                versionCode = code,
                versionName = json.optString("versionName").ifBlank { code.toString() },
                apkUrl = apk,
                changelog = json.optString("changelog").trim(),
            )
        }
    } catch (e: Exception) {
        null
    }

    private const val TAG = "UpdateChecker"

    /** Kontrolün sonucu; elle yapılan kontrolde kullanıcıya gösterilir. */
    sealed interface CheckResult {
        data class Available(val info: UpdateInfo) : CheckResult
        data object UpToDate : CheckResult
        data object Failed : CheckResult
    }

    /** Ağdan sürüm dosyasını okur; hata/bağlantı yoksa `null`. */
    private fun fetchBody(): String? = try {
        val request = Request.Builder()
            // CDN önbelleğini atlamak için zaman damgası eklenir.
            .url("$MANIFEST_URL?ts=${System.currentTimeMillis()}")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "update.json HTTP ${response.code}")
                return null
            }
            response.body?.string().orEmpty()
        }
    } catch (e: Exception) {
        Log.w(TAG, "update.json okunamadi", e)
        null
    }

    /** Güncelleme var mı, güncel mi, yoksa sunucuya ulaşılamadı mı? */
    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        val body = fetchBody() ?: return@withContext CheckResult.Failed
        val info = parse(body)
        if (info == null) {
            Log.w(TAG, "update.json ayristirilamadi")
            return@withContext CheckResult.Failed
        }
        Log.d(TAG, "update.json=${info.versionCode} current=${BuildConfig.VERSION_CODE}")
        if (isNewer(info)) CheckResult.Available(info) else CheckResult.UpToDate
    }

    /** Otomatik kontrol için kısa yol: yalnızca yeni sürüm varsa döner. */
    suspend fun fetch(): UpdateInfo? = (check() as? CheckResult.Available)?.info

    fun isNewer(info: UpdateInfo): Boolean = info.versionCode > BuildConfig.VERSION_CODE
}

/**
 * APK'yı DownloadManager ile indirir, tamamlanınca paket yükleyiciyi açar.
 * Bilinmeyen kaynak izni yoksa kullanıcıyı ilgili ayar ekranına yönlendirir.
 */
object UpdateInstaller {

    fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        val strings = Strings(appLanguageOf(activity))

        if (!activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, strings.get("update_install_permission"), Toast.LENGTH_LONG).show()
            runCatching {
                activity.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                )
            }
            return
        }

        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "BeatBox-${info.versionName}.apk"
        val file = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("BeatBox ${info.versionName}")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                activity,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        val downloadId = try {
            manager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(activity, strings.get("update_install_failed"), Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(activity, strings.get("update_downloading"), Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return
                runCatching { context.unregisterReceiver(this) }

                // İndirme gerçekten tamamlandı mı? Hata olduysa sessiz kalma.
                val successful = runCatching {
                    manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                                DownloadManager.STATUS_SUCCESSFUL
                        } else false
                    } ?: false
                }.getOrDefault(false)

                val uri = if (successful) {
                    runCatching {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }.getOrElse { manager.getUriForDownloadedFile(downloadId) }
                } else null

                if (uri == null) {
                    Toast.makeText(
                        context,
                        strings.get("update_install_failed"),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, APK_MIME)
                    clipData = ClipData.newRawUri(null, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val opened = runCatching { context.startActivity(install) }.isSuccess
                if (!opened) {
                    Toast.makeText(context, strings.get("update_install_failed"), Toast.LENGTH_LONG).show()
                }
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private const val APK_MIME = "application/vnd.android.package-archive"
}
