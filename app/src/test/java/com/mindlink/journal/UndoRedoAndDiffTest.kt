package com.mindlink.journal

import androidx.compose.ui.text.input.TextFieldValue
import com.mindlink.journal.ui.components.RichTextRenderer
import com.mindlink.journal.ui.editor.UndoRedoManager
import com.mindlink.journal.ui.history.DiffOperation
import com.mindlink.journal.ui.history.DiffUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoAndDiffTest {

    @Test
    fun testUndoRedoStackOperations() {
        val manager = UndoRedoManager()
        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)

        val state0 = TextFieldValue("Initial text")
        manager.record(state0, force = true)

        val state1 = TextFieldValue("Initial text with addition")
        manager.record(state1, force = true)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        val state2 = TextFieldValue("Initial text with addition and bold **formatting**")
        manager.record(state2, force = true)

        // Undo 1 step
        val undone = manager.undo(state2)
        assertNotNull(undone)
        assertEquals("Initial text with addition", undone?.text)
        assertTrue(manager.canUndo)
        assertTrue(manager.canRedo)

        // Undo another step
        val undone2 = manager.undo(undone!!)
        assertNotNull(undone2)
        assertEquals("Initial text", undone2?.text)
        assertFalse(manager.canUndo)
        assertTrue(manager.canRedo)

        // Redo 1 step
        val redone = manager.redo(undone2!!)
        assertNotNull(redone)
        assertEquals("Initial text with addition", redone?.text)
        assertTrue(manager.canUndo)
        assertTrue(manager.canRedo)

        // Redo back to state2
        val redone2 = manager.redo(redone!!)
        assertNotNull(redone2)
        assertEquals("Initial text with addition and bold **formatting**", redone2?.text)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
    }

    @Test
    fun testDiffComputationAdditionsAndDeletions() {
        val oldText = """
            Line 1: Meeting Notes
            Line 2: Action items to complete
            Line 3: Wrap up
        """.trimIndent()

        val newText = """
            Line 1: Meeting Notes
            Line 2: Action items to complete today
            Line 3: Wrap up
            Line 4: Next steps
        """.trimIndent()

        val diff = DiffUtils.computeLineDiff(oldText, newText)

        // There should be additions and deletions identified
        assertTrue(diff.additions > 0)
        assertTrue(diff.lines.isNotEmpty())

        val addedLines = diff.lines.filter { it.operation == DiffOperation.ADDED }
        val deletedLines = diff.lines.filter { it.operation == DiffOperation.DELETED }
        val unchangedLines = diff.lines.filter { it.operation == DiffOperation.UNCHANGED }

        assertTrue(deletedLines.isNotEmpty())
        assertTrue(addedLines.any { it.text.contains("Line 4: Next steps") })
        assertTrue(unchangedLines.any { it.text.contains("Line 1: Meeting Notes") })
    }

    @Test
    fun testRichTextRendererStripsSymbolsAndAppliesStyles() {
        val markdown = """
            # Project Architecture
            ### Detailed Overview
            This is **bold text** and this is *italic text*.
            Here is ~~strikethrough~~ and __underlined__ words.
            - Bullet point 1
            - Bullet point 2
        """.trimIndent()

        val annotated = RichTextRenderer.parseMarkdown(markdown)
        val plainText = annotated.text

        // Punctuation like ** and ~~ should NOT be in the rendered text!
        assertFalse("Rendered text should not contain ** symbols", plainText.contains("**"))
        assertFalse("Rendered text should not contain ~~ symbols", plainText.contains("~~"))
        assertFalse("Rendered text should not contain __ symbols", plainText.contains("__"))
        assertFalse("Rendered text should not contain ### prefix", plainText.contains("###"))
        assertTrue("Rendered text should contain bold text", plainText.contains("bold text"))
        assertTrue("Rendered text should contain strikethrough", plainText.contains("strikethrough"))
        assertTrue("Rendered text should contain underlined", plainText.contains("underlined"))
        assertTrue("Rendered text should format bullets with dot", plainText.contains("•"))
    }
}
