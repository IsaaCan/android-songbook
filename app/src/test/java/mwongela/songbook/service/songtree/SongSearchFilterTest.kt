package mwongela.songbook.service.songtree

import org.assertj.core.api.Assertions.assertThat
import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.CategoryType
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.general.model.SongStatus
import mwongela.songbook.songselection.search.SongSearchFilter
import mwongela.songbook.songselection.tree.SongTreeItem
import org.junit.Test

class SongSearchFilterTest {

    @Test
    fun test_matchesNameFilter() {
        val songItem = SongTreeItem.song(Song(
                id = "1",
                title = "Jolka jolka ążśźęćół ĄĄŻŚŹĘĆ Żółć Łódź",
                categories = mutableListOf(
                        Category(1, type = CategoryType.ARTIST, name = "Budka suflera")
                ),
                status = SongStatus.PUBLISHED
        ))

        assertThat(songItem.song!!.categories[0].name).isEqualTo("Budka suflera")

        assertThat(songItem.song!!.displayName())
                .isEqualTo("Jolka jolka ążśźęćół ĄĄŻŚŹĘĆ Żółć Łódź - Budka suflera")

        assertThat(SongSearchFilter("Budka").matchSong(songItem.song!!)).isTrue()
        assertThat(SongSearchFilter("budka").matchSong(songItem.song!!)).isTrue()
        assertThat(SongSearchFilter("uFL udK").matchSong(songItem.song!!)).isTrue()
        assertThat(SongSearchFilter("jolka suflera").matchSong(songItem.song!!)).isTrue()
        assertThat(SongSearchFilter("dupka").matchSong(songItem.song!!)).isFalse()
        assertThat(SongSearchFilter("dupka suflera").matchSong(songItem.song!!)).isFalse()
        // polish letters
        assertThat(SongSearchFilter("żółć łÓDŹ").matchSong(songItem.song!!)).isTrue()
        assertThat(SongSearchFilter("zolc").matchSong(songItem.song!!)).isTrue()
        assertThat(SongSearchFilter("azszecol aazszec lodz zolc").matchSong(songItem.song!!))
                .isTrue()
    }

    @Test
    fun test_filteringWithQuotes() {
        val songItem = SongTreeItem.song(Song(
                id = "1",
                title = "he's dupa",
                categories = mutableListOf(
                        Category(1, type = CategoryType.ARTIST, name = "Budka suflera")
                ),
                status = SongStatus.PUBLISHED
        ))

        assertThat(SongSearchFilter("d'upa hes").matchSong(songItem.song!!)).isTrue()
    }
}
