package mwongela.songbook.layout.dialog

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import mwongela.songbook.R
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.errorcheck.safeExecute
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.system.SoftKeyboardService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class InputDialogBuilder(
    activity: LazyInject<Activity> = appFactory.activity,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    softKeyboardService: LazyInject<SoftKeyboardService> = appFactory.softKeyboardService,
) {
    private val activity by LazyExtractor(activity)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val softKeyboardService by LazyExtractor(softKeyboardService)

    fun input(title: String, initialValue: String?, multiline: Boolean = false, action: (String) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val alertBuilder = AlertDialog.Builder(activity)
            alertBuilder.setTitle(title)

            val input = EditText(activity)
            when (multiline) {
                true -> {
                    input.isSingleLine = false
                    input.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                }
                false -> input.inputType = InputType.TYPE_CLASS_TEXT
            }
            if (initialValue != null)
                input.setText(initialValue)
            alertBuilder.setView(input)

            alertBuilder.setNegativeButton(uiResourceService.resString(R.string.action_cancel)) { _, _ -> }
            alertBuilder.setPositiveButton(uiResourceService.resString(R.string.action_info_ok)) { _, _ ->
                softKeyboardService.hideSoftKeyboard(input)
                Handler(Looper.getMainLooper()).post {
                    softKeyboardService.hideSoftKeyboard()
                }
                safeExecute {
                    action.invoke(input.text.toString())
                }
            }
            alertBuilder.setCancelable(true)
            if (!activity.isFinishing) {
                val dialog = alertBuilder.create()
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                dialog.show()
            }

            input.requestFocus()
            Handler(Looper.getMainLooper()).post {
                softKeyboardService.showSoftKeyboard(input)
            }
        }
    }

    fun input(titleResId: Int, initialValue: String?, action: (String) -> Unit) {
        val title = uiResourceService.resString(titleResId)
        input(title, initialValue, action=action)
    }

}