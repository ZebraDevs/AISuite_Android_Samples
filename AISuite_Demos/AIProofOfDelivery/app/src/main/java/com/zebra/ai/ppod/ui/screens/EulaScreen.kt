package com.zebra.ai.ppod.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.zebra.ai.ppod.R
import com.zebra.ai.ppod.repositories.PreferenceKeys.PREF_KEY_EULA_ACCEPTED
import com.zebra.ai.ppod.viewmodels.AppViewModel
import java.io.InputStream

/**************************************************************************************************/
private data class MarkdownToken(
    val type: TokenType,
    val start: Int,
    val end: Int,
    val groups: List<String>
)

/**************************************************************************************************/
private enum class TokenType {
    CODE_BLOCK,
    INLINE_CODE,
    LINK,
    BOLD,
    ITALIC,
    HEADING,
    LIST,
    BLOCKQUOTE
}
/**************************************************************************************************/

@Composable
fun EulaScreen(viewModel: AppViewModel) {

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotatedString = parseMarkdownToAnnotatedString()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.background,
            ).verticalScroll(rememberScrollState()),
    ) {

        Text(
            text = annotatedString,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffsetPosition ->
                        val layoutResult = textLayoutResult ?: return@detectTapGestures
                        val position = layoutResult.getOffsetForPosition(tapOffsetPosition)
                        annotatedString
                            .getStringAnnotations(start = position, end = position)
                            .firstOrNull { it.tag == "URL" }
                            ?.let { annotation ->
                                val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                                context.startActivity(intent)
                            }
                    }
                },
            onTextLayout = { result ->
                textLayoutResult = result
            },
            fontSize = 13.sp,
            color = Color.LightGray,
            lineHeight = 16.sp
        )

        Text(
            modifier = Modifier.wrapContentHeight()
                .fillMaxWidth()
                .padding(6.dp)
                .background(color=Color(0xFF84D692), shape = RoundedCornerShape(6.dp))
                .border(width = 1.dp,color = Color.White,shape = RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .padding(top=10.dp,start=26.dp,end=26.dp, bottom = 10.dp)
                .clickable(enabled = true, onClick = {
                    viewModel.preferences[PREF_KEY_EULA_ACCEPTED] = true
                    viewModel.preferences.commit()
                }),
            text = stringResource(R.string.accept_eula),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 16.sp,
                color = Color(0xFF005F0F)
            ))


    }
}

/**************************************************************************************************/
@Composable
fun parseMarkdownToAnnotatedString(): AnnotatedString {
    val context = LocalContext.current
    val inputStream: InputStream = context.assets.open("eula.md")
    val markdownContent = inputStream.bufferedReader().use { it.readText() }
    val linkRegex = """\[(.*?)]\((.*?)\)""".toRegex()
    val boldRegex = """\*\*(.*?)\*\*""".toRegex()
    val italicRegex = """\*(.*?)\*""".toRegex()
    val codeBlockRegex = """```([\s\S]*?)```""".toRegex()
    val inlineCodeRegex = """`(.*?)`""".toRegex()
    val headingRegex = """^(#{1,2})\s*(.*)""".toRegex(RegexOption.MULTILINE)
    val listRegex = """^- (.*)""".toRegex(RegexOption.MULTILINE)
    val blockquoteRegex = """^>\s+(.*)""".toRegex(RegexOption.MULTILINE)  // NEW

    val tokens = mutableListOf<MarkdownToken>()
    fun addMatches(pattern: Regex, type: TokenType, groupCount: Int) {
        pattern.findAll(markdownContent).forEach { result ->
            val matchedGroups = (1..groupCount).map { i -> result.groups[i]?.value ?: "" }
            tokens += MarkdownToken(
                type = type,
                start = result.range.first,
                end = result.range.last + 1,
                groups = matchedGroups
            )
        }
    }

    addMatches(codeBlockRegex, TokenType.CODE_BLOCK, 1)
    addMatches(inlineCodeRegex, TokenType.INLINE_CODE, 1)
    addMatches(linkRegex, TokenType.LINK, 2)
    addMatches(boldRegex, TokenType.BOLD, 1)
    addMatches(italicRegex, TokenType.ITALIC, 1)
    addMatches(headingRegex, TokenType.HEADING, 2)
    addMatches(listRegex, TokenType.LIST, 1)
    addMatches(blockquoteRegex, TokenType.BLOCKQUOTE, 1)
    tokens.sortBy { it.start }

    val builder = AnnotatedString.Builder()
    var currentIndex = 0
    fun appendGapText(upTo: Int) {
        if (currentIndex < upTo) {
            builder.append(markdownContent.substring(currentIndex, upTo))
            currentIndex = upTo
        }
    }
    for (token in tokens) {
        if (token.start < currentIndex) continue
        appendGapText(token.start)

        when (token.type) {
            TokenType.CODE_BLOCK -> {
                val codeContent = token.groups[0].trim()
                val styleStart = builder.length
                builder.append(codeContent)
                builder.addStyle(
                    SpanStyle(
                        background = Color(0xFFEFEFEF),
                        color = Color(0xFF333333),
                        fontFamily = FontFamily.Monospace
                    ),
                    styleStart,
                    builder.length
                )
            }
            TokenType.INLINE_CODE -> {
                val codeContent = token.groups[0]
                val styleStart = builder.length
                builder.append(codeContent)
                builder.addStyle(
                    SpanStyle(
                        background = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    ),
                    styleStart,
                    builder.length
                )
            }
            TokenType.LINK -> {
                val (linkText, linkUrl) = token.groups
                val styleStart = builder.length
                builder.append(linkText)
                builder.addStyle(
                    SpanStyle(
                        color = Color.Cyan,
                        textDecoration = TextDecoration.Underline
                    ),
                    styleStart,
                    builder.length
                )
                // Attach a string annotation with tag = "URL"
                builder.addStringAnnotation(
                    tag = "URL",
                    annotation = linkUrl,
                    start = styleStart,
                    end = builder.length
                )
            }
            TokenType.BOLD -> {
                val boldContent = token.groups[0]
                val styleStart = builder.length
                builder.append(boldContent)
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    styleStart,
                    builder.length
                )
            }
            TokenType.ITALIC -> {
                val italicContent = token.groups[0]
                val styleStart = builder.length
                builder.append(italicContent)
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    styleStart,
                    builder.length
                )
            }
            TokenType.HEADING -> {
                val headingLevel = token.groups[0].length // # or ##
                val headingText = token.groups[1]
                val styleStart = builder.length
                builder.append(headingText)
                builder.addStyle(
                    SpanStyle(
                        fontSize = if (headingLevel == 1) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (headingLevel == 1) Color.White else Color.White.copy(alpha=0.8f)
                    ),
                    styleStart,
                    builder.length
                )
            }
            TokenType.LIST -> {
                val listItem = token.groups[0]
                builder.append("• $listItem\n")
            }
            TokenType.BLOCKQUOTE -> {
                val quoteText = token.groups[0]
                val styleStart = builder.length
                builder.append(quoteText)
                builder.addStyle(
                    SpanStyle(
                        background = Color(0xFFE0E0E0),
                        fontStyle = FontStyle.Italic
                    ),
                    styleStart,
                    builder.length
                )
                builder.append("\n")
            }
        }
        currentIndex = token.end
    }

    appendGapText(markdownContent.length)

    return builder.toAnnotatedString()

}


