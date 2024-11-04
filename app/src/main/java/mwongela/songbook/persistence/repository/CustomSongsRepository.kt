package mwongela.songbook.persistence.repository

import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.util.lookup.LazyFinderByTuple
import mwongela.songbook.util.lookup.SimpleCache

data class CustomSongsRepository(
    val songs: SimpleCache<List<Song>>,
    val uncategorizedSongs: SimpleCache<List<Song>>,
    val allCustomCategory: Category,
) {

    val songFinder = LazyFinderByTuple(
        entityToId = { song -> song.songIdentifier() },
        valuesSupplier = songs
    )

    fun invalidate() {
        songs.invalidate()
        uncategorizedSongs.invalidate()

        songFinder.invalidate()
    }
}