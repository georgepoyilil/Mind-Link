package com.mindlink.journal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.ui.theme.MindIndigo
import com.mindlink.journal.ui.theme.MindTextPrimary
import com.mindlink.journal.ui.theme.PastelLavender

/**
 * High-fidelity Rich Text Renderer.
 * Parses Markdown / MS Word-like formatting tokens into native Compose AnnotatedStrings
 * without displaying raw markdown punctuation (*, **, ~~, ###, >).
 */
object RichTextRenderer {

    fun parseMarkdown(
        rawText: String,
        baseColor: Color = Color.Unspecified,
        baseFontSizeSp: Float = 16f
    ): AnnotatedString {
        if (rawText.isBlank()) return AnnotatedString("")

        return buildAnnotatedString {
            val lines = rawText.lines()
            lines.forEachIndexed { index, line ->
                val trimmed = line.trimStart()

                when {
                    trimmed.startsWith("### ") -> {
                        val headingText = trimmed.removePrefix("### ").trim()
                        val start = length
                        append(headingText)
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = (baseFontSizeSp * 1.15f).sp,
                                color = PastelLavender
                            ),
                            start,
                            length
                        )
                    }
                    trimmed.startsWith("## ") -> {
                        val headingText = trimmed.removePrefix("## ").trim()
                        val start = length
                        append(headingText)
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = (baseFontSizeSp * 1.3f).sp,
                                color = PastelLavender
                            ),
                            start,
                            length
                        )
                    }
                    trimmed.startsWith("# ") -> {
                        val headingText = trimmed.removePrefix("# ").trim()
                        val start = length
                        append(headingText)
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = (baseFontSizeSp * 1.45f).sp,
                                color = PastelLavender
                            ),
                            start,
                            length
                        )
                    }
                    trimmed.startsWith("> ") -> {
                        val quoteText = trimmed.removePrefix("> ").trim()
                        val start = length
                        append("“ ")
                        append(parseInlineStyles(quoteText, baseColor))
                        append(" ”")
                        addStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = if (baseColor != Color.Unspecified) baseColor.copy(alpha = 0.85f) else Color.Unspecified
                            ),
                            start,
                            length
                        )
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        val bulletText = trimmed.substring(2).trim()
                        append("  •  ")
                        append(parseInlineStyles(bulletText, baseColor))
                    }
                    trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                        val match = Regex("^(\\d+\\.)\\s+(.*)").find(trimmed)
                        if (match != null) {
                            val num = match.groupValues[1]
                            val text = match.groupValues[2]
                            append("  $num ")
                            append(parseInlineStyles(text, baseColor))
                        } else {
                            append(parseInlineStyles(line, baseColor))
                        }
                    }
                    else -> {
                        append(parseInlineStyles(line, baseColor))
                    }
                }

                if (index < lines.lastIndex) {
                    append("\n")
                }
            }
        }
    }

    private fun parseInlineStyles(lineText: String, baseColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = lineText.length

            while (i < len) {
                when {
                    // Strikethrough: ~~text~~
                    lineText.startsWith("~~", i) -> {
                        val close = lineText.indexOf("~~", i + 2)
                        if (close != -1) {
                            val inner = lineText.substring(i + 2, close)
                            val start = length
                            append(inner)
                            addStyle(
                                SpanStyle(textDecoration = TextDecoration.LineThrough),
                                start,
                                length
                            )
                            i = close + 2
                            continue
                        }
                    }
                    // Underline: __text__
                    lineText.startsWith("__", i) -> {
                        val close = lineText.indexOf("__", i + 2)
                        if (close != -1) {
                            val inner = lineText.substring(i + 2, close)
                            val start = length
                            append(inner)
                            addStyle(
                                SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold),
                                start,
                                length
                            )
                            i = close + 2
                            continue
                        }
                    }
                    // Bold: **text**
                    lineText.startsWith("**", i) -> {
                        val close = lineText.indexOf("**", i + 2)
                        if (close != -1) {
                            val inner = lineText.substring(i + 2, close)
                            val start = length
                            append(inner)
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                start,
                                length
                            )
                            i = close + 2
                            continue
                        }
                    }
                    // Italic: *text* (single asterisk)
                    lineText.startsWith("*", i) && (i + 1 < len && lineText[i + 1] != ' ') -> {
                        val close = lineText.indexOf("*", i + 1)
                        if (close != -1 && close > i + 1) {
                            val inner = lineText.substring(i + 1, close)
                            val start = length
                            append(inner)
                            addStyle(
                                SpanStyle(fontStyle = FontStyle.Italic),
                                start,
                                length
                            )
                            i = close + 1
                            continue
                        }
                    }
                    // Inline Code: `code`
                    lineText.startsWith("`", i) -> {
                        val close = lineText.indexOf("`", i + 1)
                        if (close != -1) {
                            val inner = lineText.substring(i + 1, close)
                            val start = length
                            append(inner)
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    color = PastelLavender,
                                    fontSize = 13.5.sp
                                ),
                                start,
                                length
                            )
                            i = close + 1
                            continue
                        }
                    }
                }

                // Regular character
                append(lineText[i])
                i++
            }
        }
    }
}

@Composable
fun RichTextView(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MindTextPrimary
) {
    val annotated = RichTextRenderer.parseMarkdown(
        rawText = text,
        baseColor = color,
        baseFontSizeSp = style.fontSize.value
    )
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color
    )
}
