package mwongela.songbook.inject

import androidx.appcompat.app.AppCompatActivity

var appFactory: AppFactory = AppFactory(null)

object AppContextFactory {
    fun createAppContext(activity: AppCompatActivity) {
        appFactory = AppFactory(activity)
    }
}
