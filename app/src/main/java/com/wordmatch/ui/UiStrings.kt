package com.wordmatch.ui

import com.wordmatch.config.GameConfig

/** All Hebrew UI copy in one place (single source of truth; app is Hebrew-only). */
internal object Ui {
    const val LOADING = "טוען..."
    const val ERROR = "שגיאה בטעינת המילים"

    // Start screen
    const val TITLE = "וורדמץ׳"
    const val PICK = "בחר נושא ומספר מילים"
    const val CATEGORY_LABEL = "נושא"
    const val CATEGORY_ALL = "הכל"
    const val SIZE_LABEL = "מספר מילים"
    const val START = "התחל"
    const val BEST_TO_BEAT = "שיא לשבור: "
    const val NO_RECORD = "אין שיא עדיין — קבע אחד!"
    const val SETTINGS_GEAR = "⚙"

    // Game screen
    const val INSTRUCTION = "התרגם את המילה"
    const val LANG_HINT = "עברית → אנגלית"
    const val INPUT_PLACEHOLDER = "תשובתך כאן"
    const val CHECK = "בדוק"
    const val FORFEIT = "מוותר"
    const val EXIT = "יציאה"
    const val HINT = "רמז"
    const val HINT_PREFIX = "רמז: "
    const val GOT_IT = "הבנתי!"
    const val CORRECT = "✅ כל הכבוד!"
    const val WRONG = "❌ עוד לא... נסה שוב"
    const val ANSWER_WAS = "התשובה היתה: "

    // Summary screen
    const val SUMMARY_TITLE = "סיום!"
    const val POINTS_WORD = "נקודות"
    const val NEW_RECORD = "🏆 שיא חדש!"
    const val PLAY_AGAIN = "שחק שוב"
    const val TO_MENU = "תפריט"
    const val SESSION_BEST_STREAK = "רצף שיא: "
    const val OF = " מתוך "
    const val CORRECT_WORD = " נכון"

    // Settings
    const val SETTINGS_TITLE = "הגדרות"
    const val SOUND = "קול"
    const val RESET_SCORES = "אפס שיאים"
    const val CLOSE = "סגור"

    /** English category key -> Hebrew label. Falls back to the raw key if unmapped. */
    private val CATEGORY_HE = mapOf(
        "animals" to "חיות", "colors" to "צבעים", "verbs" to "פעלים", "objects" to "חפצים",
        "places" to "מקומות", "fruits" to "פירות", "nature" to "טבע", "numbers" to "מספרים",
        "body" to "גוף", "clothing" to "ביגוד", "feelings" to "רגשות", "work" to "עבודה",
        "school" to "בית ספר", "music" to "מוזיקה", "people" to "אנשים", "sentences" to "משפטים",
        "names" to "שמות", "grammar" to "דקדוק", "adjectives" to "תארים", "adverbs" to "תארי פועל",
        "prepositions" to "מילות יחס", "seasons" to "עונות", "transport" to "תחבורה", "shapes" to "צורות"
    )

    fun categoryLabel(key: String?): String =
        if (key == null) CATEGORY_ALL else CATEGORY_HE[key] ?: key

    fun progress(done: Int, total: Int): String = "$done/$total"

    fun scoreToBeat(best: Int): String = if (best <= 0) NO_RECORD else "$BEST_TO_BEAT$best"

    init { require(GameConfig.DEFAULT_SESSION_SIZE in GameConfig.SESSION_SIZES) }
}
