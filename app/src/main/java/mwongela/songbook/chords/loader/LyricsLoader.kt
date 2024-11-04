package mwongela.songbook.chords.loader

import android.graphics.Typeface
import mwongela.songbook.R
import mwongela.songbook.chords.arranger.LyricsArranger
import mwongela.songbook.chords.detect.KeyDetector
import mwongela.songbook.chords.model.LyricsCloner
import mwongela.songbook.chords.model.LyricsModel
import mwongela.songbook.chords.parser.ChordParser
import mwongela.songbook.chords.parser.LyricsExtractor
import mwongela.songbook.chords.render.ChordsRenderer
import mwongela.songbook.chords.render.LyricsInflater
import mwongela.songbook.chords.syntax.MajorKey
import mwongela.songbook.chords.transpose.ChordsTransposer
import mwongela.songbook.chords.transpose.ChordsTransposerManager
import mwongela.songbook.info.logger.LoggerFactory
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.settings.chordsnotation.ChordsNotation
import mwongela.songbook.settings.preferences.PreferencesState
import mwongela.songbook.settings.theme.DisplayStyle
import mwongela.songbook.settings.theme.LyricsThemeService
import mwongela.songbook.songpreview.scroll.AutoscrollService
import mwongela.songbook.system.WindowManagerService

