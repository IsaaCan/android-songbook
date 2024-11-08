package mwongela.songbook.admin

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import mwongela.songbook.R
import mwongela.songbook.admin.antechamber.AntechamberService
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.navigation.NavigationMenuController
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.settings.preferences.PreferencesState
import mwongela.songbook.system.SoftKeyboardService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AdminService(
    navigationMenuController: LazyInject<NavigationMenuController> = appFactory.navigationMenuController,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    antechamberService: LazyInject<AntechamberService> = appFactory.antechamberService,
    activity: LazyInject<Activity> = appFactory.activity,
    softKeyboardService: LazyInject<SoftKeyboardService> = appFactory.softKeyboardService,
    songRankService: LazyInject<SongRankService> = appFactory.songRankService,
    adminCategoryManager: LazyInject<AdminCategoryManager> = appFactory.adminCategoryManager,
) {
    private val navigationMenuController by LazyExtractor(navigationMenuController)
    private val preferencesState by LazyExtractor(preferencesState)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val antechamberService by LazyExtractor(antechamberService)
    private val activity by LazyExtractor(activity)
    private val softKeyboardService by LazyExtractor(softKeyboardService)
    private val songRankService by LazyExtractor(songRankService)
    private val adminCategoryManager by LazyExtractor(adminCategoryManager)

    var userAuthToken: String
        get() = preferencesState.userAuthToken
        set(value) {
            preferencesState.userAuthToken = value
        }

    fun loginAdmin(authKey: String) {
        userAuthToken = authKey
        LoggerFactory.logger.debug("Admin token entered: [$userAuthToken]")
        checkMenuVisibility()
    }

    fun init() {
        checkMenuVisibility()
    }

    private fun checkMenuVisibility() {
        if (isAdminEnabled()) {
            LoggerFactory.logger.debug("Admin menus enabled")
        } else {
            navigationMenuController.hideAdminMenu()
        }
    }

    fun isAdminEnabled(): Boolean {
        return userAuthToken.isNotBlank()
    }

    fun updatePublicSongUi(song: Song) {
        uiInfoService.showInfo(R.string.admin_sending, indefinite = true)

        val deferred = antechamberService.updatePublicSongAsync(song)
        GlobalScope.launch(Dispatchers.Main) {
            val result = deferred.await()
            result.fold(onSuccess = {
                uiInfoService.showInfo(R.string.admin_success)
            }, onFailure = { e ->
                UiErrorHandler.handleError(e, R.string.admin_communication_breakdown)
            })
        }
    }

    fun updateRankDialog(song: Song) {
        val dlgAlert = AlertDialog.Builder(activity)
        dlgAlert.setMessage(uiResourceService.resString(R.string.admin_update_rank))

        val input = EditText(activity)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(song.rank?.toString().orEmpty())
        dlgAlert.setView(input)

        val actionCheck = uiResourceService.resString(R.string.action_info_ok)
        dlgAlert.setPositiveButton(actionCheck) { _, _ ->
            val newRank = input.text.toString().toDouble()
            updateRank(song, newRank)
        }
        dlgAlert.setNegativeButton(uiResourceService.resString(R.string.action_cancel)) { _, _ -> }
        dlgAlert.setCancelable(true)
        val dialog = dlgAlert.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()

        input.requestFocus()
        Handler(Looper.getMainLooper()).post {
            softKeyboardService.showSoftKeyboard(input)
        }
    }

    fun createCategoryDialog() {
        val dlgAlert = AlertDialog.Builder(activity)
        dlgAlert.setMessage(uiResourceService.resString(R.string.admin_create_category))

        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        dlgAlert.setView(input)

        val actionCheck = uiResourceService.resString(R.string.action_info_ok)
        dlgAlert.setPositiveButton(actionCheck) { _, _ ->
            val categoryName = input.text.toString()
            createCategory(categoryName)
        }
        dlgAlert.setNegativeButton(uiResourceService.resString(R.string.action_cancel)) { _, _ -> }
        dlgAlert.setCancelable(true)
        val dialog = dlgAlert.create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()

        input.requestFocus()
        Handler(Looper.getMainLooper()).post {
            softKeyboardService.showSoftKeyboard(input)
        }
    }

    private fun updateRank(song: Song, rank: Double?) {
        uiInfoService.showInfo(R.string.admin_sending, indefinite = true)

        val deferred = songRankService.updateRankAsync(song, rank)
        GlobalScope.launch(Dispatchers.Main) {
            val result = deferred.await()
            result.fold(onSuccess = {
                uiInfoService.showInfo(R.string.admin_success)
            }, onFailure = { e ->
                UiErrorHandler.handleError(e, R.string.admin_communication_breakdown)
            })
        }
    }

    private fun createCategory(categoryName: String) {
        uiInfoService.showInfo(R.string.admin_sending, indefinite = true)

        val deferred = adminCategoryManager.createCategoryAsync(categoryName)
        GlobalScope.launch(Dispatchers.Main) {
            val result = deferred.await()
            result.fold(onSuccess = {
                uiInfoService.showInfo(R.string.admin_success)
            }, onFailure = { e ->
                UiErrorHandler.handleError(e, R.string.admin_communication_breakdown)
            })
        }
    }
}