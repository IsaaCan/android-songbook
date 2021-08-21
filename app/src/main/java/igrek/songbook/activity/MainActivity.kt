package igrek.songbook.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import igrek.songbook.info.logger.Logger
import igrek.songbook.info.logger.LoggerFactory
import igrek.songbook.inject.AppContextFactory
import igrek.songbook.inject.LazyExtractor
import igrek.songbook.inject.LazyInject
import igrek.songbook.inject.appFactory
import igrek.songbook.util.RetryDelayed


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
        activityData.appInitializer.postInitIntent(intent)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return activityData.optionSelectDispatcher.optionsSelect(item.itemId) || super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (activityData.systemKeyDispatcher.onKeyBack())
                    return true
            }
            KeyEvent.KEYCODE_MENU -> {
                if (activityData.systemKeyDispatcher.onKeyMenu())
                    return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (activityData.systemKeyDispatcher.onVolumeUp())
                    return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (activityData.systemKeyDispatcher.onVolumeDown())
                    return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (activityData.systemKeyDispatcher.onArrowUp())
                    return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (activityData.systemKeyDispatcher.onArrowDown())
                    return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (activityData.systemKeyDispatcher.onArrowLeft())
                    return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (activityData.systemKeyDispatcher.onArrowRight())
                    return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        activityData.permissionService.onRequestPermissionsResult(permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityData.activityResultDispatcher.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

}
