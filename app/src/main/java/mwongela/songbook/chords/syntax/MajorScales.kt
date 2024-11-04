package mwongela.songbook.chords.syntax

import mwongela.songbook.chords.model.Note
import mwongela.songbook.chords.model.Note.A
import mwongela.songbook.chords.model.Note.A_FLAT
import mwongela.songbook.chords.model.Note.A_SHARP
import mwongela.songbook.chords.model.Note.B
import mwongela.songbook.chords.model.Note.B_FLAT
import mwongela.songbook.chords.model.Note.C
import mwongela.songbook.chords.model.Note.C_SHARP
import mwongela.songbook.chords.model.Note.D
import mwongela.songbook.chords.model.Note.D_FLAT
import mwongela.songbook.chords.model.Note.D_SHARP
import mwongela.songbook.chords.model.Note.E
import mwongela.songbook.chords.model.Note.E_FLAT
import mwongela.songbook.chords.model.Note.E_SHARP
import mwongela.songbook.chords.model.Note.F
import mwongela.songbook.chords.model.Note.F_SHARP
import mwongela.songbook.chords.model.Note.G
import mwongela.songbook.chords.model.Note.G_FLAT
import mwongela.songbook.chords.model.Note.G_SHARP

enum class MajorKey(
    val baseMajorNote: Note,
    val baseMinorNote: Note, // minor scale counterpart
    val notes: Set<Note>,
    val sharpness: Int, // number of sharp signs
) {

    C_MAJOR(C, A, setOf(C, D, E, F, G, A, B), 0), // C Major / A minor
    G_MAJOR(G, E, setOf(C, D, E, F_SHARP, G, A, B), 1), // G Major
    D_MAJOR(D, B, setOf(C_SHARP, D, E, F_SHARP, G, A, B), 2), // D Major
    A_MAJOR(A, F_SHARP, setOf(C_SHARP, D, E, F_SHARP, G_SHARP, A, B), 3), // A Major
    E_MAJOR(E, C_SHARP, setOf(C_SHARP, D_SHARP, E, F_SHARP, G_SHARP, A, B), 4), // E Major
    B_MAJOR(B, G_SHARP, setOf(C_SHARP, D_SHARP, E, F_SHARP, G_SHARP, A_SHARP, B), 5), // B Major
    F_SHARP_MAJOR(F_SHARP, D_SHARP, setOf(C_SHARP, D_SHARP, E_SHARP, F_SHARP, G_SHARP, A_SHARP, B), 6), // F# (Gb) Major
    //G_FLAT_MAJOR(G_FLAT, E_FLAT, setOf(D_FLAT, E_FLAT, F, G_FLAT, A_FLAT, B_FLAT, C_FLAT), -6), // Gb (F#) Major
    D_FLAT_MAJOR(D_FLAT, B_FLAT, setOf(C, D_FLAT, E_FLAT, F, G_FLAT, A_FLAT, B_FLAT), -5), // Db (C#) Major
    A_FLAT_MAJOR(A_FLAT, F, setOf(C, D_FLAT, E_FLAT, F, G, A_FLAT, B_FLAT), -4), // Ab (G#) Major
    E_FLAT_MAJOR(E_FLAT, C, setOf(C, D, E_FLAT, F, G, A_FLAT, B_FLAT), -3), // Eb (D#) Major
    B_FLAT_MAJOR(B_FLAT, G, setOf(C, D, E_FLAT, F, G, A, B_FLAT), -2), // Bb (A#) Major
    F_MAJOR(F, D, setOf(C, D, E, F, G, A, B_FLAT), -1), // F Major
    ;

    val isSharpy: Boolean get() = this.sharpness > 0
    val isFlatty: Boolean get() = this.sharpness < 0

    // Tonic (I)
    fun tonic(): Int = baseMajorNote.index

    // supertonic (ii)
    fun supertonic(): Int = moveNote(baseMajorNote.index, 2)

    // mediant (iii)
    fun mediant(): Int = moveNote(baseMajorNote.index, 4)

    // Subdominant (IV)
    fun subdominant(): Int = moveNote(baseMajorNote.index, 5)

    // Dominant (V)
    fun dominant(): Int = moveNote(baseMajorNote.index, 7)

    // submediant - base minor note index (vi)
    fun submediant(): Int = moveNote(baseMajorNote.index, 9)

    // subtonic (vii0)
    fun subtonic(): Int = moveNote(baseMajorNote.index, 11)
}

fun moveNote(noteIndex: Int, semitonesOffset: Int): Int {
    return (noteIndex + semitonesOffset + 12) % 12
}
