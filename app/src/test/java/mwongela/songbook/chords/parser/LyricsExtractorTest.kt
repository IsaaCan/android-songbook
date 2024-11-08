package mwongela.songbook.chords.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import mwongela.songbook.chords.model.LyricsFragment
import mwongela.songbook.chords.model.LyricsLine
import mwongela.songbook.chords.model.LyricsModel
import mwongela.songbook.chords.model.LyricsTextType

class LyricsExtractorTest {

    @Test
    fun test_parse_lyrics_and_mark_chords() {
        val model: LyricsModel = LyricsExtractor().parseLyrics("""
        dupa [a F C7/G(-A) G]
        [D]next li[e]ne [C]
        
           next [a]verse [G]    
        without chords
        
        
        """.trimIndent())
        assertThat(model.lines).hasSize(5)
        assertThat(model.lines).isEqualTo(listOf(
            LyricsLine(listOf(
                LyricsFragment(text = "dupa ", type = LyricsTextType.REGULAR_TEXT),
                LyricsFragment(text = "a F C7/G(-A) G", type = LyricsTextType.CHORDS),
            )),
            LyricsLine(listOf(
                LyricsFragment(text = "D", type = LyricsTextType.CHORDS),
                LyricsFragment(text = "next li", type = LyricsTextType.REGULAR_TEXT),
                LyricsFragment(text = "e", type = LyricsTextType.CHORDS),
                LyricsFragment(text = "ne ", type = LyricsTextType.REGULAR_TEXT),
                LyricsFragment(text = "C", type = LyricsTextType.CHORDS),
            )),
            LyricsLine(listOf()),
            LyricsLine(listOf(
                LyricsFragment(text = "next ", type = LyricsTextType.REGULAR_TEXT),
                LyricsFragment(text = "a", type = LyricsTextType.CHORDS),
                LyricsFragment(text = "verse ", type = LyricsTextType.REGULAR_TEXT),
                LyricsFragment(text = "G", type = LyricsTextType.CHORDS),
            )),
            LyricsLine(listOf(
                LyricsFragment(text = "without chords", type = LyricsTextType.REGULAR_TEXT),
            ))
        ))
    }

    @Test
    fun test_parse_many_brackets() {
        val model = LyricsExtractor().parseLyrics("""
        [[a ]]b[
        c
        ]
        """.trimIndent())

        assertThat(model.lines).isEqualTo(listOf(
            LyricsLine(listOf(
                LyricsFragment(text = "a ", type = LyricsTextType.CHORDS),
                LyricsFragment(text = "b", type = LyricsTextType.REGULAR_TEXT)
            )),
            LyricsLine(listOf(
                LyricsFragment(text = "c", type = LyricsTextType.CHORDS),
            ))
        ))
    }

    @Test
    fun test_parse_without_trimming() {
        val model = LyricsExtractor(trimWhitespaces = false).parseLyrics("""
           a  [a]
        bcde
        """.trimIndent())

        assertThat(model.lines).isEqualTo(
            listOf(
                LyricsLine(
                    listOf(
                        LyricsFragment(text = "   a  ", type = LyricsTextType.REGULAR_TEXT),
                        LyricsFragment(text = "a", type = LyricsTextType.CHORDS)
                    ),
                ),
                LyricsLine(
                    listOf(
                        LyricsFragment(text = "bcde", type = LyricsTextType.REGULAR_TEXT),
                    ),
                )
            )
        )
    }


    @Test
    fun test_parse_comments() {
        val model = LyricsExtractor().parseLyrics(
            """
        {this is title}
        dupa [a]
        
        {chorus}
        next [a]verse {inline comment} [G]
        """.trimIndent()
        )
        assertThat(model.lines).isEqualTo(
            listOf(
                LyricsLine(
                    listOf(
                        LyricsFragment(text = "this is title", type = LyricsTextType.COMMENT),
                    )
                ),
                LyricsLine(
                    listOf(
                        LyricsFragment(text = "dupa ", type = LyricsTextType.REGULAR_TEXT),
                        LyricsFragment(text = "a", type = LyricsTextType.CHORDS),
                    )
                ),
                LyricsLine(listOf()),
                LyricsLine(
                    listOf(
                        LyricsFragment(text = "chorus", type = LyricsTextType.COMMENT),
                    )
                ),
                LyricsLine(
                    listOf(
                        LyricsFragment(text = "next ", type = LyricsTextType.REGULAR_TEXT),
                        LyricsFragment(text = "a", type = LyricsTextType.CHORDS),
                        LyricsFragment(text = "verse ", type = LyricsTextType.REGULAR_TEXT),
                        LyricsFragment(text = "inline comment", type = LyricsTextType.COMMENT),
                        LyricsFragment(text = " ", type = LyricsTextType.REGULAR_TEXT),
                        LyricsFragment(text = "G", type = LyricsTextType.CHORDS),
                    )
                ),
            )
        )
    }
}