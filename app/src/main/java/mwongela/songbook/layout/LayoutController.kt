package mwongela.songbook.layout

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.Transition
import androidx.transition.TransitionManager
import mwongela.songbook.R
import mwongela.songbook.about.WebviewLayoutController
import mwongela.songbook.activity.ActivityController
import mwongela.songbook.admin.antechamber.AdminSongsLayoutContoller
import mwongela.songbook.billing.BillingLayoutController
import mwongela.songbook.cast.SongCastLobbyLayout
import mwongela.songbook.cast.SongCastMenuLayout
import mwongela.songbook.custom.CustomSongsListLayoutController
import mwongela.songbook.custom.EditSongLayoutController
import mwongela.songbook.editor.ChordsEditorLayoutController
import mwongela.songbook.info.errorcheck.safeExecute
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.info.logview.LogsLayoutController
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.SingletonInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.ad.AdService
import mwongela.songbook.layout.navigation.NavigationMenuController
import mwongela.songbook.playlist.PlaylistFillLayoutController
import mwongela.songbook.playlist.PlaylistLayoutController
import mwongela.songbook.room.RoomListLayoutController
import mwongela.songbook.room.RoomLobbyLayoutController
import mwongela.songbook.send.ContactLayoutController
import mwongela.songbook.send.MissingSongLayoutController
import mwongela.songbook.send.PublishSongLayoutController
import mwongela.songbook.settings.SettingsLayoutController
import mwongela.songbook.songpreview.SongPreviewLayoutController
import mwongela.songbook.songselection.favourite.FavouritesLayoutController
import mwongela.songbook.songselection.history.OpenHistoryLayoutController
import mwongela.songbook.songselection.latest.LatestSongsLayoutController
import mwongela.songbook.songselection.search.SongSearchLayoutController
import mwongela.songbook.songselection.top.TopSongsLayoutController
import mwongela.songbook.songselection.tree.SongTreeLayoutController
import mwongela.songbook.system.SystemKeyDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass


class LayoutController {
    private val activity: AppCompatActivity by LazyExtractor(appFactory.appCompatActivity)
    private val navigationMenuController: NavigationMenuController by LazyExtractor(appFactory.navigationMenuController)
    private val activityController: ActivityController by LazyExtractor(appFactory.activityController)
    private val adService: AdService by LazyExtractor(appFactory.adService)
    private val systemKeyDispatcher: SystemKeyDispatcher by LazyExtractor(appFactory.systemKeyDispatcher)

    private var mainContentLayout: CoordinatorLayout? = null
    private var currentLayout: MainLayout? = null
    private var layoutHistory: MutableList<MainLayout> = mutableListOf()
    private var registeredLayoutInjectors: Map<KClass<out MainLayout>, SingletonInject<out MainLayout>> = mapOf(
        SongTreeLayoutController::class to appFactory.songTreeLayoutController,
        SongSearchLayoutController::class to appFactory.songSearchLayoutController,
        SongPreviewLayoutController::class to appFactory.songPreviewLayoutController,
        ContactLayoutController::class to appFactory.contactLayoutController,
        SettingsLayoutController::class to appFactory.settingsLayoutController,
        EditSongLayoutController::class to appFactory.editSongLayoutController,
        ChordsEditorLayoutController::class to appFactory.chordsEditorLayoutController,
        CustomSongsListLayoutController::class to appFactory.customSongsListLayoutController,
        FavouritesLayoutController::class to appFactory.favouritesLayoutController,
        PlaylistLayoutController::class to appFactory.playlistLayoutController,
        LatestSongsLayoutController::class to appFactory.latestSongsLayoutController,
        TopSongsLayoutController::class to appFactory.topSongsLayoutController,
        OpenHistoryLayoutController::class to appFactory.openHistoryLayoutController,
        MissingSongLayoutController::class to appFactory.missingSongLayoutController,
        PublishSongLayoutController::class to appFactory.publishSongLayoutController,
        AdminSongsLayoutContoller::class to appFactory.adminSongsLayoutContoller,
        RoomListLayoutController::class to appFactory.roomListLayoutController,
        RoomLobbyLayoutController::class to appFactory.roomLobbyLayoutController,
        BillingLayoutController::class to appFactory.billingLayoutController,
        WebviewLayoutController::class to appFactory.webviewLayoutController,
        PlaylistFillLayoutController::class to appFactory.playlistFillLayoutController,
        LogsLayoutController::class to appFactory.logsLayoutController,
        SongCastLobbyLayout::class to appFactory.songCastLobbyLayout,
        SongCastMenuLayout::class to appFactory.songCastMenuLayout,
    )
    private val logger = LoggerFactory.logger
    private val layoutCache = hashMapOf<Int, View>()
    var initializedLayout: KClass<out MainLayout>? = null
        private set

