package mwongela.songbook.settings

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.R
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.layout.LayoutController
import mwongela.songbook.layout.MainLayout

class SettingsLayoutController(
    layoutController: LazyInject<LayoutController> = appFactory.layoutController,
    appCompatActivity: LazyInject<AppCompatActivity> = appFactory.appCompatActivity,
) : MainLayout {
    private val layoutController by LazyExtractor(layoutController)
    private val activity by LazyExtractor(appCompatActivity)

    override fun showLayout(layout: View) {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_content, SettingsFragment())
            .commit()
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.screen_settings
    }

    override fun onBackClicked() {
        layoutController.showPreviousLayoutOrQuit()
    }

    override fun onLayoutExit() {}
}
