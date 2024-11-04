package mwongela.songbook.playlist

import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.contextmenu.ContextMenuBuilder
import mwongela.songbook.layout.dialog.InputDialogBuilder
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.general.model.SongIdentifier
import mwongela.songbook.persistence.general.model.SongNamespace
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.playlist.Playlist
import mwongela.songbook.persistence.user.playlist.PlaylistSong
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songpreview.SongPreviewLayoutController
import io.reactivex.subjects.PublishSubject

class PlaylistService(
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    contextMenuBuilder: LazyInject<ContextMenuBuilder> = appFactory.contextMenuBuilder,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    songPreviewLayoutController: LazyInject<SongPreviewLayoutController> = appFactory.songPreviewLayoutController,
) {
    private val songsRepository by LazyExtractor(songsRepository)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val contextMenuBuilder by LazyExtractor(contextMenuBuilder)
    private val songOpener by LazyExtractor(songOpener)
    private val songPreviewLayoutController by LazyExtractor(songPreviewLayoutController)

    var currentPlaylist: Playlist? = null
    var addPlaylistSongSubject: PublishSubject<Song> = PublishSubject.create()

    fun addNewPlaylist(onSuccess: (Playlist) -> Unit = {}) {
        InputDialogBuilder().input(R.string.new_playlist_name, null) { name ->
            if (name.isNotBlank()) {
                val playlist = Playlist(0, name)
                songsRepository.playlistDao.savePlaylist(playlist)
                onSuccess(playlist)
            }
        }
    }

    fun showAddSongToPlaylistDialog(song: Song) {
        val playlists = songsRepository.playlistDao.playlistDb.playlists
        if (playlists.isEmpty()) {
            uiInfoService.showToast(R.string.no_playlists_to_add)
            addNewPlaylist { playlist ->
                addSongToPlaylist(playlist, song)
            }
            return
        }

        val actions = mutableListOf<ContextMenuBuilder.Action>()

        playlists.forEach { playlist ->
            val name = playlist.name
            val action = ContextMenuBuilder.Action(name) {
                addSongToPlaylist(playlist, song)
            }
            actions.add(action)
        }

        contextMenuBuilder.showContextMenu(R.string.choose_playlist, actions)
    }

    private fun addSongToPlaylist(playlist: Playlist, song: Song) {
        if (songsRepository.playlistDao.isSongOnPlaylist(song, playlist)) {
            uiInfoService.showInfo(
                R.string.song_already_on_playlist,
                song.displayName(),
                playlist.name
            )
            return
        }
        songsRepository.playlistDao.addSongToPlaylist(song, playlist)
        uiInfoService.showInfo(R.string.song_added_to_playlist, song.displayName(), playlist.name)
    }

    fun addSongToCurrentPlaylist(song: Song) {
        currentPlaylist?.let { currentPlaylist ->
            addSongToPlaylist(currentPlaylist, song)
            addPlaylistSongSubject.onNext(song)
        } ?: kotlin.run {
            uiInfoService.showInfo(R.string.playlist_not_selected)
        }
    }

    fun removeFromThisPlaylist(song: Song) {
        currentPlaylist?.let { currentPlaylist ->
            removeFromPlaylist(song, currentPlaylist)
        } ?: kotlin.run {
            uiInfoService.showInfo(R.string.song_is_not_on_playlist)
        }
    }

    fun removeFromPlaylist(song: Song) {
        val playlistsWithSong = songsRepository.playlistDao.playlistDb.playlists
            .filter { playlist ->
                songsRepository.playlistDao.isSongOnPlaylist(song, playlist)
            }

        when (playlistsWithSong.size) {
            0 -> {
                uiInfoService.showInfo(R.string.song_is_not_on_playlist)
                return
            }
            1 -> removeFromPlaylist(song, playlistsWithSong.first())
            else -> showRemoveSongFromPlaylistDialog(song, playlistsWithSong.toMutableList())
        }
    }

    private fun showRemoveSongFromPlaylistDialog(song: Song, playlists: MutableList<Playlist>) {
        val actions = mutableListOf<ContextMenuBuilder.Action>()

        playlists.forEach { playlist ->
            val name = playlist.name
            val action = ContextMenuBuilder.Action(name) {
                removeFromPlaylist(song, playlist)
            }
            actions.add(action)
        }

        contextMenuBuilder.showContextMenu(R.string.choose_playlist, actions)
    }

    private fun removeFromPlaylist(song: Song, playlist: Playlist) {
        songsRepository.playlistDao.removeSongFromPlaylist(song, playlist)
        uiInfoService.showInfo(
            R.string.song_removed_from_playlist,
            song.displayName(),
            playlist.name
        )
    }

    fun goToNextOrPrevious(next: Int): Boolean {
        val currentSong = songPreviewLayoutController.currentSong ?: return false
        val playlist = currentPlaylist ?: return false
        val songIndex = findSongInPlaylist(currentSong, playlist)
        if (songIndex == -1)
            return false
        val nextIndex = songIndex + next
        if (nextIndex < 0) {
            uiInfoService.showToast(R.string.playlist_at_beginning)
            return false
        }
        if (nextIndex >= playlist.songs.size) {
            uiInfoService.showToast(R.string.playlist_at_end)
            return false
        }
        val nextPlaylistSong = playlist.songs[nextIndex]
        val namespace = when {
            nextPlaylistSong.custom -> SongNamespace.Custom
            else -> SongNamespace.Public
        }
        val songId = SongIdentifier(nextPlaylistSong.songId, namespace)
        val nextSong = songsRepository.allSongsRepo.songFinder.find(songId) ?: return false
        songOpener.openSongPreview(nextSong, playlist = playlist)
        return true
    }

    fun hasNextSong(): Boolean {
        val currentSong = songPreviewLayoutController.currentSong ?: return false
        val playlist = currentPlaylist ?: return false
        val songIndex = findSongInPlaylist(currentSong, playlist)
        if (songIndex == -1)
            return false
        return songIndex + 1 < playlist.songs.size
    }

    private fun findSongInPlaylist(song: Song, playlist: Playlist): Int {
        return playlist.songs.indexOfFirst { s -> s.songId == song.id && s.custom == song.isCustom() }
    }

    fun getSongsFromPlaylist(playlist: Playlist): MutableList<Song> {
        return playlist.songs
            .mapNotNull { s ->
                val namespace = when {
                    s.custom -> SongNamespace.Custom
                    else -> SongNamespace.Public
                }
                val id = SongIdentifier(s.songId, namespace)
                val song = songsRepository.allSongsRepo.songFinder.find(id)
                song
            }
            .toMutableList()
    }

    fun isPlaylistOpen(): Boolean {
        return currentPlaylist != null
    }

    fun isSongOnCurrentPlaylist(song: Song): Boolean {
        return currentPlaylist?.let { currentPlaylist ->
            isSongOnPlaylist(song, currentPlaylist)
        } ?: false
    }

    private fun isSongOnPlaylist(song: Song, playlist: Playlist): Boolean {
        val playlistSong = PlaylistSong(song.id, song.isCustom())
        return playlistSong in playlist.songs
    }

}