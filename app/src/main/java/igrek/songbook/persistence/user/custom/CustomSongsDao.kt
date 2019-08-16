package igrek.songbook.persistence.user.custom

import android.app.Activity
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.info.logger.WrapContextError
import igrek.songbook.persistence.general.model.Category
import igrek.songbook.persistence.general.model.CategoryType
import igrek.songbook.persistence.general.model.Song
import igrek.songbook.persistence.repository.SongsRepository
import igrek.songbook.persistence.user.AbstractJsonDao
import igrek.songbook.persistence.user.migrate.Migration037CustomSongs
import javax.inject.Inject

class CustomSongsDao(path: String) : AbstractJsonDao<CustomSongsDb>(
        path,
        dbName = "customsongs",
        schemaVersion = 1,
        clazz = CustomSongsDb::class.java,
        serializer = CustomSongsDb.serializer()
) {

    val customSongs: CustomSongsDb get() = db!!
    var customCategories = listOf<Category>()

    @Inject
    lateinit var songsRepository: SongsRepository
    @Inject
    lateinit var activity: Activity

    init {
        DaggerIoc.factoryComponent.inject(this)
        read()
    }

    override fun read() {
        super.read()
        customCategories = customSongs.songs.map { song ->
            song.categoryName
        }.toSet().map { categoryName ->
            val id = 0L // TODO get next id
            Category(
                    id = id,
                    type = CategoryType.ARTIST,
                    name = categoryName,
                    custom = true,
                    songs = mutableListOf() // TODO bind with custom songs
            )
        }
    }

    override fun empty(): CustomSongsDb {
        return CustomSongsDb(mutableListOf())
    }

    override fun migrateOlder(): CustomSongsDb? {
        try {
            return Migration037CustomSongs(activity).load()
        } catch (t: Exception) {
            throw WrapContextError("Migration037CustomSongs error", t)
        }
    }

    fun saveCustomSong(newSong: CustomSong) {
        val olds = customSongs.songs
                .filter { song -> song.id != newSong.id }.toMutableList()
        if (newSong.id == 0L)
            newSong.id = nextId(olds)

        olds.add(newSong)
        customSongs.songs = olds
        songsRepository.reloadUserData()
    }

    private fun nextId(songs: MutableList<CustomSong>): Long {
        return (songs.map { song -> song.id }.max() ?: 0) + 1
    }

    fun removeCustomSong(newSong: CustomSong) {
        val olds = customSongs.songs
                .filter { song -> song.id != newSong.id }.toMutableList()
        customSongs.songs = olds
        // clean up other usages
        songsRepository.favouriteSongsDao.removeUsage(newSong.id, true)
        songsRepository.playlistDao.removeUsage(newSong.id, true)
        songsRepository.openHistoryDao.removeUsage(newSong.id, true)

        songsRepository.reloadUserData()
    }

}