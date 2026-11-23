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
- [ ] Klavye kısayolları: Space, Medya Oynat/Duraklat, İleri (→), Geri (←)
- [ ] Karıştır (shuffle) / Tekrar (repeat) modları
- [ ] Oynatma kuyruğu (queue) görünümü
- [ ] Pencere boyutu hatırlama (son boyutu kaydet, açışta uygula)
- [ ] Sistem tepsi (tray): kapatınca arka plana indirme

### Aşama 2 — Ses Kalitesi ve Kütüphane (1-2 ay)
- [ ] Gapless oynatma (parçalar arası sessiz yok)
- [ ] Crossfade (parça geçişi)
- [ ] ReplayGain / ses normalizasyonu
- [ ] Etiket (tag) düzenleme: başlık, sanatçı, albüm, tür
- [ ] Albüm sanatı görünümü (kütüphede kare izgara)

### Aşama 3 — Telefon (APK) ile Tam Uyum (2-3 ay)
- [ ] Mini player (çalma sırasında küçük çubuk)
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