    suspend fun init() {
        withContext(Dispatchers.Main) {
            activity.setContentView(R.layout.main_layout)
            mainContentLayout = activity.findViewById<CoordinatorLayout>(R.id.main_content).also {
                it.isFocusable = true
                it.isFocusableInTouchMode = true
                it.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        return@setOnKeyListener systemKeyDispatcher.onKeyDown(keyCode)
                    }
                    return@setOnKeyListener false
                }
            }
            navigationMenuController.init()

            activity.supportActionBar?.hide()
        }
    }

    fun showLayout(
        layoutClass: KClass<out MainLayout>,
        disableReturn: Boolean = false,
        onShown: (() -> Unit) = {},
    ): Job {
        val layoutInjector = registeredLayoutInjectors[layoutClass]
            ?: throw IllegalArgumentException("${layoutClass.simpleName} class not registered as layout")
        val layoutController: MainLayout = layoutInjector.get()

        if (disableReturn) {
            // remove current layout from history
            if (currentLayout in layoutHistory) {
                layoutHistory.remove(currentLayout)
            }
        }

        layoutController.let {
            if (it in layoutHistory) {
                layoutHistory.remove(it)
            }
            layoutHistory.add(it)
        }

        logger.debug("Showing layout ${layoutClass.simpleName} [${layoutHistory.size} in history]")

        return GlobalScope.launch(Dispatchers.Main) {
            showMainLayout(layoutController)
            onShown()
        }
    }

    private fun showMainLayout(mainLayout: MainLayout) {
        currentLayout?.onLayoutExit()
        currentLayout = mainLayout

        val transition: Transition = Fade()
        transition.duration = 200

        val (properLayoutView, _) = createLayout(mainLayout.getLayoutResourceId())

        val mainContentLayoutN: CoordinatorLayout = mainContentLayout ?: return
        val firstTimeView = mainContentLayoutN.childCount == 0

        mainContentLayoutN.removeAllViews()
        mainContentLayoutN.addView(properLayoutView)

        if (!firstTimeView) {
            TransitionManager.go(Scene(mainContentLayoutN, properLayoutView), transition)
        }

        mainLayout.showLayout(properLayoutView)
        postInitLayout(mainLayout)
        initializedLayout = mainLayout::class
    }

    private fun createLayout(layoutResourceId: Int): Pair<View, Boolean> {
        val inflater = activity.layoutInflater
        val properLayoutView = inflater.inflate(layoutResourceId, null)
        properLayoutView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutCache[layoutResourceId] = properLayoutView
        return properLayoutView to false
    }

    private fun postInitLayout(currentLayout: MainLayout) {
        adService.updateAdBanner(currentLayout)

        if (activityController.isAndroidTv()) {
            activity.findViewById<ImageButton>(R.id.navMenuButton)?.let {
                it.isFocusableInTouchMode = true
                it.requestFocusFromTouch()
                it.isFocusableInTouchMode = false
            }
        }
    }

    fun showPreviousLayoutOrQuit() {
        // remove current layout from last place
        try {
            val last = layoutHistory.last()
            if (last == currentLayout) {
                layoutHistory = layoutHistory.dropLast(1).toMutableList()
            }
        } catch (e: NoSuchElementException) {
            logger.error(e)
        }

        if (layoutHistory.isEmpty()) {
            activityController.quit()
            return
        }

        val previousLayout = layoutHistory.last()
        logger.debug("Showing previous layout ${previousLayout::class.simpleName} [${layoutHistory.size} in history]")
        GlobalScope.launch(Dispatchers.Main) {
            showMainLayout(previousLayout)
        }
    }

    fun isState(compareLayoutClass: KClass<out MainLayout>): Boolean {
        return compareLayoutClass.isInstance(currentLayout)
    }

    fun onBackClicked() {
        if (navigationMenuController.isDrawerShown()) {
            navigationMenuController.navDrawerHide()
            return
        }
        safeExecute {
            currentLayout?.onBackClicked()
        }
    }

}
