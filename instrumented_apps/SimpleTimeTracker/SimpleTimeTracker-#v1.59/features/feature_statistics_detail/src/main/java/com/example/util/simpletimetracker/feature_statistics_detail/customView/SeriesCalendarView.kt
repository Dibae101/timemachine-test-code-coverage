package com.example.util.simpletimetracker.feature_statistics_detail.customView

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.example.util.simpletimetracker.feature_views.SingleTapDetector
import com.example.util.simpletimetracker.feature_views.SwipeDetector
import com.example.util.simpletimetracker.feature_views.isHorizontal
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.base.Coordinates
import com.example.util.simpletimetracker.feature_statistics_detail.R
import com.example.util.simpletimetracker.feature_views.extension.dpToPx
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.parcelize.Parcelize
import androidx.core.content.withStyledAttributes

class SeriesCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(
    context,
    attrs,
    defStyleAttr,
) {

    // Attrs
    private var legendTextSize: Float = 0f
    private var legendTextColor: Int = 0
    private var selectedCellColor: Int = 0
    // Attrs

    private val columnsCount: Int = 26
    private var rowsCount: Int = 7
    private val cellPadding: Float = 0.5f.dpToPx().toFloat()
    private val cellRadius: Float = 4f.dpToPx().toFloat()
    private var cellSize: Float = 0f
    private var cellColor: Int = Color.BLACK
    private var data: List<Data> = emptyList()
    private var dataColumnCount: Int = 0
    private var panFactor: Float = 0f
    private var lastPanFactor: Float = 0f
    private var chartFullWidth: Float = 0f
    private var cellNotPresentColorAlpha = 0.2f
    private var cellPresentColorAlphaBase = 0.6f
    private var selectedCell: Data? = null
    private var selectedCellAlpha: Int = 0
    private var selectedCellAnimator: Animator? = null
    private val selectedCellWidthRatio: Float = 0.3f
    private val selectedCellAnimationDurationMs: Long = 3_000
    private val selectedCellStartAlpha: Float = 0.4f
    private var listener: (ViewData, Coordinates) -> Unit = { _, _ -> }

    private val cellPresentPaint: Paint = Paint()
    private val cellNotPresentPaint: Paint = Paint()
    private val legendTextPaint: Paint = Paint()
    private val selectedCellPaint: Paint = Paint()

    private val singleTapDetector = SingleTapDetector(
        context = context,
        onSingleTap = ::onClick,
    )
    private val swipeDetector = SwipeDetector(
        context = context,
        onSlide = ::onSwipe,
        onSlideStop = ::onSwipeStop,
    )

    private val colorLevelAlphaMap: Map<ViewData.ColorLevel, Int> =
        ViewData.ColorLevel.entries.associateWith { getPresentCellFinalAlpha(it) }

    init {
        initArgs(context, attrs, defStyleAttr)
        initPaint()
        initEditMode()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(
            superSavedState = superState,
            panFactor = panFactor,
            lastPanFactor = lastPanFactor,
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as? SavedState
        super.onRestoreInstanceState(savedState?.superSavedState ?: state)
        panFactor = savedState?.panFactor.orZero()
        lastPanFactor = savedState?.lastPanFactor.orZero()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(0, widthMeasureSpec)
        cellSize = w / columnsCount.toFloat()
        val legendTextHeight = legendTextPaint.fontMetrics.let { it.descent - it.ascent }
        val height: Float = legendTextHeight + cellSize * rowsCount
        val h = resolveSize(height.roundToInt(), heightMeasureSpec)

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        calculateDimensions()
        drawCells(canvas, w, h)
        drawSelectedCell(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> handled = true
        }

        return handled or
            singleTapDetector.onTouchEvent(event) or
            swipeDetector.onTouchEvent(event)
    }

    fun setClickListener(listener: (ViewData, Coordinates) -> Unit) {
        this.listener = listener
    }

    fun setData(
        viewData: List<ViewData>,
        rowsCount: Int,
    ) {
        if (data.size != viewData.size) {
            panFactor = 0f
            lastPanFactor = 0f
        }
        this.rowsCount = rowsCount
        data = viewData.map { Data(cell = it) }
        requestLayout()
    }

    fun setCellColor(@ColorInt color: Int) {
        cellColor = color
        initPaint()
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
                R.styleable.SeriesCalendarView, defStyleAttr, 0,
            ) {
                legendTextSize =
                    getDimensionPixelSize(R.styleable.SeriesCalendarView_seriesLegendTextSize, 14).toFloat()
                legendTextColor =
                    getColor(R.styleable.SeriesCalendarView_seriesLegendTextColor, Color.BLACK)
                selectedCellColor =
                    getColor(R.styleable.SeriesCalendarView_seriesSelectedCellColor, Color.BLACK)
            }
    }

    private fun initPaint() {
        cellPresentPaint.apply {
            isAntiAlias = true
            color = cellColor
            style = Paint.Style.FILL
        }
        cellNotPresentPaint.apply {
            isAntiAlias = true
            color = cellColor
            style = Paint.Style.FILL
            alpha = (255 * cellNotPresentColorAlpha).toInt()
        }
        legendTextPaint.apply {
            isAntiAlias = true
            color = legendTextColor
            textSize = legendTextSize
        }
        selectedCellPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private fun initEditMode() {
        if (isInEditMode) {
            setCellColor(Color.RED)
            (30 downTo 1).toList()
                .map { ViewData.Present(0, "", ViewData.ColorLevel.THREE) }
                .let { setData(it, rowsCount) }
        }
    }

    private fun calculateDimensions() {
        dataColumnCount = ceil(data.size.toFloat() / rowsCount).toInt()
        chartFullWidth = dataColumnCount * cellSize
    }

    private fun drawCells(canvas: Canvas, w: Float, h: Float) {
        var boxLeft: Float
        var boxRight: Float
        var boxTop: Float
        var boxBottom: Float

        // If chart width is less than screen - center in screen.
        val finalWidth = if (chartFullWidth < w) {
            (w + chartFullWidth) / 2
        } else {
            w
        }

        var currentLegend = ""
        data.forEachIndexed { index, point ->
            if (point.cell is ViewData.Dummy) return@forEachIndexed

            val column = index / rowsCount
            val row = index % rowsCount

            // Draw from bottom right corner so that current day would be in there.
            boxLeft = finalWidth + panFactor - (column + 1) * cellSize
            boxTop = h - (row + 1) * cellSize
            boxRight = boxLeft + cellSize
            boxBottom = boxTop + cellSize

            // Save coordinates for click event.
            point.boxLeft = boxLeft
            point.boxTop = boxTop
            point.boxRight = boxRight
            point.boxBottom = boxBottom

            // Draw legend
            val legend = point.cell.monthLegend
            if (legend != currentLegend) {
                canvas.drawText(
                    currentLegend,
                    boxLeft,
                    legendTextSize,
                    legendTextPaint,
                )
                currentLegend = legend
            }

            // Draw cell
            canvas.drawRoundRect(
                boxLeft + cellPadding,
                boxTop + cellPadding,
                boxRight - cellPadding,
                boxBottom - cellPadding,
                cellRadius,
                cellRadius,
                if (point.cell is ViewData.Present) {
                    cellPresentPaint.alpha = colorLevelAlphaMap[point.cell.colorLevel].orZero()
                    cellPresentPaint
                } else {
                    cellNotPresentPaint
                },
            )
        }
    }

    private fun drawSelectedCell(canvas: Canvas) {
        val selectedCell = selectedCell ?: return

        selectedCellPaint.color = if (selectedCell.cell is ViewData.Present) {
            Color.WHITE
        } else {
            selectedCellColor
        }
        selectedCellPaint.alpha = selectedCellAlpha

        val boxWidth = selectedCell.boxRight - selectedCell.boxLeft
        val boxHeight = selectedCell.boxBottom - selectedCell.boxTop

        canvas.drawCircle(
            selectedCell.boxLeft + boxWidth / 2f,
            selectedCell.boxTop + boxHeight / 2f,
            boxWidth * selectedCellWidthRatio / 2f,
            selectedCellPaint,
        )
    }

    private fun getPresentCellFinalAlpha(colorLevel: ViewData.ColorLevel): Int {
        val maxLevel = ViewData.ColorLevel.entries.size
        val alphaCoefficient = if (maxLevel > 1) {
            colorLevel.value.toFloat() / (maxLevel - 1)
        } else {
            1f
        }
        val baseAlpha = cellPresentColorAlphaBase + (1 - cellPresentColorAlphaBase) * alphaCoefficient
        return (255 * baseAlpha).toInt()
    }

    private fun animateSelectedCell() {
        val from = 255 * selectedCellStartAlpha
        val animator = ValueAnimator.ofInt(from.toInt(), 0)
            .also { selectedCellAnimator = it }

        animator.duration = selectedCellAnimationDurationMs
        animator.addUpdateListener {
            selectedCellAlpha = it.animatedValue as? Int
                ?: return@addUpdateListener
            invalidate()
        }
        animator.start()
    }

    private fun cancelSelectedCellAnimation() {
        selectedCellAnimator?.cancel()
        selectedCell = null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSwipe(offset: Float, direction: SwipeDetector.Direction, event: MotionEvent) {
        if (direction.isHorizontal()) {
            parent.requestDisallowInterceptTouchEvent(true)
            panFactor = lastPanFactor + offset
            coercePan()
            cancelSelectedCellAnimation()
            invalidate()
        }
    }

    private fun onClick(event: MotionEvent) {
        val x = event.x
        val y = event.y

        data.firstOrNull {
            it.boxLeft < x && it.boxTop < y && it.boxRight > x && it.boxBottom > y
        }?.let {
            val globalRect = Rect()
            getGlobalVisibleRect(globalRect)
            selectedCell = it
            listener(
                it.cell,
                // Calculate from the bottom, because when view is half scrolled on the top,
                // global rect would be cutoff, and calculation would be wrong.
                // When vew is half scrolled on the bottom is not so critical.
                Coordinates(
                    left = globalRect.left + it.boxLeft.toInt(),
                    top = globalRect.bottom - height + it.boxTop.toInt(),
                    right = globalRect.left + it.boxRight.toInt(),
                    bottom = globalRect.bottom - height + it.boxBottom.toInt(),
                ),
            )
            animateSelectedCell()
        }
    }

    private fun onSwipeStop() {
        parent.requestDisallowInterceptTouchEvent(false)
        lastPanFactor = panFactor
    }

    private fun coercePan() {
        panFactor = panFactor.coerceIn(0f, (chartFullWidth - width).coerceAtLeast(0f))
    }

    private inner class Data(
        val cell: ViewData,
        var boxLeft: Float = 0f,
        var boxTop: Float = 0f,
        var boxRight: Float = 0f,
        var boxBottom: Float = 0f,
    )

    sealed interface ViewData {
        val rangeStart: Long
        val monthLegend: String

        data class Present(
            override val rangeStart: Long,
            override val monthLegend: String,
            val colorLevel: ColorLevel,
        ) : ViewData

        data class NotPresent(
            override val rangeStart: Long,
            override val monthLegend: String,
        ) : ViewData

        data object Dummy : ViewData {
            // Not needed for dummy view.
            override val rangeStart: Long = 0L
            override val monthLegend: String = ""
        }

        enum class ColorLevel(val value: Int) {
            ONE(0),
            TWO(1),
            THREE(2),
            FOUR(3),
        }
    }

    @Parcelize
    private class SavedState(
        val superSavedState: Parcelable?,
        val panFactor: Float,
        val lastPanFactor: Float,
    ) : BaseSavedState(superSavedState)
}