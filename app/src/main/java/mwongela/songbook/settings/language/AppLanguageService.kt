package mwongela.songbook.settings.language


import android.app.Activity
import android.os.Build
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.persistence.user.UserDataDao
import mwongela.songbook.settings.preferences.PreferencesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class AppLanguageService(
    activity: LazyInject<Activity> = appFactory.activity,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
    userDataDao: LazyInject<UserDataDao> = appFactory.userDataDao,
) {
    private val activity by LazyExtractor(activity)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val preferencesState by LazyExtractor(preferencesState)
    private val userDataDao by LazyExtractor(userDataDao)

    var selectedSongLanguages: Set<SongLanguage>
        get() {
            val excludedLanguages = userDataDao.exclusionDao.exclusionDb.languages
            return SongLanguage.allKnown().filter { it.langCode !in excludedLanguages }.toSet()
        }
        set(value) {
            val excluded = (SongLanguage.allKnown() - value).map { it.langCode }.toMutableList()
            userDataDao.exclusionDao.setExcludedLanguages(excluded)
        }

    /**
     * forces locale settings
     * @param langCode language code (pl)
     */
    private fun setLocale(langCode: String?) {
        val res = activity.resources
        // Change locale settings in the app.
        val dm = res.displayMetrics
        val conf = res.configuration
        if (langCode == null) {
            conf.setLocale(null)
        } else {
            conf.setLocale(Locale(langCode.lowercase()))
        }
        @Suppress("DEPRECATION")
        res.updateConfiguration(conf, dm)
    }

    suspend fun setLocale() {
        if (preferencesState.appLanguage != AppLanguage.DEFAULT) {
            withContext(Dispatchers.Main) {
                setLocale(preferencesState.appLanguage.langCode)
            }
        }
    }

    fun updateLocale() {
        val langCode = preferencesState.appLanguage.langCode.takeIf { it.isNotBlank() }
        setLocale(langCode)
    }

    @Suppress("DEPRECATION")
    fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.resources.configuration.locales.get(0)
        } else {
            activity.resources.configuration.locale
        }
    }

    fun songLanguageEntries(): LinkedHashMap<SongLanguage, String> {
        val map = LinkedHashMap<SongLanguage, String>()
        SongLanguage.allKnown()
            .forEach { lang ->
                val locale = Locale(lang.langCode)
                val langDisplayName = locale.getDisplayLanguage(locale)
                map[lang] = langDisplayName
            }
        return map
    }

    fun languageStringEntries(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        for (item in AppLanguage.values()) {
            val displayName = uiResourceService.resString(item.displayNameResId)
            map[item.langCode] = displayName
        }
        return map
    }

    fun languageFilterEntries(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        SongLanguage.allKnown()
            .forEach { lang ->
                val locale = Locale(lang.langCode)
                val langDisplayName = locale.getDisplayLanguage(locale)
                map[lang.langCode] = langDisplayName
            }
        return map
    }

    fun setSelectedSongLanguageCodes(languageCodes: Set<String>) {
        selectedSongLanguages = SongLanguage.allKnown()
            .filter { it.langCode in languageCodes }
            .toSet()
    }
}