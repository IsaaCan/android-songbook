package mwongela.songbook.util.lookup

class SimpleCache<T>(private val supplier: () -> T) {

    private var cachedValue: T? = null

    fun get(): T {
        if (cachedValue == null)
            cachedValue = supplier.invoke()
        return cachedValue!!
    }

    fun invalidate() {
        cachedValue = null
    }

    companion object {
        inline fun <reified U> emptyList(): SimpleCache<List<U>> {
            return SimpleCache(supplier = { kotlin.collections.emptyList() })
        }
    }
}
