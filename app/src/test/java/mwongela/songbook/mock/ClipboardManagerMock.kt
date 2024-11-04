package mwongela.songbook.mock

import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.inject.SingletonInject
import mwongela.songbook.system.ClipboardManager
import org.mockito.Mockito


class ClipboardManagerMock : ClipboardManager(
        activity = SingletonInject { Mockito.mock(AppCompatActivity::class.java) },
)
