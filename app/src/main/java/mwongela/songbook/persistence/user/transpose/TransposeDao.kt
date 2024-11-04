package mwongela.songbook.persistence.user.transpose

import mwongela.songbook.persistence.general.model.SongIdentifier
import mwongela.songbook.persistence.general.model.SongNamespace
import mwongela.songbook.persistence.user.AbstractJsonDao
import io.reactivex.subjects.PublishSubject

class TransposeDao(
    path: String,
    resetOnError: Boolean = false,
) : AbstractJsonDao<TransposeDb>(
    path,
    dbName = "transpose",
    schemaVersion = 1,
    clazz = TransposeDb::class.java,
    serializer = TransposeDb.serializer()
) {
    private val transposeDb: TransposeDb get() = db!!
    private val transposeDbSubject = PublishSubject.create<TransposeDb>()

    init {
        read(resetOnError)
    }

    override fun empty(): TransposeDb {
        return TransposeDb()
    }

    fun getSongTransposition(songIdentifier: SongIdentifier): Int {
        val songFound = transposeDb.songs
            .find {
                it.songId == songIdentifier.songId
                        && it.custom == (songIdentifier.namespace == SongNamespace.Custom)
            }
        return songFound?.transposition ?: 0
    }

    fun setSongTransposition(songIdentifier: SongIdentifier, transposition: Int) {
        val songFound = transposeDb.songs
            .find {
                it.songId == songIdentifier.songId
                        && it.custom == (songIdentifier.namespace == SongNamespace.Custom)
            }
        if (songFound == null) {
            val newSong = TransposedSong(
                songIdentifier.songId,
                (songIdentifier.namespace == SongNamespace.Custom),
                transposition
            )
            transposeDb.songs.add(newSong)
        } else {
            songFound.transposition = transposition
        }
        transposeDbSubject.onNext(transposeDb)
    }

    fun removeUsage(songId: String, custom: Boolean) {
        val songFound = transposeDb.songs
            .find {
                it.songId == songId
                        && it.custom == custom
            }
        if (songFound != null) {
            transposeDb.songs.remove(songFound)
            transposeDbSubject.onNext(transposeDb)
        }
    }

}