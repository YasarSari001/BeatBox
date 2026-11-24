# BeatBox

Yerel medya oynatıcı: **Android** telefon ve **Windows** masaüstü sürümü.
Kitaplık telefonun/bilgisayarın kendi dosyalarından taranır; çevrim içi müzik veya
indirme altyapısı yoktur, her şey çevrim dışı çalışır.

[![Son sürüm](https://img.shields.io/github/v/release/YasarSari001/BeatBox?label=son%20s%C3%BCr%C3%BCm)](https://github.com/YasarSari001/BeatBox/releases/latest)

---

## İndirme

Tüm dosyalar [son sürüm sayfasında](https://github.com/YasarSari001/BeatBox/releases/latest):

| Platform | Dosya | Açıklama |
|---|---|---|
| Android 8.0+ | [`BeatBox.apk`](https://github.com/YasarSari001/BeatBox/releases/latest/download/BeatBox.apk) | Telefon/tablet sürümü |
| Windows 10/11 (x64) | [`BeatBox-windows.exe`](https://github.com/YasarSari001/BeatBox/releases/latest/download/BeatBox-windows.exe) | Kurulum paketi (WiX) |
| Windows 10/11 (x64) | [`BeatBox-windows.zip`](https://github.com/YasarSari001/BeatBox/releases/latest/download/BeatBox-windows.zip) | Kurulum gerektirmeyen taşınabilir sürüm |

Windows paketleri gömülü Java çalışma zamanı içerir; ayrıca JDK kurmanız gerekmez.
Yayın dosyalarının adları sürüm numarası içermez; bu sabit adlar sayesinde
indirme bağlantıları her yeni sürümde geçerli kalır.

## Özellikler

### Ortak

- Yerel kitaplık tarama (alt klasörler dahil, gizli dosyalar atlanır)
- Koyu / Açık / AMOLED tema ve vurgu renkleri
- Ekolayzer ön ayarları: Normal, Bass Boost, Vocal Boost, Treble Boost, Loudness
- Uyku zamanlayıcı
- 12 arayüz dili
- Dosya adından başlık/sanatçı çıkarma, `(Official Video)` gibi dağıtım etiketlerini gizleme

### Android

- Oynatma servisi: bildirim kontrolleri, ses odağı, kulaklık çıkarma davranışı
- Şarkı sözleri desteği (LRC ayrıştırma, çoklu sağlayıcı istemcisi)
- Çalma listeleri
- Kapak görselinden dinamik renk (Material You paleti)
- Oynatılamayan parçalar sessizce atlanmaz, kapak üzerinde uyarı gösterilir
- İsteğe bağlı hesap girişi (Firebase); `google-services.json` yoksa misafir olarak çalışır
- Uygulama içi güncelleme denetimi (`update.json`)

### Windows

- Telefon sürümünün yatay uyarlaması: sol ikon rayı, orta içerik, sağda kalıcı "şimdi çalıyor" paneli
- **Görselleştirici:** çalan sesten üretilen 15 bantlık gerçek zamanlı çubuklar
- **Ekolayzer:** gerçek DSP (RBJ biquad — alçak raf 110 Hz, tepe 1.4 kHz, yüksek raf 7.5 kHz)
- **Müzik, video ve fotoğraf** birlikte: Tümü / Müzik / Videolar / Fotoğraflar filtreleri
- Fotoğraf ızgarası ve tam ekran görüntüleyici (tekerlekle yakınlaştırma, önceki/sonraki)
- Video oynatıcı: FFmpeg ile çözme, ses, ilerleme çubuğu, telefonla çekilen dikey videolarda doğru döndürme
- Ayarlar: görünüm, ekolayzer, ses seviyesi, uyku zamanlayıcı, kitaplık klasörü, dil, hakkında

## Desteklenen biçimler

| Tür | Android | Windows |
|---|---|---|
| Ses | MP3, M4A/AAC, FLAC, OGG, WAV (sistem kodlayıcıları) | MP3, WAV, OGG, AIFF, AU |
| Video | — | MP4, M4V, MKV, WebM, MOV, AVI, 3GP, WMV, FLV, MPG, MPEG, TS |
| Fotoğraf | — | JPG, JPEG, PNG, GIF, BMP, WebP, JFIF |

> Windows masaüstü sürümü ses tarafında Java Sound kullanır; **M4A/AAC** ses dosyaları
> şu an oynatılamaz. Telefon sürümünde bu biçimler desteklenir.

## Kurulum

**Android:** APK'yı indirin, "bilinmeyen kaynaklardan yüklemeye izin ver" seçeneğini
açıp kurun. İlk açılışta ses dosyalarına erişim izni istenir.

**Windows:** `.exe` dosyasını çalıştırıp kurun veya `.zip` dosyasını bir klasöre açıp
`BeatBox.exe` dosyasını başlatın. İmzasız derleme olduğu için SmartScreen
"Windows korumalı bilgisayarınız" uyarısı gösterebilir: *Ek bilgi → Yine de çalıştır*.

## Derleme

Gereksinimler: JDK 17 (uygulama), JDK 21 (Windows kurulum paketi için `jpackage`),
Android SDK. Windows paketlemesi WiX gerektirir; Gradle gerekli araçları indirir.

```bash
# Android
./gradlew :app:assembleDebug          # hata ayıklama APK
./gradlew :app:assembleRelease        # imzalı sürüm APK

# Windows
./gradlew :desktop:run                # geliştirme çalıştırması
./gradlew :desktop:createDistributable :desktop:packageExe
```

Paketleme adımları JDK 21 ister:

```bash
export JAVA_HOME="$LOCALAPPDATA/BeatBox/jdk21/jdk-21.0.13.1+2"   # örnek yol
./gradlew :desktop:packageExe
```

Çıktılar:

- Kurulum paketi: `desktop/build/compose/binaries/main/exe/BeatBox-3.9.2.exe`
  (yayına `BeatBox-windows.exe` adıyla yüklenir)
- Taşınabilir sürüm: `desktop/build/compose/binaries/main/app/BeatBox/`

## Proje yapısı

| Klasör | İçerik |
|---|---|
| `app/` | Android uygulaması (Compose, oynatma servisi, kütüphane, sözler, ayarlar) |
| `core/` | Android'den bağımsız ortak kod: metinler, diller, EQ ön ayarları, dosya adı çözümleme |
| `desktop/` | Windows sürümü (Compose Desktop, ses motoru, video motoru, görselleştirici) |
| `gradle/libs.versions.toml` | Bağımlılık sürümleri |
| `docs/` | Tasarım notları ve yol haritası |

## Sürümleme

Sürüm bilgisi `app/build.gradle.kts` (`versionCode`, `versionName`) ve
`desktop/build.gradle.kts` (`packageVersion`) içinde tutulur. Android tarafındaki
güncelleme denetimi `update.json` dosyasını okur.

## Lisans

Proje **GNU General Public License v3.0** altında dağıtılır; tam metin
[`LICENSE-GPL-3.0.txt`](LICENSE-GPL-3.0.txt) dosyasındadır.

Şarkı sözü veri katmanının bir bölümü GPL-3.0 lisanslı
[Metrolist](https://github.com/MetrolistGroup/Metrolist) projesinden uyarlanmıştır.
Ayrıntılar ve diğer bağımlılıklar için [`NOTICE.md`](NOTICE.md).
