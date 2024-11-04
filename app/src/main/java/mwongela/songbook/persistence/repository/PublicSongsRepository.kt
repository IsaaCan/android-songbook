package mwongela.songbook.persistence.repository

import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.util.lookup.LazyFinderByTuple
import mwongela.songbook.util.lookup.SimpleCache

data class PublicSongsRepository(
    val versionNumber: Long,
    val categories: SimpleCache<List<Category>>,
    val songs: SimpleCache<List<Song>>
) {
    private val categoryFinder = LazyFinderByTuple(
        entityToId = { e -> e.id },
        valuesSupplier = categories
    )

    val songFinder = LazyFinderByTuple(
        entityToId = { song -> song.songIdentifier() },
        valuesSupplier = songs
    )

    fun invalidate() {
        categories.invalidate()
        songs.invalidate()

        categoryFinder.invalidate()
        songFinder.invalidate()
    }

}