package mwongela.songbook.chords.arranger.wordwrap

import mwongela.songbook.chords.render.TypefaceLengthMapper

class DoubleLineWrapper(
    private val screenWRelative: Float,
    private val lengthMapper: TypefaceLengthMapper,
    private val horizontalScroll: Boolean = false,
) {
    private val singleLineWrapper = LineWrapper(screenWRelative, lengthMapper)

    fun wrapDoubleLine(chords: Line, texts: Line): List<Line> {
        if (horizontalScroll || (texts.end() <= screenWRelative && chords.end() <= screenWRelative)) {
            return listOf(chords, texts).nonEmptyLines(texts.primalIndex)
        }

        val chordWords: List<Word> = chords.fragments.toWords(lengthMapper)
        val textWords: List<Word> = texts.fragments.toWords(lengthMapper)

        val (wrappedChordWords, wrappedTextWords) = wrapDoubleWords(chordWords, textWords)

        val chordLines: List<Line> = wrappedChordWords.toLines(texts.primalIndex)
            .clearBlanksOnEnd()
            .addLineWrappers(screenWRelative, lengthMapper)
        val textLines: List<Line> = wrappedTextWords.toLines(texts.primalIndex)
            .clearBlanksOnEnd()
            .addLineWrappers(screenWRelative, lengthMapper)
        val lines = chordLines zipUneven textLines

        return lines.nonEmptyLines(texts.primalIndex)
    }

    private fun wrapDoubleWords(
        chords: List<Word>,
        texts: List<Word>
    ): Pair<List<List<Word>>, List<List<Word>>> {
        when {
            chords.end() <= screenWRelative && texts.end() <= screenWRelative -> {
                return listOf(chords) to listOf(texts)
            }
            chords.end() <= screenWRelative -> {
                return wrapSingleWords(chords) to wrapSingleWords(texts)
            }
            texts.end() <= screenWRelative -> {
                return wrapSingleWords(chords) to wrapSingleWords(texts)
            }
        }

        val (beforeC, middleC, afterC) = chords.splitByXLimit(screenWRelative)
        val (beforeT, middleT, afterT) = texts.splitByXLimit(screenWRelative)

        val toWrapC = middleC + afterC
        val toWrapT = middleT + afterT

        val moveByC = toWrapC.firstOrNull()?.x ?: 0f
        val moveByT = toWrapT.firstOrNull()?.x ?: 0f

        // very long word, cant be wrapped
        if (moveByC <= 0f)
            return listOf(chords) to wrapSingleWords(texts)
        if (moveByT <= 0f)
            return wrapSingleWords(chords) to listOf(texts)

        if (toWrapC.isEmpty())
            return listOf(chords) to wrapSingleWords(texts)
        if (toWrapT.isEmpty())
            return wrapSingleWords(chords) to listOf(texts)

        val moveBy = listOf(moveByC, moveByT).minOrNull() ?: 0f

        if (moveBy <= 0f)
            return wrapSingleWords(chords) to wrapSingleWords(texts)

        alignToLeft(toWrapC, moveBy)
        alignToLeft(toWrapT, moveBy)

        val (nextC, nextT) = wrapDoubleWords(toWrapC, toWrapT)

        val allWrappedChords = listOf(beforeC) + nextC
        val allWrappedTexts = listOf(beforeT) + nextT

        return allWrappedChords to allWrappedTexts
    }

    private fun wrapSingleWords(words: List<Word>): List<List<Word>> {
        return singleLineWrapper.wrapWords(words)
    }

}
