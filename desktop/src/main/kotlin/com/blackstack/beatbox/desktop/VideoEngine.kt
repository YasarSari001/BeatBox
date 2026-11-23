package com.blackstack.beatbox.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.min

/**
 * Video oynatıcı.
 *
 * FFmpeg bir kare/ses parçası üretir; görüntü Compose'un çizebileceği
 * [ImageBitmap]'e çevrilir, ses Java Sound hattına yazılır. Ses hattı bloklu
 * yazdığı için oynatma hızını doğal olarak o sınırlar; böylece görüntü ve ses
 * aynı akıştan geldiği için senkron kalır.
 */
class VideoEngine {

    private var worker: Thread? = null
    private var token = 0
    private var audioLine: SourceDataLine? = null
    private var lastFrameAt = 0L
    private var rotation = 0
    private var firstFrame = true

    @Volatile private var paused = false
    @Volatile private var volume = 0.8f
    @Volatile private var currentFile: File? = null

    var frame by mutableStateOf<ImageBitmap?>(null)
        private set
    var position by mutableFloatStateOf(0f)
        private set
    var duration by mutableFloatStateOf(0f)
        private set
    var failed by mutableStateOf(false)
        private set

    /** Ekran kartına aktarılacak kare genişliği üst sınırı. */
    private val maxFrameWidth = 1280

    var onCompleted: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val isActive: Boolean get() = worker != null

