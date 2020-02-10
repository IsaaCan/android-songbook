package igrek.songbook.admin

import dagger.Lazy
import igrek.songbook.dagger.DaggerIoc
import igrek.songbook.info.logger.LoggerFactory
import igrek.songbook.settings.preferences.PreferencesDefinition
import igrek.songbook.settings.preferences.PreferencesService
import javax.inject.Inject

class AdminService {
    @Inject
    lateinit var preferencesService: Lazy<PreferencesService>

    var userAuthToken: String = ""

    fun login(key: String) {
        val match = Regex("login (.+)").matchEntire(key)
        match?.let {
            userAuthToken = it.groupValues[1]
            LoggerFactory.logger.debug("Admin token entered: $userAuthToken")
        }
    }

    init {
        DaggerIoc.factoryComponent.inject(this)
        loadPreferences()
    }

    private fun loadPreferences() {
        userAuthToken = preferencesService.get().getValue(PreferencesDefinition.UserAuthToken, String::class.java)
                ?: ""
    }
}