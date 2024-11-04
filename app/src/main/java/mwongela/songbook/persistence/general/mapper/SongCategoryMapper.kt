package mwongela.songbook.persistence.general.mapper

import android.database.Cursor
import mwongela.songbook.persistence.general.model.SongCategoryRelationship

class SongCategoryMapper : AbstractMapper<SongCategoryRelationship>() {

    override fun map(cursor: Cursor): SongCategoryRelationship {
        val songId = cursor.getLong(cursor.getColumnIndexOrThrow("song_id"))
        val categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("category_id"))
        return SongCategoryRelationship(songId.toString(), categoryId)
    }
}
