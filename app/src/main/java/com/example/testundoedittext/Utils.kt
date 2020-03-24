package com.example.testundoedittext

import java.util.regex.Matcher
import java.util.regex.Pattern

private const val HTML_PATTERN = "<([A-Za-z][A-Za-z0-9]*)\\b[^>]*>(.*?)</\\1>"
private val pattern: Pattern = Pattern.compile(HTML_PATTERN)

fun hasHTMLTags(text: String?): Boolean {
    val matcher: Matcher = pattern.matcher(text)
    return matcher.find()
}