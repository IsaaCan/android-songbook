package mwongela.songbook.mock

import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.SingletonInject
import org.mockito.Mockito


class UiInfoServiceMock : UiInfoService(
    activity = SingletonInject { Mockito.mock(AppCompatActivity::class.java) },
    uiResourceService = SingletonInject { Mockito.mock(UiResourceService::class.java) },
) {
    override fun showSnackbar(
        info: String,
        infoResId: Int,
        actionResId: Int,
        indefinite: Boolean,
        durationMillis: Int,
        action: (() -> Unit)?,
    ) {
        print(info)
    }

    override fun showToast(message: String) {
        print(message)
    }

    override fun resString(resourceId: Int, vararg args: Any?): String {
        return resourceId.toString() + args.joinToString()
    }
}
