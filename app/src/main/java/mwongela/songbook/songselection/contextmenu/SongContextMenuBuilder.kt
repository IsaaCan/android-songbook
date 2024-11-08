package mwongela.songbook.songselection.contextmenu

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import mwongela.songbook.R
import mwongela.songbook.admin.AdminService
import mwongela.songbook.admin.antechamber.AntechamberService
import mwongela.songbook.cast.SongCastMenuLayout
import mwongela.songbook.custom.CustomSongService
import mwongela.songbook.custom.share.ShareSongService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.errorcheck.safeExecute
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.layout.dialog.ConfirmDialogBuilder
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.playlist.PlaylistService
import mwongela.songbook.send.PublishSongService
import mwongela.songbook.songpreview.SongDetailsService
import mwongela.songbook.songpreview.SongPreviewLayoutController
import mwongela.songbook.songselection.favourite.FavouriteSongsService
import mwongela.songbook.util.lookup.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SongContextMenuBuilder(
    activity: LazyInject<Activity> = appFactory.activity,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    favouriteSongsService: LazyInject<FavouriteSongsService> = appFactory.favouriteSongsService,
    customSongService: LazyInject<CustomSongService> = appFactory.customSongService,
    playlistService: LazyInject<PlaylistService> = appFactory.playlistService,
    layoutController: LazyInject<LayoutController> = appFactory.layoutController,
    songPreviewLayoutController: LazyInject<SongPreviewLayoutController> = appFactory.songPreviewLayoutController,
    songDetailsService: LazyInject<SongDetailsService> = appFactory.songDetailsService,
    publishSongService: LazyInject<PublishSongService> = appFactory.publishSongService,
    adminService: LazyInject<AdminService> = appFactory.adminService,
    antechamberService: LazyInject<AntechamberService> = appFactory.antechamberService,
    shareSongService: LazyInject<ShareSongService> = appFactory.shareSongService,
) {
    private val activity by LazyExtractor(activity)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val favouriteSongsService by LazyExtractor(favouriteSongsService)
    private val customSongService by LazyExtractor(customSongService)
    private val playlistService by LazyExtractor(playlistService)
    private val layoutController by LazyExtractor(layoutController)
    private val songPreviewLayoutController by LazyExtractor(songPreviewLayoutController)
    private val songDetailsService by LazyExtractor(songDetailsService)
    private val publishSongService by LazyExtractor(publishSongService)
    private val adminService by LazyExtractor(adminService)
    private val antechamberService by LazyExtractor(antechamberService)
    private val shareSongService by LazyExtractor(shareSongService)
    private val songCastService by LazyExtractor(appFactory.songCastService)

    private var allActions: SimpleCache<List<SongContextAction>> =
        SimpleCache { createAllActions() }

    private fun createAllActions(): List<SongContextAction> {
        val actions = mutableListOf(
            SongContextAction(R.string.action_song_edit,
                availableCondition = { song -> song.isCustom() },
                executor = { song ->
                    customSongService.showEditSongScreen(song)
                }),
            SongContextAction(R.string.action_remove_from_this_playlist,
                availableCondition = { song ->
                    playlistService.isSongOnCurrentPlaylist(song)
                },
                executor = { song ->
                    playlistService.removeFromThisPlaylist(song)
                }),
            SongContextAction(R.string.action_add_to_playlist,
                availableCondition = { true },
                executor = { song ->
                    playlistService.showAddSongToPlaylistDialog(song)
                }),
            SongContextAction(R.string.action_song_set_favourite,
                availableCondition = { song ->
                    !favouriteSongsService.isSongFavourite(song) && !isSongPreviewVisible()
                },
                executor = { song ->
                    favouriteSongsService.setSongFavourite(song)
                }),
            SongContextAction(R.string.action_song_unset_favourite,
                availableCondition = { song ->
                    favouriteSongsService.isSongFavourite(song) && !isSongPreviewVisible()
                },
                executor = { song ->
                    favouriteSongsService.unsetSongFavourite(song)
                }),
            SongContextAction(R.string.action_song_remove,
                availableCondition = { song -> song.isCustom() },
                executor = { song ->
                    ConfirmDialogBuilder().confirmAction(R.string.confirm_remove_song) {
                        customSongService.removeSong(song)
                    }
                }),
            SongContextAction(R.string.action_song_copy,
                availableCondition = { true },
                executor = { song ->
                    customSongService.copySongAsCustom(song)
                }),
            SongContextAction(R.string.action_share_song,
                availableCondition = { true },
                executor = { song ->
                    shareSongService.shareSong(song)
                }),
            SongContextAction(R.string.action_song_publish,
                availableCondition = { song -> song.isCustom() },
                executor = { song ->
                    publishSongService.publishSong(song)
                }),
            SongContextAction(R.string.export_content_to_file,
                availableCondition = { song -> song.isCustom() },
                executor = { song ->
                    customSongService.exportSong(song)
                }),
            SongContextAction(R.string.song_details_title,
                availableCondition = { !isSongPreviewVisible() },
                executor = { song ->
                    songDetailsService.showSongDetails(song)
                }),
            SongContextAction(R.string.song_show_fullscreen,
                availableCondition = { isSongPreviewVisible() },
                executor = {
                    songPreviewLayoutController.toggleFullscreen()
                }),
            SongContextAction(R.string.songcast_share_with_songcast,
                availableCondition = {
                    isSongPreviewVisible() && !songCastService.isInRoom()
                },
                executor = {
                    layoutController.showLayout(SongCastMenuLayout::class)
                }),
            SongContextAction(R.string.admin_antechamber_edit_action,
                availableCondition = { adminService.isAdminEnabled() },
                executor = { song ->
                    customSongService.showEditSongScreen(song)
                }),
            SongContextAction(R.string.admin_song_content_update_action,
                availableCondition = { song -> song.isPublic() && adminService.isAdminEnabled() },
                executor = { song ->
                    adminService.updatePublicSongUi(song)
                }),
            SongContextAction(R.string.admin_antechamber_update_action,
                availableCondition = { song -> song.isAntechamber() && adminService.isAdminEnabled() },
                executor = { song ->
                    antechamberService.updateAntechamberSongUI(song)
                }),
            SongContextAction(R.string.admin_antechamber_approve_action,
                availableCondition = { song -> song.isAntechamber() && adminService.isAdminEnabled() },
                executor = { song ->
                    antechamberService.approveAntechamberSongUI(song)
                }),
            SongContextAction(R.string.admin_antechamber_approve_action,
                availableCondition = { song -> song.isCustom() && adminService.isAdminEnabled() },
                executor = { song ->
                    antechamberService.approveCustomSongUI(song)
                }),
            SongContextAction(R.string.admin_antechamber_delete_action,
                availableCondition = { song -> song.isAntechamber() && adminService.isAdminEnabled() },
                executor = { song ->
                    antechamberService.deleteAntechamberSongUI(song)
                }),
            SongContextAction(R.string.admin_update_rank,
                availableCondition = { song -> song.isPublic() && adminService.isAdminEnabled() },
                executor = { song ->
                    adminService.updateRankDialog(song)
                }),
        )

        actions.forEach { action ->
            action.displayName = uiResourceService.resString(action.displayNameResId)
        }
        return actions
    }

    private fun isSongPreviewVisible(): Boolean {
        return layoutController.isState(SongPreviewLayoutController::class)
    }

    fun showSongActions(song: Song) {
        GlobalScope.launch(Dispatchers.Main) {
            val songActions = allActions.get()
                .filter { action -> action.availableCondition(song) }
            val actionNames = songActions.map { action -> action.displayName }.toTypedArray()

            val builder = AlertDialog.Builder(activity)
            builder.setItems(actionNames) { _, item ->
                safeExecute {
                    songActions[item].executor(song)
                }
            }

            val alert = builder.create()
            if (!activity.isFinishing) {
                alert.show()
            }
        }
    }

}