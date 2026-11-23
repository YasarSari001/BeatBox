package com.blackstack.beatbox.desktop

import com.blackstack.beatbox.EQPreset
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Masaüstü çalma motoru.
 *
 * Java Sound kullanır; MP3 ve OGG desteği sınıf yolundaki MP3SPI/VorbisSPI
 * sağlayıcılarından gelir. Bu yüzden harici bir oynatıcı kurulumu gerekmez.
 *
 * Tüm çözme işi ayrı bir iş parçacığında yapılır; UI yalnızca
 * [positionSeconds], [isPlaying] ve görselleştirici değerlerini okur.
 */
class AudioEngine {

    @Volatile private var worker: Thread? = null
    @Volatile private var line: SourceDataLine? = null
    @Volatile private var runToken = 0
    @Volatile private var paused = false
    @Volatile private var framesPlayed = 0L
    @Volatile private var sampleRate = 44_100f
    @Volatile private var volume = 0.8f
    @Volatile private var eqPreset: EQPreset = EQPreset.NONE
    @Volatile private var currentChain: EqChain? = null
    @Volatile private var currentGain = 1f
    @Volatile private var nextGain = 1f
    @Volatile private var trackDuration = 0.0

    /** Parçalar arası geçiş süresi (saniye). 0 = boşluksuz (gapless) geçiş. */
    @Volatile var crossfadeSeconds: Float = 0f

    /** ReplayGain etiketlerini ses seviyesine uygula. */
    @Volatile var replayGainEnabled: Boolean = false

    /** ReplayGain okuyucu; Main tarafından etiket modülüne bağlanır. */
    var replayGainProvider: ((File) -> Float?)? = null

    /** Sıradaki parçayı hazırlar; crossfade/gapless için gereklidir. */
    var onTrackChange: ((File, Double) -> Unit)? = null

    @Volatile private var nextFile: File? = null
    @Volatile private var nextDuration = 0.0

    /** Görselleştirici için 15 bantlık enerji (0..1). */
    @Volatile var bands: FloatArray = FloatArray(BAND_COUNT)
        private set

    /** Görselleştirici için toplam ses seviyesi (0..1). */
    @Volatile var level: Float = 0f
        private set

    /** Parça doğal olarak sona erdiğinde çağrılır. */
    var onCompleted: (() -> Unit)? = null

    /** Dosya açılamazsa/çözülemezse çağrılır. */
    var onError: ((String) -> Unit)? = null

    val isPlaying: Boolean get() = worker != null

    fun positionSeconds(): Double {
        val rate = sampleRate
        return if (rate <= 0f) 0.0 else framesPlayed.toDouble() / rate
    }

    fun play(file: File, startSeconds: Double = 0.0, durationSeconds: Double = 0.0) {
        stop()
        nextFile = null
        trackDuration = durationSeconds
        currentGain = if (replayGainEnabled) {
            TagEditor.gainMultiplier(replayGainProvider?.invoke(file))
        } else 1f
        val token = runToken
        paused = false
        worker = Thread({ run(file, startSeconds, token) }, "BeatBox-Audio").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Sıradaki parçayı bildirir. Parça bitmeden önce açılıp crossfade/gapless
     * geçişte kullanılır. [durationSeconds] yeni parçanın süresidir.
     */
    fun prepareNext(file: File?, durationSeconds: Double = 0.0) {
        nextFile = file
        nextDuration = durationSeconds
        nextGain = if (file != null && replayGainEnabled) {
            TagEditor.gainMultiplier(replayGainProvider?.invoke(file))
        } else 1f
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        if (worker == null) return
        paused = false
        line?.start()
    }

    fun stop() {
        runToken += 1
        paused = false
        val running = worker
        line?.let { current ->
            runCatching {
                current.stop()
                current.flush()
                current.close()
            }
        }
        line = null
        if (running != null && running !== Thread.currentThread()) {
            runCatching { running.join(400) }
        }
        worker = null
        framesPlayed = 0
        bands = FloatArray(BAND_COUNT)
        level = 0f
    }

    /** 0..1 arası doğrusal ses seviyesi. */
    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        line?.let { applyVolume(it) }
    }

