package com.mindlink.journal.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import java.util.ArrayDeque

class UndoRedoManager(
    private val maxHistory: Int = 50
) {
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastRecordedText: String? = null
    private var lastRecordedTime: Long = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Records a text state into undo history.
     * Batches rapid keystrokes within 800ms, but captures words, whitespace, lines,
     * pastes, and formatting actions immediately.
     */
    fun record(current: TextFieldValue, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val prev = lastRecordedText

        if (prev == null) {
            lastRecordedText = current.text
            lastRecordedTime = now
            return
        }

        if (prev == current.text) {
            return
        }

        val isSignificant = force ||
            (now - lastRecordedTime > 800L) ||
            current.text.endsWith(" ") ||
            current.text.endsWith("\n") ||
            kotlin.math.abs(current.text.length - prev.length) > 1

        if (isSignificant) {
            undoStack.addLast(TextFieldValue(prev))
            if (undoStack.size > maxHistory) {
                undoStack.removeFirst()
            }
            redoStack.clear()
            lastRecordedText = current.text
            lastRecordedTime = now
        }
    }

    /**
     * Undo to the previous state.
     */
    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        lastRecordedText = previous.text
        lastRecordedTime = System.currentTimeMillis()
        return previous
    }

    /**
     * Redo to the next state.
     */
    fun redo(current: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        lastRecordedText = next.text
        lastRecordedTime = System.currentTimeMillis()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastRecordedText = null
        lastRecordedTime = 0L
    }
}
