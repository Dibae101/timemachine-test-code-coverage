package com.example.util.simpletimetracker.feature_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.Spanned
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.util.simpletimetracker.feature_views.extension.dpToPx

/**
 * Draws rounded background around marked spans.
 */
class TextViewRoundedSpans @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(
    context,
    attrs,
    defStyleAttr,
) {

    private val backgroundPadding = 6.dpToPx()
    private val drawableMiddle by lazy { getDrawable(R.drawable.bg_rounded_span_middle) }
    private val drawableStart by lazy { getDrawable(R.drawable.bg_rounded_span_start) }
    private val drawableEnd by lazy { getDrawable(R.drawable.bg_rounded_span_end) }

    override fun onDraw(canvas: Canvas) {
        drawSpans(canvas, text, layout)
        super.onDraw(canvas)
    }

    private fun drawSpans(
        canvas: Canvas,
        text: CharSequence,
        layout: Layout?,
    ) {
        if (text !is Spanned) return
        if (layout == null) return

        val spans = text.getSpans(
            0,
            text.length,
            MarkerSpan::class.java,
        )

        spans.forEach { span ->
            val charStart = text.getSpanStart(span)
            val charEnd = text.getSpanEnd(span)

            val startLineNumber = layout.getLineForOffset(charStart)
            val endLineNumber = layout.getLineForOffset(charEnd)

            val paragraphDirection = layout.getParagraphDirection(startLineNumber)
            val isRightToLeft = paragraphDirection == Layout.DIR_RIGHT_TO_LEFT

            val primaryStartOffset = layout.getPrimaryHorizontal(charStart).toInt()
            val primaryEndOffset = layout.getPrimaryHorizontal(charEnd).toInt()

            // Draw start.
            drawDrawable(
                canvas = canvas,
                drawable = (if (!isRightToLeft) drawableStart else drawableEnd)?.mutate() ?: return,
                colorInt = span.colorInt,
                start = if (!isRightToLeft) {
                    primaryStartOffset - backgroundPadding
                } else {
                    primaryStartOffset
                },
                top = layout.getLineTop(startLineNumber),
                end = if (!isRightToLeft) {
                    primaryStartOffset
                } else {
                    primaryStartOffset + backgroundPadding
                },
                bottom = layout.getLineBottom(startLineNumber),
            )

            // Draw middles.
            for (line in startLineNumber..endLineNumber) {
                var startOffset: Int
                var endOffset: Int

                when {
                    // Multiline start.
                    startLineNumber != endLineNumber && line == startLineNumber -> {
                        startOffset = primaryStartOffset
                        endOffset = if (!isRightToLeft) {
                            layout.getLineRight(line).toInt() + backgroundPadding
                        } else {
                            layout.getLineLeft(line).toInt() - backgroundPadding
                        }
                    }
                    // Multiline end.
                    startLineNumber != endLineNumber && line == endLineNumber -> {
                        startOffset = if (!isRightToLeft) {
                            layout.getLineLeft(line).toInt() - backgroundPadding
                        } else {
                            layout.getLineRight(line).toInt() + backgroundPadding
                        }
                        endOffset = primaryEndOffset
                    }
                    // Multiline middle.
                    startLineNumber != endLineNumber -> {
                        startOffset = layout.getLineLeft(line).toInt() - backgroundPadding
                        endOffset = layout.getLineRight(line).toInt() + backgroundPadding
                    }
                    // Single line.
                    else -> {
                        startOffset = primaryStartOffset
                        endOffset = primaryEndOffset
                    }
                }

                drawDrawable(
                    canvas = canvas,
                    drawable = drawableMiddle?.mutate() ?: return,
                    colorInt = span.colorInt,
                    start = startOffset,
                    top = layout.getLineTop(line),
                    end = endOffset,
                    bottom = layout.getLineBottom(line),
                )
            }

            // Draw end.
            drawDrawable(
                canvas = canvas,
                drawable = (if (!isRightToLeft) drawableEnd else drawableStart)?.mutate() ?: return,
                colorInt = span.colorInt,
                start = if (!isRightToLeft) {
                    primaryEndOffset
                } else {
                    primaryEndOffset - backgroundPadding
                },
                top = layout.getLineTop(endLineNumber),
                end = if (!isRightToLeft) {
                    primaryEndOffset + backgroundPadding
                } else {
                    primaryEndOffset
                },
                bottom = layout.getLineBottom(endLineNumber),
            )
        }
    }

    private fun drawDrawable(
        canvas: Canvas,
        drawable: Drawable,
        @ColorInt colorInt: Int,
        start: Int,
        top: Int,
        end: Int,
        bottom: Int,
    ) {
        drawable.setTint(colorInt)
        val isRtl = start > end
        // Inverts in RTL.
        drawable.setBounds(
            if (!isRtl) start + paddingStart else end + paddingStart,
            top + paddingTop,
            if (!isRtl) end + paddingStart else start + paddingStart,
            bottom + paddingTop,
        )
        drawable.draw(canvas)
    }

    private fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return ResourcesCompat.getDrawable(context.resources, resId, context.theme)
    }

    data class MarkerSpan(@ColorInt val colorInt: Int)
}