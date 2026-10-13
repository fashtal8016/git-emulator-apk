package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxAnnotation
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "private", "public", "protected", "internal", "override", "open", "abstract",
        "sealed", "data", "enum", "companion", "constructor", "init", "suspend",
        "if", "else", "when", "for", "while", "do", "break", "continue", "return",
        "is", "as", "in", "by", "lazy", "null", "true", "false", "this", "super",
        "typealias", "inline", "crossinline", "noinline", "reified", "tailrec"
    )

    private val KOTLIN_TYPES = setOf(
        "String", "Int", "Long", "Boolean", "Float", "Double", "Byte", "Short",
        "Char", "Unit", "Nothing", "Any", "List", "Map", "Set", "StateFlow",
        "Flow", "CoroutineScope", "Modifier", "Composable", "Context", "File"
    )

    fun highlight(code: String, fileExtension: String): AnnotatedString {
        return when (fileExtension.lowercase()) {
            "kt", "kts", "java" -> highlightKotlin(code)
            "xml", "html" -> highlightXml(code)
            "json" -> highlightJson(code)
            "yaml", "yml" -> highlightYaml(code)
            else -> buildAnnotatedString { append(code) }
        }
    }

    private fun highlightKotlin(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            // 1. Strings
            highlightRegex(code, "\"[^\"]*\"", SpanStyle(color = SyntaxString))
            highlightRegex(code, "\"\"\"[\\s\\S]*?\"\"\"", SpanStyle(color = SyntaxString))

            // 2. Single & multi-line comments
            highlightRegex(code, "//.*", SpanStyle(color = SyntaxComment))
            highlightRegex(code, "/\\*[\\s\\S]*?\\*/", SpanStyle(color = SyntaxComment))

            // 3. Annotations
            highlightRegex(code, "@[a-zA-Z0-9_]+", SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.SemiBold))

            // 4. Numbers
            highlightRegex(code, "\\b\\d+(\\.\\d+)?([fFL])?\\b", SpanStyle(color = SyntaxNumber))

            // 5. Functions
            highlightRegex(code, "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()", SpanStyle(color = SyntaxFunction))

            // 6. Keywords
            val wordPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b")
            val matcher = wordPattern.matcher(code)
            while (matcher.find()) {
                val word = matcher.group(1) ?: continue
                val start = matcher.start()
                val end = matcher.end()

                if (KOTLIN_KEYWORDS.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
                } else if (KOTLIN_TYPES.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Medium), start, end)
                }
            }
        }
    }

    private fun highlightXml(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            // XML tags
            highlightRegex(code, "</?[a-zA-Z0-9_:-]+", SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold))
            // Attribute names
            highlightRegex(code, "\\b[a-zA-Z0-9_:-]+(?=\\=)", SpanStyle(color = SyntaxFunction))
            // Attribute string values
            highlightRegex(code, "\"[^\"]*\"", SpanStyle(color = SyntaxString))
            // Comments
            highlightRegex(code, "<!--[\\s\\S]*?-->", SpanStyle(color = SyntaxComment))
        }
    }

    private fun highlightJson(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            // JSON keys
            highlightRegex(code, "\"[^\"]+\"(?=\\s*:)", SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.SemiBold))
            // JSON values (strings)
            highlightRegex(code, "(?<=:\\s*)\"[^\"]*\"", SpanStyle(color = SyntaxString))
            // Numbers & booleans
            highlightRegex(code, "\\b(true|false|null)\\b", SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold))
            highlightRegex(code, "\\b\\d+(\\.\\d+)?\\b", SpanStyle(color = SyntaxNumber))
        }
    }

    private fun highlightYaml(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            // Keys
            highlightRegex(code, "^[\\s]*[a-zA-Z0-9_-]+(?=:)", SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.SemiBold))
            // Strings
            highlightRegex(code, "\"[^\"]*\"|'[^']*'", SpanStyle(color = SyntaxString))
            // Comments
            highlightRegex(code, "#.*", SpanStyle(color = SyntaxComment))
            // Numbers
            highlightRegex(code, "\\b\\d+\\b", SpanStyle(color = SyntaxNumber))
        }
    }

    private fun AnnotatedString.Builder.highlightRegex(text: String, patternString: String, style: SpanStyle) {
        try {
            val pattern = Pattern.compile(patternString)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                addStyle(style, matcher.start(), matcher.end())
            }
        } catch (_: Exception) {}
    }
}
