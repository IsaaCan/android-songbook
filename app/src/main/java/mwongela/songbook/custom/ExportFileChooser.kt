package mwongela.songbook.custom

import android.app.Activity
import android.content.Intent
import android.net.Uri
import mwongela.songbook.R
import mwongela.songbook.activity.ActivityResultDispatcher
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.errorcheck.safeExecute
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import java.io.OutputStream

class ExportFileChooser(
    activity: LazyInject<Activity> = appFactory.activity,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    activityResultDispatcher: LazyInject<ActivityResultDispatcher> = appFactory.activityResultDispatcher,
) {
    private val activity by LazyExtractor(activity)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val activityResultDispatcher by LazyExtractor(activityResultDispatcher)

    private var contentToBeSaved: String = ""
    private var onSuccess: (Uri) -> Unit = {}

    fun showFileChooser(contentToBeSaved: String, filename: String, onSuccess: (Uri) -> Unit) {
        this.contentToBeSaved = contentToBeSaved
        this.onSuccess = onSuccess
        safeExecute {
            try {
                val title = uiResourceService.resString(R.string.select_file_to_export)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, filename)
                }
                activityResultDispatcher.startActivityForResult(
                    Intent.createChooser(
                        intent,
                        title,
                    )
                ) { resultCode: Int, data: Intent? ->
                    if (resultCode == Activity.RESULT_OK) {
                        onFileSelect(data?.data)
                    }
                }

            } catch (ex: android.content.ActivityNotFoundException) {
                uiInfoService.showToast(R.string.file_manager_not_found)
            }
        }
    }

    private fun onFileSelect(selectedUri: Uri?) {
        safeExecute {
            if (selectedUri != null) {
                activity.contentResolver.openOutputStream(selectedUri)
                    ?.use { outputStream: OutputStream ->
                        outputStream.write(contentToBeSaved.toByteArray())
                        onSuccess.invoke(selectedUri)
                    }
            }
        }
    }

}
