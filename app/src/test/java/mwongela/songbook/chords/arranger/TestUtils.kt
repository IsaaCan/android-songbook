package mwongela.songbook.chords.arranger

import mwongela.songbook.chords.model.ChordFragment
import mwongela.songbook.chords.model.ChordFragmentType
import mwongela.songbook.chords.render.TypefaceLengthMapper
import mwongela.songbook.chords.model.LyricsFragment
import mwongela.songbook.chords.model.LyricsTextType
import mwongela.songbook.chords.parser.ChordParser


internal fun text(str: String, x: Float = 0f, w: Float? = null): LyricsFragment {
    val width = w ?: str.length.toFloat()
    return LyricsFragment.text(str, width = width, x = x)
}

internal fun chord(str: String, x: Float = 0f, w: Float? = null): LyricsFragment {
    val width = w ?: str.length.toFloat()
    return LyricsFragment.chords(str, width = width, x = x)
}

internal fun singleChord(str: String, parser: ChordParser, x: Float): LyricsFragment {
    val singleChord = parser.recognizeSingleChord(str)
    return LyricsFragment(
        str,
        LyricsTextType.CHORDS,
        x = x,
        width = str.length.toFloat(),
        chordFragments = listOf(
            ChordFragment(
                text = str,
                type = ChordFragmentType.SINGLE_CHORD,
                singleChord = singleChord,
            )
        ),
    )
}

internal fun linewrapper(screenWRelative: Float): LyricsFragment {
    return LyricsFragment.lineWrapper.apply { x = screenWRelative - 1f; width = 1f }
}

internal val equalLengthMapper = object : TypefaceLengthMapper() {
    override fun charWidth(type: LyricsTextType, char: Char): Float {
        return 1f
    }
}