    /** Ekolayzer ön ayarı; çalan sesin üzerine anında uygulanır. */
    fun setEqPreset(preset: EQPreset) {
        eqPreset = preset
        currentChain = null
    }

    /** Dosyanın yaklaşık süresi (saniye); bilinmiyorsa 0. */
    fun durationSeconds(file: File): Double = runCatching {
        AudioSystem.getAudioInputStream(file).use { stream ->
            val length = stream.frameLength
            val rate = stream.format.frameRate
            if (length > 0L && rate > 0f) length / rate.toDouble() else estimateByBitrate(file)
        }
    }.getOrElse { estimateByBitrate(file) }

    private fun estimateByBitrate(file: File): Double {
        val bitrate = runCatching {
            AudioSystem.getAudioFileFormat(file).properties()["bitrate"] as? Int
        }.getOrNull() ?: 192_000
        if (bitrate <= 0) return 0.0
        return file.length() * 8.0 / bitrate
    }

    private fun applyVolume(target: SourceDataLine) {
        runCatching {
            val control = target.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val gain = if (volume <= 0.0001f) control.minimum else 20.0 * kotlin.math.log10(volume.toDouble())
            control.value = gain.toFloat().coerceIn(control.minimum, control.maximum)
        }
    }

    private fun run(file: File, startSeconds: Double, token: Int) {
        var completed = false
        try {
            val base = AudioSystem.getAudioInputStream(file).use { it.format }
            val pcm = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                base.sampleRate,
                16,
                base.channels,
                base.channels * 2,
                base.sampleRate,
                false,
            )
            var stream: javax.sound.sampled.AudioInputStream = openConverted(file, pcm)
            stream.use { firstStream ->
                sampleRate = pcm.sampleRate
                val frameSize = pcm.frameSize
                framesPlayed = (startSeconds * pcm.sampleRate).toLong()

                var remainingSkip = framesPlayed * frameSize
                while (remainingSkip > 0 && token == runToken) {
                    val skipped = firstStream.skip(remainingSkip)
                    if (skipped <= 0L) break
                    remainingSkip -= skipped
                }

                val audioLine = AudioSystem.getSourceDataLine(pcm)
                audioLine.open(pcm, (pcm.sampleRate * frameSize * 0.35).toInt().coerceAtLeast(4096))
                applyVolume(audioLine)
                audioLine.start()
                line = audioLine

                val meter = BandMeter(pcm.sampleRate)
                val buffer = ByteArray(8192)
                val fadeBuffer = ByteArray(8192)
                val fadeFrames = (crossfadeSeconds.coerceAtLeast(0f) * pcm.sampleRate).toLong()
                var fadeDone = 0L
                var nextStream: javax.sound.sampled.AudioInputStream? = null
                var nextChain: EqChain? = null
                var chain = currentChain ?: EqChain(pcm.sampleRate, pcm.channels, eqPreset)
                    .also { currentChain = it }

                /** Sıradaki parçayı ana akış yapar; UI'ya yeni parçayı bildirir. */
                fun advance(): Boolean {
                    val upcoming = nextStream ?: return false
                    val upcomingFile = nextFile ?: return false
                    runCatching { stream.close() }
                    stream = upcoming
                    chain = nextChain ?: EqChain(pcm.sampleRate, pcm.channels, eqPreset)
                    currentChain = chain
                    currentGain = nextGain
                    trackDuration = nextDuration
                    framesPlayed = if (fadeFrames > 0) fadeDone else 0L
                    val startedAt = framesPlayed / pcm.sampleRate.toDouble()
                    nextStream = null
                    nextChain = null
                    nextFile = null
                    fadeDone = 0L
                    onTrackChange?.invoke(upcomingFile, startedAt)
                    return true
                }

                while (token == runToken) {
                    if (paused) {
                        Thread.sleep(30)
                        continue
                    }
                    if (currentChain !== chain) {
                        chain = EqChain(pcm.sampleRate, pcm.channels, eqPreset).also { currentChain = it }
                    }

                    val remaining = trackDuration * pcm.sampleRate - framesPlayed
                    if (nextStream == null && nextFile != null && trackDuration > 0 &&
                        remaining <= fadeFrames + pcm.sampleRate * 0.1
                    ) {
                        val upcomingFile = nextFile!!
                        val opened = runCatching { openConverted(upcomingFile, pcm) }.getOrNull()
                        if (opened != null) {
                            nextStream = opened
                            nextChain = EqChain(pcm.sampleRate, pcm.channels, eqPreset)
                        } else {
                            nextFile = null
                        }
                    }

                    val read = stream.read(buffer)
                    if (read <= 0) {
                        if (nextStream == null && nextFile != null) {
                            nextStream = runCatching { openConverted(nextFile!!, pcm) }.getOrNull()
                            if (nextStream != null) nextChain = EqChain(pcm.sampleRate, pcm.channels, eqPreset)
                        }
                        if (advance()) continue
                        completed = token == runToken
                        break
                    }

                    if (eqPreset != EQPreset.NONE) chain.process(buffer, read)
                    applyGain(buffer, read, pcm.channels, currentGain)

                    var writeLength = read
                    if (nextStream != null && fadeFrames > 0) {
                        val nextRead = nextStream!!.read(fadeBuffer)
                        if (nextRead > 0) {
                            if (eqPreset != EQPreset.NONE) nextChain?.process(fadeBuffer, nextRead)
                            applyGain(fadeBuffer, nextRead, pcm.channels, nextGain)
                            val progress = (fadeDone.toFloat() / fadeFrames).coerceIn(0f, 1f)
                            mixFade(buffer, read, fadeBuffer, nextRead, progress)
                        }
                        fadeDone += read / frameSize
                    }

                    bands = meter.feed(buffer, writeLength, pcm.channels)
                    level = meter.lastLevel
                    var offset = 0
                    while (offset < writeLength && token == runToken) {
                        if (paused) {
                            Thread.sleep(20)
                            continue
                        }
                        offset += audioLine.write(buffer, offset, writeLength - offset)
                    }
                    framesPlayed += read / frameSize

                    if (nextStream != null && fadeFrames > 0 && fadeDone >= fadeFrames) {
                        advance()
                    }
                }

                runCatching { stream.close() }
                runCatching { audioLine.drain() }
                audioLine.stop()
                audioLine.close()
                if (line === audioLine) line = null
            }
        } catch (error: Exception) {
            if (token == runToken) {
                completed = false
                onError?.invoke(error.message ?: error.javaClass.simpleName)
            }
        } finally {
            val finishedToken = token == runToken
            if (finishedToken) {
                worker = null
                bands = FloatArray(BAND_COUNT)
                level = 0f
                nextFile = null
            }
            if (completed) onCompleted?.invoke()
        }
    }

    /** Dosyayı hedef 16 bit PCM biçimine çevirerek açar. */
    private fun openConverted(file: File, target: AudioFormat): javax.sound.sampled.AudioInputStream =
        AudioSystem.getAudioInputStream(target, AudioSystem.getAudioInputStream(file))

    /** 16 bit PCM tamponuna sabit kazanç uygular. */
    private fun applyGain(buffer: ByteArray, length: Int, channels: Int, gain: Float) {
        if (gain in 0.999f..1.001f) return
        var index = 0
        while (index + 1 < length) {
            val sample = ((buffer[index + 1].toInt() shl 8) or (buffer[index].toInt() and 0xFF)).toShort()
            val value = (sample * gain).toInt().coerceIn(-32_768, 32_767)
            buffer[index] = (value and 0xFF).toByte()
            buffer[index + 1] = ((value shr 8) and 0xFF).toByte()
            index += 2
        }
    }

    /**
     * Crossfade: eşit güç yasasıyla (cos/sin) iki PCM tamponunu karıştırır.
     * [progress] 0 (yalnızca eski parça) → 1 (yalnızca yeni parça).
     */
    private fun mixFade(
        destination: ByteArray,
        destinationLength: Int,
        incoming: ByteArray,
        incomingLength: Int,
        progress: Float,
    ) {
        val oldGain = kotlin.math.cos(progress * PI / 2f).toFloat()
        val newGain = kotlin.math.sin(progress * PI / 2f).toFloat()
        var index = 0
        while (index + 1 < destinationLength) {
            val old = ((destination[index + 1].toInt() shl 8) or (destination[index].toInt() and 0xFF)).toShort()
            val new = if (index + 1 < incomingLength) {
                ((incoming[index + 1].toInt() shl 8) or (incoming[index].toInt() and 0xFF)).toShort()
            } else 0
            val value = (old * oldGain + new * newGain).toInt().coerceIn(-32_768, 32_767)
            destination[index] = (value and 0xFF).toByte()
            destination[index + 1] = ((value shr 8) and 0xFF).toByte()
            index += 2
        }
    }
}

