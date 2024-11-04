package mwongela.songbook.songpreview.quickmenu

import android.annotation.SuppressLint
import android.view.View
import android.widget.Button
import android.widget.TextView
import mwongela.songbook.R
import mwongela.songbook.chords.loader.LyricsLoader
import mwongela.songbook.chords.model.convertToSharp
import mwongela.songbook.chords.syntax.ChordNames
import mwongela.songbook.chords.syntax.MajorKey
import mwongela.songbook.info.UiResourceService
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.settings.preferences.PreferencesState

// Singleton
class QuickMenuTranspose(
    lyricsLoader: LazyInject<LyricsLoader> = appFactory.lyricsLoader,
    uiResourceService: LazyInject<UiResourceService> = appFactory.uiResourceService,
    preferencesState: LazyInject<PreferencesState> = appFactory.preferencesState,
) {
    private val lyricsLoader by LazyExtractor(lyricsLoader)
    private val uiResourceService by LazyExtractor(uiResourceService)
    private val preferencesState by LazyExtractor(preferencesState)

    var isVisible = false
        set(visible) {
            field = visible
            quickMenuView?.let { quickMenuView ->
                if (visible) {
                    quickMenuView.visibility = View.VISIBLE
                    updateTranspositionText()
                } else {
                    quickMenuView.visibility = View.GONE
                }
            }
        }
    private var quickMenuView: View? = null
    private var transposedByLabel: TextView? = null

    /**
     * @return is feature active - has impact on song preview (panel may be hidden)
     */
    val isFeatureActive: Boolean
        get() = lyricsLoader.isTransposed

    fun setQuickMenuView(quickMenuView: View) {
        this.quickMenuView = quickMenuView

        transposedByLabel = quickMenuView.findViewById(R.id.transposedByLabel)

        val transposeM5Button = quickMenuView.findViewById<Button>(R.id.transposeM5Button)
        transposeM5Button.setOnClickListener {
            lyricsLoader.onTransposeEvent(-5)
        }

        val transposeM1Button = quickMenuView.findViewById<Button>(R.id.transposeM1Button)
        transposeM1Button.setOnClickListener {
            lyricsLoader.onTransposeEvent(-1)
        }

        val transpose0Button = quickMenuView.findViewById<Button>(R.id.transpose0Button)
        transpose0Button.setOnClickListener {
            lyricsLoader.onTransposeResetEvent()
        }

        val transposeP1Button = quickMenuView.findViewById<Button>(R.id.transposeP1Button)
        transposeP1Button.setOnClickListener {
            lyricsLoader.onTransposeEvent(+1)
        }

        val transposeP5Button = quickMenuView.findViewById<Button>(R.id.transposeP5Button)
        transposeP5Button.setOnClickListener {
            lyricsLoader.onTransposeEvent(+5)
        }

        updateTranspositionText()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTranspositionText() {
        val semitonesDisplayName = lyricsLoader.transposedByDisplayName
        val transposedByText =
            uiResourceService.resString(R.string.transposed_by_semitones, semitonesDisplayName)

        val detectedKeyStr = buildSongKeyName(lyricsLoader.songKey)
        val detectedKeyText =
            uiResourceService.resString(R.string.song_detected_key, detectedKeyStr)

        transposedByLabel?.text = "$transposedByText\n$detectedKeyText"
    }

    private fun buildSongKeyName(key: MajorKey): String {
        val notation = preferencesState.chordsNotation
        val forceSharps = preferencesState.forceSharpNotes
        val baseMajorNote = when (forceSharps) {
            true -> convertToSharp(key.baseMajorNote, notation)
            false -> key.baseMajorNote
        }
        val baseMinorNote = when (forceSharps) {
            true -> convertToSharp(key.baseMinorNote, notation)
            false -> key.baseMinorNote
        }
        val majorNoteName = ChordNames.formatNoteName(notation, baseMajorNote, false)
        val minorNoteName = ChordNames.formatNoteName(notation, baseMinorNote, true)
        return "$majorNoteName / $minorNoteName"
    }

    fun onTransposedEvent() {
        if (isVisible) {
            updateTranspositionText()
        }
    }

}
