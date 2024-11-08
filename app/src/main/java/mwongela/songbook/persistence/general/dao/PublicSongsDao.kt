package mwongela.songbook.persistence.general.dao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.openDatabase
import mwongela.songbook.persistence.general.mapper.CategoryMapper
import mwongela.songbook.persistence.general.mapper.SongCategoryMapper
import mwongela.songbook.persistence.general.mapper.SongMapper
import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.general.model.SongCategoryRelationship
import java.io.File


class PublicSongsDao(private val dbFile: File) : AbstractSqliteDao() {

    private val songMapper = SongMapper()
    private val categoryMapper = CategoryMapper()
    private val songCategoryMapper = SongCategoryMapper()

    private var songsDbHelper: SQLiteDatabase? = null

    private val supportedDbVersion = 1

    override fun getDatabase(): SQLiteDatabase {
        if (songsDbHelper == null)
            songsDbHelper = openDatabase(dbFile)
        return songsDbHelper!!
    }

    private fun openDatabase(songsDbFile: File): SQLiteDatabase {
        if (!songsDbFile.exists())
            throw NoSuchFileException(
                songsDbFile,
                null,
                "Database file does not exist: ${songsDbFile.absolutePath}"
            )
        val db = openDatabase(songsDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.disableWriteAheadLogging()
        return db
    }

    fun close() {
        songsDbHelper?.close()
        songsDbHelper = null
    }

    fun readAllCategories(): MutableList<Category> {
        return readEntities("SELECT * FROM songs_category", categoryMapper)
    }

    fun readAllSongs(): MutableList<Song> {
        return readEntities("SELECT * FROM songs_song", songMapper)
    }

    fun readAllSongCategories(): MutableList<SongCategoryRelationship> {
        return readEntities("SELECT * FROM songs_song_categories", songCategoryMapper)
    }

    fun readDbVersionNumber(): Long? {
        val mapper: (Cursor) -> Long =
            { cursor -> cursor.getLong(cursor.getColumnIndexOrThrow("value")) }
        return queryOneValue(
            mapper,
            null,
            "SELECT value FROM songs_info WHERE name = 'version_number'"
        )
    }

    fun verifyDbVersion(dbVersion: Long) {
        if (dbVersion < supportedDbVersion)
            throw RuntimeException("local db version $dbVersion is not supported anymore")
    }

}