/** Görselleştiricinin çubuk sayısı. */
const val BAND_COUNT = 15

/**
 * 16 bit PCM üzerinde çalışan üç bantlı ekolayzer.
 *
 * Alçak raf + orta tepe + yüksek raf süzgeçleri seri bağlanır; her kanal
 * kendi durumunu tutar. Ön ayar [EQPreset] ile seçilir.
 */
private class EqChain(sampleRate: Float, private val channels: Int, preset: EQPreset) {

    private val stages: List<Array<Biquad>> = buildStages(sampleRate, preset)

    private fun buildStages(rate: Float, preset: EQPreset): List<Array<Biquad>> {
        val fs = rate.toDouble().coerceAtLeast(8_000.0)
        val (low, mid, high) = when (preset) {
            EQPreset.NONE -> Triple(0.0, 0.0, 0.0)
            EQPreset.BASS_BOOST -> Triple(9.0, -1.0, 0.0)
            EQPreset.VOCAL -> Triple(-4.0, 6.0, 1.5)
            EQPreset.TREBLE -> Triple(0.0, 1.0, 8.0)
            EQPreset.LOUDNESS -> Triple(5.0, 3.0, 5.0)
        }
        return listOf(
            Array(channels) { Biquad().lowShelf(fs, 110.0, low) },
            Array(channels) { Biquad().peaking(fs, 1_400.0, 0.9, mid) },
            Array(channels) { Biquad().highShelf(fs, 7_500.0, high) },
        )
    }

