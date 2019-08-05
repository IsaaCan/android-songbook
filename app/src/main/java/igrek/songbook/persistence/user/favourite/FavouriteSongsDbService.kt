package igrek.songbook.persistence.user.favourite

import igrek.songbook.persistence.user.AbstractUserDataService
import igrek.songbook.persistence.user.custom.CustomSongsDb

class FavouriteSongsDbService(path: String) : AbstractUserDataService<FavouriteSongsDb>(
        path,
        dbName = "favourites",
        schemaVersion = 2,
        clazz = FavouriteSongsDb::class.java,
        serializer = FavouriteSongsDb.serializer()
) {

    override fun empty(): FavouriteSongsDb {
        return FavouriteSongsDb(emptyList())
    }

}