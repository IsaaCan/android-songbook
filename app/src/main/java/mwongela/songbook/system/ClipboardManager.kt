package mwongela.songbook.system

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory


open class ClipboardManager(
    activity: LazyInject<Activity> = appFactory.activity,
) {
    private val activity by LazyExtractor(activity)

    fun copyToSystemClipboard(text: String) {
        val clipboard = activity.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getFromSystemClipboard(): String? {
        val clipboard = activity.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(
                MIMETYPE_TEXT_PLAIN
            ) == true
        ) {
            val item = clipboard.primaryClip?.getItemAt(0) ?: return null
            return item.text?.toString()
        }
        return null
    }
}
