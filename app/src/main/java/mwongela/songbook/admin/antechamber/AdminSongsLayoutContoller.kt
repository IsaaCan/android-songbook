package mwongela.songbook.admin.antechamber


import android.view.View
import android.widget.Button
import mwongela.songbook.R
import mwongela.songbook.custom.CustomSongService
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.InflatedLayout
import mwongela.songbook.layout.contextmenu.ContextMenuBuilder
import mwongela.songbook.layout.dialog.ConfirmDialogBuilder
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.songpreview.SongOpener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AdminSongsLayoutContoller(
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    customSongService: LazyInject<CustomSongService> = appFactory.customSongService,
    antechamberService: LazyInject<AntechamberService> = appFactory.antechamberService,
) : InflatedLayout(
    _layoutResourceId = R.layout.screen_admin_songs
) {
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val songOpener by LazyExtractor(songOpener)
    private val customSongService by LazyExtractor(customSongService)
    private val antechamberService by LazyExtractor(antechamberService)

    private var itemsListView: AntechamberSongListView? = null
    private var experimentalSongs: MutableList<Song> = mutableListOf()

    private var subscriptions = mutableListOf<Disposable>()
    var fetchRequestSubject: PublishSubject<Boolean> = PublishSubject.create()

    override fun showLayout(layout: View) {
        super.showLayout(layout)

        itemsListView = layout.findViewById(R.id.itemsList)
        itemsListView!!.init(
            activity,
            onClick = this::onSongClick,
            onLongClick = this::onSongLongClick,
            onMore = this::onMoreMenu
        )
        updateItemsList()

        layout.findViewById<Button>(R.id.updateButton).setOnClickListener {
            downloadSongs()
        }

        subscriptions.forEach { s -> s.dispose() }
        subscriptions.clear()
        subscriptions.add(fetchRequestSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                GlobalScope.launch(Dispatchers.Main) {
                    val result = antechamberService.downloadSongsAsync().await()
                    result.fold(onSuccess = { downloadedSongs ->
                        experimentalSongs = downloadedSongs.toMutableList()
                    }, onFailure = { e ->
                        UiErrorHandler().handleError(e, R.string.error_communication_breakdown)
                    })
                }
            }, UiErrorHandler::handleError)
        )
    }

    private fun updateItemsList() {
        itemsListView?.setItems(experimentalSongs)
    }

    private fun downloadSongs() {
        uiInfoService.showInfo(R.string.admin_downloading_antechamber, indefinite = true)
        GlobalScope.launch(Dispatchers.Main) {
            val result = antechamberService.downloadSongsAsync().await()
            result.fold(onSuccess = { downloadedSongs ->
                experimentalSongs = downloadedSongs.toMutableList()
                uiInfoService.showInfo(R.string.admin_downloaded_antechamber)
                updateItemsList()
            }, onFailure = { e ->
                UiErrorHandler.handleError(e, R.string.admin_communication_breakdown)
            })
        }
    }

    private fun onSongClick(song: Song) {
        songOpener.openSongPreview(song)
    }

    private fun onSongLongClick(song: Song) {
        onMoreMenu(song)
    }

    private fun onMoreMenu(song: Song) {
        ContextMenuBuilder().showContextMenu(generateMenuOptions(song))
    }

    private fun deleteAntechamberSongUI(song: Song) {
        val message1 =
            uiResourceService.resString(R.string.admin_antechamber_confirm_delete, song.toString())
        ConfirmDialogBuilder().confirmAction(message1) {
            uiInfoService.showInfo(R.string.admin_sending, indefinite = true)
            GlobalScope.launch(Dispatchers.Main) {
                val result = antechamberService.deleteAntechamberSongAsync(song).await()
                result.fold(onSuccess = {
                    experimentalSongs.remove(song)
                    uiInfoService.showInfo(R.string.admin_success)
                    updateItemsList()
                }, onFailure = { e ->
                    UiErrorHandler.handleError(e, R.string.admin_communication_breakdown)
                })
            }
        }
    }

    private fun generateMenuOptions(song: Song): List<ContextMenuBuilder.Action> {
        return listOf(
            ContextMenuBuilder.Action(R.string.admin_antechamber_edit_action) {
                customSongService.showEditSongScreen(song)
            },
            ContextMenuBuilder.Action(R.string.admin_antechamber_update_action) {
                antechamberService.updateAntechamberSongUI(song)
            },
            ContextMenuBuilder.Action(R.string.admin_antechamber_approve_action) {
                antechamberService.approveAntechamberSongUI(song)
            },
            ContextMenuBuilder.Action(R.string.admin_antechamber_delete_action) {
                deleteAntechamberSongUI(song)
            }
        )
    }
}
