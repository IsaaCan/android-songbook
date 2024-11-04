package mwongela.songbook.persistence.user.favourite

import kotlinx.serialization.Serializable

@Serializable
data class FavouriteSongsDb(
    val favourites: MutableList<FavouriteSong> = mutableListOf()
)

@Serializable
data class FavouriteSong(
    val songId: String,
    val custom: Boolean,
)