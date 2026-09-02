package com.example.util.simpletimetracker.feature_dialogs.duration.customView

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import com.example.util.simpletimetracker.feature_views.SwipeDetector
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.extension.toDuration
import com.example.util.simpletimetracker.feature_dialogs.R
import kotlin.math.round
import androidx.core.content.withStyledAttributes
import com.example.util.simpletimetracker.feature_views.ColorUtils

class DurationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(
    context,
    attrs,
    defStyleAttr,
) {
    var listener: Listener? = null

    // Attrs
    private var textColor: Int = 0
    private var backgroundColor: Int = 0
    private var legendTextColor: Int = 0
    private var legendTextSize: Float = 0f
    private var legendPadding: Float = 0f
    // End of attrs

    private var data: ViewData = ViewData.Empty
    private val textPaint: Paint = Paint()
    private val backgroundPaintTop: Paint = Paint()
    private val backgroundPaintBottom: Paint = Paint()
    private val legendTextPaint: Paint = Paint()
    private var textHeight: Float = 0f
    private var textStartHorizontal: Float = 0f
    private var textStartVertical: Float = 0f
    private val swipeSpeedCoefficient: Float = 2f
    private val pageAlpha: Float = 0.3f
    private val pageBackgroundAlpha: Float = 0.5f
    private val settlingAnimationDurationMs: Long = 300
    private val bounds: Rect = Rect()
    private val minPageValue = 0
    private val maxPageValue = 59
    private val pageSize = maxPageValue - minPageValue + 1
    private var fieldStateHours: FieldState = FieldState()
    private var fieldStateMinutes: FieldState = FieldState()
    private var fieldStateSeconds: FieldState = FieldState()

    private val hourString: String by lazy { context.getString(R.string.time_hour) }
    private val minuteString: String by lazy { context.getString(R.string.time_minute) }
    private val secondString: String by lazy { context.getString(R.string.time_second) }

    private val swipeDetector = SwipeDetector(
        context = context,
        onSlide = ::onEventSwipe,
        onSlideStop = ::onEventSwipeStop,
    )

    init {
        initArgs(context, attrs, defStyleAttr)
        initPaint()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(0, widthMeasureSpec)
        val h = resolveSize(w, heightMeasureSpec)

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        calculateDimensions(w, h)
        drawText(canvas, w, h)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> handled = true
        }

        return handled or
            swipeDetector.onTouchEvent(event)
    }

    fun setData(data: ViewData) {
        // If still animating - don't change, it will change on animation end.
        this.data = data.copy(
            hours = if (fieldStateHours.isSettling || fieldStateHours.isSwiping) {
                this.data.hours
            } else {
                data.hours
            },
            minutes = if (fieldStateMinutes.isSettling || fieldStateMinutes.isSwiping) {
                this.data.minutes
            } else {
                data.minutes
            },
            seconds = if (fieldStateSeconds.isSettling || fieldStateSeconds.isSwiping) {
                this.data.seconds
            } else {
                data.seconds
            },
        )
        invalidate()
    }

    private fun initArgs(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) {
        context
            .withStyledAttributes(
                attrs,
                R.styleable.DurationView, defStyleAttr, 0,
            ) {
                textColor = getColor(
                    R.styleable.DurationView_durationTextColor, Color.BLACK,
                )
                backgroundColor = getColor(
                    R.styleable.DurationView_durationBackgroundColor, Color.WHITE,
                )
                legendTextColor = getColor(
                    R.styleable.DurationView_durationLegendTextColor, Color.BLACK,
                )
                legendTextSize = getDimensionPixelSize(
                    R.styleable.DurationView_durationLegendTextSize, 14,
                ).toFloat()
                legendPadding = getDimensionPixelSize(
                    R.styleable.DurationView_durationLegendPadding, 0,
                ).toFloat()
            }
    }

    private fun initPaint() {
        textPaint.apply {
            isAntiAlias = true
            color = textColor
        }
        legendTextPaint.apply {
            isAntiAlias = true
            color = legendTextColor
            textSize = legendTextSize
        }
    }

    private fun calculateDimensions(w: Float, h: Float) {
        val legendsTextWidth = listOfNotNull(
            hourString,
            minuteString,
            secondString.takeIf { data.showSeconds },
        ).map(legendTextPaint::measureText).sum()
        val paddingsCount = if (data.showSeconds) 2 else 1
        val desiredWidth = w - legendsTextWidth - paddingsCount * legendPadding
        setTextSizeForWidth(
            paint = textPaint,
            data = data,
            desiredWidth = desiredWidth,
        )

        val fullTextWidth = textPaint.measureText(data.hours.format()) +
            textPaint.measureText(data.minutes.format()) +
            data.seconds.takeIf { data.showSeconds }
                ?.let { textPaint.measureText(it.format()) }.orZero() +
            legendsTextWidth
        textStartHorizontal = (w - fullTextWidth - 2 * legendPadding) / 2f

        textPaint.getTextBounds("0", 0, 1, bounds)
        textHeight = bounds.height().toFloat()
        textStartVertical = textHeight + (h - textHeight) / 2f

        val backgroundColorWithAlpha = ColorUtils.changeAlpha(backgroundColor, pageBackgroundAlpha)
        backgroundPaintTop.shader = LinearGradient(
            0f,
            0f,
            0f,
            textStartVertical - textHeight,
            backgroundColorWithAlpha,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        backgroundPaintBottom.shader = LinearGradient(
            0f,
            textStartVertical,
            0f,
            h,
            Color.TRANSPARENT,
            backgroundColorWithAlpha,
            Shader.TileMode.CLAMP,
        )
    }

    private fun drawText(canvas: Canvas, w: Float, h: Float) {
        // Center text
        var currentTextStartHorizontal = textStartHorizontal

        val hoursText = data.hours.format()
        val minutesText = data.minutes.format()
        val secondsText = data.seconds.format()
        val hoursNotEmpty = textHasValues(hoursText)
        val minutesNotEmpty = hoursNotEmpty || textHasValues(minutesText)
        val secondsNotEmpty = hoursNotEmpty || minutesNotEmpty || textHasValues(secondsText)

        // Hours
        var color = if (hoursNotEmpty) textColor else legendTextColor
        textPaint.color = color
        canvas.drawText(
            hoursText,
            currentTextStartHorizontal,
            textStartVertical + fieldStateHours.panFactor,
            textPaint,
        )
        fieldStateHours = fieldStateHours.copy(
            leftPx = currentTextStartHorizontal,
            rightPx = currentTextStartHorizontal + textPaint.measureText(hoursText),
        )
        drawPages(
            canvas = canvas,
            w = w,
            h = h,
            data = data.hours,
            currentTextStartHorizontal = currentTextStartHorizontal,
            panFactor = fieldStateHours.panFactor,
            hasMinLimit = false,
            hasMaxLimit = false,
        )
        currentTextStartHorizontal += textPaint.measureText(hoursText)
        canvas.drawText(hourString, currentTextStartHorizontal, textStartVertical, legendTextPaint)
        currentTextStartHorizontal += legendTextPaint.measureText(hourString) + legendPadding

        // Minutes
        color = if (minutesNotEmpty) textColor else legendTextColor
        textPaint.color = color
        canvas.drawText(
            minutesText,
            currentTextStartHorizontal,
            textStartVertical + fieldStateMinutes.panFactor,
            textPaint,
        )
        fieldStateMinutes = fieldStateMinutes.copy(
            leftPx = currentTextStartHorizontal,
            rightPx = currentTextStartHorizontal + textPaint.measureText(minutesText),
        )
        drawPages(
            canvas = canvas,
            w = w,
            h = h,
            data = data.minutes,
            currentTextStartHorizontal = currentTextStartHorizontal,
            panFactor = fieldStateMinutes.panFactor,
        )
        currentTextStartHorizontal += textPaint.measureText(minutesText)
        canvas.drawText(minuteString, currentTextStartHorizontal, textStartVertical, legendTextPaint)
        currentTextStartHorizontal += legendTextPaint.measureText(minuteString) + legendPadding

        // Seconds
        if (data.showSeconds) {
            color = if (secondsNotEmpty) textColor else legendTextColor
            textPaint.color = color
            canvas.drawText(
                secondsText,
                currentTextStartHorizontal,
                textStartVertical + fieldStateSeconds.panFactor,
                textPaint,
            )
            fieldStateSeconds = fieldStateSeconds.copy(
                leftPx = currentTextStartHorizontal,
                rightPx = currentTextStartHorizontal + textPaint.measureText(secondsText),
            )
            drawPages(
                canvas = canvas,
                w = w,
                h = h,
                data = data.seconds,
                currentTextStartHorizontal = currentTextStartHorizontal,
                panFactor = fieldStateSeconds.panFactor,
            )
            currentTextStartHorizontal += textPaint.measureText(secondsText)
            canvas.drawText(secondString, currentTextStartHorizontal, textStartVertical, legendTextPaint)
            currentTextStartHorizontal += legendTextPaint.measureText(secondString)
        }
    }

    private fun drawPages(
        canvas: Canvas,
        w: Float,
        h: Float,
        data: Long,
        currentTextStartHorizontal: Float,
        panFactor: Float,
        hasMinLimit: Boolean = true,
        hasMaxLimit: Boolean = true,
    ) {
        val defaultAlpha = 255
        textPaint.color = legendTextColor
        textPaint.alpha = (defaultAlpha * pageAlpha).toInt()

        var pageNumber = 1
        var valueToDraw: Long
        var textToDraw: String
        var currentPageY: Float
        var currentPageX: Float
        val dataFormatted = data.format()

        // Top pages
        while (true) {
            currentPageY = textStartVertical - textHeight * pageNumber + panFactor
            // Do not draw over screen.
            if (currentPageY < 0) break
            valueToDraw = (data - pageNumber)
                .let { if (it < minPageValue && hasMinLimit) it + pageSize else it }
            textToDraw = valueToDraw.format()
            if (valueToDraw >= 0) {
                currentPageX = getPageStartHorizontal(textToDraw, dataFormatted, currentTextStartHorizontal)
                canvas.drawText(textToDraw, currentPageX, currentPageY, textPaint)
            }
            pageNumber += 1
        }

        // Bottom pages.
        pageNumber = 1
        while (true) {
            currentPageY = textStartVertical + textHeight * pageNumber + panFactor
            // Do not draw over screen.
            if (currentPageY - textHeight > h) break
            valueToDraw = (data + pageNumber)
                .let { if (it > maxPageValue && hasMaxLimit) it - pageSize else it }
            textToDraw = valueToDraw.format()
            currentPageX = getPageStartHorizontal(textToDraw, dataFormatted, currentTextStartHorizontal)
            canvas.drawText(textToDraw, currentPageX, currentPageY, textPaint)
            pageNumber += 1
        }
        textPaint.alpha = defaultAlpha

        // Draw alpha gradient
        canvas.drawRect(
            0f,
            0f,
            w,
            textStartVertical - textHeight,
            backgroundPaintTop,
        )
        canvas.drawRect(
            0f,
            textStartVertical,
            w,
            h,
            backgroundPaintBottom,
        )
    }

    private fun getPageStartHorizontal(
        textToDraw: String,
        dataFormatted: String,
        currentTextStartHorizontal: Float,
    ): Float {
        // Move text to the right if it is longer or shorted than current data.
        // Otherwise if for ex. current data is 99, next 100 will overdraw on minutes.
        return if (textToDraw.length == dataFormatted.length) {
            currentTextStartHorizontal
        } else {
            currentTextStartHorizontal +
                textPaint.measureText(dataFormatted) -
                textPaint.measureText(textToDraw)
        }
    }

    private fun setTextSizeForWidth(
        paint: Paint,
        data: ViewData,
        desiredWidth: Float,
    ) {
        val testTextSize = 48f
        paint.textSize = testTextSize
        val width = paint.measureText(data.hours.format()) +
            paint.measureText(data.minutes.format()) +
            if (data.showSeconds) paint.measureText(data.seconds.format()) else 0f

        val desiredTextSize = testTextSize * desiredWidth / width
        paint.textSize = desiredTextSize
    }

    private fun textHasValues(text: String): Boolean {
        return text != "00"
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onEventSwipe(
        offset: Float,
        direction: SwipeDetector.Direction,
        event: MotionEvent,
    ) {
        startSwipe(
            event = event,
            offset = offset,
            isOtherSwipingInProgress = fieldStateMinutes.isSwiping ||
                fieldStateSeconds.isSwiping,
            getState = { fieldStateHours },
            setState = { fieldStateHours = it },
        )
        startSwipe(
            event = event,
            offset = offset,
            isOtherSwipingInProgress = fieldStateHours.isSwiping ||
                fieldStateSeconds.isSwiping,
            getState = { fieldStateMinutes },
            setState = { fieldStateMinutes = it },
        )
        startSwipe(
            event = event,
            offset = offset,
            isOtherSwipingInProgress = fieldStateHours.isSwiping ||
                fieldStateMinutes.isSwiping,
            getState = { fieldStateSeconds },
            setState = { fieldStateSeconds = it },
        )
    }

    private fun onEventSwipeStop() {
        startSettling(
            data = data.hours,
            getState = { fieldStateHours },
            setState = { fieldStateHours = it },
            onSettled = { listener?.onValueSelected(data.copy(hours = it)) },
            hasMinLimit = false,
            hasMaxLimit = false,
        )
        startSettling(
            data = data.minutes,
            getState = { fieldStateMinutes },
            setState = { fieldStateMinutes = it },
            onSettled = { listener?.onValueSelected(data.copy(minutes = it)) },
        )
        startSettling(
            data = data.seconds,
            getState = { fieldStateSeconds },
            setState = { fieldStateSeconds = it },
            onSettled = { listener?.onValueSelected(data.copy(seconds = it)) },
        )
    }

    private fun startSwipe(
        event: MotionEvent,
        offset: Float,
        isOtherSwipingInProgress: Boolean,
        getState: () -> FieldState,
        setState: (FieldState) -> Unit,
    ) {
        val coordinatesInRange = event.x in getState().leftPx..getState().rightPx
        if (
            (!isOtherSwipingInProgress && coordinatesInRange) ||
            getState().isSwiping
        ) {
            // Cancel current animation on new swipe.
            if (getState().isSettling) {
                getState().animator?.cancel()
            }
            getState().copy(
                panFactor = getState().lastPanFactor + offset * swipeSpeedCoefficient,
                isSwiping = true,
            ).let(setState)
            invalidate()
        }
    }

    private fun startSettling(
        data: Long,
        getState: () -> FieldState,
        setState: (FieldState) -> Unit,
        onSettled: (Long) -> Unit,
        hasMinLimit: Boolean = true,
        hasMaxLimit: Boolean = true,
    ) {
        if (!getState().isSwiping) return
        val snapTo: Long = round(getState().panFactor / textHeight).toLong().let {
            when {
                hasMinLimit -> it
                data - it < 0 -> data // Prevents overshooting zero on hours.
                else -> it
            }
        }
        val snapToPx: Float = snapTo * textHeight

        val animator = ValueAnimator.ofFloat(getState().panFactor, snapToPx)
        animator.duration = settlingAnimationDurationMs
        animator.addUpdateListener { animation ->
            getState().copy(
                panFactor = animation.animatedValue as Float,
                lastPanFactor = animation.animatedValue as Float,
            ).let(setState)
            invalidate()
        }
        animator.doOnEnd {
            getState().copy(
                isSettling = false,
                panFactor = 0f,
                lastPanFactor = 0f,
            ).let(setState)
            (data - snapTo).let {
                when {
                    it < minPageValue && hasMinLimit -> it + pageSize
                    it > maxPageValue && hasMaxLimit -> it - pageSize
                    else -> it
                }
            }.coerceAtLeast(0).let(onSettled)
            invalidate()
        }
        animator.start()

        getState().copy(
            isSwiping = false,
            isSettling = true,
            animator = animator,
        ).let(setState)
    }

    private fun Long.format(): String {
        return this.toDuration()
    }

    private data class FieldState(
        val leftPx: Float = 0f,
        val rightPx: Float = 0f,
        val panFactor: Float = 0f,
        val lastPanFactor: Float = 0f,
        val isSwiping: Boolean = false,
        val isSettling: Boolean = false,
        val animator: ValueAnimator? = null,
    )

    fun interface Listener {
        fun onValueSelected(viewData: ViewData)
    }

    data class ViewData(
        val hours: Long,
        val minutes: Long,
        val seconds: Long,
        val showSeconds: Boolean,
    ) {

        companion object {
            val Empty = ViewData(
                hours = 0,
                minutes = 0,
                seconds = 0,
                showSeconds = true,
            )
        }
    }
}