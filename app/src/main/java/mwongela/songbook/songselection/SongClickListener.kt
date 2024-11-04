package mwongela.songbook.songselection

import mwongela.songbook.songselection.tree.SongTreeItem

interface SongClickListener {

    fun onSongItemClick(item: SongTreeItem)

    fun onSongItemLongClick(item: SongTreeItem)

}
