package mwongela.songbook.settings.chordsnotation


import mwongela.songbook.info.UiResourceService
import mwongela.songbook.info.logger.LoggerFactory.logger
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.settings.language.AppLanguageService
import mwongela.songbook.settings.preferences.PreferencesState
import java.util.Locale

class ChordsNotationService(
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    appLanguageService: LazyInject<AppLanguageService> = appFactory.appLanguageService,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
) {
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val appLanguageService by LazyExtractor(appLanguageService)
    private val preferencesState by LazyExtractor(preferencesState)

    var chordsNotation: ChordsNotation
        get() = preferencesState.chordsNotation
        set(value) {
            preferencesState.chordsNotation = value
        }

    private val germanNotationLangs = setOf(
        "pl", // Poland
        "de", // German - Austria, Germany
        "da", // Denmark
        "sv", // Swedish
        "no", // Norwegian
        "nb", // Norwegian - Bokml
        "nn", // Norwegian Nynorsk
        "is", // Icelandic
        "et", // Estonian
        "fi", // Finnish
        "sr", // Serbian
        "hr", // Croatian
        "bs", // Bosnian
        "sl", // Slovenian
        "sk", // Slovak
        "cs", // Czech
        "hu", // Hungarian
    )

    fun setDefaultChordsNotation() {
        // running for the first time - set german / polish notation if lang pl
        // set default chords notation depending on locale settings
        val current: Locale = appLanguageService.getCurrentLocale()
        val lang = current.language

        chordsNotation = if (lang in germanNotationLangs) {
            ChordsNotation.GERMAN
        } else {
            ChordsNotation.ENGLISH
        }
        logger.info("Default chords notation set: $chordsNotation")
    }

    fun chordsNotationEntries(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        for (item in ChordsNotation.values()) {
            val displayName = uiResourceService.resString(item.displayNameResId)
            map[item.id.toString()] = displayName
        }
        return map
    }

    val chordsNotationDisplayNames: LinkedHashMap<ChordsNotation, String> by lazy {
        val map = LinkedHashMap<ChordsNotation, String>()
        for (item in ChordsNotation.values()) {
            val displayName = this.uiResourceService.resString(item.displayNameResId)
            map[item] = displayName
        }
        map
    }

}