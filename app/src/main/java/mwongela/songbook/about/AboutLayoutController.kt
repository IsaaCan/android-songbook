package mwongela.songbook.about

import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import mwongela.songbook.BuildConfig
import mwongela.songbook.R
import mwongela.songbook.info.UiInfoService
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.repository.SongsRepository
import mwongela.songbook.secret.CommanderService
import mwongela.songbook.system.LinkOpener
import mwongela.songbook.system.PackageInfoService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutLayoutController(
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    commanderService: LazyInject<CommanderService> = appFactory.commanderService,
    packageInfoService: LazyInject<PackageInfoService> = appFactory.packageInfoService,
    songsRepository: LazyInject<SongsRepository> = appFactory.songsRepository,
    uiInfoService: LazyInject<UiInfoService> = appFactory.uiInfoService,
    webviewLayoutController: LazyInject<WebviewLayoutController> = appFactory.webviewLayoutController,
) {
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val secretCommandService by LazyExtractor(commanderService)
    private val packageInfoService by LazyExtractor(packageInfoService)
    private val songsRepository by LazyExtractor(songsRepository)
    private val uiInfoService by LazyExtractor(uiInfoService)
    private val webviewLayoutController by LazyExtractor(webviewLayoutController)

    fun showAbout() {
        val appVersionName = packageInfoService.versionName
        val buildDate = BuildConfig.BUILD_DATE.formatYYYMMDD()
        val appVersionCode = packageInfoService.versionCode.toString()
        val dbVersionNumber = songsRepository.publicSongsRepo.versionNumber.toString()
        val title = uiResourceService.resString(R.string.nav_about)

        val variant = when {
            BuildConfig.DEBUG -> "debug"
            else -> "release"
        }
        val appVersionLong = "$variant $appVersionCode $buildDate"
        val message = uiResourceService.resString(
            R.string.ui_about_content,
            appVersionName,
            appVersionLong,
            dbVersionNumber
        )
        val spannedMessage = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY)

        // set button almost hidden by setting color
        val postProcessor = { alertDialog: AlertDialog ->
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setTextColor(uiResourceService.getColor(R.color.unlockAction))
            }
        }
        uiInfoService.dialogThreeChoices(
            title = title,
            message = spannedMessage,
            positiveButton = R.string.action_info_ok,
            positiveAction = {},
            negativeButton = R.string.action_rate_app,
            negativeAction = { LinkOpener().openInGoogleStore() },
            neutralButton = R.string.action_secret,
            neutralAction = { secretCommandService.showUnlockAlert() },
            postProcessor = postProcessor,
            richMessage = true,
        )
    }

    fun showManual() {
        webviewLayoutController.openUrlUserGuide()
    }

    fun showFirstTimeManualPrompt() {
        uiInfoService.showInfoAction(
            infoResId = R.string.prompt_first_time_manual,
            durationMillis = 20_000,
            actionResId = R.string.prompt_first_time_manual_action,
            action = { showManual() }
        )
    }

    private fun Date.formatYYYMMDD(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        return dateFormat.format(this)
    }

}
