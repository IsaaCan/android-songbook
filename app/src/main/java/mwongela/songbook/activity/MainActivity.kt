package mwongela.songbook.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.info.errorcheck.RetryDelayed
import mwongela.songbook.info.logger.Logger
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.AppContextFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory


open class MainActivity(
    mainActivityData: LazyInject<MainActivityData> = appFactory.activityData,
) : AppCompatActivity() {

    private var activityData by LazyExtractor(mainActivityData)

    private val logger: Logger = LoggerFactory.logger

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            logger.info("Creating Dependencies container...")
            AppContextFactory.createAppContext(this)
            recreateFields() // Workaround for reusing finished activities by Android
            super.onCreate(savedInstanceState)
            activityData.appInitializer.init()
        } catch (t: Throwable) {
            logger.fatal(t)
            throw t
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logger.debug("MainActivity::onNewIntent")
        activityData.appInitializer.newIntentFromActivity(intent)
    }

    private fun recreateFields() {
        activityData = appFactory.activityData.get()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activityData.activityController.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityData.activityController.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        Handler(Looper.getMainLooper()).post {
            RetryDelayed(10, 500, UninitializedPropertyAccessException::class.java) {
                activityData.activityController.onStart()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        activityData.activityController.onStop()
    }

    override fun onResume() {
        super.onResume()
        activityData.activityController.onResume()
    }

    override fun onPause() {
        super.onPause()
        activityData.activityController.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return activityData.optionSelectDispatcher.optionsSelect(item.itemId) || super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (activityData.systemKeyDispatcher.onKeyDown(keyCode))
            return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        activityData.permissionService.onRequestPermissionsResult(permissions, grantResults)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityData.activityResultDispatcher.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}
