package mwongela.songbook.chords.transform

import mwongela.songbook.chords.arranger.LyricsArranger
import mwongela.songbook.chords.arranger.singleChord
import mwongela.songbook.chords.arranger.text
import mwongela.songbook.chords.detect.KeyDetector
import mwongela.songbook.chords.model.LyricsCloner
import mwongela.songbook.chords.model.LyricsModel
import mwongela.songbook.chords.parser.ChordParser
import mwongela.songbook.chords.parser.LyricsExtractor
import mwongela.songbook.chords.render.ChordsRenderer
import mwongela.songbook.chords.render.MonospaceLyricsInflater
import mwongela.songbook.chords.transpose.ChordsTransposer
import mwongela.songbook.settings.chordsnotation.ChordsNotation
import mwongela.songbook.settings.theme.DisplayStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LyricsTransformTest {

    @Test
    fun test_transform_one_line() {
        val rawContent = "Food alone won't [Gm7]ease the hun-[Am7]ger [Bbsus2]in [C]their [Dm]eyes"
        val arrangedLyrics = transformLyrics(rawContent)

        /* Should be transformed into:
                         Gm7          Am7 Bbsus2 C     Dm
        Food alone won't ease the hun-ger in     their eyes
         */
        val chordParser = ChordParser(ChordsNotation.ENGLISH)
        assertThat(arrangedLyrics.lines).hasSize(2)
        assertThat(arrangedLyrics.lines[0].fragments).containsExactly(
            singleChord("Gm7", chordParser, x = 17f),
            singleChord("Am7", chordParser, x = 30f),
            singleChord("Bbsus2", chordParser, x = 34f),
            singleChord("C", chordParser, x = 41f),
            singleChord("Dm", chordParser, x = 47f),
        )
        assertThat(arrangedLyrics.lines[1].fragments).containsExactly(
            text("Food alone won't", x = 0f, w = 17f),
            text("ease the hun-", x = 17f),
            text("ger", x = 30f, w = 4f),
            text("in", x = 34f, w = 3f),
            text("their", x = 41f, w = 6f),
            text("eyes", x = 47f),
        )
    }

}

fun transformLyrics(
    rawContent: String,
    fontsize: Float = 1f,
    notation: ChordsNotation = ChordsNotation.ENGLISH,
    trimWhitespaces: Boolean = true,
    transposedBy: Int = 0,
    forceSharps: Boolean = false,
    originalModifiers: Boolean = true,
    screenW: Float = 100f,
): LyricsModel {

    // extract lyrics: split
    val lyrics: LyricsModel = LyricsExtractor(trimWhitespaces=trimWhitespaces).parseLyrics(rawContent)
    // parse chords
    val chordParser = ChordParser(notation)
    chordParser.parseAndFillChords(lyrics)

    // transpose
    val transposedLyrics: LyricsModel = ChordsTransposer().transposeLyrics(lyrics, transposedBy)
    val songKey = KeyDetector().detectKey(transposedLyrics)
    // format chords (render)
    ChordsRenderer(notation, songKey, forceSharps=forceSharps).formatLyrics(
        transposedLyrics,
        originalModifiers = originalModifiers,
    )

    // inflate text
    val lyricsInflater = MonospaceLyricsInflater(fontsize) // DUMMY inflater
    val tmpLyrics: LyricsModel = LyricsCloner().cloneLyrics(transposedLyrics)
    val inflatedLyrics: LyricsModel = lyricsInflater.inflateLyrics(tmpLyrics)

    // arrange lyrics and chords
    val screenWRelative = screenW / fontsize
    val lyricsWrapper = LyricsArranger(
        DisplayStyle.ChordsAbove,
        screenWRelative,
        lyricsInflater.lengthMapper,
    )
    return lyricsWrapper.arrangeModel(inflatedLyrics)
}
