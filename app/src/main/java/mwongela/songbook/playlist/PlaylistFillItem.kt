package mwongela.songbook.playlist

import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.songselection.tree.SongTreeItem

class PlaylistFillItem private constructor(song: Song) : SongTreeItem(song, null) {

    companion object {
        fun song(song: Song): PlaylistFillItem {
            return PlaylistFillItem(song)
        }
    }

}
