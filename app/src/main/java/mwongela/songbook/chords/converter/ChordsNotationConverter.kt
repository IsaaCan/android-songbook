package mwongela.songbook.chords.converter

import mwongela.songbook.chords.detect.KeyDetector
import mwongela.songbook.chords.model.LyricsFragment
import mwongela.songbook.chords.model.LyricsTextType
import mwongela.songbook.chords.parser.ChordParser
import mwongela.songbook.chords.parser.LyricsExtractor
import mwongela.songbook.chords.render.ChordsRenderer
import mwongela.songbook.settings.chordsnotation.ChordsNotation

class ChordsNotationConverter(
    private val fromNotation: ChordsNotation,
    private val toNotation: ChordsNotation,
    private val forceSharps: Boolean = false,
) {

    fun convertLyrics(input: String, originalModifiers: Boolean = false): String {
        val lyrics = LyricsExtractor().parseLyrics(input)
        ChordParser(fromNotation).parseAndFillChords(lyrics)
        val key = KeyDetector().detectKey(lyrics)
        ChordsRenderer(toNotation, key, forceSharps).formatLyrics(lyrics, originalModifiers)
        return lyrics.displayString()
    }

    fun convertChordFragments(chord: String, originalModifiers: Boolean = false): String {
        val unknowns = mutableSetOf<String>()
        val chordFragments = ChordParser(fromNotation).parseSingleChordFragments(chord, unknowns)
        val lyricsFragment =
            LyricsFragment(chord, LyricsTextType.CHORDS, chordFragments = chordFragments)
        ChordsRenderer(toNotation, null, forceSharps).renderLyricsFragment(
            lyricsFragment,
            originalModifiers
        )
        return lyricsFragment.text
    }

}