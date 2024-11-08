package mwongela.songbook.persistence.user.history

import mwongela.songbook.persistence.general.model.SongNamespace
import mwongela.songbook.persistence.user.AbstractJsonDao
import io.reactivex.subjects.PublishSubject
import java.util.Date

class OpenHistoryDao(
    path: String,
    resetOnError: Boolean = false,
) : AbstractJsonDao<OpenHistoryDb>(
    path,
    dbName = "history",
    schemaVersion = 1,
    clazz = OpenHistoryDb::class.java,
    serializer = OpenHistoryDb.serializer()
) {
    val historyDb: OpenHistoryDb get() = db!!
    val historyDbSubject = PublishSubject.create<OpenHistoryDb>()

    private val openedHistoryLimit = 50

    init {
        read(resetOnError)
    }

    override fun empty(): OpenHistoryDb {
        return OpenHistoryDb()
    }

    fun registerOpenedSong(songId: String, namespace: SongNamespace) {
        if (namespace == SongNamespace.Ephemeral)
            return

        val custom = namespace == SongNamespace.Custom
        // remove other occurrences and old history
        historyDb.songs = historyDb.songs
            .filter { s -> !(s.songId == songId && s.custom == custom) }
            .take(openedHistoryLimit - 1)
            .toMutableList()
        historyDb.songs.add(0, OpenedSong(songId, custom, Date().time))
        historyDbSubject.onNext(historyDb)
    }

    fun removeUsage(songId: String, custom: Boolean) {
        historyDb.songs = historyDb.songs.filter { song ->
            !(song.songId == songId && song.custom == custom)
        }.toMutableList()
    }
}