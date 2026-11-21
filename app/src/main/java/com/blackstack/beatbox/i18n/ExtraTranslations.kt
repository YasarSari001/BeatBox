package com.blackstack.beatbox.i18n
/**
 * Arayüzde sonradan ihtiyaç duyulan ek çeviriler.
 *
 * Ana çeviri tablosu [Models.kt] içinde; bu dosya onu şişirmemek için
 * ayrı tutulur ve `Strings` sınıfında ana tabloyla birleştirilir.
 */
internal val extraTranslations: Map<String, Map<String, String>> = mapOf(
    "register" to mapOf(
        "en" to "Sign Up", "tr" to "Kayıt Ol", "es" to "Registrarse", "fr" to "S'inscrire",
        "de" to "Registrieren", "it" to "Registrati", "pt" to "Cadastrar",
        "ru" to "Зарегистрироваться", "ja" to "新規登録", "zh" to "注册",
        "ar" to "إنشاء حساب", "ko" to "회원가입"
    ),
    "continue_guest" to mapOf(
        "en" to "Continue as guest", "tr" to "Misafir Olarak Devam Et",
        "es" to "Continuar como invitado", "fr" to "Continuer en tant qu'invité",
        "de" to "Als Gast fortfahren", "it" to "Continua come ospite",
        "pt" to "Continuar como convidado", "ru" to "Продолжить как гость",
        "ja" to "ゲストとして続行", "zh" to "以访客身份继续",
        "ar" to "المتابعة كضيف", "ko" to "게스트로 계속"
    ),
    "email" to mapOf(
        "en" to "Email", "tr" to "E-posta", "es" to "Correo electrónico", "fr" to "E-mail",
        "de" to "E-Mail", "it" to "Email", "pt" to "E-mail", "ru" to "Эл. почта",
        "ja" to "メールアドレス", "zh" to "电子邮箱", "ar" to "البريد الإلكتروني", "ko" to "이메일"
    ),
    "password" to mapOf(
        "en" to "Password", "tr" to "Şifre", "es" to "Contraseña", "fr" to "Mot de passe",
        "de" to "Passwort", "it" to "Password", "pt" to "Senha", "ru" to "Пароль",
        "ja" to "パスワード", "zh" to "密码", "ar" to "كلمة المرور", "ko" to "비밀번호"
    ),
    "forgot_password" to mapOf(
        "en" to "Forgot password", "tr" to "Şifremi unuttum", "es" to "Olvidé mi contraseña",
        "fr" to "Mot de passe oublié", "de" to "Passwort vergessen", "it" to "Password dimenticata",
        "pt" to "Esqueci a senha", "ru" to "Забыли пароль?", "ja" to "パスワードを忘れた",
        "zh" to "忘记密码", "ar" to "نسيت كلمة المرور", "ko" to "비밀번호 찾기"
    ),
    "full_name" to mapOf(
        "en" to "Full name", "tr" to "Ad Soyad", "es" to "Nombre completo", "fr" to "Nom complet",
        "de" to "Vollständiger Name", "it" to "Nome completo", "pt" to "Nome completo",
        "ru" to "Имя и фамилия", "ja" to "氏名", "zh" to "姓名", "ar" to "الاسم الكامل", "ko" to "이름"
    ),
    "password_min" to mapOf(
        "en" to "Password (min. 6 characters)", "tr" to "Şifre (en az 6 karakter)",
        "es" to "Contraseña (mín. 6 caracteres)", "fr" to "Mot de passe (6 caractères min.)",
        "de" to "Passwort (mind. 6 Zeichen)", "it" to "Password (min. 6 caratteri)",
        "pt" to "Senha (mín. 6 caracteres)", "ru" to "Пароль (не менее 6 символов)",
        "ja" to "パスワード（6文字以上）", "zh" to "密码（至少6个字符）",
        "ar" to "كلمة المرور (6 أحرف على الأقل)", "ko" to "비밀번호 (최소 6자)"
    ),
    "register_hint" to mapOf(
        "en" to "A verification link will be sent to your email after registration.",
        "tr" to "Kayıt sonrası e-posta adresinize doğrulama bağlantısı gönderilir.",
        "es" to "Se enviará un enlace de verificación a tu correo tras registrarte.",
        "fr" to "Un lien de vérification sera envoyé à votre e-mail après l'inscription.",
        "de" to "Nach der Registrierung wird ein Bestätigungslink an Ihre E-Mail gesendet.",
        "it" to "Dopo la registrazione riceverai un link di verifica via email.",
        "pt" to "Um link de verificação será enviado ao seu e-mail após o cadastro.",
        "ru" to "После регистрации на вашу почту придёт ссылка для подтверждения.",
        "ja" to "登録後に確認リンクがメールで送信されます。",
        "zh" to "注册后将向您的邮箱发送验证链接。",
        "ar" to "سيتم إرسال رابط تحقق إلى بريدك الإلكتروني بعد التسجيل.",
        "ko" to "가입 후 이메일로 인증 링크가 전송됩니다."
    ),
    "or" to mapOf(
        "en" to "or", "tr" to "veya", "es" to "o", "fr" to "ou", "de" to "oder",
        "it" to "o", "pt" to "ou", "ru" to "или", "ja" to "または", "zh" to "或",
        "ar" to "أو", "ko" to "또는"
    ),
    "continue_google" to mapOf(
        "en" to "Continue with Google", "tr" to "Google ile devam et",
        "es" to "Continuar con Google", "fr" to "Continuer avec Google",
        "de" to "Mit Google fortfahren", "it" to "Continua con Google",
        "pt" to "Continuar com o Google", "ru" to "Продолжить с Google",
        "ja" to "Google で続行", "zh" to "使用 Google 继续",
        "ar" to "المتابعة باستخدام Google", "ko" to "Google로 계속"
    ),
    "guest_available" to mapOf(
        "en" to "Guest login is available.", "tr" to "Misafir girişi kullanılabilir.",
        "es" to "El inicio de sesión como invitado está disponible.",
        "fr" to "La connexion invité est disponible.",
        "de" to "Gastanmeldung ist verfügbar.",
        "it" to "L'accesso come ospite è disponibile.",
        "pt" to "O login como convidado está disponível.",
        "ru" to "Доступен гостевой вход.",
        "ja" to "ゲストログインを利用できます。", "zh" to "可使用访客登录。",
        "ar" to "تسجيل الدخول كضيف متاح.", "ko" to "게스트 로그인을 사용할 수 있습니다."
    ),
    "save" to mapOf(
        "en" to "Save", "tr" to "Kaydet", "es" to "Guardar", "fr" to "Enregistrer",
        "de" to "Speichern", "it" to "Salva", "pt" to "Salvar", "ru" to "Сохранить",
        "ja" to "保存", "zh" to "保存", "ar" to "حفظ", "ko" to "저장"
    ),
    "listener" to mapOf(
        "en" to "Listener", "tr" to "Dinleyici", "es" to "Oyente", "fr" to "Auditeur",
        "de" to "Hörer", "it" to "Ascoltatore", "pt" to "Ouvinte", "ru" to "Слушатель",
        "ja" to "リスナー", "zh" to "听众", "ar" to "مستمع", "ko" to "청취자"
    ),
    "logout" to mapOf(
        "en" to "Log out", "tr" to "Çıkış Yap", "es" to "Cerrar sesión", "fr" to "Se déconnecter",
        "de" to "Abmelden", "it" to "Esci", "pt" to "Sair", "ru" to "Выйти",
        "ja" to "ログアウト", "zh" to "退出登录", "ar" to "تسجيل الخروج", "ko" to "로그아웃"
    ),
    "edit_profile" to mapOf(
        "en" to "Edit profile", "tr" to "Profil Düzenle", "es" to "Editar perfil",
        "fr" to "Modifier le profil", "de" to "Profil bearbeiten", "it" to "Modifica profilo",
        "pt" to "Editar perfil", "ru" to "Редактировать профиль",
        "ja" to "プロフィールを編集", "zh" to "编辑个人资料",
        "ar" to "تعديل الملف الشخصي", "ko" to "프로필 편집"
    ),
    "session" to mapOf(
        "en" to "Session", "tr" to "Oturum", "es" to "Sesión", "fr" to "Session",
        "de" to "Sitzung", "it" to "Sessione", "pt" to "Sessão", "ru" to "Сеанс",
        "ja" to "セッション", "zh" to "会话", "ar" to "الجلسة", "ko" to "세션"
    ),
    "choose_avatar" to mapOf(
        "en" to "Choose avatar", "tr" to "Avatar Seçin", "es" to "Elegir avatar",
        "fr" to "Choisir un avatar", "de" to "Avatar wählen", "it" to "Scegli avatar",
        "pt" to "Escolher avatar", "ru" to "Выбрать аватар",
        "ja" to "アバターを選択", "zh" to "选择头像", "ar" to "اختر صورة رمزية", "ko" to "아바타 선택"
    ),
    "display_name" to mapOf(
        "en" to "Display name", "tr" to "Görünen Ad", "es" to "Nombre visible",
        "fr" to "Nom affiché", "de" to "Anzeigename", "it" to "Nome visualizzato",
        "pt" to "Nome de exibição", "ru" to "Отображаемое имя",
        "ja" to "表示名", "zh" to "显示名称", "ar" to "الاسم المعروض", "ko" to "표시 이름"
    ),
    "about_bio" to mapOf(
        "en" to "About / Bio", "tr" to "Hakkımda / Bio", "es" to "Acerca de / Bio",
        "fr" to "À propos / Bio", "de" to "Über mich / Bio", "it" to "Informazioni / Bio",
        "pt" to "Sobre / Bio", "ru" to "О себе / Био", "ja" to "自己紹介 / バイオ",
        "zh" to "关于 / 简介", "ar" to "نبذة / سيرة", "ko" to "소개 / 약력"
    ),
    "phone" to mapOf(
        "en" to "Phone", "tr" to "Telefon", "es" to "Teléfono", "fr" to "Téléphone",
        "de" to "Telefon", "it" to "Telefono", "pt" to "Telefone", "ru" to "Телефон",
        "ja" to "電話番号", "zh" to "电话", "ar" to "الهاتف", "ko" to "전화번호"
    ),
    "unlimited_music" to mapOf(
        "en" to "Unlimited music & lossless audio active",
        "tr" to "Sınırsız müzik ve kayıpsız ses aktif",
        "es" to "Música ilimitada y audio sin pérdidas activo",
        "fr" to "Musique illimitée et audio sans perte actif",
        "de" to "Unbegrenzte Musik und verlustfreies Audio aktiv",
        "it" to "Musica illimitata e audio senza perdite attivo",
        "pt" to "Música ilimitada e áudio sem perdas ativo",
        "ru" to "Безлимитная музыка и звук без потерь активны",
        "ja" to "無制限の音楽とロスレス音声が有効",
        "zh" to "无限音乐和无损音频已启用",
        "ar" to "موسيقى غير محدودة وصوت بلا فقدان مفعّل",
        "ko" to "무제한 음악 및 무손실 오디오 활성화"
    ),
    "welcome_back" to mapOf(
        "en" to "Welcome back", "tr" to "Tekrar hoş geldin", "es" to "Bienvenido de nuevo",
        "fr" to "Bon retour", "de" to "Willkommen zurück", "it" to "Bentornato",
        "pt" to "Bem-vindo de volta", "ru" to "С возвращением", "ja" to "おかえりなさい",
        "zh" to "欢迎回来", "ar" to "مرحبًا بعودتك", "ko" to "다시 오신 것을 환영합니다"
    ),
    "profile_updated" to mapOf(
        "en" to "Profile updated", "tr" to "Profil güncellendi", "es" to "Perfil actualizado",
        "fr" to "Profil mis à jour", "de" to "Profil aktualisiert", "it" to "Profilo aggiornato",
        "pt" to "Perfil atualizado", "ru" to "Профиль обновлён",
        "ja" to "プロフィールを更新しました", "zh" to "个人资料已更新",
        "ar" to "تم تحديث الملف الشخصي", "ko" to "프로필이 업데이트되었습니다"
    ),
    "logged_out" to mapOf(
        "en" to "Signed out", "tr" to "Oturum kapatıldı", "es" to "Sesión cerrada",
        "fr" to "Déconnecté", "de" to "Abgemeldet", "it" to "Disconnesso",
        "pt" to "Sessão encerrada", "ru" to "Выход выполнен", "ja" to "ログアウトしました",
        "zh" to "已退出登录", "ar" to "تم تسجيل الخروج", "ko" to "로그아웃되었습니다"
    ),
    "voice_search_unsupported" to mapOf(
        "en" to "Voice search is not supported", "tr" to "Sesli arama desteklenmiyor",
        "es" to "La búsqueda por voz no es compatible",
        "fr" to "La recherche vocale n'est pas prise en charge",
        "de" to "Sprachsuche wird nicht unterstützt",
        "it" to "La ricerca vocale non è supportata",
        "pt" to "A pesquisa por voz não é suportada",
        "ru" to "Голосовой поиск не поддерживается",
        "ja" to "音声検索はサポートされていません", "zh" to "不支持语音搜索",
        "ar" to "البحث الصوتي غير مدعوم", "ko" to "음성 검색이 지원되지 않습니다"
    ),
    "permission_required" to mapOf(
        "en" to "Permission required", "tr" to "İzin Gerekli", "es" to "Permiso necesario",
        "fr" to "Autorisation requise", "de" to "Berechtigung erforderlich",
        "it" to "Autorizzazione necessaria", "pt" to "Permissão necessária",
        "ru" to "Требуется разрешение", "ja" to "権限が必要です", "zh" to "需要权限",
        "ar" to "الإذن مطلوب", "ko" to "권한 필요"
    ),
    "grant_permission" to mapOf(
        "en" to "Grant permission", "tr" to "İzin Ver", "es" to "Conceder permiso",
        "fr" to "Autoriser", "de" to "Erlauben", "it" to "Consenti",
        "pt" to "Conceder permissão", "ru" to "Разрешить", "ja" to "許可する",
        "zh" to "授予权限", "ar" to "منح الإذن", "ko" to "권한 허용"
    ),
    "user_profile" to mapOf(
        "en" to "User profile", "tr" to "Kullanıcı profili", "es" to "Perfil de usuario",
        "fr" to "Profil utilisateur", "de" to "Benutzerprofil", "it" to "Profilo utente",
        "pt" to "Perfil do usuário", "ru" to "Профиль пользователя",
        "ja" to "ユーザープロフィール", "zh" to "用户资料",
        "ar" to "ملف المستخدم", "ko" to "사용자 프로필"
    ),
    "voice_search" to mapOf(
        "en" to "Voice search", "tr" to "Sesli arama", "es" to "Búsqueda por voz",
        "fr" to "Recherche vocale", "de" to "Sprachsuche", "it" to "Ricerca vocale",
        "pt" to "Pesquisa por voz", "ru" to "Голосовой поиск", "ja" to "音声検索",
        "zh" to "语音搜索", "ar" to "البحث الصوتي", "ko" to "음성 검색"
    ),
    "sleep_timer_active" to mapOf(
        "en" to "Sleep timer active", "tr" to "Uyku zamanlayıcısı aktif",
        "es" to "Temporizador activo", "fr" to "Minuteur actif",
        "de" to "Schlaf-Timer aktiv", "it" to "Timer sonno attivo",
        "pt" to "Timer de sono ativo", "ru" to "Таймер сна активен",
        "ja" to "スリープタイマー作動中", "zh" to "睡眠定时器已启用",
        "ar" to "مؤقت النوم مفعّل", "ko" to "수면 타이머 활성화"
    ),
    "play_order" to mapOf(
        "en" to "Playback order", "tr" to "Oynatma Sırası", "es" to "Orden de reproducción",
        "fr" to "Ordre de lecture", "de" to "Wiedergabereihenfolge",
        "it" to "Ordine di riproduzione", "pt" to "Ordem de reprodução",
        "ru" to "Порядок воспроизведения", "ja" to "再生順", "zh" to "播放顺序",
        "ar" to "ترتيب التشغيل", "ko" to "재생 순서"
    ),
    "volume" to mapOf(
        "en" to "Volume", "tr" to "Ses", "es" to "Volumen", "fr" to "Volume",
        "de" to "Lautstärke", "it" to "Volume", "pt" to "Volume", "ru" to "Громкость",
        "ja" to "音量", "zh" to "音量", "ar" to "مستوى الصوت", "ko" to "볼륨"
    ),
    "share_summary" to mapOf(
        "en" to "Share summary", "tr" to "Özeti Paylaş", "es" to "Compartir resumen",
        "fr" to "Partager le résumé", "de" to "Zusammenfassung teilen",
        "it" to "Condividi riepilogo", "pt" to "Compartilhar resumo",
        "ru" to "Поделиться итогами", "ja" to "まとめを共有", "zh" to "分享总结",
        "ar" to "مشاركة الملخص", "ko" to "요약 공유"
    ),
    "clear_history" to mapOf(
        "en" to "Clear history", "tr" to "Geçmişi Temizle", "es" to "Borrar historial",
        "fr" to "Effacer l'historique", "de" to "Verlauf löschen", "it" to "Cancella cronologia",
        "pt" to "Limpar histórico", "ru" to "Очистить историю", "ja" to "履歴を消去",
        "zh" to "清除历史记录", "ar" to "مسح السجل", "ko" to "기록 지우기"
    ),
    "no_history" to mapOf(
        "en" to "No listening history yet", "tr" to "Henüz dinleme geçmişi yok",
        "es" to "Aún no hay historial", "fr" to "Aucun historique pour l'instant",
        "de" to "Noch kein Hörverlauf", "it" to "Nessuna cronologia",
        "pt" to "Ainda não há histórico", "ru" to "История прослушиваний пуста",
        "ja" to "再生履歴はまだありません", "zh" to "暂无收听记录",
        "ar" to "لا يوجد سجل استماع بعد", "ko" to "아직 재생 기록이 없습니다"
    ),
    "splash_tagline" to mapOf(
        "en" to "FEEL • LISTEN • LIVE", "tr" to "HİSSET • DİNLE • YAŞA",
        "es" to "SIENTE • ESCUCHA • VIVE", "fr" to "RESSENS • ÉCOUTE • VIS",
        "de" to "FÜHLEN • HÖREN • LEBEN", "it" to "SENTI • ASCOLTA • VIVI",
        "pt" to "SINTA • OUÇA • VIVA", "ru" to "ЧУВСТВУЙ • СЛУШАЙ • ЖИВИ",
        "ja" to "感じる • 聴く • 生きる", "zh" to "感受 • 聆听 • 生活",
        "ar" to "اشعر • استمع • عش", "ko" to "느껴라 • 들어라 • 살아라"
    ),
    "sleep_5_min" to mapOf(
        "en" to "5 min", "tr" to "5 dk", "es" to "5 min", "fr" to "5 min",
        "de" to "5 Min.", "it" to "5 min", "pt" to "5 min", "ru" to "5 мин",
        "ja" to "5分", "zh" to "5 分钟", "ar" to "5 دقائق", "ko" to "5분"
    ),
    "sleep_120_min" to mapOf(
        "en" to "120 min", "tr" to "120 dk", "es" to "120 min", "fr" to "120 min",
        "de" to "120 Min.", "it" to "120 min", "pt" to "120 min", "ru" to "120 мин",
        "ja" to "120分", "zh" to "120 分钟", "ar" to "120 دقيقة", "ko" to "120분"
    ),
    "hq_audio" to mapOf(
        "en" to "HQ AUDIO", "tr" to "HQ SES", "es" to "AUDIO HQ", "fr" to "AUDIO HQ",
        "de" to "HQ-AUDIO", "it" to "AUDIO HQ", "pt" to "ÁUDIO HQ", "ru" to "HQ-АУДИО",
        "ja" to "HQオーディオ", "zh" to "高品质音频", "ar" to "صوت عالي الجودة", "ko" to "고음질 오디오"
    ),
    "collapse" to mapOf(
        "en" to "Collapse", "tr" to "Daralt", "es" to "Contraer", "fr" to "Réduire",
        "de" to "Einklappen", "it" to "Riduci", "pt" to "Recolher", "ru" to "Свернуть",
        "ja" to "折りたたむ", "zh" to "收起", "ar" to "طي", "ko" to "접기"
    ),
    "favorite" to mapOf(
        "en" to "Favorite", "tr" to "Favori", "es" to "Favorito", "fr" to "Favori",
        "de" to "Favorit", "it" to "Preferito", "pt" to "Favorito", "ru" to "Избранное",
        "ja" to "お気に入り", "zh" to "收藏", "ar" to "المفضلة", "ko" to "즐겨찾기"
    ),
    "queue" to mapOf(
        "en" to "Queue", "tr" to "Sıra", "es" to "Cola", "fr" to "File d'attente",
        "de" to "Warteschlange", "it" to "Coda", "pt" to "Fila", "ru" to "Очередь",
        "ja" to "キュー", "zh" to "队列", "ar" to "قائمة الانتظار", "ko" to "대기열"
    ),
    "info" to mapOf(
        "en" to "Info", "tr" to "Bilgi", "es" to "Información", "fr" to "Infos",
        "de" to "Info", "it" to "Info", "pt" to "Informações", "ru" to "Информация",
        "ja" to "情報", "zh" to "信息", "ar" to "معلومات", "ko" to "정보"
    ),
    "share" to mapOf(
        "en" to "Share", "tr" to "Paylaş", "es" to "Compartir", "fr" to "Partager",
        "de" to "Teilen", "it" to "Condividi", "pt" to "Compartilhar", "ru" to "Поделиться",
        "ja" to "共有", "zh" to "分享", "ar" to "مشاركة", "ko" to "공유"
    ),
    "play_pause" to mapOf(
        "en" to "Play/Pause", "tr" to "Oynat/Duraklat", "es" to "Reproducir/Pausar",
        "fr" to "Lecture/Pause", "de" to "Wiedergabe/Pause", "it" to "Riproduci/Pausa",
        "pt" to "Reproduzir/Pausar", "ru" to "Воспроизведение/Пауза",
        "ja" to "再生/一時停止", "zh" to "播放/暂停", "ar" to "تشغيل/إيقاف مؤقت", "ko" to "재생/일시정지"
    ),
    "repeat" to mapOf(
        "en" to "Repeat", "tr" to "Tekrar", "es" to "Repetir", "fr" to "Répéter",
        "de" to "Wiederholen", "it" to "Ripeti", "pt" to "Repetir", "ru" to "Повтор",
        "ja" to "リピート", "zh" to "重复", "ar" to "تكرار", "ko" to "반복"
    ),
    "share_message" to mapOf(
        "en" to "Listening on BeatBox:", "tr" to "BeatBox'ta dinliyorum:",
        "es" to "Escuchando en BeatBox:", "fr" to "J'écoute sur BeatBox :",
        "de" to "Ich höre gerade auf BeatBox:", "it" to "In ascolto su BeatBox:",
        "pt" to "Ouvindo no BeatBox:", "ru" to "Слушаю в BeatBox:",
        "ja" to "BeatBoxで再生中:", "zh" to "正在 BeatBox 上收听：",
        "ar" to "أستمع على BeatBox:", "ko" to "BeatBox에서 듣는 중:"
    ),
    "continue" to mapOf(
        "en" to "Continue", "tr" to "Devam Et", "es" to "Continuar", "fr" to "Continuer",
        "de" to "Weiter", "it" to "Continua", "pt" to "Continuar", "ru" to "Продолжить",
        "ja" to "続ける", "zh" to "继续", "ar" to "متابعة", "ko" to "계속"
    ),
    "start_listening" to mapOf(
        "en" to "Start listening", "tr" to "Müziğe Başla", "es" to "Empezar a escuchar",
        "fr" to "Commencer à écouter", "de" to "Jetzt hören", "it" to "Inizia ad ascoltare",
        "pt" to "Começar a ouvir", "ru" to "Начать слушать", "ja" to "聴き始める",
        "zh" to "开始聆听", "ar" to "ابدأ الاستماع", "ko" to "듣기 시작"
    ),
    "download" to mapOf(
        "en" to "Download", "tr" to "İndir", "es" to "Descargar", "fr" to "Télécharger",
        "de" to "Herunterladen", "it" to "Scarica", "pt" to "Baixar", "ru" to "Скачать",
        "ja" to "ダウンロード", "zh" to "下载", "ar" to "تنزيل", "ko" to "다운로드"
    ),
    "folders_selected" to mapOf(
        "en" to "%d folders selected", "tr" to "%d klasör seçili",
        "es" to "%d carpetas seleccionadas", "fr" to "%d dossiers sélectionnés",
        "de" to "%d Ordner ausgewählt", "it" to "%d cartelle selezionate",
        "pt" to "%d pastas selecionadas", "ru" to "Выбрано папок: %d",
        "ja" to "%d 個のフォルダを選択", "zh" to "已选择 %d 个文件夹",
        "ar" to "تم تحديد %d مجلدات", "ko" to "%d개 폴더 선택됨"
    ),
    "select_music_folders" to mapOf(
        "en" to "Select the music folders to scan", "tr" to "Müzik klasörlerini seç",
        "es" to "Selecciona las carpetas de música", "fr" to "Sélectionnez les dossiers de musique",
        "de" to "Musikordner auswählen", "it" to "Seleziona le cartelle musicali",
        "pt" to "Selecione as pastas de música", "ru" to "Выберите папки с музыкой",
        "ja" to "音楽フォルダを選択", "zh" to "选择音乐文件夹",
        "ar" to "اختر مجلدات الموسيقى", "ko" to "음악 폴더 선택"
    ),
    "cache_subtitle" to mapOf(
        "en" to "RAM and image cache", "tr" to "RAM ve görsel önbelleği",
        "es" to "RAM y caché de imágenes", "fr" to "RAM et cache d'images",
        "de" to "RAM- und Bildcache", "it" to "RAM e cache immagini",
        "pt" to "RAM e cache de imagens", "ru" to "ОЗУ и кеш изображений",
        "ja" to "RAM と画像キャッシュ", "zh" to "内存与图片缓存",
        "ar" to "ذاكرة الوصول العشوائي وذاكرة الصور", "ko" to "RAM 및 이미지 캐시"
    ),
    "default_bio" to mapOf(
        "en" to "Music is the food of the soul ✨", "tr" to "Müzik ruhun gıdasıdır ✨",
        "es" to "La música es el alimento del alma ✨", "fr" to "La musique est la nourriture de l'âme ✨",
        "de" to "Musik ist die Nahrung der Seele ✨", "it" to "La musica è il cibo dell'anima ✨",
        "pt" to "A música é o alimento da alma ✨", "ru" to "Музыка — пища для души ✨",
        "ja" to "音楽は心の糧 ✨", "zh" to "音乐是心灵的食粮 ✨",
        "ar" to "الموسيقى غذاء الروح ✨", "ko" to "음악은 영혼의 양식 ✨"
    ),
    "login_title" to mapOf(
        "en" to "Sign in to BeatBox", "tr" to "BeatBox'a Giriş Yap", "es" to "Iniciar sesión en BeatBox",
        "fr" to "Se connecter à BeatBox", "de" to "Bei BeatBox anmelden", "it" to "Accedi a BeatBox",
        "pt" to "Entrar no BeatBox", "ru" to "Вход в BeatBox", "ja" to "BeatBoxにログイン",
        "zh" to "登录 BeatBox", "ar" to "تسجيل الدخول إلى BeatBox", "ko" to "BeatBox 로그인"
    ),
    "register_title" to mapOf(
        "en" to "Create a BeatBox account", "tr" to "BeatBox'a Kayıt Ol", "es" to "Crear cuenta en BeatBox",
        "fr" to "Créer un compte BeatBox", "de" to "BeatBox-Konto erstellen", "it" to "Crea un account BeatBox",
        "pt" to "Criar conta no BeatBox", "ru" to "Регистрация в BeatBox", "ja" to "BeatBoxアカウントを作成",
        "zh" to "创建 BeatBox 账户", "ar" to "إنشاء حساب BeatBox", "ko" to "BeatBox 계정 만들기"
    ),
    "guest_user" to mapOf(
        "en" to "Guest user", "tr" to "Misafir Kullanıcı", "es" to "Usuario invitado",
        "fr" to "Utilisateur invité", "de" to "Gastbenutzer", "it" to "Utente ospite",
        "pt" to "Usuário convidado", "ru" to "Гость", "ja" to "ゲストユーザー",
        "zh" to "访客用户", "ar" to "مستخدم ضيف", "ko" to "게스트 사용자"
    ),
    "show_password" to mapOf(
        "en" to "Show password", "tr" to "Şifreyi göster", "es" to "Mostrar contraseña",
        "fr" to "Afficher le mot de passe", "de" to "Passwort anzeigen", "it" to "Mostra password",
        "pt" to "Mostrar senha", "ru" to "Показать пароль", "ja" to "パスワードを表示",
        "zh" to "显示密码", "ar" to "إظهار كلمة المرور", "ko" to "비밀번호 표시"
    ),
    "hide_password" to mapOf(
        "en" to "Hide password", "tr" to "Şifreyi gizle", "es" to "Ocultar contraseña",
        "fr" to "Masquer le mot de passe", "de" to "Passwort verbergen", "it" to "Nascondi password",
        "pt" to "Ocultar senha", "ru" to "Скрыть пароль", "ja" to "パスワードを隠す",
        "zh" to "隐藏密码", "ar" to "إخفاء كلمة المرور", "ko" to "비밀번호 숨기기"
    ),
    "err_enter_email_password" to mapOf(
        "en" to "Please enter your email and password.", "tr" to "Lütfen e-posta ve şifrenizi girin.",
        "es" to "Introduce tu correo y contraseña.", "fr" to "Veuillez saisir votre e-mail et mot de passe.",
        "de" to "Bitte E-Mail und Passwort eingeben.", "it" to "Inserisci email e password.",
        "pt" to "Informe seu e-mail e senha.", "ru" to "Введите электронную почту и пароль.",
        "ja" to "メールアドレスとパスワードを入力してください。", "zh" to "请输入电子邮箱和密码。",
        "ar" to "يرجى إدخال البريد الإلكتروني وكلمة المرور.", "ko" to "이메일과 비밀번호를 입력하세요."
    ),
    "err_invalid_email" to mapOf(
        "en" to "Enter a valid email address.", "tr" to "Geçerli bir e-posta adresi girin.",
        "es" to "Introduce un correo electrónico válido.", "fr" to "Saisissez une adresse e-mail valide.",
        "de" to "Geben Sie eine gültige E-Mail-Adresse ein.", "it" to "Inserisci un indirizzo email valido.",
        "pt" to "Informe um endereço de e-mail válido.", "ru" to "Введите действительный адрес эл. почты.",
        "ja" to "有効なメールアドレスを入力してください。", "zh" to "请输入有效的电子邮箱地址。",
        "ar" to "أدخل عنوان بريد إلكتروني صالح.", "ko" to "유효한 이메일 주소를 입력하세요."
    ),
    "err_fill_fields" to mapOf(
        "en" to "Please enter your name, email and password.", "tr" to "Lütfen ad soyad, e-posta ve şifre girin.",
        "es" to "Introduce tu nombre, correo y contraseña.", "fr" to "Saisissez votre nom, e-mail et mot de passe.",
        "de" to "Bitte Name, E-Mail und Passwort eingeben.", "it" to "Inserisci nome, email e password.",
        "pt" to "Informe seu nome, e-mail e senha.", "ru" to "Введите имя, эл. почту и пароль.",
        "ja" to "名前、メールアドレス、パスワードを入力してください。", "zh" to "请输入姓名、电子邮箱和密码。",
        "ar" to "يرجى إدخال الاسم والبريد الإلكتروني وكلمة المرور.", "ko" to "이름, 이메일, 비밀번호를 입력하세요."
    ),
    "err_password_min" to mapOf(
        "en" to "Password must be at least 6 characters.", "tr" to "Şifre en az 6 karakter olmalı.",
        "es" to "La contraseña debe tener al menos 6 caracteres.", "fr" to "Le mot de passe doit comporter au moins 6 caractères.",
        "de" to "Das Passwort muss mindestens 6 Zeichen haben.", "it" to "La password deve avere almeno 6 caratteri.",
        "pt" to "A senha deve ter pelo menos 6 caracteres.", "ru" to "Пароль должен содержать не менее 6 символов.",
        "ja" to "パスワードは6文字以上で入力してください。", "zh" to "密码至少需要 6 个字符。",
        "ar" to "يجب أن تكون كلمة المرور 6 أحرف على الأقل.", "ko" to "비밀번호는 최소 6자여야 합니다."
    ),
    "err_enter_email_reset" to mapOf(
        "en" to "Enter a valid email address to reset your password.",
        "tr" to "Şifre sıfırlamak için geçerli bir e-posta adresi girin.",
        "es" to "Introduce un correo válido para restablecer la contraseña.",
        "fr" to "Saisissez un e-mail valide pour réinitialiser le mot de passe.",
        "de" to "Geben Sie eine gültige E-Mail-Adresse zum Zurücksetzen ein.",
        "it" to "Inserisci un'email valida per reimpostare la password.",
        "pt" to "Informe um e-mail válido para redefinir a senha.",
        "ru" to "Введите действительный адрес эл. почты для сброса пароля.",
        "ja" to "パスワードをリセットするには有効なメールアドレスを入力してください。",
        "zh" to "请输入有效的电子邮箱以重置密码。",
        "ar" to "أدخل بريدًا إلكترونيًا صالحًا لإعادة تعيين كلمة المرور.",
        "ko" to "비밀번호를 재설정하려면 유효한 이메일 주소를 입력하세요."
    ),
    "google_sign_in_failed" to mapOf(
        "en" to "Google sign-in failed.", "tr" to "Google girişi başarısız.",
        "es" to "Error al iniciar sesión con Google.", "fr" to "Échec de la connexion Google.",
        "de" to "Google-Anmeldung fehlgeschlagen.", "it" to "Accesso con Google non riuscito.",
        "pt" to "Falha no login com Google.", "ru" to "Не удалось войти через Google.",
        "ja" to "Google ログインに失敗しました。", "zh" to "Google 登录失败。",
        "ar" to "فشل تسجيل الدخول عبر Google.", "ko" to "Google 로그인에 실패했습니다."
    ),
    "firebase_not_configured" to mapOf(
        "en" to "Firebase is not configured. Add app/google-services.json and rebuild.",
        "tr" to "Firebase yapılandırılmadı. app/google-services.json dosyasını ekleyip yeniden derleyin.",
        "es" to "Firebase no está configurado. Añade app/google-services.json y recompila.",
        "fr" to "Firebase n'est pas configuré. Ajoutez app/google-services.json et recompilez.",
        "de" to "Firebase ist nicht konfiguriert. Fügen Sie app/google-services.json hinzu und bauen Sie neu.",
        "it" to "Firebase non è configurato. Aggiungi app/google-services.json e ricompila.",
        "pt" to "O Firebase não está configurado. Adicione app/google-services.json e recompile.",
        "ru" to "Firebase не настроен. Добавьте app/google-services.json и пересоберите.",
        "ja" to "Firebase が設定されていません。app/google-services.json を追加して再ビルドしてください。",
        "zh" to "Firebase 未配置。请添加 app/google-services.json 并重新构建。",
        "ar" to "لم يتم تكوين Firebase. أضف app/google-services.json وأعد البناء.",
        "ko" to "Firebase가 구성되지 않았습니다. app/google-services.json을 추가하고 다시 빌드하세요."
    ),
    "err_session_not_found" to mapOf(
        "en" to "Could not retrieve session information.", "tr" to "Oturum bilgisi alınamadı.",
        "es" to "No se pudo obtener la información de la sesión.", "fr" to "Impossible de récupérer les informations de session.",
        "de" to "Sitzungsinformationen konnten nicht abgerufen werden.", "it" to "Impossibile recuperare le informazioni della sessione.",
        "pt" to "Não foi possível obter as informações da sessão.", "ru" to "Не удалось получить данные сеанса.",
        "ja" to "セッション情報を取得できませんでした。", "zh" to "无法获取会话信息。",
        "ar" to "تعذر استرداد معلومات الجلسة.", "ko" to "세션 정보를 가져올 수 없습니다."
    ),
    "err_account_no_session" to mapOf(
        "en" to "Account created but session information could not be retrieved.",
        "tr" to "Hesap oluşturuldu ancak oturum bilgisi alınamadı.",
        "es" to "Cuenta creada, pero no se pudo obtener la sesión.",
        "fr" to "Compte créé, mais les informations de session sont indisponibles.",
        "de" to "Konto erstellt, aber Sitzungsinformationen fehlen.",
        "it" to "Account creato ma impossibile ottenere la sessione.",
        "pt" to "Conta criada, mas não foi possível obter a sessão.",
        "ru" to "Аккаунт создан, но данные сеанса не получены.",
        "ja" to "アカウントは作成されましたが、セッション情報を取得できませんでした。",
        "zh" to "账户已创建，但无法获取会话信息。",
        "ar" to "تم إنشاء الحساب لكن تعذر استرداد معلومات الجلسة.",
        "ko" to "계정은 생성되었지만 세션 정보를 가져올 수 없습니다."
    ),
    "err_google_session" to mapOf(
        "en" to "Could not open a Google session.", "tr" to "Google oturumu açılamadı.",
        "es" to "No se pudo abrir la sesión de Google.", "fr" to "Impossible d'ouvrir une session Google.",
        "de" to "Google-Sitzung konnte nicht geöffnet werden.", "it" to "Impossibile aprire una sessione Google.",
        "pt" to "Não foi possível abrir a sessão do Google.", "ru" to "Не удалось открыть сеанс Google.",
        "ja" to "Google セッションを開けませんでした。", "zh" to "无法打开 Google 会话。",
        "ar" to "تعذر فتح جلسة Google.", "ko" to "Google 세션을 열 수 없습니다."
    ),
    "reset_email_sent" to mapOf(
        "en" to "A password reset link has been sent to your email.",
        "tr" to "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.",
        "es" to "Se ha enviado un enlace de restablecimiento a tu correo.",
        "fr" to "Un lien de réinitialisation a été envoyé à votre e-mail.",
        "de" to "Ein Link zum Zurücksetzen wurde an Ihre E-Mail gesendet.",
        "it" to "Un link per reimpostare la password è stato inviato via email.",
        "pt" to "Um link de redefinição foi enviado para seu e-mail.",
        "ru" to "Ссылка для сброса пароля отправлена на вашу почту.",
        "ja" to "パスワード再設定リンクをメールに送信しました。",
        "zh" to "密码重置链接已发送至您的邮箱。",
        "ar" to "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك الإلكتروني.",
        "ko" to "비밀번호 재설정 링크가 이메일로 전송되었습니다."
    ),
    "action_completed" to mapOf(
        "en" to "Done.", "tr" to "İşlem tamamlandı.", "es" to "Hecho.",
        "fr" to "Terminé.", "de" to "Fertig.", "it" to "Fatto.",
        "pt" to "Concluído.", "ru" to "Готово.", "ja" to "完了しました。",
        "zh" to "已完成。", "ar" to "تم.", "ko" to "완료되었습니다."
    ),
    "auth_generic_error" to mapOf(
        "en" to "Sign-in failed. Please try again.", "tr" to "Giriş yapılamadı. Lütfen tekrar deneyin.",
        "es" to "Error al iniciar sesión. Inténtalo de nuevo.", "fr" to "Échec de la connexion. Veuillez réessayer.",
        "de" to "Anmeldung fehlgeschlagen. Bitte erneut versuchen.", "it" to "Accesso non riuscito. Riprova.",
        "pt" to "Falha no login. Tente novamente.", "ru" to "Не удалось войти. Попробуйте снова.",
        "ja" to "ログインに失敗しました。もう一度お試しください。", "zh" to "登录失败，请重试。",
        "ar" to "فشل تسجيل الدخول. يرجى المحاولة مرة أخرى.", "ko" to "로그인에 실패했습니다. 다시 시도하세요."
    ),
    "auth_wrong_password" to mapOf(
        "en" to "Incorrect email or password.", "tr" to "E-posta veya şifre hatalı.",
        "es" to "Correo o contraseña incorrectos.", "fr" to "E-mail ou mot de passe incorrect.",
        "de" to "E-Mail oder Passwort ist falsch.", "it" to "Email o password errati.",
        "pt" to "E-mail ou senha incorretos.", "ru" to "Неверная почта или пароль.",
        "ja" to "メールアドレスまたはパスワードが正しくありません。", "zh" to "电子邮箱或密码错误。",
        "ar" to "البريد الإلكتروني أو كلمة المرور غير صحيحة.", "ko" to "이메일 또는 비밀번호가 올바르지 않습니다."
    ),
    "auth_user_not_found" to mapOf(
        "en" to "No account found for this email.", "tr" to "Bu e-posta ile kayıtlı bir hesap bulunamadı.",
        "es" to "No hay ninguna cuenta con este correo.", "fr" to "Aucun compte trouvé pour cet e-mail.",
        "de" to "Kein Konto für diese E-Mail gefunden.", "it" to "Nessun account trovato per questa email.",
        "pt" to "Nenhuma conta encontrada para este e-mail.", "ru" to "Аккаунт с такой почтой не найден.",
        "ja" to "このメールアドレスのアカウントが見つかりません。", "zh" to "未找到使用该邮箱的账户。",
        "ar" to "لم يتم العثور على حساب لهذا البريد.", "ko" to "이 이메일로 등록된 계정이 없습니다."
    ),
    "auth_email_in_use" to mapOf(
        "en" to "This email address is already in use.", "tr" to "Bu e-posta adresi zaten kullanılıyor.",
        "es" to "Este correo ya está en uso.", "fr" to "Cette adresse e-mail est déjà utilisée.",
        "de" to "Diese E-Mail-Adresse wird bereits verwendet.", "it" to "Questo indirizzo email è già in uso.",
        "pt" to "Este e-mail já está em uso.", "ru" to "Этот адрес эл. почты уже используется.",
        "ja" to "このメールアドレスは既に使用されています。", "zh" to "该电子邮箱已被使用。",
        "ar" to "هذا البريد الإلكتروني مستخدم بالفعل.", "ko" to "이미 사용 중인 이메일 주소입니다."
    ),
    "auth_missing_email" to mapOf(
        "en" to "Email address is required.", "tr" to "E-posta adresi gerekli.",
        "es" to "Se requiere correo electrónico.", "fr" to "L'adresse e-mail est requise.",
        "de" to "E-Mail-Adresse ist erforderlich.", "it" to "L'email è obbligatoria.",
        "pt" to "O e-mail é obrigatório.", "ru" to "Требуется адрес эл. почты.",
        "ja" to "メールアドレスが必要です。", "zh" to "需要电子邮箱地址。",
        "ar" to "البريد الإلكتروني مطلوب.", "ko" to "이메일 주소가 필요합니다."
    ),
    "auth_missing_password" to mapOf(
        "en" to "Password is required.", "tr" to "Şifre gerekli.",
        "es" to "Se requiere contraseña.", "fr" to "Le mot de passe est requis.",
        "de" to "Passwort ist erforderlich.", "it" to "La password è obbligatoria.",
        "pt" to "A senha é obrigatória.", "ru" to "Требуется пароль.",
        "ja" to "パスワードが必要です。", "zh" to "需要密码。",
        "ar" to "كلمة المرور مطلوبة.", "ko" to "비밀번호가 필요합니다."
    ),
    "auth_too_many_requests" to mapOf(
        "en" to "Too many attempts. Please try again later.", "tr" to "Çok fazla deneme yapıldı. Lütfen biraz sonra tekrar deneyin.",
        "es" to "Demasiados intentos. Inténtalo más tarde.", "fr" to "Trop de tentatives. Réessayez plus tard.",
        "de" to "Zu viele Versuche. Bitte später erneut versuchen.", "it" to "Troppi tentativi. Riprova più tardi.",
        "pt" to "Muitas tentativas. Tente novamente mais tarde.", "ru" to "Слишком много попыток. Повторите позже.",
        "ja" to "試行回数が多すぎます。しばらくしてから再試行してください。", "zh" to "尝试次数过多，请稍后再试。",
        "ar" to "محاولات كثيرة جدًا. يرجى المحاولة لاحقًا.", "ko" to "시도 횟수가 너무 많습니다. 나중에 다시 시도하세요."
    ),
    "auth_network" to mapOf(
        "en" to "Check your internet connection.", "tr" to "İnternet bağlantınızı kontrol edin.",
        "es" to "Comprueba tu conexión a Internet.", "fr" to "Vérifiez votre connexion Internet.",
        "de" to "Überprüfen Sie Ihre Internetverbindung.", "it" to "Controlla la connessione Internet.",
        "pt" to "Verifique sua conexão com a internet.", "ru" to "Проверьте подключение к интернету.",
        "ja" to "インターネット接続を確認してください。", "zh" to "请检查您的网络连接。",
        "ar" to "تحقق من اتصالك بالإنترنت.", "ko" to "인터넷 연결을 확인하세요."
    ),
    "auth_user_disabled" to mapOf(
        "en" to "This account has been disabled.", "tr" to "Bu hesap devre dışı bırakılmış.",
        "es" to "Esta cuenta ha sido deshabilitada.", "fr" to "Ce compte a été désactivé.",
        "de" to "Dieses Konto wurde deaktiviert.", "it" to "Questo account è stato disabilitato.",
        "pt" to "Esta conta foi desativada.", "ru" to "Этот аккаунт отключён.",
        "ja" to "このアカウントは無効化されています。", "zh" to "该账户已被停用。",
        "ar" to "تم تعطيل هذا الحساب.", "ko" to "이 계정은 비활성화되었습니다."
    ),
    "auth_operation_not_allowed" to mapOf(
        "en" to "Email/password sign-in is not enabled in the Firebase console.",
        "tr" to "E-posta/şifre girişi Firebase konsolunda etkin değil.",
        "es" to "El inicio con correo/contraseña no está habilitado en Firebase.",
        "fr" to "La connexion e-mail/mot de passe n'est pas activée dans Firebase.",
        "de" to "E-Mail/Passwort-Anmeldung ist in der Firebase-Konsole nicht aktiviert.",
        "it" to "L'accesso email/password non è abilitato nella console Firebase.",
        "pt" to "O login por e-mail/senha não está ativado no console do Firebase.",
        "ru" to "Вход по почте/паролю не включён в консоли Firebase.",
        "ja" to "Firebase コンソールでメール/パスワードログインが有効になっていません。",
        "zh" to "Firebase 控制台未启用邮箱/密码登录。",
        "ar" to "تسجيل الدخول بالبريد/كلمة المرور غير مفعّل في وحدة تحكم Firebase.",
        "ko" to "Firebase 콘솔에서 이메일/비밀번호 로그인이 활성화되지 않았습니다."
    ),
    "auth_account_diff_credential" to mapOf(
        "en" to "This email is registered with a different sign-in method.",
        "tr" to "Bu e-posta farklı bir giriş yöntemiyle kayıtlı.",
        "es" to "Este correo está registrado con otro método de acceso.",
        "fr" to "Cet e-mail est enregistré avec une autre méthode de connexion.",
        "de" to "Diese E-Mail ist mit einer anderen Anmeldemethode registriert.",
        "it" to "Questa email è registrata con un altro metodo di accesso.",
        "pt" to "Este e-mail está registrado com outro método de login.",
        "ru" to "Эта почта зарегистрирована другим способом входа.",
        "ja" to "このメールアドレスは別のログイン方法で登録されています。",
        "zh" to "该邮箱已使用其他登录方式注册。",
        "ar" to "هذا البريد مسجل بطريقة تسجيل دخول مختلفة.", "ko" to "이 이메일은 다른 로그인 방식으로 등록되어 있습니다."
    ),
    "auth_requires_recent_login" to mapOf(
        "en" to "Please sign in again to continue.", "tr" to "Bu işlem için yeniden giriş yapmanız gerekiyor.",
        "es" to "Inicia sesión de nuevo para continuar.", "fr" to "Veuillez vous reconnecter pour continuer.",
        "de" to "Bitte melden Sie sich erneut an.", "it" to "Accedi di nuovo per continuare.",
        "pt" to "Entre novamente para continuar.", "ru" to "Войдите снова, чтобы продолжить.",
        "ja" to "続行するには再度ログインしてください。", "zh" to "请重新登录以继续。",
        "ar" to "يرجى تسجيل الدخول مرة أخرى للمتابعة.", "ko" to "계속하려면 다시 로그인하세요."
    ),
    "auth_invalid_action_code" to mapOf(
        "en" to "The link has expired or is invalid.", "tr" to "Bağlantının süresi dolmuş veya geçersiz.",
        "es" to "El enlace ha caducado o no es válido.", "fr" to "Le lien a expiré ou est invalide.",
        "de" to "Der Link ist abgelaufen oder ungültig.", "it" to "Il link è scaduto o non valido.",
        "pt" to "O link expirou ou é inválido.", "ru" to "Ссылка истекла или недействительна.",
        "ja" to "リンクの有効期限が切れているか無効です。", "zh" to "链接已过期或无效。",
        "ar" to "انتهت صلاحية الرابط أو أنه غير صالح.", "ko" to "링크가 만료되었거나 유효하지 않습니다."
    ),
    "google_web_client_missing" to mapOf(
        "en" to "No web client ID found for Google sign-in.", "tr" to "Google girişi için web istemci kimliği bulunamadı.",
        "es" to "No se encontró el ID de cliente web para Google.", "fr" to "Aucun ID client web trouvé pour Google.",
        "de" to "Keine Web-Client-ID für Google-Anmeldung gefunden.", "it" to "Nessun ID client web trovato per Google.",
        "pt" to "Nenhum ID de cliente web encontrado para o Google.", "ru" to "Не найден веб-клиент для входа через Google.",
        "ja" to "Google ログイン用のウェブクライアント ID が見つかりません。",
        "zh" to "未找到用于 Google 登录的 Web 客户端 ID。",
        "ar" to "لم يتم العثور على معرّف عميل ويب لتسجيل الدخول عبر Google.",
        "ko" to "Google 로그인용 웹 클라이언트 ID를 찾을 수 없습니다."
    ),
    "google_credential_missing" to mapOf(
        "en" to "Could not retrieve Google credentials.", "tr" to "Google kimlik bilgisi alınamadı.",
        "es" to "No se pudieron obtener las credenciales de Google.", "fr" to "Impossible d'obtenir les identifiants Google.",
        "de" to "Google-Anmeldedaten konnten nicht abgerufen werden.", "it" to "Impossibile ottenere le credenziali Google.",
        "pt" to "Não foi possível obter as credenciais do Google.", "ru" to "Не удалось получить данные Google.",
        "ja" to "Google の認証情報を取得できませんでした。", "zh" to "无法获取 Google 凭据。",
        "ar" to "تعذر استرداد بيانات اعتماد Google.", "ko" to "Google 자격 증명을 가져올 수 없습니다."
    ),
    "google_no_account" to mapOf(
        "en" to "No Google account found on this device. Please add one.",
        "tr" to "Cihazda kullanılabilir Google hesabı bulunamadı. Lütfen Google hesabı ekleyin.",
        "es" to "No hay ninguna cuenta de Google en este dispositivo. Añade una.",
        "fr" to "Aucun compte Google sur cet appareil. Veuillez en ajouter un.",
        "de" to "Kein Google-Konto auf diesem Gerät. Bitte hinzufügen.",
        "it" to "Nessun account Google sul dispositivo. Aggiungine uno.",
        "pt" to "Nenhuma conta Google neste dispositivo. Adicione uma.",
        "ru" to "На устройстве нет аккаунта Google. Добавьте его.",
        "ja" to "この端末に Google アカウントがありません。追加してください。",
        "zh" to "此设备上没有 Google 账户，请添加一个。",
        "ar" to "لا يوجد حساب Google على هذا الجهاز. يرجى إضافة واحد.",
        "ko" to "이 기기에 Google 계정이 없습니다. 추가하세요."
    ),
    "google_token_unreadable" to mapOf(
        "en" to "Could not read the Google ID token.", "tr" to "Google kimlik belirteci okunamadı.",
        "es" to "No se pudo leer el token de Google.", "fr" to "Impossible de lire le jeton Google.",
        "de" to "Google-ID-Token konnte nicht gelesen werden.", "it" to "Impossibile leggere il token Google.",
        "pt" to "Não foi possível ler o token do Google.", "ru" to "Не удалось прочитать токен Google.",
        "ja" to "Google ID トークンを読み取れませんでした。", "zh" to "无法读取 Google ID 令牌。",
        "ar" to "تعذر قراءة رمز Google.", "ko" to "Google ID 토큰을 읽을 수 없습니다."
    ),
    "lyrics_service_unavailable" to mapOf(
        "en" to "Lyrics service is unavailable.", "tr" to "Şarkı sözü servisi kullanılamıyor.",
        "es" to "El servicio de letras no está disponible.", "fr" to "Le service de paroles est indisponible.",
        "de" to "Songtext-Dienst ist nicht verfügbar.", "it" to "Il servizio testi non è disponibile.",
        "pt" to "O serviço de letras não está disponível.", "ru" to "Сервис текстов песен недоступен.",
        "ja" to "歌詞サービスを利用できません。", "zh" to "歌词服务不可用。",
        "ar" to "خدمة الكلمات غير متاحة.", "ko" to "가사 서비스를 사용할 수 없습니다."
    ),
    "lyrics_not_found" to mapOf(
        "en" to "No lyrics found. Check the song title and artist.",
        "tr" to "Şarkı sözü bulunamadı. Lütfen şarkı adı ve sanatçı bilgilerini kontrol edin.",
        "es" to "No se encontraron letras. Comprueba el título y el artista.",
        "fr" to "Aucune parole trouvée. Vérifiez le titre et l'artiste.",
        "de" to "Kein Songtext gefunden. Prüfen Sie Titel und Künstler.",
        "it" to "Nessun testo trovato. Controlla titolo e artista.",
        "pt" to "Nenhuma letra encontrada. Verifique o título e o artista.",
        "ru" to "Текст не найден. Проверьте название и исполнителя.",
        "ja" to "歌詞が見つかりません。曲名とアーティストを確認してください。",
        "zh" to "未找到歌词，请检查歌曲名称和艺术家。",
        "ar" to "لم يتم العثور على كلمات. تحقق من اسم الأغنية والفنان.",
        "ko" to "가사를 찾을 수 없습니다. 곡명과 아티스트를 확인하세요."
    ),
    "lyrics_search_error" to mapOf(
        "en" to "Search error.", "tr" to "Arama hatası.", "es" to "Error de búsqueda.",
        "fr" to "Erreur de recherche.", "de" to "Suchfehler.", "it" to "Errore di ricerca.",
        "pt" to "Erro de pesquisa.", "ru" to "Ошибка поиска.", "ja" to "検索エラー。",
        "zh" to "搜索出错。", "ar" to "خطأ في البحث.", "ko" to "검색 오류."
    ),
    "lyrics_offline" to mapOf(
        "en" to "No internet connection. A local LRC file or cache can be used for offline lyrics.",
        "tr" to "İnternet bağlantısı yok. Çevrimdışı sözler için yerel LRC dosyası veya önbellek kullanılabilir.",
        "es" to "Sin conexión. Puedes usar un archivo LRC local o la caché.",
        "fr" to "Pas de connexion. Un fichier LRC local ou le cache peut être utilisé.",
        "de" to "Keine Internetverbindung. Eine lokale LRC-Datei oder der Cache kann genutzt werden.",
        "it" to "Nessuna connessione. Puoi usare un file LRC locale o la cache.",
        "pt" to "Sem conexão. Use um arquivo LRC local ou o cache.",
        "ru" to "Нет подключения. Можно использовать локальный LRC-файл или кеш.",
        "ja" to "インターネット接続がありません。ローカル LRC ファイルまたはキャッシュを利用できます。",
        "zh" to "无网络连接，可使用本地 LRC 文件或缓存。",
        "ar" to "لا يوجد اتصال بالإنترنت. يمكن استخدام ملف LRC محلي أو ذاكرة التخزين المؤقت.",
        "ko" to "인터넷 연결이 없습니다. 로컬 LRC 파일이나 캐시를 사용할 수 있습니다."
    ),
    "lyrics_load_error" to mapOf(
        "en" to "Error loading lyrics: %s", "tr" to "Şarkı sözü yüklenirken hata oluştu: %s",
        "es" to "Error al cargar las letras: %s", "fr" to "Erreur lors du chargement des paroles : %s",
        "de" to "Fehler beim Laden des Songtexts: %s", "it" to "Errore nel caricamento del testo: %s",
        "pt" to "Erro ao carregar a letra: %s", "ru" to "Ошибка загрузки текста: %s",
        "ja" to "歌詞の読み込みエラー: %s", "zh" to "加载歌词出错：%s",
        "ar" to "خطأ في تحميل الكلمات: %s", "ko" to "가사 로드 오류: %s"
    ),
    "unknown_error" to mapOf(
        "en" to "Unknown error", "tr" to "Bilinmeyen hata", "es" to "Error desconocido",
        "fr" to "Erreur inconnue", "de" to "Unbekannter Fehler", "it" to "Errore sconosciuto",
        "pt" to "Erro desconhecido", "ru" to "Неизвестная ошибка", "ja" to "不明なエラー",
        "zh" to "未知错误", "ar" to "خطأ غير معروف", "ko" to "알 수 없는 오류"
    ),
    "storage_access" to mapOf(
        "en" to "All files access", "tr" to "Tüm dosyalara erişim",
        "es" to "Acceso a todos los archivos", "fr" to "Accès à tous les fichiers",
        "de" to "Zugriff auf alle Dateien", "it" to "Accesso a tutti i file",
        "pt" to "Acesso a todos os arquivos", "ru" to "Доступ ко всем файлам",
        "ja" to "すべてのファイルへのアクセス", "zh" to "所有文件访问权限",
        "ar" to "الوصول إلى جميع الملفات", "ko" to "모든 파일 접근"
    ),
    "storage_access_desc" to mapOf(
        "en" to "Allow reading .lrc files and writing ID3 tags",
        "tr" to "LRC dosyalarını okumak ve ID3 etiketlerini yazmak için",
        "es" to "Permite leer archivos .lrc y escribir etiquetas ID3",
        "fr" to "Permet de lire les fichiers .lrc et d'écrire les balises ID3",
        "de" to "Erlaubt das Lesen von .lrc-Dateien und Schreiben von ID3-Tags",
        "it" to "Consente di leggere file .lrc e scrivere tag ID3",
        "pt" to "Permite ler arquivos .lrc e gravar tags ID3",
        "ru" to "Чтение файлов .lrc и запись тегов ID3",
        "ja" to ".lrc ファイルの読み取りと ID3 タグの書き込みを許可",
        "zh" to "允许读取 .lrc 文件并写入 ID3 标签",
        "ar" to "السماح بقراءة ملفات .lrc وكتابة علامات ID3",
        "ko" to ".lrc 파일 읽기 및 ID3 태그 쓰기 허용"
    ),
    "storage_access_granted" to mapOf(
        "en" to "Granted", "tr" to "İzin verildi", "es" to "Concedido",
        "fr" to "Accordé", "de" to "Erteilt", "it" to "Concesso",
        "pt" to "Concedido", "ru" to "Разрешено", "ja" to "許可済み",
        "zh" to "已授予", "ar" to "تم المنح", "ko" to "허용됨"
    ),
    "download_completed" to mapOf(
        "en" to "%s downloaded", "tr" to "%s indirildi", "es" to "%s descargado",
        "fr" to "%s téléchargé", "de" to "%s heruntergeladen", "it" to "%s scaricato",
        "pt" to "%s baixado", "ru" to "%s загружено", "ja" to "%s をダウンロードしました",
        "zh" to "%s 已下载", "ar" to "تم تنزيل %s", "ko" to "%s 다운로드 완료"
    ),
    "download_failed" to mapOf(
        "en" to "Download failed: %s", "tr" to "İndirme başarısız: %s",
        "es" to "Error al descargar: %s", "fr" to "Échec du téléchargement : %s",
        "de" to "Download fehlgeschlagen: %s", "it" to "Download non riuscito: %s",
        "pt" to "Falha no download: %s", "ru" to "Ошибка загрузки: %s",
        "ja" to "ダウンロードに失敗しました: %s", "zh" to "下载失败：%s",
        "ar" to "فشل التنزيل: %s", "ko" to "다운로드 실패: %s"
    ),
    "profile" to mapOf(
        "en" to "Profile", "tr" to "Profil", "es" to "Perfil",
        "fr" to "Profil", "de" to "Profil", "it" to "Profilo",
        "pt" to "Perfil", "ru" to "Профиль", "ja" to "プロフィール",
        "zh" to "个人资料", "ar" to "الملف الشخصي", "ko" to "프로필"
    ),
    "vip_premium" to mapOf(
        "en" to "BeatBox VIP Premium 🌟", "tr" to "BeatBox VIP Premium 🌟",
        "es" to "BeatBox VIP Premium 🌟", "fr" to "BeatBox VIP Premium 🌟",
        "de" to "BeatBox VIP Premium 🌟", "it" to "BeatBox VIP Premium 🌟",
        "pt" to "BeatBox VIP Premium 🌟", "ru" to "BeatBox VIP Premium 🌟",
        "ja" to "BeatBox VIP Premium 🌟", "zh" to "BeatBox VIP Premium 🌟",
        "ar" to "BeatBox VIP Premium 🌟", "ko" to "BeatBox VIP Premium 🌟"
    ),
    "download_dir_error" to mapOf(
        "en" to "Music folder could not be created",
        "tr" to "Müzik klasörü oluşturulamadı",
        "es" to "No se pudo crear la carpeta de música",
        "fr" to "Impossible de créer le dossier de musique",
        "de" to "Musikordner konnte nicht erstellt werden",
        "it" to "Impossibile creare la cartella musicale",
        "pt" to "Não foi possível criar a pasta de música",
        "ru" to "Не удалось создать папку музыки",
        "ja" to "音楽フォルダを作成できませんでした",
        "zh" to "无法创建音乐文件夹",
        "ar" to "تعذر إنشاء مجلد الموسيقى",
        "ko" to "음악 폴더를 만들 수 없습니다"
    ),
    "speed" to mapOf(
        "en" to "Playback speed", "tr" to "Oynatma hızı", "es" to "Velocidad de reproducción",
        "fr" to "Vitesse de lecture", "de" to "Wiedergabegeschwindigkeit",
        "it" to "Velocità di riproduzione", "pt" to "Velocidade de reprodução",
        "ru" to "Скорость воспроизведения", "ja" to "再生速度", "zh" to "播放速度",
        "ar" to "سرعة التشغيل", "ko" to "재생 속도"
    ),
    "more_options" to mapOf(
        "en" to "More options", "tr" to "Diğer seçenekler", "es" to "Más opciones",
        "fr" to "Plus d'options", "de" to "Weitere Optionen", "it" to "Altre opzioni",
        "pt" to "Mais opções", "ru" to "Ещё", "ja" to "その他のオプション",
        "zh" to "更多选项", "ar" to "خيارات أكثر", "ko" to "추가 옵션"
    ),
    "appearance_title" to mapOf(
        "en" to "Appearance", "tr" to "Görünüm", "es" to "Apariencia", "fr" to "Apparence",
        "de" to "Darstellung", "it" to "Aspetto", "pt" to "Aparência", "ru" to "Оформление",
        "ja" to "外観", "zh" to "外观", "ar" to "المظهر", "ko" to "모양"
    ),
    "appearance_theme" to mapOf(
        "en" to "Theme", "tr" to "Tema", "es" to "Tema", "fr" to "Thème",
        "de" to "Design", "it" to "Tema", "pt" to "Tema", "ru" to "Тема",
        "ja" to "テーマ", "zh" to "主题", "ar" to "السمة", "ko" to "테마"
    ),
    "theme_mode" to mapOf(
        "en" to "Theme mode", "tr" to "Tema modu", "es" to "Modo de tema",
        "fr" to "Mode du thème", "de" to "Designmodus", "it" to "Modalità tema",
        "pt" to "Modo do tema", "ru" to "Режим темы", "ja" to "テーマモード",
        "zh" to "主题模式", "ar" to "وضع السمة", "ko" to "테마 모드"
    ),
    "theme_mode_auto" to mapOf(
        "en" to "Automatic", "tr" to "Otomatik", "es" to "Automático", "fr" to "Automatique",
        "de" to "Automatisch", "it" to "Automatico", "pt" to "Automático", "ru" to "Автоматически",
        "ja" to "自動", "zh" to "自动", "ar" to "تلقائي", "ko" to "자동"
    ),
    "theme_mode_light" to mapOf(
        "en" to "Light", "tr" to "Açık", "es" to "Claro", "fr" to "Clair",
        "de" to "Hell", "it" to "Chiaro", "pt" to "Claro", "ru" to "Светлая",
        "ja" to "ライト", "zh" to "浅色", "ar" to "فاتح", "ko" to "라이트"
    ),
    "theme_mode_dark" to mapOf(
        "en" to "Dark", "tr" to "Koyu", "es" to "Oscuro", "fr" to "Sombre",
        "de" to "Dunkel", "it" to "Scuro", "pt" to "Escuro", "ru" to "Тёмная",
        "ja" to "ダーク", "zh" to "深色", "ar" to "داكن", "ko" to "다크"
    ),
    "pure_black" to mapOf(
        "en" to "Pure black", "tr" to "Zifiri siyah", "es" to "Negro puro",
        "fr" to "Noir pur", "de" to "Reines Schwarz", "it" to "Nero puro",
        "pt" to "Preto puro", "ru" to "Чистый чёрный", "ja" to "ピュアブラック",
        "zh" to "纯黑", "ar" to "أسود نقي", "ko" to "퓨어 블랙"
    ),
    "material_you" to mapOf(
        "en" to "Material You", "tr" to "Material You", "es" to "Material You",
        "fr" to "Material You", "de" to "Material You", "it" to "Material You",
        "pt" to "Material You", "ru" to "Material You", "ja" to "Material You",
        "zh" to "Material You", "ar" to "Material You", "ko" to "Material You"
    ),
    "material_you_desc" to mapOf(
        "en" to "Use wallpaper colors", "tr" to "Duvar kağıdı renklerini kullan",
        "es" to "Usar colores del fondo de pantalla", "fr" to "Utiliser les couleurs du fond d'écran",
        "de" to "Hintergrundbildfarben verwenden", "it" to "Usa i colori dello sfondo",
        "pt" to "Usar cores do papel de parede", "ru" to "Использовать цвета обоев",
        "ja" to "壁紙の色を使用", "zh" to "使用壁纸颜色", "ar" to "استخدام ألوان الخلفية",
        "ko" to "배경 화면 색상 사용"
    ),
    "ui_enhancements" to mapOf(
        "en" to "UI enhancements", "tr" to "Arayüz iyileştirmeleri",
        "es" to "Mejoras de interfaz", "fr" to "Améliorations de l'interface",
        "de" to "UI-Verbesserungen", "it" to "Miglioramenti dell'interfaccia",
        "pt" to "Melhorias da interface", "ru" to "Улучшения интерфейса",
        "ja" to "UIの強化", "zh" to "界面增强", "ar" to "تحسينات الواجهة",
        "ko" to "UI 개선"
    ),
    "ui_enhancements_desc" to mapOf(
        "en" to "Smooth color transitions and motion", "tr" to "Yumuşak renk geçişleri ve hareketler",
        "es" to "Transiciones de color y movimiento suaves",
        "fr" to "Transitions de couleurs et animations fluides",
        "de" to "Sanfte Farbübergänge und Bewegung",
        "it" to "Transizioni di colore e movimento fluide",
        "pt" to "Transições de cor e movimento suaves",
        "ru" to "Плавные переходы цветов и анимация",
        "ja" to "滑らかな色の遷移と動き", "zh" to "平滑的颜色过渡和动画",
        "ar" to "انتقالات ألوان وحركة سلسة", "ko" to "부드러운 색상 전환과 움직임"
    ),
    "song_artwork_colors" to mapOf(
        "en" to "Album artwork colors", "tr" to "Albüm kapağı renkleri",
        "es" to "Colores de la portada", "fr" to "Couleurs de la pochette",
        "de" to "Coverfarben", "it" to "Colori della copertina",
        "pt" to "Cores da capa", "ru" to "Цвета обложки",
        "ja" to "アルバムアートの色", "zh" to "专辑封面颜色",
        "ar" to "ألوان غلاف الألبوم", "ko" to "앨범 아트 색상"
    ),
    "song_artwork_colors_desc" to mapOf(
        "en" to "Take colors from the playing song's cover",
        "tr" to "Çalan şarkının kapağından renk al",
        "es" to "Tomar colores de la portada de la canción",
        "fr" to "Prendre les couleurs de la pochette du morceau",
        "de" to "Farben vom Cover des laufenden Titels übernehmen",
        "it" to "Prendi i colori dalla copertina del brano",
        "pt" to "Usar cores da capa da música",
        "ru" to "Брать цвета с обложки играющей песни",
        "ja" to "再生中の曲のジャケットから色を取得",
        "zh" to "从正在播放的歌曲封面取色",
        "ar" to "أخذ الألوان من غلاف الأغنية المشغلة",
        "ko" to "재생 중인 곡의 커버에서 색상 추출"
    ),
    "palette_style" to mapOf(
        "en" to "Palette style", "tr" to "Palet stili", "es" to "Estilo de paleta",
        "fr" to "Style de palette", "de" to "Palettenstil", "it" to "Stile tavolozza",
        "pt" to "Estilo da paleta", "ru" to "Стиль палитры", "ja" to "パレットスタイル",
        "zh" to "调色板风格", "ar" to "نمط اللوحة", "ko" to "팔레트 스타일"
    ),
    "palette_tonal_spot" to mapOf(
        "en" to "Tonal spot", "tr" to "Tonlu leke", "es" to "Punto tonal",
        "fr" to "Tonalité ponctuelle", "de" to "Tonale Fläche", "it" to "Punto tonale",
        "pt" to "Ponto tonal", "ru" to "Тональное пятно", "ja" to "トーナルスポット",
        "zh" to "色调点", "ar" to "بقعة لونية", "ko" to "톤 스팟"
    ),
    "palette_vibrant" to mapOf(
        "en" to "Vibrant", "tr" to "Canlı", "es" to "Vibrante", "fr" to "Vibrant",
        "de" to "Lebendig", "it" to "Vivace", "pt" to "Vibrante", "ru" to "Яркая",
        "ja" to "ビビッド", "zh" to "鲜艳", "ar" to "نابض بالحياة", "ko" to "비비드"
    ),
    "palette_expressive" to mapOf(
        "en" to "Expressive", "tr" to "İfadeci", "es" to "Expresivo", "fr" to "Expressif",
        "de" to "Ausdrucksstark", "it" to "Espressivo", "pt" to "Expressivo",
        "ru" to "Выразительная", "ja" to "エクスプレッシブ", "zh" to "表现力",
        "ar" to "تعبيري", "ko" to "익스프레시브"
    ),
    "palette_rainbow" to mapOf(
        "en" to "Rainbow", "tr" to "Gökkuşağı", "es" to "Arcoíris", "fr" to "Arc-en-ciel",
        "de" to "Regenbogen", "it" to "Arcobaleno", "pt" to "Arco-íris", "ru" to "Радуга",
        "ja" to "レインボー", "zh" to "彩虹", "ar" to "قوس قزح", "ko" to "레인보우"
    ),
    "palette_fruit_salad" to mapOf(
        "en" to "Fruit salad", "tr" to "Meyve salatası", "es" to "Ensalada de frutas",
        "fr" to "Salade de fruits", "de" to "Fruchtsalat", "it" to "Macedonia",
        "pt" to "Salada de frutas", "ru" to "Фруктовый салат", "ja" to "フルーツサラダ",
        "zh" to "水果沙拉", "ar" to "سلطة الفواكه", "ko" to "프루트 샐러드"
    ),
    "palette_monochrome" to mapOf(
        "en" to "Monochrome", "tr" to "Tek renk", "es" to "Monocromático",
        "fr" to "Monochrome", "de" to "Monochrom", "it" to "Monocromatico",
        "pt" to "Monocromático", "ru" to "Монохромная", "ja" to "モノクローム",
        "zh" to "单色", "ar" to "أحادي اللون", "ko" to "모노크롬"
    ),
    "palette_neutral" to mapOf(
        "en" to "Neutral", "tr" to "Nötr", "es" to "Neutro", "fr" to "Neutre",
        "de" to "Neutral", "it" to "Neutro", "pt" to "Neutro", "ru" to "Нейтральная",
        "ja" to "ニュートラル", "zh" to "中性", "ar" to "محايد", "ko" to "뉴트럴"
    ),
    "palette_content" to mapOf(
        "en" to "Content", "tr" to "İçerik", "es" to "Contenido", "fr" to "Contenu",
        "de" to "Inhalt", "it" to "Contenuto", "pt" to "Conteúdo", "ru" to "Контентная",
        "ja" to "コンテンツ", "zh" to "内容", "ar" to "المحتوى", "ko" to "콘텐츠"
    ),
    "theme_color" to mapOf(
        "en" to "Theme color", "tr" to "Tema rengi", "es" to "Color del tema",
        "fr" to "Couleur du thème", "de" to "Designfarbe", "it" to "Colore del tema",
        "pt" to "Cor do tema", "ru" to "Цвет темы", "ja" to "テーマカラー",
        "zh" to "主题颜色", "ar" to "لون السمة", "ko" to "테마 색상"
    ),
    "theme_color_desc" to mapOf(
        "en" to "Base color of the app palette", "tr" to "Uygulama paletinin temel rengi",
        "es" to "Color base de la paleta de la app", "fr" to "Couleur de base de la palette",
        "de" to "Basisfarbe der App-Palette", "it" to "Colore base della tavolozza",
        "pt" to "Cor base da paleta do app", "ru" to "Базовый цвет палитры приложения",
        "ja" to "アプリのパレットの基本色", "zh" to "应用调色板的基础色",
        "ar" to "اللون الأساسي للوحة التطبيق", "ko" to "앱 팔레트의 기본 색상"
    ),
    "color_hex" to mapOf(
        "en" to "Color code (HEX)", "tr" to "Renk kodu (HEX)", "es" to "Código de color (HEX)",
        "fr" to "Code couleur (HEX)", "de" to "Farbcode (HEX)", "it" to "Codice colore (HEX)",
        "pt" to "Código de cor (HEX)", "ru" to "Код цвета (HEX)", "ja" to "カラーコード（HEX）",
        "zh" to "颜色代码（HEX）", "ar" to "رمز اللون (HEX)", "ko" to "색상 코드 (HEX)"
    ),
    "theme_preview" to mapOf(
        "en" to "Live preview", "tr" to "Canlı önizleme", "es" to "Vista previa en vivo",
        "fr" to "Aperçu en direct", "de" to "Live-Vorschau", "it" to "Anteprima live",
        "pt" to "Prévia ao vivo", "ru" to "Живой предпросмотр", "ja" to "ライブプレビュー",
        "zh" to "实时预览", "ar" to "معاينة مباشرة", "ko" to "실시간 미리보기"
    ),
    "theme_preview_title" to mapOf(
        "en" to "Your music, your colors", "tr" to "Müziğin, renklerin",
        "es" to "Tu música, tus colores", "fr" to "Ta musique, tes couleurs",
        "de" to "Deine Musik, deine Farben", "it" to "La tua musica, i tuoi colori",
        "pt" to "Sua música, suas cores", "ru" to "Твоя музыка — твои цвета",
        "ja" to "あなたの音楽、あなたの色", "zh" to "你的音乐，你的色彩",
        "ar" to "موسيقاك، ألوانك", "ko" to "당신의 음악, 당신의 색"
    ),
    "theme_preview_subtitle" to mapOf(
        "en" to "This is how the palette looks", "tr" to "Palet böyle görünüyor",
        "es" to "Así se ve la paleta", "fr" to "Voici à quoi ressemble la palette",
        "de" to "So sieht die Palette aus", "it" to "Ecco come appare la tavolozza",
        "pt" to "É assim que a paleta fica", "ru" to "Так выглядит палитра",
        "ja" to "パレットはこんな感じ", "zh" to "调色板效果预览",
        "ar" to "هكذا تبدو اللوحة", "ko" to "팔레트 미리보기"
    ),
    "playback" to mapOf(
        "en" to "Playback", "tr" to "Oynatma", "es" to "Reproducción", "fr" to "Lecture",
        "de" to "Wiedergabe", "it" to "Riproduzione", "pt" to "Reprodução",
        "ru" to "Воспроизведение", "ja" to "再生", "zh" to "播放",
        "ar" to "التشغيل", "ko" to "재생"
    ),
    "integrations" to mapOf(
        "en" to "Integrations", "tr" to "Entegrasyonlar"
    ),
    "integrations_desc" to mapOf(
        "en" to "Scrobbling and lyrics sources", "tr" to "Scrobble ve şarkı sözü kaynakları"
    ),
    "listenbrainz" to mapOf(
        "en" to "ListenBrainz", "tr" to "ListenBrainz"
    ),
    "listenbrainz_enabled" to mapOf(
        "en" to "Scrobble to ListenBrainz", "tr" to "ListenBrainz'e scrobble et"
    ),
    "listenbrainz_desc" to mapOf(
        "en" to "Send your listening history with a personal token", "tr" to "Dinleme geçmişini kişisel token ile gönder"
    ),
    "listenbrainz_token" to mapOf(
        "en" to "ListenBrainz token", "tr" to "ListenBrainz token'ı"
    ),
    "listenbrainz_token_missing" to mapOf(
        "en" to "No token configured", "tr" to "Token ayarlanmadı"
    ),
    "listenbrainz_token_hint" to mapOf(
        "en" to "Create a token at listenbrainz.org/profile and paste it here.", "tr" to "listenbrainz.org/profile adresinden token oluşturup buraya yapıştırın."
    ),
    "now_playing" to mapOf(
        "en" to "Send now playing", "tr" to "Şimdi çalıyor gönder"
    ),
    "test_connection" to mapOf(
        "en" to "Test connection", "tr" to "Bağlantıyı test et"
    ),
    "connected_as" to mapOf(
        "en" to "Connected as %s", "tr" to "%s olarak bağlandı"
    ),
    "connection_failed" to mapOf(
        "en" to "Connection failed: %s", "tr" to "Bağlantı başarısız: %s"
    ),
    "lyrics_providers" to mapOf(
        "en" to "Lyrics providers", "tr" to "Şarkı sözü kaynakları"
    ),
    "lyrics_providers_desc" to mapOf(
        "en" to "Sources are searched in order; turn off the ones you do not want.", "tr" to "Kaynaklar sırayla denenir; istemediklerinizi kapatın."
    ),
    "lyrics_settings" to mapOf(
        "en" to "Lyrics screen", "tr" to "Şarkı sözü ekranı"
    ),
    "lyrics_size_hint" to mapOf(
        "en" to "Text size: %s sp", "tr" to "Metin boyutu: %s sp"
    ),
    "lyrics_glow" to mapOf(
        "en" to "Glow on active line", "tr" to "Aktif satırda ışıma"
    ),
    "lyrics_click_seek" to mapOf(
        "en" to "Tap a line to seek", "tr" to "Satıra dokununca o ana atla"
    ),
    "token" to mapOf(
        "en" to "Token", "tr" to "Token"
    ),
    "set" to mapOf(
        "en" to "Set", "tr" to "Ayarla"
    ),
    "lyrics_options" to mapOf(
        "en" to "Lyrics options", "tr" to "Şarkı sözü seçenekleri"
    ),
    "lyrics_copy" to mapOf(
        "en" to "Copy lyrics", "tr" to "Sözleri kopyala"
    ),
    "lyrics_copied" to mapOf(
        "en" to "Lyrics copied", "tr" to "Sözler kopyalandı"
    ),
    "lyrics_reload" to mapOf(
        "en" to "Reload (ignore cache)", "tr" to "Yeniden yükle (önbelleği yok say)"
    ),
    "update_available" to mapOf(
        "en" to "Update available", "tr" to "Güncelleme hazır"
    ),
    "update_version" to mapOf(
        "en" to "New version", "tr" to "Yeni sürüm"
    ),
    "update_download" to mapOf(
        "en" to "Download", "tr" to "İndir"
    ),
    "update_later" to mapOf(
        "en" to "Later", "tr" to "Sonra"
    ),
    "update_downloading" to mapOf(
        "en" to "Downloading update…", "tr" to "Güncelleme indiriliyor…"
    ),
    "update_install_permission" to mapOf(
        "en" to "Allow installing apps from BeatBox, then try again.",
        "tr" to "BeatBox'tan uygulama yüklemeye izin verip tekrar deneyin."
    ),
    "update_install_failed" to mapOf(
        "en" to "Could not install the update.", "tr" to "Güncelleme yüklenemedi."
    )
)
