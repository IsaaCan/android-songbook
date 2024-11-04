package mwongela.songbook.songselection.favourite

import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.analytics.AnalyticsLogger
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.repository.SongsRepository
import io.reactivex.subjects.PublishSubject

class FavouriteSongsService(
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
) {
    private val songsRepository by LazyExtractor(songsRepository)
    private val uiInfoService by LazyExtractor(uiInfoService)

    var updateFavouriteSongSubject: PublishSubject<Song> = PublishSubject.create()

    fun isSongFavourite(song: Song): Boolean {
        return songsRepository.favouriteSongsDao.isSongFavourite(song.songIdentifier())
    }

    fun getFavouriteSongs(): Set<Song> {
        return songsRepository.favouriteSongsDao.getFavouriteSongs()
    }

    fun setSongFavourite(song: Song) {
        songsRepository.favouriteSongsDao.setSongFavourite(song)
        uiInfoService.showInfo(R.string.favourite_song_has_been_set)
        updateFavouriteSongSubject.onNext(song)
        AnalyticsLogger().logEventSongFavourited(song)
    }

    fun unsetSongFavourite(song: Song) {
        songsRepository.favouriteSongsDao.unsetSongFavourite(song)
        uiInfoService.showInfo(R.string.favourite_song_has_been_unset)
        updateFavouriteSongSubject.onNext(song)
    }

}