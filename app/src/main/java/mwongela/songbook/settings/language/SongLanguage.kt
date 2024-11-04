package mwongela.songbook.settings.language

enum class SongLanguage(val langCode: String) {

    UNKNOWN("_"),

    ENGLISH("en"),

    POLISH("pl");

    companion object {
        fun parseByLangCode(langCode: String): SongLanguage? {
            return values().firstOrNull { v -> v.langCode == langCode }
        }

        fun allKnown(): Set<SongLanguage> = values().filterNot { it == UNKNOWN }.toSet()
    }
}
