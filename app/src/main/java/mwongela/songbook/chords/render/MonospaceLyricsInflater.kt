package mwongela.songbook.chords.render

import mwongela.songbook.chords.model.LyricsFragment
import mwongela.songbook.chords.model.LyricsModel
import mwongela.songbook.chords.model.LyricsTextType
import mwongela.songbook.chords.model.lineWrapperChar

class MonospaceLyricsInflater(
    private val fontsize: Float = 1f,
){
    val lengthMapper = TypefaceLengthMapper()

    fun inflateLyrics(model: LyricsModel): LyricsModel {
        calculateTextWidth(" ", LyricsTextType.REGULAR_TEXT)
        calculateTextWidth(" ", LyricsTextType.CHORDS)
        calculateTextWidth(" ", LyricsTextType.COMMENT)
        calculateTextWidth(lineWrapperChar.toString(), LyricsTextType.REGULAR_TEXT)

        model.lines.forEach { line ->
            line.fragments.forEach { fragment ->
                inflateFragment(fragment)
            }
        }
        return model
    }

    private fun inflateFragment(fragment: LyricsFragment) {
        fragment.width = fragment.text.length * fontsize
        calculateTextWidth(fragment.text, fragment.type)
    }

    private fun calculateTextWidth(text: String, type: LyricsTextType) {
        text.forEach { c: Char ->
            lengthMapper.put(type, c, fontsize)
        }
    }
}