    fun play(file: File, startSeconds: Double = 0.0) {
        stop()
        val runId = token
        currentFile = file
        paused = false
        failed = false
        firstFrame = true
        lastFrameAt = 0L
        position = startSeconds.toFloat()
        worker = Thread({ run(file, startSeconds, runId) }, "BeatBox-Video").apply {
            isDaemon = true
            start()
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        applyVolume()
    }

    fun stop() {
        token += 1
        paused = false
        val running = worker
        audioLine?.let { line ->
            runCatching {
                line.stop()
                line.flush()
                line.close()
            }
        }
        audioLine = null
        if (running != null && running !== Thread.currentThread()) {
            runCatching { running.join(500) }
        }
        worker = null
        currentFile = null
        frame = null
        position = 0f
    }

    private fun applyVolume() {
        val line = audioLine ?: return
        runCatching {
            val control = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val gain = if (volume <= 0.0001f) control.minimum else 20.0 * kotlin.math.log10(volume.toDouble())
            control.value = gain.toFloat().coerceIn(control.minimum, control.maximum)
        }
    }

    private fun run(file: File, startSeconds: Double, runId: Int) {
        var grabber: FFmpegFrameGrabber? = null
        val converter = Java2DFrameConverter()
        var completed = false
        try {
            grabber = FFmpegFrameGrabber(file).apply {
                sampleFormat = AV_SAMPLE_FMT_S16
                start()
            }
            duration = (grabber.lengthInTime / 1_000_000.0).toFloat().coerceAtLeast(0f)
            // Telefon videoları dikey çekilip 90°/270° döndürme bayrağıyla saklanır.
            rotation = (((grabber.displayRotation % 360) + 360).toInt()) % 360
            if (startSeconds > 0.5) {
                grabber.setTimestamp((startSeconds * 1_000_000).toLong())
            }

            val sampleBuffer = ByteArray(16 * 1024)
            while (runId == token) {
                if (paused) {
                    Thread.sleep(40)
                    continue
                }
                val grabbed = grabber.grab() ?: break

                val samples = grabbed.samples
                if (samples != null && samples.isNotEmpty()) {
                    val line = ensureAudioLine(grabber)
                    val shorts = samples[0] as? ShortBuffer
                    if (line != null && shorts != null) {
                        var remaining = shorts.remaining()
                        var offset = 0
                        while (remaining > 0) {
                            val chunk = min(remaining, sampleBuffer.size / 2)
                            val bytes = chunk * 2
                            for (index in 0 until chunk) {
                                val value = shorts.get()
                                sampleBuffer[index * 2] = (value.toInt() and 0xFF).toByte()
                                sampleBuffer[index * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
                            }
                            line.write(sampleBuffer, 0, bytes)
                            remaining -= chunk
                            offset += chunk
                            if (runId != token) break
                        }
                    }
                }

                val image = grabbed.image
                if (image != null) {
                    val now = grabbed.timestamp
                    // 32 fps üstünü çizmeye gerek yok; dönüşüm maliyetli.
                    // İlk kare her zaman çizilir: kısa kliplerde zaman damgası
                    // sıfırdan başladığı için kare atlanabiliyordu.
                    if (firstFrame || now - lastFrameAt >= 32_000L) {
                        val bitmap = converter.convert(grabbed)
                        if (bitmap != null) {
                            lastFrameAt = now
                            firstFrame = false
                            frame = bitmap.toDisplayBitmap(rotation)
                        }
                    }
                }

                position = (grabbed.timestamp / 1_000_000.0).toFloat().coerceAtLeast(0f)

                // Sesi olmayan videolarda akışı gerçek zamana yay.
                if (samples == null || samples.isEmpty()) {
                    Thread.sleep(16)
                }
            }
            completed = runId == token
        } catch (error: Exception) {
            if (runId == token) {
                failed = true
                onError?.invoke(error.message ?: error.javaClass.simpleName)
            }
        } finally {
            runCatching { grabber?.stop() }
            runCatching { grabber?.close() }
            runCatching { converter.close() }
            runCatching {
                audioLine?.stop()
                audioLine?.close()
            }
            audioLine = null
            if (runId == token) worker = null
            if (completed) onCompleted?.invoke()
        }
    }

    private fun ensureAudioLine(grabber: FFmpegFrameGrabber): SourceDataLine? {
        audioLine?.let { return it }
        val rate = grabber.sampleRate.toFloat()
        val channels = grabber.audioChannels.coerceAtLeast(1)
        if (rate <= 0f) return null
        return runCatching {
            val format = AudioFormat(rate, 16, channels, true, false)
            AudioSystem.getSourceDataLine(format).apply {
                open(format, 64 * 1024)
                start()
            }.also {
                audioLine = it
                applyVolume()
            }
        }.getOrNull()
    }

    /** Kareyi döndürüp ekran için ölçekler (bellek ve çizim maliyetini düşürür). */
    private fun BufferedImage.toDisplayBitmap(rotationDegrees: Int): ImageBitmap {
        val oriented = if (rotationDegrees == 90 || rotationDegrees == 180 || rotationDegrees == 270) {
            rotateImage(this, rotationDegrees)
        } else {
            this
        }
        val targetWidth = oriented.width.coerceAtMost(maxFrameWidth)
        if (targetWidth == oriented.width) return oriented.toComposeImageBitmap()
        val targetHeight = (oriented.height.toDouble() * targetWidth / oriented.width).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(oriented, 0, 0, targetWidth, targetHeight, null)
        graphics.dispose()
        return scaled.toComposeImageBitmap()
    }

    private fun rotateImage(src: BufferedImage, degrees: Int): BufferedImage {
        val swap = degrees == 90 || degrees == 270
        val outWidth = if (swap) src.height else src.width
        val outHeight = if (swap) src.width else src.height
        val out = BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = out.createGraphics()
        val transform = AffineTransform()
        when (degrees) {
            90 -> {
                transform.translate(src.height.toDouble(), 0.0)
                transform.rotate(Math.PI / 2)
            }
            180 -> {
                transform.translate(src.width.toDouble(), src.height.toDouble())
                transform.rotate(Math.PI)
            }
            270 -> {
                transform.translate(0.0, src.width.toDouble())
                transform.rotate(-Math.PI / 2)
            }
        }
        graphics.drawImage(src, transform, null)
        graphics.dispose()
        return out
    }
}
