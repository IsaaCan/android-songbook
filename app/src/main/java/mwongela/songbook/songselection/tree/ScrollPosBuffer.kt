package mwongela.songbook.songselection.tree

import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.songselection.listview.ListScrollPosition

class ScrollPosBuffer {

    private val storedScrollPositions = hashMapOf<Category?, ListScrollPosition>()

    fun storeScrollPosition(category: Category?, y: ListScrollPosition?) {
        if (y != null)
            storedScrollPositions[category] = y
    }

    fun restoreScrollPosition(category: Category?): ListScrollPosition? {
        return storedScrollPositions[category]
    }

    fun hasScrollPositionStored(category: Category?): Boolean {
        return storedScrollPositions.containsKey(category)
    }
}
