package mwongela.songbook.chords.detect

import mwongela.songbook.chords.model.Note
import mwongela.songbook.chords.parser.ChordParser
import mwongela.songbook.chords.parser.LyricsExtractor
import mwongela.songbook.chords.syntax.MajorKey
import mwongela.songbook.settings.chordsnotation.ChordsNotation
import org.assertj.core.api.Assertions
import org.junit.Test

class KeyDetectorTest {

    @Test
    fun test_detectLyricsKey() {
        val lyrics = LyricsExtractor().parseLyrics("""
        dupa [Am F C7/G# G]
        [F] next [Am]verse [G]
        """.trimIndent())

        val chordParser = ChordParser(ChordsNotation.ENGLISH)
        chordParser.parseAndFillChords(lyrics)

        val uniqueNotes = UniqueChordsFinder().findUniqueNotesInLyrics(lyrics)
        Assertions.assertThat(uniqueNotes).isEqualTo(setOf(
            Note.A.index, Note.F.index, Note.C.index, Note.G.index
        ))

        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.C_MAJOR)
    }

    @Test
    fun test_detectGMajorKey() {
        val lyrics = LyricsExtractor().parseLyrics("[F# E D G]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.G_MAJOR)
    }

    @Test
    fun test_detectDMajorKey() {
        val lyrics = LyricsExtractor().parseLyrics("[E D A]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.A_MAJOR)
    }

    @Test
    fun test_detectMajorKeyWithMinorChord() {
        val lyrics = LyricsExtractor().parseLyrics("[F#m E D]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val scores = KeyDetector().detectKeyScores(lyrics)
        println(scores)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.D_MAJOR)
    }

    @Test
    fun test_detectAMajorKeyWithMinorChord() {
        val lyrics = LyricsExtractor().parseLyrics("[F#m E D A]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val scores = KeyDetector().detectKeyScores(lyrics)
        println(scores)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.A_MAJOR)
    }

    @Test
    fun test_detectFMajorKey() {
        val lyrics = LyricsExtractor().parseLyrics("[Dm C Bb F]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.F_MAJOR)
    }

    @Test
    fun test_detectBbMajorKey() {
        val lyrics = LyricsExtractor().parseLyrics("[Dm C Bb F Eb]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.B_FLAT_MAJOR)
    }

    @Test
    fun test_favourDominantChords() {
        val lyrics = LyricsExtractor().parseLyrics("[G D C G]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.G_MAJOR)
    }

    @Test
    fun test_allChromaticChords() {
        val lyrics = LyricsExtractor().parseLyrics("[C C# D D# E F F# G G# A Bb B]")
        ChordParser(ChordsNotation.ENGLISH).parseAndFillChords(lyrics)
        val scores = KeyDetector().detectKeyScores(lyrics)
        println(scores)
        val key = KeyDetector().detectKey(lyrics)
        Assertions.assertThat(key).isEqualTo(MajorKey.C_MAJOR)
    }

}