// Singleton
class LyricsLoader(
    autoscrollService: LazyInject<AutoscrollService> = appFactory.autoscrollService,
    lyricsThemeService: LazyInject<LyricsThemeService> = appFactory.lyricsThemeService,
    windowManagerService: LazyInject<WindowManagerService> = appFactory.windowManagerService,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
) {
    private val autoscrollService by LazyExtractor(autoscrollService)
    private val lyricsThemeService by LazyExtractor(lyricsThemeService)
    private val windowManagerService by LazyExtractor(windowManagerService)
    private val preferencesState by LazyExtractor(preferencesState)
    private val uiInfoService by LazyExtractor(appFactory.uiInfoService)

    private val logger = LoggerFactory.logger
    private val chordsTransposerManager = ChordsTransposerManager()
    private var screenW = 0
    private var originalSongNotation: ChordsNotation = ChordsNotation.default
    var originalLyrics: LyricsModel = LyricsModel()
        private set
    var transposedLyrics: LyricsModel = LyricsModel()
        private set
    var arrangedLyrics: LyricsModel = LyricsModel()
        private set
    var songKey: MajorKey = MajorKey.C_MAJOR
        private set

    fun load(
        fileContent: String,
        screenW: Int?,
        initialTransposed: Int,
        srcNotation: ChordsNotation,
    ) {
        chordsTransposerManager.run {
            val transposed = when {
                preferencesState.restoreTransposition -> initialTransposed
                else -> 0
            }
            reset(transposed)
        }
        autoscrollService.reset()

        if (screenW != null)
            this.screenW = screenW

        originalSongNotation = srcNotation

        originalLyrics = if (fileContent.isEmpty()) {
            LyricsModel()
        } else {
            val lyricsExtractor = LyricsExtractor(trimWhitespaces = preferencesState.trimWhitespaces)
            val lyrics = lyricsExtractor.parseLyrics(fileContent)
            val unknownChords = ChordParser(srcNotation).parseAndFillChords(lyrics)
            unknownChords.takeIf { it.isNotEmpty() }?.let {
                val warningMessage = uiInfoService.resString(R.string.unknown_chords_in_song, unknownChords.joinToString(", "))
                uiInfoService.showInfo(warningMessage)
            }
            lyrics
        }

        transposeAndFormatLyrics()
    }

    private fun transposeAndFormatLyrics() {
        val lyrics = ChordsTransposer().transposeLyrics(originalLyrics, chordsTransposerManager.transposedBy)
        songKey = KeyDetector().detectKey(lyrics)

        val toNotation = preferencesState.chordsNotation
        val originalModifiers = when {
            toNotation == originalSongNotation && chordsTransposerManager.transposedBy == 0 -> true
            else -> false
        }
        ChordsRenderer(toNotation, songKey, preferencesState.forceSharpNotes).formatLyrics(
            lyrics,
            originalModifiers,
        )
        transposedLyrics = lyrics

        arrangeLyrics()
    }

    private fun arrangeLyrics() {
        val lyrics = LyricsCloner().cloneLyrics(transposedLyrics)

        val realFontsize = windowManagerService.dp2px(lyricsThemeService.fontsize)
        val screenWRelative = screenW.toFloat() / realFontsize
        val typeface = lyricsThemeService.fontTypeface.typeface
        val displayStyle = lyricsThemeService.displayStyle

        val lyricsInflater = LyricsInflater(typeface, realFontsize)
        val inflatedLyrics = lyricsInflater.inflateLyrics(lyrics)

        val lyricsWrapper = LyricsArranger(
            displayStyle,
            screenWRelative,
            lyricsInflater.lengthMapper,
            preferencesState.horizontalScroll
        )
        arrangedLyrics = lyricsWrapper.arrangeModel(inflatedLyrics)
    }

    fun onPreviewSizeChange(screenW: Int) {
        this.screenW = screenW
        arrangeLyrics()
    }

    fun onFontSizeChanged() {
        arrangeLyrics()
    }

    fun onTransposed() {
        transposeAndFormatLyrics()
    }

    val isTransposed: Boolean get() = chordsTransposerManager.isTransposed
    val transposedByDisplayName: String get() = chordsTransposerManager.transposedByDisplayName

    fun onTransposeEvent(semitones: Int) {
        chordsTransposerManager.onTransposeEvent(semitones)
    }

    fun onTransposeResetEvent() {
        chordsTransposerManager.onTransposeResetEvent()
    }

    fun loadEphemeralLyrics(
        content: String,
        screenW: Int,
        srcNotation: ChordsNotation,
    ): LyricsModel {
        val trimWhitespaces: Boolean = preferencesState.trimWhitespaces
        val toNotation: ChordsNotation = preferencesState.chordsNotation
        val forceSharpNotes: Boolean = preferencesState.forceSharpNotes
        val fontsize: Float = preferencesState.fontsize
        val typeface: Typeface = preferencesState.fontTypeface.typeface
        val displayStyle: DisplayStyle = preferencesState.chordsDisplayStyle
        val horizontalScroll: Boolean = preferencesState.horizontalScroll

        // Extract lyrics and chords
        val lyricsExtractor = LyricsExtractor(trimWhitespaces = trimWhitespaces)
        val loadedLyrics = lyricsExtractor.parseLyrics(content)
        // Parse chords
        val unknownChords = ChordParser(srcNotation).parseAndFillChords(loadedLyrics)
        unknownChords.takeIf { it.isNotEmpty() }?.let {
            logger.warn("Unknown chords: ${unknownChords.joinToString(", ")}")
        }

        // Format chords
        val songKey = KeyDetector().detectKey(loadedLyrics)
        val originalModifiers = toNotation == originalSongNotation
        ChordsRenderer(toNotation, songKey, forceSharpNotes).formatLyrics(
            loadedLyrics,
            originalModifiers,
        )

        // Inflate text
        val realFontsize = windowManagerService.dp2px(fontsize)
        val screenWRelative = screenW.toFloat() / realFontsize
        val lyricsInflater = LyricsInflater(typeface, realFontsize)
        val inflatedLyrics = lyricsInflater.inflateLyrics(loadedLyrics)

        // Arrange lines
        val lyricsWrapper = LyricsArranger(
            displayStyle,
            screenWRelative,
            lyricsInflater.lengthMapper,
            horizontalScroll,
        )
        return lyricsWrapper.arrangeModel(inflatedLyrics)
    }

}