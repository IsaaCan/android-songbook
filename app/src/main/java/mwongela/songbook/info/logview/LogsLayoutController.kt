package mwongela.songbook.info.logview

import android.annotation.SuppressLint
import android.view.View
import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.logger.LogEntry
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.InflatedLayout
import mwongela.songbook.system.ClipboardManager

@SuppressLint("CheckResult")
class LogsLayoutController(
    clipboardManager: LazyInject<ClipboardManager> = appFactory.clipboardManager,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
) : InflatedLayout(
    _layoutResourceId = R.layout.screen_logs
) {
    private val clipboardManager by LazyExtractor(clipboardManager)
    private val uiInfoService by LazyExtractor(uiInfoService)

    private var itemsListView: LogListView? = null

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        itemsListView = layout.findViewById<LogListView>(R.id.itemsListView)?.also {
            it.init()
            it.enableNestedScrolling()
            it.onClickCallback = { item: LogEntry ->
                copyItemToClipboard(item)
            }
            it.items = listOf()
        }

        itemsListView?.let {
            populateItems(it)
        }
    }

    private fun populateItems(listView: LogListView) {
        listView.items = LoggerFactory.sessionLogs.reversed()
    }

    private fun copyItemToClipboard(item: LogEntry) {
        clipboardManager.copyToSystemClipboard(item.message)
        uiInfoService.showInfo(R.string.copied_to_clipboard)
    }
}
