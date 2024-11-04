package mwongela.songbook.songselection.search

import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.songselection.tree.SongTreeItem

class SongSearchItem private constructor(song: Song) : SongTreeItem(song, null) {

    companion object {
        fun song(song: Song): SongSearchItem {
            return SongSearchItem(song)
        }
    }

}
