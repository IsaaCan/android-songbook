package mwongela.songbook.settings.preferences

import mwongela.songbook.cast.CastScrollControl
import mwongela.songbook.chords.diagram.guitar.ChordDiagramStyle
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.settings.buttons.MediaButtonBehaviours
import mwongela.songbook.settings.chordsnotation.ChordsNotation
import mwongela.songbook.settings.enums.ChordsInstrument
import mwongela.songbook.settings.enums.CustomSongsOrdering
import mwongela.songbook.settings.homescreen.HomeScreenEnum
import mwongela.songbook.settings.language.AppLanguage
import mwongela.songbook.settings.theme.ColorScheme
import mwongela.songbook.settings.theme.DisplayStyle
import mwongela.songbook.settings.theme.FontTypeface
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferencesState(
    preferencesService: LazyInject<PreferencesService> = appFactory.preferencesService,
) {
    internal val preferencesService by LazyExtractor(preferencesService)

    var fontsize: Float by PreferenceDelegate(PreferencesField.Fontsize)
    var appLanguage: AppLanguage by PreferenceDelegate(PreferencesField.AppLanguage)
    var chordsNotation: ChordsNotation by PreferenceDelegate(PreferencesField.ChordsNotationId)
    var fontTypeface: FontTypeface by PreferenceDelegate(PreferencesField.FontTypefaceId)
    var colorScheme: ColorScheme by PreferenceDelegate(PreferencesField.ColorSchemeId)
    var autoscrollSpeed: Float by PreferenceDelegate(PreferencesField.AutoscrollSpeed)
    var autoscrollSpeedAutoAdjustment: Boolean by PreferenceDelegate(PreferencesField.AutoscrollSpeedAutoAdjustment)
    var autoscrollSpeedVolumeKeys: Boolean by PreferenceDelegate(PreferencesField.AutoscrollSpeedVolumeKeys)
    var randomFavouriteSongsOnly: Boolean by PreferenceDelegate(PreferencesField.RandomFavouriteSongsOnly)
    var randomPlaylistSongs: Boolean by PreferenceDelegate(PreferencesField.RandomPlaylistSongs)
    var restoreTransposition: Boolean by PreferenceDelegate(PreferencesField.RestoreTransposition)
    var chordsInstrument: ChordsInstrument by PreferenceDelegate(PreferencesField.ChordsInstrument)
    var userAuthToken: String by PreferenceDelegate(PreferencesField.UserAuthToken)
    var appExecutionCount: Long by PreferenceDelegate(PreferencesField.AppExecutionCount)
    var adsStatus: Long by PreferenceDelegate(PreferencesField.AdsStatus)
    var chordsDisplayStyle: DisplayStyle by PreferenceDelegate(PreferencesField.ChordsDisplayStyle)
    var chordsEditorFontTypeface: FontTypeface by PreferenceDelegate(PreferencesField.ChordsEditorFontTypeface)
    var keepScreenOn: Boolean by PreferenceDelegate(PreferencesField.KeepScreenOn)
    var anonymousUsageData: Boolean by PreferenceDelegate(PreferencesField.AnonymousUsageData)
    var chordDiagramStyle: ChordDiagramStyle by PreferenceDelegate(PreferencesField.ChordDiagramStyle)
    var updateDbOnStartup: Boolean by PreferenceDelegate(PreferencesField.UpdateDbOnStartup)
    var trimWhitespaces: Boolean by PreferenceDelegate(PreferencesField.TrimWhitespaces)
    var autoscrollAutostart: Boolean by PreferenceDelegate(PreferencesField.AutoscrollAutostart)
    var autoscrollForwardNextSong: Boolean by PreferenceDelegate(PreferencesField.AutoscrollForwardNextSong)
    var autoscrollShowEyeFocus: Boolean by PreferenceDelegate(PreferencesField.AutoscrollShowEyeFocus)
    var autoscrollIndividualSpeed: Boolean by PreferenceDelegate(PreferencesField.AutoscrollIndividualSpeed)
    var horizontalScroll: Boolean by PreferenceDelegate(PreferencesField.HorizontalScroll)
    var mediaButtonBehaviour: MediaButtonBehaviours by PreferenceDelegate(PreferencesField.MediaButtonBehaviour)
    var purchasedAdFree: Boolean by PreferenceDelegate(PreferencesField.PurchasedAdFree)
    var homeScreen: HomeScreenEnum by PreferenceDelegate(PreferencesField.HomeScreen)
    var forceSharpNotes: Boolean by PreferenceDelegate(PreferencesField.ForceSharpNotes)
    var customSongsOrdering: CustomSongsOrdering by PreferenceDelegate(PreferencesField.CustomSongsOrdering)
    var songLyricsSearch: Boolean by PreferenceDelegate(PreferencesField.SongLyricsSearch)
    var syncBackupAutomatically: Boolean by PreferenceDelegate(PreferencesField.SyncBackupAutomatically)
    var lastDriveBackupTimestamp: Long by PreferenceDelegate(PreferencesField.LastDriveBackupTimestamp)
    var deviceId: String by PreferenceDelegate(PreferencesField.DeviceId)
    var lastAppVersionCode: Long by PreferenceDelegate(PreferencesField.LastAppVersionCode)
    var saveCustomSongsBackups: Boolean by PreferenceDelegate(PreferencesField.SaveCustomSongsBackups)
    var castScrollControl: CastScrollControl by PreferenceDelegate(PreferencesField.CastScrollControl)

}

class PreferenceDelegate<T : Any>(
    private val field: PreferencesField
) : ReadWriteProperty<PreferencesState, T> {

    override fun getValue(thisRef: PreferencesState, property: KProperty<*>): T {
        return thisRef.preferencesService.getValue(field)
    }

    override fun setValue(thisRef: PreferencesState, property: KProperty<*>, value: T) {
        thisRef.preferencesService.setValue(field, value)
    }
}
