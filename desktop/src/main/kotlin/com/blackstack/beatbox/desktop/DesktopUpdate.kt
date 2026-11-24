package com.blackstack.beatbox.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

/*
 * Masaüstü güncelleme denetimi.
 *
 * Android sürümüyle aynı `update.json` dosyası okunur: oradaki versionCode
 * bu uygulamanın sürüm koduyla karşılaştırılır. İndirme adresi dosyadan
 * gelmez; yayın dosyalarının adları sürümden bağımsız olduğu için
 * (BeatBox-windows.exe) adres her sürümde geçerli kalır.
 */

/** `update.json` içindeki yayın bilgisi. */
data class DesktopUpdate(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
)

object DesktopUpdateChecker {
    /**
     * Uygulamanın kendi sürümü. Değerler elle tutulmaz: build sırasında
     * `version.properties` dosyasından üretilen `BeatBoxVersion`'dan gelir.
     */
    const val VERSION_CODE = BeatBoxVersion.CODE
    const val VERSION_NAME = BeatBoxVersion.NAME

    /**
     * Sabit asset adı: her yeni sürümde aynı dosya adı yüklendiği için bu
     * adres sürümden bağımsız olarak çalışır.
     */
    const val DOWNLOAD_URL =
        "https://github.com/YasarSari001/BeatBox/releases/latest/download/BeatBox-windows.exe"

    /** Sürüm dosyası; Android tarafıyla aynı kaynak. */
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/YasarSari001/BeatBox/main/update.json"

    /**
     * Öncelikli kaynak: raw.githubusercontent.com dosyayı dakikalarca
     * önbellekte tutabildiği için GitHub Contents API'si önce denenir.
     */
    private const val API_URL =
        "https://api.github.com/repos/YasarSari001/BeatBox/contents/update.json"

    private const val USER_AGENT = "BeatBox-Desktop"

    /** Geçici kurulum dosyalarının ad kalıbı (temizlik için ortak tutulur). */
    private const val INSTALLER_PREFIX = "BeatBox-update-"
    private const val INSTALLER_SUFFIX = ".exe"

    /** Denetimin sonucu; Hakkında ekranında gösterilir. */
    sealed interface Result {
        data class Available(val update: DesktopUpdate) : Result
        data object UpToDate : Result
        data object Failed : Result
    }

    /** Ağdan sürüm dosyasını okuyup karşılaştırır. */
    suspend fun check(): Result = withContext(Dispatchers.IO) {
        val body = fetchBody() ?: return@withContext Result.Failed
        val update = parseManifest(body) ?: return@withContext Result.Failed
        if (isNewer(update)) Result.Available(update) else Result.UpToDate
    }

    /** Yayındaki sürüm bu uygulamadan yeni mi? */
    fun isNewer(update: DesktopUpdate): Boolean = update.versionCode > VERSION_CODE

