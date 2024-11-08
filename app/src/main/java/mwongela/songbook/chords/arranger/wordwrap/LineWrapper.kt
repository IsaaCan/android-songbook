package mwongela.songbook.chords.arranger.wordwrap

import mwongela.songbook.chords.render.TypefaceLengthMapper

class LineWrapper(
    private val screenWRelative: Float,
    private val lengthMapper: TypefaceLengthMapper,
    private val horizontalScroll: Boolean = false,
) {

    fun wrapLine(line: Line): List<Line> {
        if (line.end() <= screenWRelative || horizontalScroll) {
            return listOf(line).nonEmptyLines(line.primalIndex)
        }

        val words: List<Word> = line.fragments.toWords(lengthMapper)

        val wrappedWords = wrapWords(words)

        return wrappedWords.toLines(line.primalIndex)
            .clearBlanksOnEnd()
            .addLineWrappers(screenWRelative, lengthMapper)
            .nonEmptyLines(line.primalIndex)
    }

    internal fun wrapWords(words: List<Word>): List<List<Word>> {
        if (words.isEmpty())
            return emptyList()
        if (words.end() <= screenWRelative)
            return listOf(words)

        val (before, middle, after) = words.splitByXLimit(screenWRelative)

        val toWrap = middle + after
        val moveBy = toWrap.firstOrNull()?.x ?: 0f

        if (toWrap.isEmpty())
            return listOf(words)
        // very long word, cant be wrapped
        if (moveBy <= 0f)
            return listOf(words)

        alignToLeft(toWrap, moveBy)

        return listOf(before) + wrapWords(toWrap)
    }

}
