package mwongela.songbook.inject

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.about.AboutLayoutController
import mwongela.songbook.about.WebviewLayoutController
import mwongela.songbook.activity.ActivityController
import mwongela.songbook.activity.ActivityResultDispatcher
import mwongela.songbook.activity.AppInitializer
import mwongela.songbook.activity.MainActivityData
import mwongela.songbook.activity.OptionSelectDispatcher
import mwongela.songbook.admin.AdminCategoryManager
import mwongela.songbook.admin.AdminService
import mwongela.songbook.admin.SongRankService
import mwongela.songbook.admin.antechamber.AdminSongsLayoutContoller
import mwongela.songbook.admin.antechamber.AntechamberService
import mwongela.songbook.billing.BillingLayoutController
import mwongela.songbook.billing.BillingService
import mwongela.songbook.cast.SongCastLobbyLayout
import mwongela.songbook.cast.SongCastMenuLayout
import mwongela.songbook.cast.SongCastService
import mwongela.songbook.chords.diagram.ChordDiagramsService
import mwongela.songbook.chords.loader.LyricsLoader
import mwongela.songbook.custom.CustomSongService
import mwongela.songbook.custom.CustomSongsListLayoutController
import mwongela.songbook.custom.EditSongLayoutController
import mwongela.songbook.custom.ExportFileChooser
import mwongela.songbook.custom.ImportFileChooser
import mwongela.songbook.custom.SongImportFileChooser
import mwongela.songbook.custom.share.ShareSongService
import mwongela.songbook.custom.sync.EditorSessionService
import mwongela.songbook.editor.ChordsEditorLayoutController
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.analytics.CrashlyticsLogger
import mwongela.songbook.info.logger.Logger
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.info.logview.LogsLayoutController
import mwongela.songbook.layout.GlobalFocusTraverser
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.layout.ad.AdService
import mwongela.songbook.layout.contextmenu.ContextMenuBuilder
import mwongela.songbook.layout.navigation.NavigationMenuController
import mwongela.songbook.persistence.DeviceIdProvider
import mwongela.songbook.persistence.LocalFilesystem
import mwongela.songbook.persistence.general.SongsUpdater
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.UserDataDao
import mwongela.songbook.persistence.user.custom.CustomSongsBackuper
import mwongela.songbook.playlist.PlaylistFillLayoutController
import mwongela.songbook.playlist.PlaylistLayoutController
import mwongela.songbook.playlist.PlaylistService
import mwongela.songbook.room.BluetoothService
import mwongela.songbook.room.RoomListLayoutController
import mwongela.songbook.room.RoomLobby
import mwongela.songbook.room.RoomLobbyLayoutController
import mwongela.songbook.secret.CommanderService
import mwongela.songbook.secret.CommanderUtils
import mwongela.songbook.send.ContactLayoutController
import mwongela.songbook.send.MissingSongLayoutController
import mwongela.songbook.send.PublishSongLayoutController
import mwongela.songbook.send.PublishSongService
import mwongela.songbook.send.SendMessageService
import mwongela.songbook.settings.SettingsLayoutController
import mwongela.songbook.settings.buttons.MediaButtonService
import mwongela.songbook.settings.chordsnotation.ChordsNotationService
import mwongela.songbook.settings.enums.SettingsEnumService
import mwongela.songbook.settings.homescreen.HomeScreenEnumService
import mwongela.songbook.settings.language.AppLanguageService
import mwongela.songbook.settings.preferences.PreferencesService
import mwongela.songbook.settings.preferences.PreferencesState
import mwongela.songbook.settings.preferences.SharedPreferencesService
import mwongela.songbook.settings.sync.BackupSyncManager
import mwongela.songbook.settings.theme.LyricsThemeService
import mwongela.songbook.songpreview.SongDetailsService
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.songpreview.SongPreviewLayoutController
import mwongela.songbook.songpreview.quickmenu.QuickMenuAutoscroll
import mwongela.songbook.songpreview.quickmenu.QuickMenuCast
import mwongela.songbook.songpreview.quickmenu.QuickMenuTranspose
import mwongela.songbook.songpreview.scroll.AutoscrollService
import mwongela.songbook.songpreview.scroll.ScrollService
import mwongela.songbook.songselection.contextmenu.SongContextMenuBuilder
import mwongela.songbook.songselection.favourite.FavouriteSongsService
import mwongela.songbook.songselection.favourite.FavouritesLayoutController
import mwongela.songbook.songselection.history.OpenHistoryLayoutController
import mwongela.songbook.songselection.latest.LatestSongsLayoutController
import mwongela.songbook.songselection.random.RandomSongOpener
import mwongela.songbook.songselection.search.SongSearchLayoutController
import mwongela.songbook.songselection.top.TopSongsLayoutController
import mwongela.songbook.songselection.tree.ScrollPosBuffer
import mwongela.songbook.songselection.tree.SongTreeLayoutController
import mwongela.songbook.system.ClipboardManager
import mwongela.songbook.system.PackageInfoService
import mwongela.songbook.system.PermissionService
import mwongela.songbook.system.SoftKeyboardService
import mwongela.songbook.system.SystemKeyDispatcher
import mwongela.songbook.system.WindowManagerService
import okhttp3.OkHttpClient


