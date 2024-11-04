package mwongela.songbook.persistence.repository

import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.CategoryType
import mwongela.songbook.util.lookup.LazyFinderByTuple
import mwongela.songbook.util.lookup.SimpleCache

data class AllSongsRepository(
    val publicSongsRepo: PublicSongsRepository,
    val customSongsRepo: CustomSongsRepository,
) {

    val songs = SimpleCache {
        publicSongsRepo.songs.get() + customSongsRepo.songs.get()
    }

    val categories = SimpleCache {
        publicSongsRepo.categories.get()
    }

    var publicCategories: SimpleCache<List<Category>> = SimpleCache {
        publicSongsRepo.categories.get().filter { c -> c.type != CategoryType.CUSTOM }
    }

    val songFinder = LazyFinderByTuple(
        entityToId = { song -> song.songIdentifier() },
        valuesSupplier = songs
    )

    private val categoryFinder = LazyFinderByTuple(
        entityToId = { e -> e.id },
        valuesSupplier = categories
    )

    fun invalidate() {
        publicSongsRepo.invalidate()
        customSongsRepo.invalidate()

        songs.invalidate()
        categories.invalidate()
        publicCategories.invalidate()
        songFinder.invalidate()
        categoryFinder.invalidate()
    }

}