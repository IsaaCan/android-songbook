package mwongela.songbook.persistence.general.mapper

import android.database.Cursor
import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.CategoryType

class CategoryMapper : AbstractMapper<Category>() {

    override fun map(cursor: Cursor): Category {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val typeId = cursor.getLong(cursor.getColumnIndexOrThrow("type_id"))
        val type = CategoryType.parseById(typeId)
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        return Category(id, type, name)
    }

}