    /** 16 bit little-endian örnekleri yerinde işler. */
    fun process(buffer: ByteArray, length: Int) {
        var index = 0
        var channel = 0
        while (index + 1 < length) {
            val sample = ((buffer[index + 1].toInt() shl 8) or (buffer[index].toInt() and 0xFF)).toShort()
            var value = sample / 32_768.0
            for (stage in stages) value = stage[channel].process(value)
            val clamped = (value * 32_768.0).toInt().coerceIn(-32_768, 32_767)
            buffer[index] = (clamped and 0xFF).toByte()
            buffer[index + 1] = ((clamped shr 8) and 0xFF).toByte()
            index += 2
            channel = (channel + 1) % channels
        }
    }
}

/** RBJ cookbook tabanlı ikinci derece süzgeç. */
private class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(input: Double): Double {
        val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = input
        y2 = y1
        y1 = output
        return output
    }

    fun lowShelf(fs: Double, freq: Double, gainDb: Double): Biquad {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * (freq / fs).coerceIn(0.0001, 0.49)
        val cosW = cos(w0)
        val alpha = sin(w0) / 2.0 * sqrt(2.0)
        val sqrtA = sqrt(a)
        val a0 = (a + 1) + (a - 1) * cosW + 2 * sqrtA * alpha
        b0 = a * ((a + 1) - (a - 1) * cosW + 2 * sqrtA * alpha) / a0
        b1 = 2 * a * ((a - 1) - (a + 1) * cosW) / a0
        b2 = a * ((a + 1) - (a - 1) * cosW - 2 * sqrtA * alpha) / a0
        a1 = -2 * ((a - 1) + (a + 1) * cosW) / a0
        a2 = ((a + 1) + (a - 1) * cosW - 2 * sqrtA * alpha) / a0
        return this
    }

    fun highShelf(fs: Double, freq: Double, gainDb: Double): Biquad {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * (freq / fs).coerceIn(0.0001, 0.49)
        val cosW = cos(w0)
        val alpha = sin(w0) / 2.0 * sqrt(2.0)
        val sqrtA = sqrt(a)
        val a0 = (a + 1) - (a - 1) * cosW + 2 * sqrtA * alpha
        b0 = a * ((a + 1) + (a - 1) * cosW + 2 * sqrtA * alpha) / a0
        b1 = -2 * a * ((a - 1) + (a + 1) * cosW) / a0
        b2 = a * ((a + 1) + (a - 1) * cosW - 2 * sqrtA * alpha) / a0
        a1 = 2 * ((a - 1) - (a + 1) * cosW) / a0
        a2 = ((a + 1) - (a - 1) * cosW - 2 * sqrtA * alpha) / a0
        return this
    }

    fun peaking(fs: Double, freq: Double, q: Double, gainDb: Double): Biquad {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * (freq / fs).coerceIn(0.0001, 0.49)
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1))
        val a0 = 1 + alpha / a
        b0 = (1 + alpha * a) / a0
        b1 = (-2 * cosW) / a0
        b2 = (1 - alpha * a) / a0
        a1 = (-2 * cosW) / a0
        a2 = (1 - alpha / a) / a0
        return this
    }
}

