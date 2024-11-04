package mwongela.songbook.mock

import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.custom.share.ShareSongService
import mwongela.songbook.inject.SingletonInject
import org.mockito.Mockito

class ShareSongServiceMock : ShareSongService(
    songOpener = SingletonInject { SongOpenerMock() },
    activity = SingletonInject { Mockito.mock(AppCompatActivity::class.java) },
)