package mwongela.songbook.settings.chordsnotation

import mwongela.songbook.R

enum class ChordsNotation(val id: Long, val displayNameResId: Int, val shortNameResId: Int) {

    ENGLISH(3, R.string.notation_english, R.string.notation_short_english),

    GERMAN(1, R.string.notation_german, R.string.notation_short_german),

    GERMAN_IS(2, R.string.notation_german_is, R.string.notation_short_german_is),

    DUTCH(5, R.string.notation_dutch, R.string.notation_short_dutch),

    JAPANESE(6, R.string.notation_japanese, R.string.notation_short_japanese),

    SOLFEGE(4, R.string.notation_solfege, R.string.notation_short_solfege),

    ;

    companion object {
        val default: ChordsNotation = ENGLISH

        fun parseById(id: Long?): ChordsNotation? {
            if (id == null)
                return null
            return values().firstOrNull { v -> v.id == id }
        }

        fun mustParseById(id: Long?): ChordsNotation {
            if (id == null)
                return default
            return parseById(id) ?: default
        }

        fun deserialize(id: Long): ChordsNotation? {
            return values().firstOrNull { v -> v.id == id }
        }
    }
}