private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)

/**
 * PCM tamponundan 15 bantlık enerji çıkarır.
 *
 * Her bant için tek kutuplu alçak geçiren süzgeç koşar; komşu iki süzgecin
 * farkı o bandın enerjisini verir. Sonuç hızlı yükselip yavaş düşer, böylece
 * çubuklar titremek yerine akıcı hareket eder.
 */
private class BandMeter(sampleRate: Float) {

    private val cutoffs = floatArrayOf(
        55f, 85f, 125f, 180f, 260f, 370f, 520f, 740f,
        1_000f, 1_400f, 2_000f, 2_900f, 4_200f, 6_200f, 9_500f,
    )
    private val coeff = FloatArray(BAND_COUNT) { index ->
        (1.0 - exp(-2.0 * PI * cutoffs[index] / sampleRate.toDouble())).toFloat().coerceIn(0.0005f, 0.9f)
    }
    private val state = FloatArray(BAND_COUNT)
    private val smoothed = FloatArray(BAND_COUNT)

    var lastLevel: Float = 0f
        private set

    fun feed(buffer: ByteArray, length: Int, channels: Int): FloatArray {
        var sum = 0.0
        var frames = 0
        var index = 0
        val safeChannels = channels.coerceAtLeast(1)
        while (index + 1 < length) {
            var mono = 0f
            for (channel in 0 until safeChannels) {
                val position = index + channel * 2
                if (position + 1 >= length) break
                val sample = ((buffer[position + 1].toInt() shl 8) or (buffer[position].toInt() and 0xFF)).toShort()
                mono += sample / 32_768.0f
            }
            mono /= safeChannels
            sum += (mono * mono).toDouble()
            frames++

            var previous = 0f
            for (band in 0 until BAND_COUNT) {
                state[band] += coeff[band] * (mono - state[band])
                val diff = state[band] - previous
                previous = state[band]
                val magnitude = abs(diff)
                smoothed[band] = if (magnitude > smoothed[band]) {
                    smoothed[band] + (magnitude - smoothed[band]) * 0.6f
                } else {
                    smoothed[band] * 0.82f
                }
            }
            index += safeChannels * 2
        }

        lastLevel = if (frames == 0) 0f else sqrt(sum / frames).toFloat().coerceIn(0f, 1f)

        val result = FloatArray(BAND_COUNT)
        for (band in 0 until BAND_COUNT) {
            // Yüksek bantlar doğal olarak daha zayıf: hafif telafi + kırpma.
            val boost = 1f + band * 0.09f
            result[band] = (smoothed[band] * 26f * boost).coerceIn(0f, 1f)
        }
        return result
    }
}
