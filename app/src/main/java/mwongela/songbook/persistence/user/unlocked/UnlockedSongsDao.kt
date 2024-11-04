package mwongela.songbook.persistence.user.unlocked

import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.AbstractJsonDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class UnlockedSongsDao(
    path: String,
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    resetOnError: Boolean = false,
) : AbstractJsonDao<UnlockedSongsDb>(
    path,
    dbName = "unlocked",
    schemaVersion = 1,
    clazz = UnlockedSongsDb::class.java,
    serializer = UnlockedSongsDb.serializer(),
) {
    private val songsRepository by LazyExtractor(songsRepository)

    val unlockedSongs: UnlockedSongsDb get() = db!!

    init {
        read(resetOnError)
    }

    override fun empty(): UnlockedSongsDb {
        return UnlockedSongsDb(mutableListOf())
    }

    fun unlockKey(key: String) {
        val keys = unlockedSongs.keys
        if (key !in keys)
            keys.add(key)

        runBlocking(Dispatchers.IO) {
            songsRepository.saveAndReloadAllSongs()
        }
    }

}