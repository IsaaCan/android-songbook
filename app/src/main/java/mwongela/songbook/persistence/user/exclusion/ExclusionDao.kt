package mwongela.songbook.persistence.user.exclusion

import mwongela.songbook.persistence.user.AbstractJsonDao

class ExclusionDao(
    path: String,
    resetOnError: Boolean = false,
) : AbstractJsonDao<ExclusionDb>(
    path,
    dbName = "exclusion",
    schemaVersion = 2,
    clazz = ExclusionDb::class.java,
    serializer = ExclusionDb.serializer()
) {
    val exclusionDb: ExclusionDb get() = db!!

    init {
        read(resetOnError)
    }

    override fun empty(): ExclusionDb {
        return ExclusionDb()
    }

    fun setExcludedLanguages(languages: MutableList<String>) {
        exclusionDb.languages = languages
    }

}