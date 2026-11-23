package com.blackstack.beatbox.desktop

import com.blackstack.beatbox.Strings

/*
 * Masaüstüne özgü metinler.
 *
 * Ortak metin tablosu :core içindeki Strings'ten gelir; burada yalnızca
 * masaüstünde olup Android'de karşılığı olmayan anahtarlar tanımlanır.
 * Tüm anahtarlar 12 dilde (TR, EN, ES, FR, DE, IT, PT, RU, JA, ZH, AR, KO)
 * çevrilidir. Bilinmeyen anahtar, Strings'in kendi geri dönüşüne bırakılır.
 */
private val desktopStrings: Map<String, Map<String, String>> = mapOf(
    "volume" to mapOf(
        "tr" to "Ses seviyesi", "en" to "Volume",
        "es" to "Volumen", "fr" to "Volume",
        "de" to "Lautstärke", "it" to "Volume",
        "pt" to "Volume", "ru" to "Громкость",
        "ja" to "音量", "zh" to "音量",
        "ar" to "مستوى الصوت", "ko" to "볼륨"
    ),
    "start_listening" to mapOf(
        "tr" to "Dinlemeye başla", "en" to "Start listening",
        "es" to "Empezar a escuchar", "fr" to "Commencer à écouter",
        "de" to "Jetzt hören", "it" to "Inizia ad ascoltare",
        "pt" to "Começar a ouvir", "ru" to "Начать слушать",
        "ja" to "聴き始める", "zh" to "开始聆听",
        "ar" to "ابدأ الاستماع", "ko" to "듣기 시작"
    ),
    "now_playing" to mapOf(
        "tr" to "Şimdi çalıyor", "en" to "Now playing",
        "es" to "Reproduciendo ahora", "fr" to "En cours de lecture",
        "de" to "Wird gerade gespielt", "it" to "In riproduzione",
        "pt" to "Tocando agora", "ru" to "Сейчас играет",
        "ja" to "再生中", "zh" to "正在播放",
        "ar" to "جارٍ التشغيل الآن", "ko" to "현재 재생 중"
    ),
    "nothing_playing" to mapOf(
        "tr" to "Bir parça seçip çalmaya başla", "en" to "Pick a track to start",
        "es" to "Selecciona una pista para empezar", "fr" to "Sélectionnez un titre pour commencer",
        "de" to "Wählen Sie einen Titel zum Starten aus", "it" to "Scegli un brano per iniziare",
        "pt" to "Escolha uma faixa para começar", "ru" to "Выберите трек, чтобы начать",
        "ja" to "トラックを選択して再生を開始", "zh" to "选择一首曲目开始播放",
        "ar" to "اختر مقطعًا للبدء في التشغيل", "ko" to "트랙을 선택하여 재생 시작"
    ),
    "theme_mode" to mapOf(
        "tr" to "Tema modu", "en" to "Theme mode",
        "es" to "Modo de tema", "fr" to "Mode thème",
        "de" to "Themenmodus", "it" to "Modalità tema",
        "pt" to "Modo de tema", "ru" to "Режим темы",
        "ja" to "テーマモード", "zh" to "主题模式",
        "ar" to "وضع المظهر", "ko" to "테마 모드"
    ),
    "theme_dark" to mapOf(
        "tr" to "Koyu", "en" to "Dark",
        "es" to "Oscuro", "fr" to "Sombre",
        "de" to "Dunkel", "it" to "Scuro",
        "pt" to "Escuro", "ru" to "Темная",
        "ja" to "ダーク", "zh" to "深色",
        "ar" to "داكن", "ko" to "다크"
    ),
    "theme_light" to mapOf(
        "tr" to "Açık", "en" to "Light",
        "es" to "Claro", "fr" to "Clair",
        "de" to "Hell", "it" to "Chiaro",
        "pt" to "Claro", "ru" to "Светлая",
        "ja" to "ライト", "zh" to "浅色",
        "ar" to "فاتح", "ko" to "라이트"
    ),
    "theme_amoled" to mapOf(
        "tr" to "AMOLED", "en" to "AMOLED",
        "es" to "AMOLED", "fr" to "AMOLED",
        "de" to "AMOLED", "it" to "AMOLED",
        "pt" to "AMOLED", "ru" to "AMOLED",
        "ja" to "AMOLED", "zh" to "AMOLED",
        "ar" to "AMOLED", "ko" to "AMOLED"
    ),
    "accent_color" to mapOf(
        "tr" to "Vurgu rengi", "en" to "Accent color",
        "es" to "Color de acento", "fr" to "Couleur d'accentuation",
        "de" to "Akzentfarbe", "it" to "Colore di accento",
        "pt" to "Cor de destaque", "ru" to "Цвет акцента",
        "ja" to "アクセントカラー", "zh" to "强调色",
        "ar" to "لون التمييز", "ko" to "강조 색상"
    ),
    "visualizer" to mapOf(
        "tr" to "Görselleştirici", "en" to "Visualizer",
        "es" to "Visualizador", "fr" to "Visualiseur",
        "de" to "Visualizer", "it" to "Visualizzatore",
        "pt" to "Visualizador", "ru" to "Визуализатор",
        "ja" to "ビジュアライザー", "zh" to "可视化器",
        "ar" to "المُصَوِّر", "ko" to "비주얼라이저"
    ),
    "visualizer_desc" to mapOf(
        "tr" to "Çalan müziğin frekans çubuklarını göster", "en" to "Show frequency bars of the playing audio",
        "es" to "Mostrar barras de frecuencia del audio reproduciendo", "fr" to "Afficher les barres de fréquence de l'audio en cours",
        "de" to "Frequenzbalken des abgespielten Audios anzeigen", "it" to "Mostra le barre di frequenza dell'audio in riproduzione",
        "pt" to "Mostrar barras de frequência do áudio em reprodução", "ru" to "Показать полосы частоты воспроизводимого аудио",
        "ja" to "再生中のオーディオの周波数バーを表示", "zh" to "显示正在播放的音频频率条",
        "ar" to "إظهار أشرطة تردد الصوت الجاري تشغيله", "ko" to "재생 중인 오디오의 주파수 막대 표시"
    ),
    "refresh_library" to mapOf(
        "tr" to "Kitaplığı yenile", "en" to "Refresh library",
        "es" to "Actualizar biblioteca", "fr" to "Actualiser la bibliothèque",
        "de" to "Bibliothek aktualisieren", "it" to "Aggiorna libreria",
        "pt" to "Atualizar biblioteca", "ru" to "Обновить библиотеку",
        "ja" to "ライブラリを更新", "zh" to "刷新媒体库",
        "ar" to "تحديث المكتبة", "ko" to "라이브러리 새로고침"
    ),
    "refresh_library_desc" to mapOf(
        "tr" to "Seçili klasörü yeniden tara", "en" to "Rescan the selected folder",
        "es" to "Volver a escanear la carpeta seleccionada", "fr" to "Réanalyser le dossier sélectionné",
        "de" to "Ausgewählten Ordner neu scannen", "it" to "Rianalizza la cartella selezionata",
        "pt" to "Reescanear a pasta selecionada", "ru" to "Повторно отсканировать выбранную папку",
        "ja" to "選択したフォルダを再スキャン", "zh" to "重新扫描所选文件夹",
        "ar" to "إعادة مسح المجلد المحدد", "ko" to "선택한 폴더 다시 스캔"
    ),
    "no_folder" to mapOf(
        "tr" to "Klasör seçilmedi", "en" to "No folder selected",
        "es" to "No se seleccionó carpeta", "fr" to "Aucun dossier sélectionné",
        "de" to "Kein Ordner ausgewählt", "it" to "Nessuna cartella selezionata",
        "pt" to "Nenhuma pasta selecionada", "ru" to "Папка не выбрана",
        "ja" to "フォルダが選択されていません", "zh" to "未选择文件夹",
        "ar" to "لم يتم تحديد مجلد", "ko" to "폴더가 선택되지 않음"
    ),
    "total_duration" to mapOf(
        "tr" to "Toplam süre", "en" to "Total duration",
        "es" to "Duración total", "fr" to "Durée totale",
        "de" to "Gesamtdauer", "it" to "Durata totale",
        "pt" to "Duração total", "ru" to "Общая продолжительность",
        "ja" to "総再生時間", "zh" to "总时长",
        "ar" to "المدة الإجمالية", "ko" to "총 재생 시간"
    ),
    "tracks" to mapOf(
        "tr" to "parça", "en" to "tracks",
        "es" to "pistas", "fr" to "titres",
        "de" to "Titel", "it" to "brani",
        "pt" to "faixas", "ru" to "треков",
        "ja" to "トラック", "zh" to "首曲目",
        "ar" to "مسارات", "ko" to "개 트랙"
    ),
    "recent_tracks" to mapOf(
        "tr" to "Son eklenenler", "en" to "Recently added",
        "es" to "Añadidas recientemente", "fr" to "Ajoutées récemment",
        "de" to "Kürzlich hinzugefügt", "it" to "Aggiunte di recente",
        "pt" to "Adicionadas recentemente", "ru" to "Недавно добавленные",
        "ja" to "最近追加された", "zh" to "最近添加",
        "ar" to "المضافة مؤخرًا", "ko" to "최근 추가됨"
    ),
    "desktop_build" to mapOf(
        "tr" to "Windows sürümü", "en" to "Windows build",
        "es" to "Compilación de Windows", "fr" to "Version Windows",
        "de" to "Windows-Build", "it" to "Build Windows",
        "pt" to "Compilação do Windows", "ru" to "Сборка Windows",
        "ja" to "Windows版", "zh" to "Windows版本",
        "ar" to "إصدار ويندوز", "ko" to "Windows 빌드"
    ),
    "supported_formats" to mapOf(
        "tr" to "Desteklenen biçimler", "en" to "Supported formats",
        "es" to "Formatos soportados", "fr" to "Formats pris en charge",
        "de" to "Unterstützte Formate", "it" to "Formati supportati",
        "pt" to "Formatos suportados", "ru" to "Поддерживаемые форматы",
        "ja" to "サポートされている形式", "zh" to "支持的格式",
        "ar" to "التنسيقات المدعومة", "ko" to "지원되는 형식"
    ),
    "sleep_timer_off" to mapOf(
        "tr" to "Kapalı", "en" to "Off",
        "es" to "Apagado", "fr" to "Désactivé",
        "de" to "Aus", "it" to "Spento",
        "pt" to "Desligado", "ru" to "Выключен",
        "ja" to "オフ", "zh" to "关闭",
        "ar" to "مغالق", "ko" to "끔"
    ),
    "sleep_remaining" to mapOf(
        "tr" to "Kalan süre", "en" to "Remaining",
        "es" to "Restante", "fr" to "Restant",
        "de" to "Verbleibend", "it" to "Rimanente",
        "pt" to "Restante", "ru" to "Осталось",
        "ja" to "残り時間", "zh" to "剩余时间",
        "ar" to "المتبقي", "ko" to "남은 시간"
    ),
    "min_short" to mapOf(
        "tr" to "dk", "en" to "min",
        "es" to "min", "fr" to "min",
        "de" to "Min", "it" to "min",
        "pt" to "min", "ru" to "мин",
        "ja" to "分", "zh" to "分",
        "ar" to "دقيقة", "ko" to "분"
    ),
    "library_hint" to mapOf(
        "tr" to "Müzik klasörünü seç", "en" to "Choose your music folder",
        "es" to "Selecciona tu carpeta de música", "fr" to "Choisissez votre dossier de musique",
        "de" to "Wählen Sie Ihren Musikordner aus", "it" to "Scegli la tua cartella musicale",
        "pt" to "Escolha sua pasta de música", "ru" to "Выберите папку с музыкой",
        "ja" to "ミュージックフォルダを選択", "zh" to "选择你的音乐文件夹",
        "ar" to "اختر مجلد الموسيقى الخاص بك", "ko" to "음악 폴더 선택"
    ),
    "playback_section" to mapOf(
        "tr" to "Çalma", "en" to "Playback",
        "es" to "Reproducción", "fr" to "Lecture",
        "de" to "Wiedergabe", "it" to "Riproduzione",
        "pt" to "Reprodução", "ru" to "Воспроизведение",
        "ja" to "再生", "zh" to "播放",
        "ar" to "التشغيل", "ko" to "재생"
    ),
    "sort_by" to mapOf(
        "tr" to "Sırala", "en" to "Sort",
        "es" to "Ordenar", "fr" to "Trier",
        "de" to "Sortieren", "it" to "Ordina",
        "pt" to "Ordenar", "ru" to "Сортировка",
        "ja" to "並べ替え", "zh" to "排序",
        "ar" to "ترتيب", "ko" to "정렬"
    ),
    "sort_title" to mapOf(
        "tr" to "Ada göre", "en" to "By name",
        "es" to "Por nombre", "fr" to "Par nom",
        "de" to "Nach Name", "it" to "Per nome",
        "pt" to "Por nome", "ru" to "По имени",
        "ja" to "名前順", "zh" to "按名称",
        "ar" to "حسب الاسم", "ko" to "이름순"
    ),
    "all_media" to mapOf(
        "tr" to "Tümü", "en" to "All",
        "es" to "Todos", "fr" to "Tous",
        "de" to "Alle", "it" to "Tutti",
        "pt" to "Todos", "ru" to "Все",
        "ja" to "すべて", "zh" to "全部",
        "ar" to "الكل", "ko" to "전체"
    ),
    "music" to mapOf(
        "tr" to "Müzik", "en" to "Music",
        "es" to "Música", "fr" to "Musique",
        "de" to "Musik", "it" to "Musica",
        "pt" to "Música", "ru" to "Музыка",
        "ja" to "ミュージック", "zh" to "音乐",
        "ar" to "موسيقى", "ko" to "음악"
    ),
    "videos" to mapOf(
        "tr" to "Videolar", "en" to "Videos",
        "es" to "Vídeos", "fr" to "Vidéos",
        "de" to "Videos", "it" to "Video",
        "pt" to "Vídeos", "ru" to "Видео",
        "ja" to "動画", "zh" to "视频",
        "ar" to "مقاطع فيديو", "ko" to "비디오"
    ),
    "photos" to mapOf(
        "tr" to "Fotoğraflar", "en" to "Photos",
        "es" to "Fotos", "fr" to "Photos",
        "de" to "Fotos", "it" to "Foto",
        "pt" to "Fotos", "ru" to "Фото",
        "ja" to "写真", "zh" to "照片",
        "ar" to "صور", "ko" to "사진"
    ),
    "sort_recent" to mapOf(
        "tr" to "Tarihe göre", "en" to "By date",
        "es" to "Por fecha", "fr" to "Par date",
        "de" to "Nach Datum", "it" to "Per data",
        "pt" to "Por data", "ru" to "По дате",
        "ja" to "日付順", "zh" to "按日期",
        "ar" to "حسب التاريخ", "ko" to "날짜순"
    ),
)

fun Strings.dt(key: String): String {
    val extra = desktopStrings[key]
    if (extra != null) return extra[lang.code] ?: extra["en"] ?: get(key)
    return get(key)
}

/** 1:05:20 / 4:12 biçiminde süre. */
fun formatLongTime(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, total % 60)
    else "%d:%02d".format(minutes, total % 60)
}
