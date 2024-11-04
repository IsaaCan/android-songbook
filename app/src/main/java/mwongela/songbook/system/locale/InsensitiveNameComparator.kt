package mwongela.songbook.system.locale

import java.text.Collator
import java.util.Locale

class InsensitiveNameComparator<T>(
    private val locale: Locale,
    private val nameAccessor: (obj: T) -> String
) : Comparator<T> {

    private val stringCollator = Collator.getInstance(locale)

    override fun compare(lhs: T?, rhs: T?): Int {
        val lName = accessName(lhs)?.lowercase(locale)
        val rName = accessName(rhs)?.lowercase(locale)
        return stringCollator.compare(lName, rName)
    }

    private fun accessName(obj: T?): String? {
        if (obj == null)
            return null
        return nameAccessor.invoke(obj)
    }
}