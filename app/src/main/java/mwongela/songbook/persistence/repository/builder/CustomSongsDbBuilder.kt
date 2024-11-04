package mwongela.songbook.persistence.repository.builder

import mwongela.songbook.info.UiResourceService
import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.CategoryType
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.repository.CustomSongsRepository
import mwongela.songbook.persistence.user.UserDataDao
import mwongela.songbook.persistence.user.custom.CustomCategory
import mwongela.songbook.persistence.user.custom.CustomSongMapper
import mwongela.songbook.util.lookup.FinderByTuple
import mwongela.songbook.util.lookup.SimpleCache

class CustomSongsDbBuilder(private val userDataDao: UserDataDao) {

    fun buildCustom(uiResourceService: UiResourceService): CustomSongsRepository {
        val allCustomCategory = Category(
            id = CategoryType.CUSTOM.id,
            type = CategoryType.CUSTOM,
            name = null,
            custom = false,
            songs = mutableListOf()
        )
        refillCategoryDisplayName(uiResourceService, allCustomCategory)
        val (customSongs, customSongsUncategorized) = assembleCustomSongs(allCustomCategory)
        return CustomSongsRepository(
            songs = SimpleCache { customSongs },
            uncategorizedSongs = SimpleCache { customSongsUncategorized },
            allCustomCategory = allCustomCategory
        )
    }

    private fun refillCategoryDisplayName(
        uiResourceService: UiResourceService,
        category: Category
    ) {
        category.displayName = when {
            category.type.localeStringId != null ->
                uiResourceService.resString(category.type.localeStringId)
            else -> category.name
        }
    }

    private fun assembleCustomSongs(customGeneralCategory: Category): Pair<List<Song>, List<Song>> {
        val customSongs = userDataDao.customSongsDao.customSongs.songs
        val mapper = CustomSongMapper()

        // bind custom categories to songs
        userDataDao.customSongsDao.customCategories = customSongs
            .asSequence()
            .map { song ->
                song.categoryName
            }.toSet()
            .filterNotNull()
            .filter { it.isNotEmpty() }
            .map { categoryName ->
                CustomCategory(name = categoryName)
            }.toList()
        val customCategoryFinder = FinderByTuple(userDataDao.customSongsDao.customCategories) {
            it.name
        }
        val customSongsUncategorized = mutableListOf<Song>()
        val customModelSongs = mutableListOf<Song>()

        customSongs.forEach { customSong ->
            val song = mapper.customSongToSong(customSong)

            song.categories = mutableListOf(customGeneralCategory)
            customGeneralCategory.songs.add(song)

            customModelSongs.add(song)

            val customCategory: CustomCategory? = customCategoryFinder.find(
                customSong.categoryName ?: ""
            )
            if (customCategory == null) {
                customSongsUncategorized.add(song)
            } else {
                customCategory.songs.add(song)
            }
        }

        return customModelSongs to customSongsUncategorized
    }

}