package mwongela.songbook.system

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.info.errorcheck.ContextError
import mwongela.songbook.info.errorcheck.UiErrorHandler
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory

class LinkOpener(
    appCompatActivity: LazyInject<AppCompatActivity> = appFactory.appCompatActivity,
) {
    private val activity by LazyExtractor(appCompatActivity)

    fun openPage(url: String) {
        try {
            val urlActivity = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(urlActivity)
        } catch (t: SecurityException) {
            UiErrorHandler().handleError(ContextError("Failed to open a link: SecurityException", t))
        } catch (t: ActivityNotFoundException) {
            UiErrorHandler().handleError(ContextError("No application found to open a link", t))
        } catch (t: Throwable) {
            UiErrorHandler().handleError(ContextError("Failed to open a link", t))
        }
    }

    fun openInGoogleStore() {
        try {
            val urlActivity = Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    "http://play.google.com/store/apps/details?id=" + activity.packageName
                )
            )
            activity.startActivity(urlActivity)
        } catch (e: ActivityNotFoundException) {
            val uri = Uri.parse("market://details?id=" + activity.packageName)
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            activity.startActivity(goToMarket)
        } catch (t: Throwable) {
            UiErrorHandler().handleError(ContextError("Failed to open Google Play Store", t))
        }
    }

    fun openPrivacyPolicy() {
        openPage("https://docs.google.com/document/d/e/2PACX-1vTRgTqRx6Cwbn_uuLXCuad9YEK3qY7XNxMkil26ZBV5XZ_qn6L-CaXu3M39k-Gc6OErnCmsrY8QPT8e/pub")
    }
}