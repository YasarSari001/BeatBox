# BeatBox Masaüstü Geliştirme Planı

## 1. Mevcut Durum (Masaüstü)
- Ses oynatıcı (Java Sound + MP3/OGG SPI)
- Video oynatıcı (JavaCV/FFmpeg)
- Fotoğraf görüntüleyici (ImageIO)
- Ekolayzer (5 preset), Visualizer (15 bant)
- Tema: DARK/LIGHT/AMOLED/PURE_BLACK + 6 vurgu rengi
- Dil: TR ve EN (core'da 12 dil var)
- NowPlayingPanel açma/kapama butonu eklendi
- `desktop/build.gradle.kts`: EXE paketleme (WiX) tanımlı

## 2. Hedef Mimari
- `:core` modülü: AppLanguage, EQPreset, Strings, FilenameMetadata, ExtraTranslations (zaten var)
- `:desktop`: Compose Multiplatform UI, Java Sound (ses), JavaCV (video)
- Ortak playback state: Main.kt'te `current`, `isPlaying`, `position`, `volume`, `eqPreset`

## 3. Feature Roadmap (3 Aşama)

### Aşama 1 — Temel Eksikler (bu ay)
- [x] Klavye kısayolları: Space, Medya Oynat/Duraklat, İleri (→), Geri (←) — 2026-11-23
- [x] Karıştır (shuffle) / Tekrar (repeat) modları — 2026-11-23
- [x] Oynatma kuyruğu (queue) görünümü — 2026-11-23
- [x] Pencere boyutu hatırlama (son boyutu kaydet, açışta uygula) — 2026-11-23
- [x] Sistem tepsi (tray): kapatınca arka plana indirme — 2026-11-23

### Aşama 2 — Ses Kalitesi ve Kütüphane (1-2 ay)
- [x] Gapless oynatma (parçalar arası sessiz yok) — 2026-11-23
- [x] Crossfade (parça geçişi) — 2026-11-23
- [x] ReplayGain / ses normalizasyonu — 2026-11-23
- [x] Etiket (tag) düzenleme: başlık, sanatçı, albüm, tür, yıl, parça no — 2026-11-23
- [x] Albüm sanatı görünümü (kütüphane kare ızgarası) — 2026-11-23

### Aşama 3 — Telefon (APK) ile Tam Uyum (2-3 ay)
- [x] Mini player (çalma sırasında küçük çubuk) — 2026-11-23
- [ ] Sistem bildirimi / kilit ekranı kontrolü
- [x] Şarkı sözleri (lyrics) paneli — 2026-11-23
- [ ] Bluetooth kulaklık / çıkış cihazı seçimi
- [ ] Tema AUTO modu (sistem dinamik rengi takibi)

## 4. Referans Projeler
- jaatayushh/ayushmuzic — Compose Multiplatform, libmpv, sistem tepsi, MSI paketleme
- wesley666/LynMusic — cross-platform (Win/Lin/Mac/Android/iOS)
- SkyD666/UPlayer — basit Compose Desktop masaüstü örnek
- k-tigre/music-player — Media3/ExoPlayer + Compose Desktop, play queue
- clementine-player — gapless, ReplayGain, smart playlist, tag editor

## 5. Cevaplar (alındı)
1. Hedef işletim sistemi: **yalnızca Windows**.
2. Öncelikli özellikler: **şarkı sözleri (APK'daki gibi), sistem tepsisi, etiket (ID3) düzenleme**.
3. Sistem tepsisi ve kapatınca arka planda kalma: **isteniyor**.
4. Etiket düzenleme: **yazılabilir** (salt okunur değil).
5. Şarkı sözleri: **APK'daki gibi** — önce yerel `.lrc`, sonra çevrimiçi servisler.

Öncelik sırası: 1) lyrics, 2) tray + arka plan, 3) etiket düzenleme,
4) kuyruk / shuffle / repeat / kısayollar, 5) gapless / crossfade / mini player.

## 6. Uygulama Günlüğü

### 2026-11-23 — Şarkı sözleri (Aşama 3'ten öne alındı) ✅
- `desktop/.../desktop/lyrics/LyricsModels.kt`: veri modelleri, `LyricsState`,
  dosya tabanlı önbellek (`~/.beatbox/lyrics_cache`, Room yerine).
- `LyricsProvider.kt`: yerel `.lrc` → disk önbelleği → LRCLIB → Lyrics.ovh sırası.
  Ağ için JDK `HttpClient` kullanılır; ek bağımlılık yok. Bağımlılıksız JSON
  dize okuyucu (regex geri izlemesi yerine elle tarama).
- `LRCParser.kt`: `[mm:ss.xx]`, `[hh:mm:ss.xxx]`, çoklu zaman damgası,
  `[offset:]`, gelişmiş `<mm:ss.xx>` kelime zamanları, künye satırı atlama.
- `LyricsPage.kt`: otomatik kayan satırlar, geçerli satır + kelime vurgusu,
  satıra tıklayınca o ana atlama, ±0,5 sn gecikme, kaynak rozeti, yenileme,
  düz metin (senkronize olmayan) görünümü.
- `Main.kt`: `Page.LYRICS` ve sol menüde "Sözler" sekmesi.
- `DesktopStrings.kt`: 8 yeni anahtar × 12 dil.
- `LRCParserTest.kt`: 9 birim testi (Gradle `:desktop:test` yeşil).
- Metrolist kaynaklı dosyalara GPL-3.0 uyarlama başlıkları eklendi.
- Canlı LRCLIB denemesi: "Daft Punk – Get Lucky" → 112 satır senkronize söz.

