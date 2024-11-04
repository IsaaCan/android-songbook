package mwongela.songbook.chords.diagram.guitar

import mwongela.songbook.R

enum class ChordDiagramStyle(val id: Long, val nameResId: Int) {

    Horizontal(1, R.string.chord_diagram_style_horizontal),

    Vertical(2, R.string.chord_diagram_style_vertical),

    ;

    companion object {
        val default = Vertical

        fun parseById(id: Long): ChordDiagramStyle? {
            return values().firstOrNull { v -> v.id == id }
        }

        fun mustParseById(id: Long): ChordDiagramStyle {
            return parseById(id) ?: default
        }
    }
}
