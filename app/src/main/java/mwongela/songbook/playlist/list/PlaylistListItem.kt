package mwongela.songbook.playlist.list

import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.user.playlist.Playlist

open class PlaylistListItem(
    val playlist: Playlist? = null,
    val song: Song? = null,
) {

    override fun toString(): String {
        return when {
            playlist != null -> """[${playlist.name}]"""
            song != null -> song.displayName()
            else -> ""
        }
    }

}
