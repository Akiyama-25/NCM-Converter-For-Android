package com.example.ncmconverter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletItem(val indent: Int, val text: String) : MarkdownBlock
    data class OrderedItem(val indent: Int, val number: String, val text: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

internal object MarkdownParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val hrRegex = Regex("^(\\*{3,}|-{3,}|_{3,})\\s*$")
    private val bulletRegex = Regex("^(\\s*)([-*•·])\\s+(.*)$")
    private val orderedRegex = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
    private val tableSepRegex = Regex("^\\|?(\\s*:?-{2,}:?\\s*\\|)+\\s*:?-{2,}:?\\s*\\|?$")

    fun parse(markdown: String): List<MarkdownBlock> {
        val lines = markdown.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 1. Fenced Code Block
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // consume closing ```
                blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
                continue
            }

            // 2. Horizontal Rule
            if (hrRegex.matches(trimmed)) {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // 3. Heading
            val headingMatch = headingRegex.matchEntire(trimmed)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].trim()
                blocks.add(MarkdownBlock.Heading(level, text))
                i++
                continue
            }

            // 4. Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trimStart())
                    i++
                }
                blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString("\n")))
                continue
            }

            // 5. Markdown Table
            if (trimmed.contains("|") && i + 1 < lines.size && tableSepRegex.matches(lines[i + 1].trim())) {
                val headers = parseTableRow(trimmed)
                i += 2 // skip header and separator row
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().let { it.isNotEmpty() && it.contains("|") }) {
                    rows.add(parseTableRow(lines[i].trim()))
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
                continue
            }

            // 6. Bullet List Item
            val bulletMatch = bulletRegex.matchEntire(rawLine)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1].length / 2
                val text = bulletMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.BulletItem(indent, text))
                i++
                continue
            }

            // 7. Ordered List Item
            val orderedMatch = orderedRegex.matchEntire(rawLine)
            if (orderedMatch != null) {
                val indent = orderedMatch.groupValues[1].length / 2
                val number = orderedMatch.groupValues[2]
                val text = orderedMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.OrderedItem(indent, number, text))
                i++
                continue
            }

            // 8. Paragraph (collect consecutive plain lines)
            val paraLines = mutableListOf<String>()
            while (i < lines.size) {
                val curRaw = lines[i]
                val curTrim = curRaw.trim()
                if (curTrim.isEmpty() ||
                    curTrim.startsWith("```") ||
                    hrRegex.matches(curTrim) ||
                    headingRegex.matches(curTrim) ||
                    curTrim.startsWith(">") ||
                    bulletRegex.matches(curRaw) ||
                    orderedRegex.matches(curRaw) ||
                    (curTrim.contains("|") && i + 1 < lines.size && tableSepRegex.matches(lines[i + 1].trim()))
                ) {
                    break
                }
                paraLines.add(curTrim)
                i++
            }
            if (paraLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
            } else {
                i++
            }
        }

        return blocks
    }

    private fun parseTableRow(line: String): List<String> {
        return line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }
    }
}

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val colorScheme = MaterialTheme.colorScheme

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        MarkdownHeading(level = block.level, text = block.text)
                    }
                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = buildInlineMarkdown(block.text, colorScheme),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            ),
                            color = colorScheme.onSurface
                        )
                    }
                    is MarkdownBlock.BulletItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (block.indent * 14).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(if (block.indent == 0) 6.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (block.indent == 0) colorScheme.primary
                                        else colorScheme.secondary
                                    )
                            )
                            Text(
                                text = buildInlineMarkdown(block.text, colorScheme),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is MarkdownBlock.OrderedItem -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (block.indent * 14).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colorScheme.primaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                            ) {
                                Text(
                                    text = block.number,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = buildInlineMarkdown(block.text, colorScheme),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is MarkdownBlock.BlockQuote -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(colorScheme.primary)
                                )
                                Text(
                                    text = buildInlineMarkdown(block.text, colorScheme),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    ),
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                    is MarkdownBlock.CodeBlock -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (block.language.isNotEmpty()) {
                                    Text(
                                        text = block.language.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    Text(
                                        text = block.code,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    is MarkdownBlock.Table -> {
                        MarkdownTable(headers = block.headers, rows = block.rows)
                    }
                    is MarkdownBlock.Divider -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeading(level: Int, text: String) {
    val colorScheme = MaterialTheme.colorScheme
    val (fontSize, fontWeight, color, topPad) = when (level) {
        1 -> HeadingSpec(19.sp, FontWeight.Bold, colorScheme.primary, 4.dp)
        2 -> HeadingSpec(16.sp, FontWeight.SemiBold, colorScheme.primary, 6.dp)
        3 -> HeadingSpec(14.sp, FontWeight.SemiBold, colorScheme.secondary, 4.dp)
        else -> HeadingSpec(13.sp, FontWeight.Medium, colorScheme.onSurface, 2.dp)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = topPad)) {
        Text(
            text = buildInlineMarkdown(text, colorScheme),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            lineHeight = (fontSize.value + 5).sp
        )
        if (level == 1) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 1.5.dp,
                color = colorScheme.primary.copy(alpha = 0.25f)
            )
        }
    }
}

private data class HeadingSpec(
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val fontWeight: FontWeight,
    val color: Color,
    val topPadding: androidx.compose.ui.unit.Dp
)

@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                headers.forEach { h ->
                    Text(
                        text = buildInlineMarkdown(h, colorScheme),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            rows.forEach { row ->
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    headers.indices.forEach { colIdx ->
                        val cell = row.getOrNull(colIdx) ?: ""
                        Text(
                            text = buildInlineMarkdown(cell, colorScheme),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private val inlineTokenRegex = Regex(
    "(`[^`]+`)|(\\*\\*\\*[^*]+\\*\\*\\*)|(\\*\\*[^*]+\\*\\*)|(__[^_]+__)|(\\*[^*\\n]+\\*)|(_[^_\\n]+_)|(~~[^~]+~~)|(\\[([^\\]]+)\\]\\(([^)]+)\\))|(https?://[^\\s)]+)"
)

@Composable
internal fun buildInlineMarkdown(
    text: String,
    colorScheme: androidx.compose.material3.ColorScheme
): AnnotatedString {
    return remember(text, colorScheme) {
        parseInlineMarkdown(text, colorScheme)
    }
}

internal fun parseInlineMarkdown(
    text: String,
    colorScheme: androidx.compose.material3.ColorScheme
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in inlineTokenRegex.findAll(text)) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
            // Inline code `code`
            match.groups[1] != null -> {
                val content = token.substring(1, token.length - 1)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary,
                        background = colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    append(" $content ")
                }
            }
            // Bold + Italic ***text***
            match.groups[2] != null -> {
                val content = token.substring(3, token.length - 3)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append(content)
                }
            }
            // Bold **text** or __text__
            match.groups[3] != null || match.groups[4] != null -> {
                val content = token.substring(2, token.length - 2)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colorScheme.onSurface)) {
                    append(content)
                }
            }
            // Italic *text* or _text_
            match.groups[5] != null || match.groups[6] != null -> {
                val content = token.substring(1, token.length - 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(content)
                }
            }
            // Strikethrough ~~text~~
            match.groups[7] != null -> {
                val content = token.substring(2, token.length - 2)
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(content)
                }
            }
            // Markdown Link [label](url)
            match.groups[8] != null -> {
                val label = match.groups[9]?.value ?: token
                val url = match.groups[10]?.value ?: ""
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(label)
                }
            }
            // Bare URL https://...
            match.groups[11] != null -> {
                withLink(
                    LinkAnnotation.Url(
                        url = token,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(token)
                }
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}