class AppFactory(
    private var _activity: AppCompatActivity?,
) {

    val activity: LazyInject<Activity> = SingletonInject { _activity!! }
    val appCompatActivity: LazyInject<AppCompatActivity> = SingletonInject { _activity!! }

    val context: LazyInject<Context> = SingletonInject { _activity!!.applicationContext }
    val logger: LazyInject<Logger> = PrototypeInject { LoggerFactory.logger }
    val sharedPreferences: LazyInject<SharedPreferences> =
        PrototypeInject { SharedPreferencesService.sharedPreferencesCreator(_activity!!) }

    /* Services */
    val activityData = SingletonInject { MainActivityData() }
    val activityController = SingletonInject { ActivityController() }
    val appInitializer = SingletonInject { AppInitializer() }
    val optionSelectDispatcher = SingletonInject { OptionSelectDispatcher() }
    val systemKeyDispatcher = SingletonInject { SystemKeyDispatcher() }
    val windowManagerService = SingletonInject { WindowManagerService() }
    val uiResourceService = SingletonInject { UiResourceService() }
    val uiInfoService = SingletonInject { UiInfoService() }
    val autoscrollService = SingletonInject { AutoscrollService() }
    val lyricsLoader = SingletonInject { LyricsLoader() }
    val preferencesService = SingletonInject { PreferencesService() }
    val scrollPosBuffer = SingletonInject { ScrollPosBuffer() }
    val layoutController = SingletonInject { LayoutController() }
    val softKeyboardService = SingletonInject { SoftKeyboardService() }
    val songTreeLayoutController = SingletonInject { SongTreeLayoutController() }
    val songPreviewLayoutController = SingletonInject { SongPreviewLayoutController() }
    val quickMenuTranspose = SingletonInject { QuickMenuTranspose() }
    val quickMenuAutoscroll = SingletonInject { QuickMenuAutoscroll() }
    val navigationMenuController = SingletonInject { NavigationMenuController() }
    val songSearchLayoutController = SingletonInject { SongSearchLayoutController() }
    val aboutLayoutController = SingletonInject { AboutLayoutController() }
    val contactLayoutController = SingletonInject { ContactLayoutController() }
    val localFilesystem = SingletonInject { LocalFilesystem() }
    val songsRepository = SingletonInject { SongsRepository() }
    val permissionService = SingletonInject { PermissionService() }
    val commanderService = SingletonInject { CommanderService() }
    val commanderUtils = SingletonInject { CommanderUtils() }
    val packageInfoService = SingletonInject { PackageInfoService() }
    val settingsLayoutController = SingletonInject { SettingsLayoutController() }
    val songDetailsService = SingletonInject { SongDetailsService() }
    val sendMessageService = SingletonInject { SendMessageService() }
    val songImportFileChooser = SingletonInject { SongImportFileChooser() }
    val exportFileChooser = SingletonInject { ExportFileChooser() }
    val okHttpClient = SingletonInject { OkHttpClient() }
    val customSongService = SingletonInject { CustomSongService() }
    val editSongLayoutController = SingletonInject { EditSongLayoutController() }
    val songsUpdater = SingletonInject { SongsUpdater() }
    val randomSongOpener = SingletonInject { RandomSongOpener() }
    val appLanguageService = SingletonInject { AppLanguageService() }
    val favouriteSongsService = SingletonInject { FavouriteSongsService() }
    val favouritesLayoutController = SingletonInject { FavouritesLayoutController() }
    val chordsNotationService = SingletonInject { ChordsNotationService() }
    val preferencesState = SingletonInject { PreferencesState() }
    val lyricsThemeService = SingletonInject { LyricsThemeService() }
    val customSongsListLayoutController = SingletonInject { CustomSongsListLayoutController() }
    val songContextMenuBuilder = SingletonInject { SongContextMenuBuilder() }
    val chordsEditorLayoutController = SingletonInject { ChordsEditorLayoutController() }
    val contextMenuBuilder = SingletonInject { ContextMenuBuilder() }
    val userDataDao = SingletonInject { UserDataDao() }
    val playlistLayoutController = SingletonInject { PlaylistLayoutController() }
    val playlistFillLayoutController = SingletonInject { PlaylistFillLayoutController() }
    val playlistService = SingletonInject { PlaylistService() }
    val latestSongsLayoutController = SingletonInject { LatestSongsLayoutController() }
    val topSongsLayoutController = SingletonInject { TopSongsLayoutController() }
    val songOpener = SingletonInject { SongOpener() }
    val openHistoryLayoutController = SingletonInject { OpenHistoryLayoutController() }
    val chordDiagramsService = SingletonInject { ChordDiagramsService() }
    val publishSongLayoutController = SingletonInject { PublishSongLayoutController() }
    val missingSongLayoutController = SingletonInject { MissingSongLayoutController() }
    val publishSongService = SingletonInject { PublishSongService() }
    val adminService = SingletonInject { AdminService() }
    val adminSongsLayoutContoller = SingletonInject { AdminSongsLayoutContoller() }
    val antechamberService = SingletonInject { AntechamberService() }
    val backupSyncManager = SingletonInject { BackupSyncManager() }
    val adService = SingletonInject { AdService() }
    val songRankService = SingletonInject { SongRankService() }
    val clipboardManager = SingletonInject { ClipboardManager() }
    val adminCategoryManager = SingletonInject { AdminCategoryManager() }
    val roomListLayoutController = SingletonInject { RoomListLayoutController() }
    val roomLobbyLayoutController = SingletonInject { RoomLobbyLayoutController() }
    val bluetoothService = SingletonInject { BluetoothService() }
    val roomLobby = SingletonInject { RoomLobby() }
    val importFileChooser = SingletonInject { ImportFileChooser() }
    val activityResultDispatcher = SingletonInject { ActivityResultDispatcher() }
    val shareSongService = SingletonInject { ShareSongService() }
    val mediaButtonService = SingletonInject { MediaButtonService() }
    val billingService = SingletonInject { BillingService() }
    val billingLayoutController = SingletonInject { BillingLayoutController() }
    val homeScreenEnumService = SingletonInject { HomeScreenEnumService() }
    val settingsEnumService = SingletonInject { SettingsEnumService() }
    val globalFocusTraverser = SingletonInject { GlobalFocusTraverser() }
    val webviewLayoutController = SingletonInject { WebviewLayoutController() }
    val crashlyticsLogger = SingletonInject { CrashlyticsLogger() }
    val deviceIdProvider = SingletonInject { DeviceIdProvider() }
    val editorSessionService = SingletonInject { EditorSessionService() }
    val logsLayoutController = SingletonInject { LogsLayoutController() }
    val customSongsBackuper = SingletonInject { CustomSongsBackuper() }
    val songCastMenuLayout = SingletonInject { SongCastMenuLayout() }
    val songCastLobbyLayout = SingletonInject { SongCastLobbyLayout() }
    val songCastService = SingletonInject { SongCastService() }
    val quickMenuCast = SingletonInject { QuickMenuCast() }
    val scrollService = SingletonInject { ScrollService() }
}