### 2026-11-23 — Sistem tepsisi ✅
- `DesktopTray.kt`: `ApplicationScope.BeatBoxTray` — 32×32 kodla çizilen nota
  simgesi (ek görsel dosyası yok), araç ipucu ve menü: göster, çal/duraklat,
  önceki, sonraki, çıkış.
- `TrayController`: tepsi menüsü ile pencere durumu arasında köprü; dil ve
  çalma durumu anlık yansıtılır.
- Kapat düğmesi artık varsayılan olarak pencereyi gizler (müzik devam eder).
- `SettingsPage`: "Kapatınca sistem tepsisinde kal" anahtarı; tercih
  `close_to_tray` olarak saklanır.
- 4 yeni metin anahtarı × 12 dil.
- `:desktop:compileKotlin` ve `:desktop:test` yeşil; uygulama 30 sn çalıştırılıp
  çökmediği doğrulandı.

### 2026-11-23 — Etiket (ID3) düzenleme ✅
- `net.jthink:jaudiotagger:3.0.1` bağımlılığı (LGPL-2.1, GPL-3.0 ile uyumlu).
- `TagEditor.kt`: MP3/FLAC/OGG/M4A/WAV/AIFF etiket okuma-yazma; boş alan silme.
- `TagEditorDialog.kt`: düzenleme penceresi (6 alan), kaydetme/iptal ve hata
  gösterimi; 7 yeni metin anahtarı × 12 dil.
- `Library.kt`: tarama artık gömülü etiketi dosya adına tercih eder;
  `Track.album` eklendi ve lyrics isteğinde albüm kullanılıyor.
- `Main.kt`: kütüphane satırında düzenleme düğmesi; kaydedince liste ve
  oynatılan parça anında güncellenir.
- `TagEditorTest.kt`: üretilen sessiz WAV üzerinde yaz/oku ve alan silme
  testleri yeşil (Türkçe karakterler dahil).

### 2026-11-23 — Aşama 1 tamamlandı: kuyruk, karıştırma, kısayollar ✅
- `QueuePage.kt`: kuyruk sayfası; sıralı liste, çal/kaldır/temizle, boş durum.
  Kütüphane satırındaki kuyruk düğmesiyle parça eklenir; kuyruk boşken normal
  kitaplık sırası kullanılır.
- Shuffle ve tekrar (kapalı/tümü/tek) oynatıcı panelinde düğmelerle
  yönetilir, `shuffle` / `repeat_mode` tercihlerinde saklanır.
- `pickTrack()` saf yardımcı fonksiyonu otomatik geçişte ve kullanıcı
  komutlarında aynı kuralları uygular (repeat-one, shuffle, sona gelince dönme).
- Klavye: Space (çal/duraklat), ←/→ (önceki/sonraki), medya tuşları
  (Play/Pause, Media Next/Previous). Metin alanı odaktayken düz tuşlar
  devre dışı; Ctrl ile birleşimler asla engellenmez.
- Pencere boyutu kapatma/çıkışta kaydedilir (`window_width`/`window_height`).
- 12 yeni metin anahtarı × 12 dil. `:desktop:test` yeşil; uygulama 25 sn
  çalıştırılıp çalışır durumda kaldı (çökme/istisna yok).

### 2026-11-23 — Mini player ✅
- `MiniPlayer.kt`: tam panel gizlendiğinde çıkan 68 dp'lik çubuk; parça
  bilgisi, çal/duraklat, sonraki, ilerleme çizgisi ve tam paneli açma düğmesi.
- `now_playing_visible` tercihi artık açılışta okunuyor ve her değişimde
  yazılıyor (önce içeride her zaman `true` ile başlıyordu).
- 1 metin anahtarı × 12 dil; `:desktop:compileKotlin` ve `:desktop:test` yeşil.

### 2026-11-23 — Aşama 2: ses kalitesi ve albüm ızgarası ✅
- `AudioEngine`: tek `SourceDataLine` üzerinde çalışan crossfade/gapless motoru.
  Sıradaki parça motorla önceden paylaşılır (`prepareNext`); bitişten önce
  ikinci PCM akışı açılır, eşit güç (cos/sin) yasasıyla karıştırılır ve
  `onTrackChange` ile UI'ya haber verilir. Crossfade 0 ise line kapatılmadan
  akış değiştirilir (gapless).
- `ReplayGain`: `TagEditor.replayGainDb()` TXXX/vorbis etiketlerini okur,
  dB → çarpan dönüşümü 0,25×–4× aralığında sınırlandırılır ve PCM'ye uygulanır.
- `SettingsPage`: geçiş süresi (0/2/4/6 sn, 0 = boşluksuz) ve ReplayGain
  anahtarı; tercihler `crossfade_seconds` / `replay_gain`.
- `AlbumArt.kt` + `AlbumsPage.kt`: gömülü JPEG/PNG kapaklarını önbelleğe alıp
  kare ızgarada gösterir; albüme tıklayınca kütüphane o albüme filtrelenir.
- 8 metin anahtarı × 12 dil. `:desktop:test` yeşil; 1,1 sn'lik iki üretilmiş
  WAV ile yapılan geçiş denemesinde ikinci parçaya sorunsuz geçildiği
  doğrulandı (deneme dosyası kalıcı testlere alınmadı).