package com.blackstack.beatbox

enum class EQPreset(val label: String, val labelTR: String, val desc: String, val descTR: String) {
    NONE("Normal", "Normal", "Original balanced audio profile", "Orijinal dengeli ses profili"),
    BASS_BOOST("Bass Boost", "Bas Güçlendirme", "Deep low-end punch & enhanced rhythm", "Derin bas vuruşları ve güçlü ritim"),
    VOCAL("Vocal Boost", "Vokal", "Crystal clear vocals & speech clarity", "Net ve ön planda vokaller"),
    TREBLE("Treble Boost", "Tiz", "Bright highs & crisp instrument details", "Parlak tizler ve enstrüman detayları"),
    LOUDNESS("Loudness", "Yüksek Ses", "Maximized volume & rich fullness", "Maksimum ses seviyesi ve dolgunluk");
}
