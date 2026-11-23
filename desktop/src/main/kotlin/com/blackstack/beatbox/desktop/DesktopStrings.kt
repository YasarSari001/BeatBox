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
    "lyrics" to mapOf(
        "tr" to "Sözler", "en" to "Lyrics",
        "es" to "Letras", "fr" to "Paroles",
        "de" to "Songtext", "it" to "Testi",
        "pt" to "Letras", "ru" to "Текст песни",
        "ja" to "歌詞", "zh" to "歌词",
        "ar" to "كلمات الأغنية", "ko" to "가사"
    ),
    "lyrics_source" to mapOf(
        "tr" to "Kaynak", "en" to "Source",
        "es" to "Fuente", "fr" to "Source",
        "de" to "Quelle", "it" to "Fonte",
        "pt" to "Fonte", "ru" to "Источник",
        "ja" to "提供元", "zh" to "来源",
        "ar" to "المصدر", "ko" to "출처"
    ),
    "lyrics_refresh" to mapOf(
        "tr" to "Sözleri yenile", "en" to "Refresh lyrics",
        "es" to "Actualizar letras", "fr" to "Actualiser les paroles",
        "de" to "Songtext aktualisieren", "it" to "Aggiorna testi",
        "pt" to "Atualizar letras", "ru" to "Обновить текст",
        "ja" to "歌詞を更新", "zh" to "刷新歌词",
        "ar" to "تحديث الكلمات", "ko" to "가사 새로고침"
    ),
    "lyrics_find_hint" to mapOf(
        "tr" to "Bir şarkı çalarken sözler burada görünür.", "en" to "Play a track to see its lyrics here.",
        "es" to "Reproduce una pista para ver su letra aquí.", "fr" to "Lancez un titre pour voir ses paroles ici.",
        "de" to "Spiele einen Titel ab, um hier den Songtext zu sehen.", "it" to "Riproduci un brano per vedere qui il testo.",
        "pt" to "Reproduza uma faixa para ver a letra aqui.", "ru" to "Включите трек, чтобы увидеть здесь текст песни.",
        "ja" to "曲を再生するとここに歌詞が表示されます。", "zh" to "播放歌曲即可在此查看歌词。",
        "ar" to "شغّل أغنية لعرض كلماتها هنا.", "ko" to "곡을 재생하면 여기에 가사가 표시됩니다."
    ),
    "lyrics_loading" to mapOf(
        "tr" to "Sözler yükleniyor…", "en" to "Loading lyrics…",
        "es" to "Cargando letras…", "fr" to "Chargement des paroles…",
        "de" to "Songtext wird geladen…", "it" to "Caricamento dei testi…",
        "pt" to "Carregando letras…", "ru" to "Загрузка текста…",
        "ja" to "歌詞を読み込み中…", "zh" to "正在加载歌词…",
        "ar" to "جارٍ تحميل الكلمات…", "ko" to "가사를 불러오는 중…"
    ),
    "lyrics_not_found" to mapOf(
        "tr" to "Bu şarkı için söz bulunamadı.", "en" to "No lyrics found for this song.",
        "es" to "No se encontraron letras para esta canción.", "fr" to "Aucune parole trouvée pour ce titre.",
        "de" to "Für diesen Titel wurde kein Songtext gefunden.", "it" to "Nessun testo trovato per questo brano.",
        "pt" to "Nenhuma letra encontrada para esta música.", "ru" to "Для этой песни текст не найден.",
        "ja" to "この曲の歌詞は見つかりませんでした。", "zh" to "未找到这首歌的歌词。",
        "ar" to "لم يتم العثور على كلمات لهذه الأغنية.", "ko" to "이 곡의 가사를 찾을 수 없습니다."
    ),
    "lyrics_network_error" to mapOf(
        "tr" to "Sözlere ulaşılamadı. İnternet bağlantınızı kontrol edin.",
        "en" to "Couldn't reach the lyrics service. Check your connection.",
        "es" to "No se pudo conectar con el servicio de letras. Comprueba tu conexión.",
        "fr" to "Impossible de joindre le service de paroles. Vérifiez votre connexion.",
        "de" to "Songtext-Dienst nicht erreichbar. Prüfe deine Verbindung.",
        "it" to "Impossibile raggiungere il servizio dei testi. Controlla la connessione.",
        "pt" to "Não foi possível acessar o serviço de letras. Verifique sua conexão.",
        "ru" to "Не удалось подключиться к сервису текстов. Проверьте соединение.",
        "ja" to "歌詞サービスに接続できませんでした。接続を確認してください。",
        "zh" to "无法连接歌词服务，请检查网络连接。",
        "ar" to "تعذّر الوصول إلى خدمة الكلمات. تحقّق من اتصالك.",
        "ko" to "가사 서비스에 연결할 수 없습니다. 연결을 확인하세요."
    ),
    "lyrics_unsynced" to mapOf(
        "tr" to "Zaman damgası yok — düz metin olarak gösteriliyor.",
        "en" to "No timestamps — showing plain text.",
        "es" to "Sin marcas de tiempo: se muestra como texto plano.",
        "fr" to "Pas d'horodatage — affichage en texte brut.",
        "de" to "Keine Zeitstempel – als reiner Text angezeigt.",
        "it" to "Nessun timestamp: mostrato come testo semplice.",
        "pt" to "Sem carimbos de tempo — exibindo texto simples.",
        "ru" to "Нет временных меток — показан обычный текст.",
        "ja" to "タイムスタンプなし — プレーンテキストで表示中。",
        "zh" to "无时间戳——以纯文本显示。",
        "ar" to "لا توجد طوابع زمنية — يُعرض كنص عادي.",
        "ko" to "타임스탬프 없음 — 일반 텍스트로 표시 중."
    ),
    "tray_show" to mapOf(
        "tr" to "BeatBox'ı göster", "en" to "Show BeatBox",
        "es" to "Mostrar BeatBox", "fr" to "Afficher BeatBox",
        "de" to "BeatBox anzeigen", "it" to "Mostra BeatBox",
        "pt" to "Mostrar BeatBox", "ru" to "Показать BeatBox",
        "ja" to "BeatBoxを表示", "zh" to "显示 BeatBox",
        "ar" to "إظهار BeatBox", "ko" to "BeatBox 표시"
    ),
    "tray_exit" to mapOf(
        "tr" to "Çıkış", "en" to "Exit",
        "es" to "Salir", "fr" to "Quitter",
        "de" to "Beenden", "it" to "Esci",
        "pt" to "Sair", "ru" to "Выход",
        "ja" to "終了", "zh" to "退出",
        "ar" to "خروج", "ko" to "종료"
    ),
    "close_to_tray" to mapOf(
        "tr" to "Kapatınca sistem tepsisinde kal",
        "en" to "Keep running in the system tray when closed",
        "es" to "Seguir en la bandeja del sistema al cerrar",
        "fr" to "Rester dans la zone de notification à la fermeture",
        "de" to "Beim Schließen im Infobereich bleiben",
        "it" to "Resta nella barra delle applicazioni alla chiusura",
        "pt" to "Continuar na bandeja do sistema ao fechar",
        "ru" to "Оставаться в трее при закрытии",
        "ja" to "閉じてもタスクトレイに残す",
        "zh" to "关闭后保留在系统托盘",
        "ar" to "البقاء في شريط النظام عند الإغلاق",
        "ko" to "닫아도 시스템 트레이에 유지"
    ),
    "close_to_tray_desc" to mapOf(
        "tr" to "Kapat düğmesi pencereyi gizler; müzik çalmaya devam eder.",
        "en" to "The close button hides the window and playback keeps going.",
        "es" to "El botón cerrar oculta la ventana y la reproducción continúa.",
        "fr" to "Le bouton Fermer masque la fenêtre et la lecture continue.",
        "de" to "Die Schaltfläche „Schließen“ blendet das Fenster aus, die Wiedergabe läuft weiter.",
        "it" to "Il pulsante Chiudi nasconde la finestra e la riproduzione continua.",
        "pt" to "O botão fechar oculta a janela e a reprodução continua.",
        "ru" to "Кнопка закрытия скрывает окно, воспроизведение продолжается.",
        "ja" to "閉じるボタンはウィンドウを隠し、再生は続きます。",
        "zh" to "关闭按钮会隐藏窗口，播放继续进行。",
        "ar" to "زر الإغلاق يخفي النافذة ويستمر التشغيل.",
        "ko" to "닫기 버튼은 창을 숨기고 재생은 계속됩니다."
    ),
    "tag_editor" to mapOf(
        "tr" to "Etiketleri düzenle", "en" to "Edit tags",
        "es" to "Editar etiquetas", "fr" to "Modifier les étiquettes",
        "de" to "Tags bearbeiten", "it" to "Modifica tag",
        "pt" to "Editar etiquetas", "ru" to "Изменить теги",
        "ja" to "タグを編集", "zh" to "编辑标签",
        "ar" to "تعديل الوسوم", "ko" to "태그 편집"
    ),
    "tag_save" to mapOf(
        "tr" to "Kaydet", "en" to "Save",
        "es" to "Guardar", "fr" to "Enregistrer",
        "de" to "Speichern", "it" to "Salva",
        "pt" to "Salvar", "ru" to "Сохранить",
        "ja" to "保存", "zh" to "保存",
        "ar" to "حفظ", "ko" to "저장"
    ),
    "tag_save_failed" to mapOf(
        "tr" to "Etiketler kaydedilemedi.", "en" to "Couldn't save the tags.",
        "es" to "No se pudieron guardar las etiquetas.", "fr" to "Impossible d'enregistrer les étiquettes.",
        "de" to "Tags konnten nicht gespeichert werden.", "it" to "Impossibile salvare i tag.",
        "pt" to "Não foi possível salvar as etiquetas.", "ru" to "Не удалось сохранить теги.",
        "ja" to "タグを保存できませんでした。", "zh" to "无法保存标签。",
        "ar" to "تعذّر حفظ الوسوم.", "ko" to "태그를 저장할 수 없습니다."
    ),
    "tag_album" to mapOf(
        "tr" to "Albüm", "en" to "Album",
        "es" to "Álbum", "fr" to "Album",
        "de" to "Album", "it" to "Album",
        "pt" to "Álbum", "ru" to "Альбом",
        "ja" to "アルバム", "zh" to "专辑",
        "ar" to "الألبوم", "ko" to "앨범"
    ),
    "tag_genre" to mapOf(
        "tr" to "Tür", "en" to "Genre",
        "es" to "Género", "fr" to "Genre",
        "de" to "Genre", "it" to "Genere",
        "pt" to "Gênero", "ru" to "Жанр",
        "ja" to "ジャンル", "zh" to "流派",
        "ar" to "النوع", "ko" to "장르"
    ),
    "tag_year" to mapOf(
        "tr" to "Yıl", "en" to "Year",
        "es" to "Año", "fr" to "Année",
        "de" to "Jahr", "it" to "Anno",
        "pt" to "Ano", "ru" to "Год",
        "ja" to "年", "zh" to "年份",
        "ar" to "السنة", "ko" to "연도"
    ),
    "tag_track_number" to mapOf(
        "tr" to "Parça no", "en" to "Track no",
        "es" to "N.º de pista", "fr" to "N° de piste",
        "de" to "Titelnummer", "it" to "N. traccia",
        "pt" to "N.º da faixa", "ru" to "Номер трека",
        "ja" to "トラック番号", "zh" to "音轨号",
        "ar" to "رقم المقطع", "ko" to "트랙 번호"
    ),
    "repeat_off" to mapOf(
        "tr" to "Tekrar kapalı", "en" to "Repeat off",
        "es" to "Repetición desactivada", "fr" to "Répétition désactivée",
        "de" to "Wiederholung aus", "it" to "Ripetizione disattivata",
        "pt" to "Repetição desativada", "ru" to "Повтор выключен",
        "ja" to "リピートオフ", "zh" to "关闭循环",
        "ar" to "التكرار متوقف", "ko" to "반복 끄기"
    ),
    "repeat_all" to mapOf(
        "tr" to "Tümünü tekrarla", "en" to "Repeat all",
        "es" to "Repetir todo", "fr" to "Répéter tout",
        "de" to "Alle wiederholen", "it" to "Ripeti tutto",
        "pt" to "Repetir tudo", "ru" to "Повторять все",
        "ja" to "すべてリピート", "zh" to "循环播放全部",
        "ar" to "تكرار الكل", "ko" to "전체 반복"
    ),
    "repeat_one" to mapOf(
        "tr" to "Şarkıyı tekrarla", "en" to "Repeat one",
        "es" to "Repetir una canción", "fr" to "Répéter le titre",
        "de" to "Titel wiederholen", "it" to "Ripeti il brano",
        "pt" to "Repetir uma faixa", "ru" to "Повторять трек",
        "ja" to "1曲リピート", "zh" to "单曲循环",
        "ar" to "تكرار أغنية واحدة", "ko" to "한 곡 반복"
    ),
    "queue" to mapOf(
        "tr" to "Kuyruk", "en" to "Queue",
        "es" to "Cola", "fr" to "File d'attente",
        "de" to "Warteschlange", "it" to "Coda",
        "pt" to "Fila", "ru" to "Очередь",
        "ja" to "キュー", "zh" to "播放队列",
        "ar" to "قائمة التشغيل", "ko" to "재생 목록"
    ),
    "queue_count" to mapOf(
        "tr" to "%d parça", "en" to "%d tracks",
        "es" to "%d pistas", "fr" to "%d titres",
        "de" to "%d Titel", "it" to "%d brani",
        "pt" to "%d faixas", "ru" to "%d треков",
        "ja" to "%d 曲", "zh" to "%d 首",
        "ar" to "%d مقطع", "ko" to "%d곡"
    ),
    "queue_clear" to mapOf(
        "tr" to "Kuyruğu temizle", "en" to "Clear queue",
        "es" to "Vaciar la cola", "fr" to "Vider la file",
        "de" to "Warteschlange leeren", "it" to "Svuota la coda",
        "pt" to "Limpar a fila", "ru" to "Очистить очередь",
        "ja" to "キューをクリア", "zh" to "清空队列",
        "ar" to "مسح قائمة التشغيل", "ko" to "재생 목록 비우기"
    ),
    "queue_empty" to mapOf(
        "tr" to "Kuyruk boş. Kütüphaneden parça ekleyin.", "en" to "The queue is empty. Add tracks from your library.",
        "es" to "La cola está vacía. Añade pistas desde la biblioteca.", "fr" to "La file est vide. Ajoutez des titres depuis la bibliothèque.",
        "de" to "Die Warteschlange ist leer. Füge Titel aus der Bibliothek hinzu.", "it" to "La coda è vuota. Aggiungi brani dalla libreria.",
        "pt" to "A fila está vazia. Adicione faixas da biblioteca.", "ru" to "Очередь пуста. Добавьте треки из библиотеки.",
        "ja" to "キューは空です。ライブラリから曲を追加してください。", "zh" to "队列为空。请从音乐库添加歌曲。",
        "ar" to "قائمة التشغيل فارغة. أضف مقاطع من المكتبة.", "ko" to "재생 목록이 비어 있습니다. 보관함에서 곡을 추가하세요."
    ),
    "queue_remove" to mapOf(
        "tr" to "Kuyruktan çıkar", "en" to "Remove from queue",
        "es" to "Quitar de la cola", "fr" to "Retirer de la file",
        "de" to "Aus Warteschlange entfernen", "it" to "Rimuovi dalla coda",
        "pt" to "Remover da fila", "ru" to "Убрать из очереди",
        "ja" to "キューから削除", "zh" to "从队列移除",
        "ar" to "إزالة من قائمة التشغيل", "ko" to "재생 목록에서 제거"
    ),
    "queue_add" to mapOf(
        "tr" to "Kuyruğa ekle", "en" to "Add to queue",
        "es" to "Añadir a la cola", "fr" to "Ajouter à la file",
        "de" to "Zur Warteschlange hinzufügen", "it" to "Aggiungi alla coda",
        "pt" to "Adicionar à fila", "ru" to "Добавить в очередь",
        "ja" to "キューに追加", "zh" to "加入队列",
        "ar" to "إضافة إلى قائمة التشغيل", "ko" to "재생 목록에 추가"
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
