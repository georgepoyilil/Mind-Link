package com.mindlink.journal.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mindlink.journal.ui.theme.PastelLavender
import com.mindlink.journal.ui.theme.PastelMint

class MarkdownVisualTransformation(
    private val delimiterAlpha: Float = 0.28f,
    private val baseFontSizeSp: Float = 16f
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val delimiterStyle = SpanStyle(
            color = Color.Gray.copy(alpha = delimiterAlpha),
            fontSize = (baseFontSizeSp * 0.75f).sp
        )

        val transformed = buildAnnotatedString {
            append(raw)

            // 1. Process block line prefixes (###, ##, #, >, - )
            var lineStart = 0
            while (lineStart < raw.length) {
                var lineEnd = raw.indexOf('\n', lineStart)
                if (lineEnd == -1) lineEnd = raw.length

                val line = raw.substring(lineStart, lineEnd)
                val trimmed = line.trimStart()
                val leadingWhitespace = line.length - trimmed.length

                when {
                    trimmed.startsWith("### ") -> {
                        // Mark delimiter
                        addStyle(delimiterStyle, lineStart + leadingWhitespace, lineStart + leadingWhitespace + 4)
                        // Mark heading
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = PastelLavender,
                                fontSize = (baseFontSizeSp * 1.15f).sp
                            ),
                            lineStart + leadingWhitespace + 4,
                            lineEnd
                        )
                    }
                    trimmed.startsWith("## ") -> {
                        addStyle(delimiterStyle, lineStart + leadingWhitespace, lineStart + leadingWhitespace + 3)
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = PastelLavender,
                                fontSize = (baseFontSizeSp * 1.25f).sp
                            ),
                            lineStart + leadingWhitespace + 3,
                            lineEnd
                        )
                    }
                    trimmed.startsWith("# ") -> {
                        addStyle(delimiterStyle, lineStart + leadingWhitespace, lineStart + leadingWhitespace + 2)
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                color = PastelLavender,
                                fontSize = (baseFontSizeSp * 1.35f).sp
                            ),
                            lineStart + leadingWhitespace + 2,
                            lineEnd
                        )
                    }
                    trimmed.startsWith("> ") -> {
                        addStyle(delimiterStyle, lineStart + leadingWhitespace, lineStart + leadingWhitespace + 2)
                        addStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = Color.White.copy(alpha = 0.85f)
                            ),
                            lineStart + leadingWhitespace + 2,
                            lineEnd
                        )
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        addStyle(
                            SpanStyle(color = PastelMint, fontWeight = FontWeight.Bold),
                            lineStart + leadingWhitespace,
                            lineStart + leadingWhitespace + 2
                        )
                    }
                }

                lineStart = lineEnd + 1
            }

            // 2. Process inline bold (**text**)
            highlightDelimited(raw, "**", SpanStyle(fontWeight = FontWeight.Bold), delimiterStyle)

            // 3. Process inline underline (__text__)
            highlightDelimited(raw, "__", SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold), delimiterStyle)

            // 4. Process inline strikethrough (~~text~~)
            highlightDelimited(raw, "~~", SpanStyle(textDecoration = TextDecoration.LineThrough), delimiterStyle)

            // 5. Process inline italic (*text*)
            highlightSingleAsteriskItalic(raw, delimiterStyle)
        }

        return TransformedText(transformed, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.highlightDelimited(
        raw: String,
        delim: String,
        contentStyle: SpanStyle,
        delimiterStyle: SpanStyle
    ) {
        var cursor = 0
        val dLen = delim.length
        while (cursor < raw.length) {
            val start = raw.indexOf(delim, cursor)
            if (start == -1) break
            val end = raw.indexOf(delim, start + dLen)
            if (end == -1) break

            // Style open delimiter
            addStyle(delimiterStyle, start, start + dLen)
            // Style content
            addStyle(contentStyle, start + dLen, end)
            // Style close delimiter
            addStyle(delimiterStyle, end, end + dLen)

            cursor = end + dLen
        }
    }

    private fun AnnotatedString.Builder.highlightSingleAsteriskItalic(
        raw: String,
        delimiterStyle: SpanStyle
    ) {
        var cursor = 0
        while (cursor < raw.length) {
            val start = raw.indexOf('*', cursor)
            if (start == -1) break
            // If it's part of **, skip it
            if ((start > 0 && raw[start - 1] == '*') || (start + 1 < raw.length && raw[start + 1] == '*')) {
                cursor = start + 1
                continue
            }
            val end = raw.indexOf('*', start + 1)
            if (end == -1) break
            if ((end + 1 < raw.length && raw[end + 1] == '*') || (end > 0 && raw[end - 1] == '*')) {
                cursor = end + 1
                continue
            }
            if (end > start + 1) {
                addStyle(delimiterStyle, start, start + 1)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start + 1, end)
                addStyle(delimiterStyle, end, end + 1)
            }
            cursor = end + 1
        }
    }
}