    /**
     * Kurulum paketini indirir ve geçici dosyayı döner.
     *
     * `onProgress` 0..1 arasında ilerleme bildirir; iptal edilirse yarım kalan
     * dosya silinir.
     */
    suspend fun downloadInstaller(onProgress: suspend (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val target = File.createTempFile(INSTALLER_PREFIX, INSTALLER_SUFFIX)
            var connection: HttpURLConnection? = null
            try {
                connection = (URI.create(DOWNLOAD_URL).toURL().openConnection() as HttpURLConnection)
                    .apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("Accept", "application/octet-stream")
                        setRequestProperty("Cache-Control", "no-cache")
                    }
                if (connection.responseCode !in 200..299) {
                    throw IOException("HTTP ${connection.responseCode}")
                }
                val total = connection.contentLengthLong
                connection.inputStream.use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (total > 0) {
                                onProgress((written.toDouble() / total).toFloat().coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                target
            } catch (e: Throwable) {
                target.delete()
                throw e
            } finally {
                connection?.disconnect()
            }
        }

    /**
     * İndirilen kurulum paketini uygulama kapandıktan sonra başlatır.
     *
     * Kurulum dosyaları çalışan uygulama tarafından kilitli olduğu için
     * sabit bir gecikme yerine bu sürecin gerçekten bitmesi beklenir: Windows'ta
     * `Wait-Process`, diğer platformlarda `kill -0` yoklaması. Bekleme ayrı bir
     * yardımcı süreçte yapıldığı için çağıran taraf hemen uygulamayı kapatabilir.
     */
    fun installAfterExit(installer: File): Boolean = runCatching {
        val pid = ProcessHandle.current().pid()
        val path = installer.absolutePath
        val helper = if (isWindows()) {
            val quoted = path.replace("'", "''")
            listOf(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-WindowStyle",
                "Hidden",
                "-Command",
                "Wait-Process -Id $pid -ErrorAction SilentlyContinue; " +
                    "Start-Process -FilePath '$quoted'",
            )
        } else {
            listOf(
                "sh",
                "-c",
                "while kill -0 $pid 2>/dev/null; do sleep 1; done; \"$path\" &",
            )
        }
        ProcessBuilder(helper).start()
        true
    }.getOrDefault(false)

    /**
     * Önceki oturumlardan kalan geçici kurulum dosyalarını siler.
     *
     * Kurulum başlatıldıktan sonra dosya yerinde kalır (kurulum sihirbazı
     * onu çalıştırır), bu yüzden gerçek temizlik bir sonraki açılışta yapılır.
     */
    fun cleanupTempInstallers(): Int {
        val dir = File(System.getProperty("java.io.tmpdir")) ?: return 0
        val stale = dir.listFiles { file ->
            file.isFile && file.name.startsWith(INSTALLER_PREFIX) && file.name.endsWith(INSTALLER_SUFFIX)
        } ?: return 0
        return stale.count { it.delete() }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    /** JSON gövdesini ayrıştırır; zorunlu alanlar yoksa `null` döner. */
    fun parseManifest(body: String): DesktopUpdate? {
        val fields = mutableMapOf<String, String>()
        STRING_FIELD.findAll(body).forEach { match ->
            fields.putIfAbsent(match.groupValues[1], unescape(match.groupValues[2]))
        }
        val code = NUMBER_FIELD.find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: fields["versionCode"]?.toIntOrNull()
            ?: return null
        if (code <= 0) return null
        return DesktopUpdate(
            versionCode = code,
            versionName = fields["versionName"]?.trim()?.ifBlank { code.toString() }
                ?: code.toString(),
            changelog = fields["changelog"]?.trim().orEmpty(),
        )
    }

    private fun fetchBody(): String? = fetchFromApi() ?: fetchFromRaw()

    /** Contents API dosyayı base64 içerikle döner. */
    private fun fetchFromApi(): String? {
        val body = readText(
            "$API_URL?ts=${System.currentTimeMillis()}",
            "application/vnd.github+json",
        ) ?: return null
        val encoded = parseField(body, "content")?.filterNot { it.isWhitespace() }
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            String(Base64.getMimeDecoder().decode(encoded), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun fetchFromRaw(): String? =
        readText("$MANIFEST_URL?ts=${System.currentTimeMillis()}")

    private fun parseField(body: String, key: String): String? =
        STRING_FIELD.findAll(body)
            .firstOrNull { it.groupValues[1] == key }
            ?.let { unescape(it.groupValues[2]) }

    private fun readText(url: String, accept: String? = null): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                // CDN önbelleğini atlamak için zaman damgası ve başlıklar kullanılır.
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("User-Agent", USER_AGENT)
                accept?.let { setRequestProperty("Accept", it) }
            }
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** JSON kaçış dizilerini (\n, \", \uXXXX …) çözer. */
    private fun unescape(raw: String): String {
        if ('\\' !in raw) return raw
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\' || i == raw.lastIndex) {
                out.append(c)
                i++
                continue
            }
            i++
            when (val escaped = raw[i]) {
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                '"' -> out.append('"')
                '\\' -> out.append('\\')
                '/' -> out.append('/')
                'u' -> {
                    val hex = raw.substring(i + 1, minOf(i + 5, raw.length))
                    val code = hex.toIntOrNull(16)
                    if (code != null) {
                        out.append(code.toChar())
                        i += 4
                    } else {
                        out.append('u')
                    }
                }
                else -> out.append(escaped)
            }
            i++
        }
        return out.toString()
    }

    private val STRING_FIELD =
        Regex("\"([A-Za-z_]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
    private val NUMBER_FIELD = Regex("\"versionCode\"\\s*:\\s*(\\d+)")
}
