package mwongela.songbook.activity


import android.app.Activity
import android.content.Intent
import mwongela.songbook.BuildConfig
import mwongela.songbook.R
import mwongela.songbook.about.AboutLayoutController
import mwongela.songbook.about.WebviewLayoutController
import mwongela.songbook.admin.AdminService
import mwongela.songbook.custom.SongImportFileChooser
import mwongela.songbook.custom.share.ShareSongService
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.layout.MainLayout
import mwongela.songbook.layout.ad.AdService
import mwongela.songbook.persistence.general.SongsUpdater
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.persistence.user.UserDataDao
import mwongela.songbook.settings.chordsnotation.ChordsNotationService
import mwongela.songbook.settings.homescreen.HomeScreenEnum
import mwongela.songbook.settings.language.AppLanguageService
import mwongela.songbook.settings.preferences.PreferencesState
import mwongela.songbook.settings.sync.BackupSyncManager
import mwongela.songbook.songpreview.SongOpener
import mwongela.songbook.system.LinkOpener
import mwongela.songbook.system.WindowManagerService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class AppInitializer(
    windowManagerService: LazyInject<WindowManagerService> = appFactory.windowManagerService,
    layoutController: LazyInject<LayoutController> = appFactory.layoutController,
    songsUpdater: LazyInject<SongsUpdater> = appFactory.songsUpdater,
    appLanguageService: LazyInject<AppLanguageService> = appFactory.appLanguageService,
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    adminService: LazyInject<AdminService> = appFactory.adminService,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
    chordsNotationService: LazyInject<ChordsNotationService> = appFactory.chordsNotationService,
    adService: LazyInject<AdService> = appFactory.adService,
    activityController: LazyInject<ActivityController> = appFactory.activityController,
    activity: LazyInject<Activity> = appFactory.activity,
    shareSongService: LazyInject<ShareSongService> = appFactory.shareSongService,
    songImportFileChooser: LazyInject<SongImportFileChooser> = appFactory.songImportFileChooser,
    songOpener: LazyInject<SongOpener> = appFactory.songOpener,
    aboutLayoutController: LazyInject<AboutLayoutController> = appFactory.aboutLayoutController,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    backupSyncManager: LazyInject<BackupSyncManager> = appFactory.backupSyncManager,
    userDataDao: LazyInject<UserDataDao> = appFactory.userDataDao,
    webviewLayoutController: LazyInject<WebviewLayoutController> = appFactory.webviewLayoutController,
) {
    private val windowManagerService by LazyExtractor(windowManagerService)
    private val layoutController by LazyExtractor(layoutController)
    private val songsUpdater by LazyExtractor(songsUpdater)
    private val appLanguageService by LazyExtractor(appLanguageService)
    private val songsRepository by LazyExtractor(songsRepository)
    private val adminService by LazyExtractor(adminService)
    private val preferencesState by LazyExtractor(preferencesState)
    private val chordsNotationService by LazyExtractor(chordsNotationService)
    private val adService by LazyExtractor(adService)
    private val activityController by LazyExtractor(activityController)
    private val activity by LazyExtractor(activity)
    private val shareSongService by LazyExtractor(shareSongService)
    private val songImportFileChooser by LazyExtractor(songImportFileChooser)
    private val songOpener by LazyExtractor(songOpener)
    private val aboutLayoutController by LazyExtractor(aboutLayoutController)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val backupSyncManager by LazyExtractor(backupSyncManager)
    private val userDataDao by LazyExtractor(userDataDao)
    private val webviewLayoutController by LazyExtractor(webviewLayoutController)

    private val logger = LoggerFactory.logger
    private val debugInitEnabled = false
    private var initJob: Job? = null
    private val initChannel: Channel<Boolean> = Channel()

    fun init() {
        initJob = null
        if (debugInitEnabled && BuildConfig.DEBUG) {
            debugInit()
        }

        logger.info("Initializing application...")

        syncInit()

        initJob = GlobalScope.launch {
            if (!userDataDao.loadOrExit())
                return@launch
            songsRepository.reloadSongsDb()

            appLanguageService.setLocale()
            layoutController.init()

            if (preferencesState.homeScreen == HomeScreenEnum.LAST_SONG && songOpener.hasLastSong()) {
                songOpener.openLastSong()
            } else {
                layoutController.showLayout(getStartingScreen()).join()
            }

            songsUpdater.checkUpdateIsAvailable()

            adService.initialize()
            appLanguageService.setLocale() // fix locale after admob init

            adminService.init()

            when {
                isRunningFirstTime() -> firstRunInit()
                preferencesState.appExecutionCount == 50L -> promptRateApp()
                BuildConfig.VERSION_CODE > preferencesState.lastAppVersionCode -> promptChangelog()
            }
            reportExecution()

            activityController.initialized = true
            postInit()

            val activityName = activity::class.simpleName
            initChannel.close()
            logger.info("Application has been initialized ($activityName, execution #${preferencesState.appExecutionCount})")
        }
    }

    private fun syncInit() {
        songImportFileChooser.init()  // has to be done inside activity.onCreate
    }

    private fun postInit() {
        postInitIntent(activity.intent)
        if (backupSyncManager.needsAutomaticBackup()) {
            backupSyncManager.makeAutomaticBackup()
        }
    }

    private fun postInitIntent(intent: Intent?) {
        intent?.getStringExtra("encodedSong")?.let { encodedSong ->
            shareSongService.openSharedEncodedSong(encodedSong)
        }
    }

    fun newIntentFromActivity(intent: Intent?) {
        if (initJob?.isActive != false) return // true or null
        postInitIntent(intent)
    }

    private fun firstRunInit() {
        logger.debug("First run init")
        chordsNotationService.setDefaultChordsNotation()
        aboutLayoutController.showFirstTimeManualPrompt()
    }

    private fun debugInit() {
        // Allow showing the activity even if the device is locked
        windowManagerService.showAppWhenLocked()
    }

    private fun isRunningFirstTime(): Boolean {
        return preferencesState.appExecutionCount == 0L
    }

    private fun reportExecution() {
        preferencesState.appExecutionCount += 1
        preferencesState.lastAppVersionCode = BuildConfig.VERSION_CODE.toLong()
    }

    private fun getStartingScreen(): KClass<out MainLayout> {
        return preferencesState.homeScreen.layoutClass
    }

    private fun promptRateApp() {
        uiInfoService.dialogThreeChoices(
            titleResId = R.string.prompt_rate_app_title,
            messageResId = R.string.prompt_rate_app_body,
            negativeButton = R.string.action_cancel,
            negativeAction = {},
            positiveButton = R.string.action_info_yes,
            positiveAction = { LinkOpener().openInGoogleStore() }
        )
    }

    private fun promptChangelog() {
        uiInfoService.showInfoAction(
            R.string.changelog_checkout_latest_changes,
            actionResId = R.string.action_open_changelog,
            durationMillis = 20_000,
        ) {
            webviewLayoutController.openChangelog()
        }
    }

    suspend fun waitUntilInitialized(): Boolean {
        return try {
            initChannel.receive()
            true
        } catch (e: ClosedReceiveChannelException) {
            true
        } catch (e: CancellationException) {
            false
        }
    }
}
