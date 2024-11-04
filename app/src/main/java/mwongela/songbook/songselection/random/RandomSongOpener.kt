package mwongela.songbook.songselection.random

import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.playlist.Playlist
import mwongela.songbook.playlist.PlaylistService
import mwongela.songbook.settings.language.AppLanguageService
import mwongela.songbook.settings.preferences.PreferencesState
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songselection.favourite.FavouriteSongsService
import java.util.Random

class RandomSongOpener(
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    favouriteSongsService: LazyInject<FavouriteSongsService> = appFactory.favouriteSongsService,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
    appLanguageService: LazyInject<AppLanguageService> = appFactory.appLanguageService,
    playlistService: LazyInject<PlaylistService> = appFactory.playlistService,
) {
    private val songsRepository by LazyExtractor(songsRepository)
    private val favouriteSongsService by LazyExtractor(favouriteSongsService)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val songOpener by LazyExtractor(songOpener)
    private val preferencesState by LazyExtractor(preferencesState)
    private val appLanguageService by LazyExtractor(appLanguageService)
    private val playlistService by LazyExtractor(playlistService)

    fun openRandomSong() {
        val playlist: Playlist? = when {
            preferencesState.randomPlaylistSongs && playlistService.isPlaylistOpen() -> {
                playlistService.currentPlaylist!!
            }
            else -> null
        }

        val songsCollection = songsToShuffle(playlist)

        val randomSong = getRandomSong(songsCollection)
        if (randomSong == null) {
            uiInfoService.showInfo(R.string.no_songs_to_shuffle)
            return
        }

        songOpener.openSongPreview(randomSong, playlist = playlist)
    }

    private fun songsToShuffle(playlist: Playlist?): List<Song> {
        return when {
            playlist != null -> {
                playlistService.getSongsFromPlaylist(playlist)
            }
            preferencesState.randomFavouriteSongsOnly -> {
                favouriteSongsService.getFavouriteSongs().toList()
            }
            else -> {
                val acceptedLanguages = appLanguageService.selectedSongLanguages
                val acceptedLangCodes = acceptedLanguages.map { lang -> lang.langCode } + "" + null
                songsRepository.allSongsRepo.songs.get()
                    .filter { song -> song.language in acceptedLangCodes }
            }
        }
    }

    private fun getRandomSong(songsCollection: List<Song>): Song? {
        if (songsCollection.isEmpty())
            return null
        return songsCollection[Random().nextInt(songsCollection.size)]
    }
}
