package com.example.util.simpletimetracker.core.extension

import android.text.Html.TagHandler
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml

fun String.fromHtml(tagHandler: TagHandler? = null): Spanned {
    return this.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY, null, tagHandler)
}

fun String.trimIfNotBlank(): String {
    return if (this.isNotBlank()) return this.trim() else this
}

fun String.removeTrailingZeroes(): String {
    return (if ('.' in this) trimEnd('0') else this).trimEnd('.')
}