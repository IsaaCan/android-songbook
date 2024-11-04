package mwongela.songbook.send

import android.view.View
import android.widget.Button
import android.widget.EditText
import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.analytics.AnalyticsLogger
import mwongela.songbook.info.errorcheck.SafeClickListener
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.layout.MainLayout
import mwongela.songbook.layout.dialog.ConfirmDialogBuilder
import mwongela.songbook.system.SoftKeyboardService

class MissingSongLayoutController(
    layoutController: LazyInject<LayoutController> = appFactory.layoutController,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    sendMessageService: LazyInject<SendMessageService> = appFactory.sendMessageService,
    softKeyboardService: LazyInject<SoftKeyboardService> = appFactory.softKeyboardService,
) : MainLayout {
    private val layoutController by LazyExtractor(layoutController)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val sendMessageService by LazyExtractor(sendMessageService)
    private val softKeyboardService by LazyExtractor(softKeyboardService)

    private var missingSongMessageEdit: EditText? = null

    override fun showLayout(layout: View) {
        missingSongMessageEdit = layout.findViewById(R.id.missingSongMessageEdit)

        layout.findViewById<Button>(R.id.contactSendButton)?.setOnClickListener(SafeClickListener {
            sendMessage()
        })
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.screen_contact_missing_song
    }

    override fun onBackClicked() {
        layoutController.showPreviousLayoutOrQuit()
    }

    override fun onLayoutExit() {
        softKeyboardService.hideSoftKeyboard()
    }

    private fun sendMessage() {
        val message = missingSongMessageEdit?.text?.toString()

        if (message.isNullOrBlank()) {
            uiInfoService.showToast(uiResourceService.resString(R.string.fill_in_all_fields))
            return
        }

        if (!message.matches(Regex(".+-.+"))) {
            uiInfoService.showToast(uiResourceService.resString(R.string.missing_song_title_invalid_format))
            return
        }

        val subject = uiResourceService.resString(R.string.contact_subject_missing_song)

        ConfirmDialogBuilder().confirmAction(R.string.confirm_send_contact) {
            sendMessageService.sendContactMessage(
                message, origin = MessageOrigin.MISSING_SONG,
                subject = subject
            )
            AnalyticsLogger().logEventMissingSongRequested(message)
        }
    }

}
