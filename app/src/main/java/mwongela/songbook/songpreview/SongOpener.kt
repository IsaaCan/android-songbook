package mwongela.songbook.songpreview

import mwongela.songbook.R
import mwongela.songbook.cast.SongCastService
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.analytics.AnalyticsLogger
import mwongela.songbook.info.logger.LoggerFactory.logger
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.general.model.SongIdentifier
import mwongela.songbook.persistence.general.model.SongNamespace
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.history.OpenedSong
import mwongela.songbook.persistence.user.playlist.Playlist
import mwongela.songbook.playlist.PlaylistService
import mwongela.songbook.util.defaultScope
import kotlinx.coroutines.launch

open class SongOpener(
    layoutController: LazyInject<LayoutController> = appFactory.layoutController,
    songPreviewLayoutController: LazyInject<SongPreviewLayoutController> = appFactory.songPreviewLayoutController,
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    songCastService: LazyInject<SongCastService> = appFactory.songCastService,
    playlistService: LazyInject<PlaylistService> = appFactory.playlistService,
) {
    private val layoutController by LazyExtractor(layoutController)
    private val songPreviewLayoutController by LazyExtractor(songPreviewLayoutController)
    private val songsRepository by LazyExtractor(songsRepository)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val songCastService by LazyExtractor(songCastService)
    private val playlistService by LazyExtractor(playlistService)

    fun openSongPreview(
        song: Song,
        playlist: Playlist? = null,
        onInit: (() -> Unit)? = null,
    ) {
        logger.info("Opening song: $song")
        playlistService.currentPlaylist = playlist
        songPreviewLayoutController.currentSong = song
        onInit?.let {
            songPreviewLayoutController.addOnInitListener(onInit)
        }
        defaultScope.launch {
            songCastService.presentMyOpenedSong(song)
        }
        layoutController.showLayout(SongPreviewLayoutController::class)
        songsRepository.openHistoryDao.registerOpenedSong(song.id, song.namespace)
        AnalyticsLogger().logEventSongOpened(song)
    }

    private fun openSongIdentifier(songIdentifier: SongIdentifier): Boolean {
        songsRepository.allSongsRepo.songFinder.find(songIdentifier)?.let { song ->
            openSongPreview(song)
            return true
        }
        return false
    }

    fun openLastSong() {
        playlistService.currentPlaylist = null
        songPreviewLayoutController.currentSong?.let { currentSong ->
            defaultScope.launch {
                songCastService.presentMyOpenedSong(currentSong)
            }
            layoutController.showLayout(SongPreviewLayoutController::class)
            return
        }

        val openedSong: OpenedSong = songsRepository.openHistoryDao.historyDb.songs.firstOrNull()
            ?: run {
                uiInfoService.showInfo(R.string.no_last_song)
                return
            }

        val namespace = when {
            openedSong.custom -> SongNamespace.Custom
            else -> SongNamespace.Public
        }
        val songIdentifier = SongIdentifier(openedSong.songId, namespace)
        val opened = openSongIdentifier(songIdentifier)
        if (!opened) {
            uiInfoService.showInfo(R.string.no_last_song)
        }
    }

    fun hasLastSong(): Boolean {
        songsRepository.openHistoryDao.historyDb.songs.firstOrNull()
            ?.let { openedSong: OpenedSong ->
                val namespace = when {
                    openedSong.custom -> SongNamespace.Custom
                    else -> SongNamespace.Public
                }
                val songIdentifier = SongIdentifier(openedSong.songId, namespace)
                val song = songsRepository.allSongsRepo.songFinder.find(songIdentifier)
                if (song != null)
                    return true
            }
        return false
    }
}