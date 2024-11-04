package mwongela.songbook.persistence.user.custom

import mwongela.songbook.persistence.general.model.Song

data class CustomCategory(
    val name: String,
    val songs: MutableList<Song> = mutableListOf()
)