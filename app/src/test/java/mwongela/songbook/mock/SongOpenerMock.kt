package mwongela.songbook.mock

import mwongela.songbook.cast.SongCastService
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.inject.SingletonInject
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songpreview.SongPreviewLayoutController
import org.mockito.Mockito

class SongOpenerMock : SongOpener(
    layoutController = SingletonInject { Mockito.mock(LayoutController::class.java) },
    songPreviewLayoutController = SingletonInject { Mockito.mock(SongPreviewLayoutController::class.java) },
    songsRepository = SingletonInject { Mockito.mock(SongsRepository::class.java) },
    uiInfoService = SingletonInject { Mockito.mock(UiInfoService::class.java) },
    songCastService = SingletonInject { Mockito.mock(SongCastService::class.java) },
)