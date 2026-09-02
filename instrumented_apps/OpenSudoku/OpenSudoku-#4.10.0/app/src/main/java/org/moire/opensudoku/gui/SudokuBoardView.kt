/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2026 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.moire.opensudoku.gui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.CellMarks
import org.moire.opensudoku.game.DoubleMarksMode
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SameValueHighlightMode
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.utils.BackgroundColorPrefs
import org.moire.opensudoku.gui.screen.game_play.Timer
import org.moire.opensudoku.utils.Colors
import org.moire.opensudoku.utils.FontSelector
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Sudoku board widget.
 */
class SudokuBoardView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
	var selectedCell: Cell? = null
		private set(value) {
			if (field !== value) {
				field = value
				onSelectedCellUpdate(value)
			}
		}

	internal var highlightedValue = 0
		set(value) {
			if (field != value) {
				field = value
				postInvalidate()
			}
		}
	internal var highlightDirectlyWrongValues = false
		set(value) {
			if (field != value) {
				field = value
				postInvalidate()
			}
		}
	internal var highlightIndirectlyWrongValues = false
		set(value) {
			if (field != value) {
				field = value
				postInvalidate()
			}
		}
	internal var highlightInvalidPencilMarks = false
		set(value) {
			if (field != value) {
				field = value
				postInvalidate()
			}
		}
	internal var highlightTouchedCell = true
	internal var autoHideTouchedCellHint = true
	internal var sameValueHighlightMode = SameValueHighlightMode.NONE
		set(value) {
			if (field != value) {
				field = value
				postInvalidate()
			}
		}
	internal var doubleMarksMode: DoubleMarksMode = DoubleMarksMode.WITH_CORNER
	internal var isEvenBoxColoringEnabled: Boolean = true

	var onReadonlyChangeListener: (() -> Unit)? = null

	/**
	 * Registers callback which will be invoked when user taps the cell.
	 */
	internal var onCellTappedListener: (Cell) -> Unit = {}

	/**
	 * Callback invoked when cell is selected. Cell selection can change without user interaction.
	 */
	internal lateinit var onSelectedCellUpdate: (Cell?) -> Unit

	/**
	 * Stores the background and foreground paints for each cell so they can be drawn
	 * in one pass. First two indices are the cell's row and column, the last one is
	 * paints for the cell's background, text, or marks text, respectively.
	 *
	 *
	 * So [1][3][0] is the background ([0]) paint of the cell in the second ([1]) row
	 * in the fourth ([3]) column.
	 */
	val textValue = Paint()
	var textMarkColor = 0
	val textMarkPaint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
	}
	val textFocused = Paint()
	val textMarkFocused = Paint()
	val textHighlighted = Paint()
	var textMarkHighlightedColor: Int = 0
	val textGivenValue = Paint()
	val textEvenCells = Paint()
	var textEvenCellsMarksColor = 0
	val backgroundEvenCells = Paint()
	val textEvenGivenValueCells = Paint()
	val backgroundEvenGivenValueCells = Paint()
	val textTouched = Paint()
	var textTouchedMarksColor = 0
	val textInvalid = Paint()
	val background = Paint()
	val backgroundGivenValue = Paint()
	val backgroundTouched = Paint()
	val backgroundHighlighted = Paint()
	val backgroundMarksHighlighted = Paint()
	val backgroundInvalid = Paint()
	val backgroundCustomColor = Paint()
	val selectedCellFrame = Paint()
	val line = Paint()
	val sectorLine = Paint()

	var marksTextSize: Float = 0f

	/** Move the cell focus to the right if a value (not mark) is entered  */
	internal var moveCellSelectionOnPress = false

	private var blinkingDigit: Int = 0
	private val blinker = Blinker(this)
	private var cellWidth = 0f
	private var cellHeight = 0f
	internal var touchedCell: Cell? = null
	private lateinit var game: SudokuGame
	private lateinit var _board: SudokuBoard
	private var isInitialized = false
	private val bounds = Rect()
	private var numberTop = 0
	private var marksTop = 0f
	private var sectorLineWidth = 0

	private val customColors = IntArray(10)

	var fontSet: FontSelector


	init {
		isFocusable = true
		isFocusableInTouchMode = true
		textValue.isAntiAlias = true
		textGivenValue.isAntiAlias = true
		textInvalid.isAntiAlias = true
		textEvenCells.isAntiAlias = true
		textTouched.isAntiAlias = true
		textFocused.isAntiAlias = true
		textHighlighted.isAntiAlias = true
		textMarkFocused.isAntiAlias = true
		setAllColorsFromThemedContext(context)
		contentDescription = context.getString(R.string.sudoku_board_widget)
		fontSet = FontSelector(context)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		if (!isInitialized) return
		val boardWidth = width
		val boardHeight = height
		val paddingLeft = paddingLeft
		val paddingTop = paddingTop
		val paddingRight = paddingRight
		val paddingBottom = paddingBottom

		// Ensure the whole canvas starts with the background colour, in case any other colours are transparent.
		canvas.drawRect(0f, 0f, boardWidth.toFloat(), boardHeight.toFloat(), background)

		// draw cells
		var cellLeft: Int
		var cellTop: Int
		val numberAscent = textValue.ascent()
		if (highlightIndirectlyWrongValues) {
			board.solutionCount // make sure solution is known for cells to use for highlighting
		}
		var backgroundPaint: Paint
		var valuePaint: Paint

		for (row in 0..8) {
			for (col in 0..8) {
				val cell = board.getCell(row, col)
				val boxNumber = row / 3 + col / 3 + 1 // 1-based
				if (!isEvenBoxColoringEnabled || boxNumber % 2 != 0) {
					// Default colours for ordinary cells
					backgroundPaint = if (cell.isEditable) background else backgroundGivenValue
					valuePaint = if (cell.isEditable) textValue else textGivenValue
					textMarkPaint.color = textMarkColor
				} else {
					// Even boxes colors
					backgroundPaint = if (cell.isEditable) backgroundEvenCells else backgroundEvenGivenValueCells
					valuePaint = if (cell.isEditable) textEvenCells else textEvenGivenValueCells
					textMarkPaint.color = textEvenCellsMarksColor
				}

				// Highlight this cell if (a) we're highlighting cells in the same row/column as the touched cell, and (b) this cell is in that row or column.
				if ((highlightTouchedCell && (row == touchedCell?.rowIndex || col == touchedCell?.columnIndex))
					|| (sameValueHighlightMode == SameValueHighlightMode.HINTS_ONLY && cell.hintHighlight == HintHighlight.CAUSE)
				) {
					backgroundPaint = backgroundTouched
					valuePaint = textTouched
					textMarkPaint.color = textTouchedMarksColor
				}
				// Seeing that a cell is invalid is more important than it being highlighted.
				else if (shouldHighlightAsInvalid(cell)) {
					backgroundPaint = backgroundInvalid
					valuePaint = textInvalid
				} else if (shouldHighlightWholeCellForSameValue(cell)) {
					backgroundPaint = backgroundHighlighted
					valuePaint = textHighlighted
					textMarkPaint.color = textMarkHighlightedColor
				} else if (shouldHighlightWholeCellForSameMark(cell)) {
					backgroundPaint = backgroundMarksHighlighted
					textMarkPaint.color = textMarkHighlightedColor
				}
				// Custom background color - only if editable, empty, and NO other background was "set" by highlights or filled state.
				else if (cell.isEditable && cell.value == 0 && cell.color != 0) {
					backgroundPaint = backgroundCustomColor
					backgroundCustomColor.color = customColors[cell.color]
				}

				// Draw the cell background
				cellLeft = (col * cellWidth + paddingLeft).roundToInt()
				cellTop = (row * cellHeight + paddingTop).roundToInt()
				canvas.drawRect(cellLeft.toFloat(), cellTop.toFloat(), cellLeft + cellWidth, cellTop + cellHeight, backgroundPaint)

				// Draw cell contents
				val value = cell.value
				if (value != 0) {
					if (value != blinkingDigit) { // draw the digit if not in the blinking phase, otherwise hide the digit
						val numberLeft = ((cellWidth - textValue.measureText(fontSet.convert(value))) / 2).toInt()
						canvas.drawText(fontSet.convert(value), (cellLeft + numberLeft).toFloat(), cellTop + numberTop - numberAscent, valuePaint)
					}
				} else {
					drawCellMarks(cell, cellLeft, cellTop, canvas)
				}

				// highlight selected cell
				if ((!isReadOnlyPreview && cell == selectedCell)
					|| (sameValueHighlightMode == SameValueHighlightMode.HINTS_ONLY && cell.hintHighlight == HintHighlight.TARGET)
				) {
					cellLeft = (col * cellWidth).roundToInt() + paddingLeft
					cellTop = (row * cellHeight).roundToInt() + paddingTop

					// The stroke is drawn half inside and half outside the given cell. Compensate by adjusting the cell's bounds by half the
					// stroke width to move it entirely inside the cell.
					val halfStrokeWidth = selectedCellFrame.strokeWidth / 2
					selectedCellFrame.alpha = 128
					canvas.drawRect(
						cellLeft + halfStrokeWidth,
						cellTop + halfStrokeWidth,
						cellLeft + cellWidth - halfStrokeWidth,
						cellTop + cellHeight - halfStrokeWidth,
						selectedCellFrame
					)
				}
			}
		}

		// draw vertical lines
		for (c in 0..9) {
			val x = c * cellWidth + paddingLeft
			canvas.drawLine(x, paddingTop.toFloat(), x, (boardHeight - paddingBottom).toFloat(), line)
		}

		// draw horizontal lines
		for (rowIndex in 0..9) {
			val y = rowIndex * cellHeight + paddingTop
			canvas.drawLine(paddingLeft.toFloat(), y, (boardWidth - paddingRight).toFloat(), y, line)
		}
		val sectorLineWidth1 = sectorLineWidth / 2
		val sectorLineWidth2 = sectorLineWidth1 + sectorLineWidth % 2

		// draw sector (thick) lines
		var c = 0
		while (c <= 9) {
			val x = c * cellWidth + paddingLeft
			canvas.drawRect(
				x - sectorLineWidth1,
				paddingTop.toFloat(),
				x + sectorLineWidth2,
				(boardHeight - paddingBottom).toFloat(),
				sectorLine
			)
			c += 3
		}
		var r = 0
		while (r <= 9) {
			val y = r * cellHeight + paddingTop
			canvas.drawRect(
				paddingLeft.toFloat(),
				y - sectorLineWidth1,
				(boardWidth - paddingRight).toFloat(),
				y + sectorLineWidth2,
				sectorLine
			)
			r += 3
		}
	}

	// Only mark editable cells as errors, there's no point marking a given cell with an error (partly because the user can't change it, and partly
	// because then the user can't easily see which of the cells containing the error are editable).
	private fun shouldHighlightAsInvalid(cell: Cell): Boolean {
		if (cell.value != 0 && (board.isEditMode || cell.isEditable)) {
			if (board.isEditMode || highlightIndirectlyWrongValues) {
				return !cell.isCorrect
			}

			if (highlightDirectlyWrongValues) {
				board.validateAllCells()
				return !cell.isValid
			}
		}
		return false
	}

	private fun shouldHighlightWholeCellForSameValue(cell: Cell): Boolean {
		return when (sameValueHighlightMode) {
			SameValueHighlightMode.NUMBERS, SameValueHighlightMode.NUMBERS_AND_MARK_VALUES, SameValueHighlightMode.NUMBERS_AND_MARK_CELLS -> {
				highlightedValue != 0 && cell.value == highlightedValue
			}

			SameValueHighlightMode.HINTS_ONLY -> {
				cell.hintHighlight == HintHighlight.REGION
			}

			else -> {
				false
			}
		}
	}

	private fun shouldHighlightWholeCellForSameMark(cell: Cell): Boolean {
		return when (sameValueHighlightMode) {
			SameValueHighlightMode.NUMBERS_AND_MARK_CELLS -> {
				cell.value == 0 && (
						cell.primaryMarks.hasNumber(highlightedValue)
								|| (doubleMarksMode != DoubleMarksMode.SINGLE && cell.secondaryMarks.hasNumber(highlightedValue))
						)
			}

			else -> {
				false
			}
		}
	}

	// Each cell is divided to 3 rows for all marks drawing
	private fun drawCellMarks(cell: Cell, cellLeft: Int, cellTop: Int, canvas: Canvas) {
		if (doubleMarksMode != DoubleMarksMode.SINGLE) {
			if (!cell.primaryMarks.isEmpty) { // primary marks are drawn in the 2nd row only, centre-aligned.
				val numberOfCols = cell.primaryMarks.marksValues.size
				val positionShift = if (numberOfCols < 4) 1 else 0
				textMarkPaint.isFakeBoldText = true
				setMarksTextSize(cell)

				if (sameValueHighlightMode == SameValueHighlightMode.NUMBERS_AND_MARK_VALUES) {
					cell.primaryMarks.marksValues.indexOf(highlightedValue).let { index ->
						if (index >= 0) {
							drawHighlightCircleAt(0, numberOfCols + 2 * positionShift, index + positionShift, cellLeft, cellTop, canvas)
						}
					}
				}

				cell.primaryMarks.marksValues.forEachIndexed { index, value ->
					val isInvalid =
						highlightInvalidPencilMarks && (
								cell.row?.contains(value) == true || cell.column?.contains(value) == true || cell.sector?.contains(value) == true
						)
					drawCellMarkAt(0, numberOfCols + 2 * positionShift, index + positionShift, cellLeft, cellTop, canvas, value, isInvalid)
				}
			}

			if (!cell.secondaryMarks.isEmpty) { // secondary marks go to 1st and 3rd row
				textMarkPaint.isFakeBoldText = false
				textMarkPaint.textSize = marksTextSize * 0.9f
				if (sameValueHighlightMode == SameValueHighlightMode.NUMBERS_AND_MARK_VALUES) {
					cell.secondaryMarks.marksValues.indexOf(highlightedValue).let { highlightIndex ->
						if (highlightIndex >= 0) {
							val position = getSecondaryMarkPosition(cell.secondaryMarks.marksValues, highlightIndex)
							drawHighlightCircleAt(5, 0, position, cellLeft, cellTop, canvas)
						}
					}
				}

				cell.secondaryMarks.marksValues.forEachIndexed { index, value ->
					val position = getSecondaryMarkPosition(cell.secondaryMarks.marksValues, index)
					val isInvalid =
						highlightInvalidPencilMarks && (
								cell.row?.contains(value) == true || cell.column?.contains(value) == true || cell.sector?.contains(value) == true
						)
					drawCellMarkAt(5, 0, position, cellLeft, cellTop, canvas, value, isInvalid)
				}
			}

		} else { // single marks mode (positional across all 3 rows)
			textMarkPaint.isFakeBoldText = false
			textMarkPaint.textSize = marksTextSize

			if (sameValueHighlightMode == SameValueHighlightMode.NUMBERS_AND_MARK_VALUES && cell.primaryMarks.marksValues.contains(highlightedValue)) {
				drawHighlightCircleAt(3, 3, highlightedValue - 1, cellLeft, cellTop, canvas)
			}
			for (value in cell.primaryMarks.marksValues) {
				val isInvalid =
					highlightInvalidPencilMarks && (
							cell.row?.contains(value) == true || cell.column?.contains(value) == true || cell.sector?.contains(value) == true
					)
				drawCellMarkAt(3, 3, value - 1, cellLeft, cellTop, canvas, value, isInvalid)
			}
		}
	}

	private fun getSecondaryMarkPosition(values: List<Int>, index: Int): Int {
		if (doubleMarksMode == DoubleMarksMode.WITH_POSITIONAL) {
			return values[index] - 1
		}

		var atCornerIndex = index
		var remainingSize = values.size

		// top-left corner
		val corner1Count = (remainingSize + 3) / 4
		if (atCornerIndex < corner1Count) {
			return atCornerIndex
		}
		atCornerIndex -= corner1Count
		remainingSize -= corner1Count

		// top-right corner
		val corner2Count = (remainingSize + 2) / 3
		if (atCornerIndex < corner2Count) {
			return 5 - corner2Count + atCornerIndex
		}
		atCornerIndex -= corner2Count
		remainingSize -= corner2Count

		// bottom-left corner
		val corner3Count = (remainingSize + 1) / 2
		if (atCornerIndex < corner3Count) {
			return 5 + atCornerIndex
		}
		atCornerIndex -= corner3Count
		remainingSize -= corner3Count

		// bottom-right corner
		val corner4Count = remainingSize
		return 9 - corner4Count + atCornerIndex
	}

	// Determine the font size to use
	private fun setMarksTextSize(cell: Cell) {
		val allMarksText = cell.primaryMarks.marksValues.joinToString("")
		textMarkPaint.textSize = marksTextSize
		textMarkPaint.getTextBounds(allMarksText, 0, allMarksText.length, bounds)
		if (bounds.width() / cellWidth > 0.97f) { // if necessary scale down the size of text, and recalculate the bounds
			textMarkPaint.textSize = textMarkPaint.textSize * 0.97f * cellWidth / bounds.width()
		}
	}

	private fun drawCellMarkAt(
		row0Cols: Int,
		row1Cols: Int,
		position: Int,
		cellLeft: Int,
		cellTop: Int,
		canvas: Canvas,
		value: Int,
		isInvalid: Boolean = false
	) {
		val (posX, posY) = getCellMarkXY(row0Cols, row1Cols, position, cellLeft, cellTop)
		if (isInvalid) {
			canvas.drawCircle(posX, posY, textMarkPaint.textSize * 0.55f, backgroundInvalid)
		}
		val markValueStr = fontSet.convert(value)
		val originalColor = textMarkPaint.color // we need to restore the color for the rest of marks in this cell
		if (value == highlightedValue) textMarkPaint.color = textMarkHighlightedColor
		if (isInvalid) textMarkPaint.color = textInvalid.color

		textMarkPaint.textAlign = Paint.Align.LEFT
		textMarkPaint.getTextBounds(markValueStr, 0, 1, bounds)
		canvas.drawText(markValueStr, posX - bounds.exactCenterX(), posY + bounds.height() / 2, textMarkPaint)
		textMarkPaint.color = originalColor
	}

	private fun drawHighlightCircleAt(row0Cols: Int, row1Cols: Int, position: Int, cellLeft: Int, cellTop: Int, canvas: Canvas) {
		val (posX, posY) = getCellMarkXY(row0Cols, row1Cols, position, cellLeft, cellTop)
		canvas.drawCircle(posX, posY, textMarkPaint.textSize * 0.55f, backgroundMarksHighlighted)
	}

	private fun getCellMarkXY(row0Cols: Int, row1Cols: Int, position: Int, cellLeft: Int, cellTop: Int): Pair<Float, Float> {
		var markCol = position
		var markRow = 0
		var numberOfCols = row0Cols
		if (markCol >= numberOfCols) {
			markCol -= row0Cols
			markRow = 1
			numberOfCols = row1Cols
		}
		if (markCol >= numberOfCols) {
			markCol -= row1Cols
			markRow = 2
			numberOfCols = 9 - row0Cols - row1Cols
		}

		val markSpaceWidth = (cellWidth - 2 * sectorLineWidth) / numberOfCols
		val markSpaceHeight = (cellHeight - 2 * sectorLineWidth) / 3
		val posX = cellLeft + sectorLineWidth + (markCol + 0.5f) * markSpaceWidth
		val posY = cellTop + sectorLineWidth + (markRow + 0.5f) * markSpaceHeight
		return posX to posY
	}

	fun getColor(context: Context, color: Colors): Int {
		return MaterialColors.getColor(context, color.attr, Color.TRANSPARENT)
	}

	fun setAllColorsFromThemedContext(context: Context) {
		// Grid lines
		line.color = getColor(context, Colors.LINE)
		sectorLine.color = getColor(context, Colors.SECTOR_LINE)

		// Normal cell
		textValue.color = getColor(context, Colors.VALUE_TEXT)
		textMarkColor = getColor(context, Colors.MARKS_TEXT)
		background.color = getColor(context, Colors.BACKGROUND)

		// Default view behaviour is to highlight a view that has the focus. This highlights the entire board and leads to incorrect colours.
		defaultFocusHighlightEnabled = false

		// Read-only cell
		textGivenValue.color = getColor(context, Colors.GIVEN_VALUE_TEXT)
		backgroundGivenValue.color = getColor(context, Colors.GIVEN_VALUE_BACKGROUND)

		// Even 3x3 boxes
		isEvenBoxColoringEnabled = getColor(context, Colors.EVEN_BACKGROUND) != Color.TRANSPARENT
		textEvenCells.color = getColor(context, Colors.EVEN_TEXT)
		textEvenCellsMarksColor = getColor(context, Colors.EVEN_TEXT)
		backgroundEvenCells.color = getColor(context, Colors.EVEN_BACKGROUND)
		textEvenGivenValueCells.color = getColor(context, Colors.EVEN_GIVEN_VALUE_TEXT)
		backgroundEvenGivenValueCells.color = getColor(context, Colors.EVEN_GIVEN_VALUE_BACKGROUND)

		// Touched
		textTouched.color = getColor(context, Colors.TOUCHED_TEXT)
		textTouchedMarksColor = getColor(context, Colors.TOUCHED_MARKS_TEXT)
		backgroundTouched.color = getColor(context, Colors.TOUCHED_BACKGROUND)

		// Selected / focused
		selectedCellFrame.color = (getColor(context, Colors.SELECTED_BACKGROUND))

		// Highlighted cell
		textHighlighted.color = getColor(context, Colors.HIGHLIGHTED_TEXT)
		backgroundHighlighted.color = getColor(context, Colors.HIGHLIGHTED_BACKGROUND)
		textMarkHighlightedColor = getColor(context, Colors.HIGHLIGHTED_MARKS_TEXT)
		backgroundMarksHighlighted.color = getColor(context, Colors.HIGHLIGHTED_MARKS_BACKGROUND)

		// Invalid values
		textInvalid.color = getColor(context, Colors.TEXT_ERROR)
		backgroundInvalid.color = getColor(context, Colors.BACKGROUND_ERROR)

		for (i in 1..9) {
			customColors[i] = BackgroundColorPrefs.getColor(context, i)
		}
	}

	/**
	 * Reloads custom background colors from preferences and redraws the board.
	 */
	fun reloadCustomColors() {
		for (i in 1..9) {
			customColors[i] = BackgroundColorPrefs.getColor(context, i)
		}
		invalidate()
	}

	fun setGame(game: SudokuGame) {
		this.game = game
		board = game.board
	}

	var board: SudokuBoard
		get() = _board
		set(newBoard) {
			_board = newBoard
			_board.ensureOnChangeListener(this::postInvalidate)
			isInitialized = true
			postInvalidate()
		}

	var isReadOnlyPreview: Boolean = false
		set(newValue) {
			if (field != newValue) {
				field = newValue
				postInvalidate()
				onReadonlyChangeListener?.invoke()
			}
		}

	fun hideTouchedCellHint() {
		touchedCell = null
		postInvalidate()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val widthSize = MeasureSpec.getSize(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		val heightSize = MeasureSpec.getSize(heightMeasureSpec)
		var width: Int
		var height: Int
		if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
			width = widthSize
		} else {
			width = DEFAULT_BOARD_SIZE
		}
		if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
			height = heightSize
		} else {
			height = DEFAULT_BOARD_SIZE
		}

		// Ensure the board is square
		height = min(height, width)
		width = height
		cellWidth = (width - paddingLeft - paddingRight) / 9.0f
		cellHeight = (height - paddingTop - paddingBottom) / 9.0f
		setMeasuredDimension(width, height)
		val cellTextSize = cellHeight * 0.75f
		textValue.textSize = cellTextSize
		textGivenValue.textSize = cellTextSize
		textInvalid.textSize = cellTextSize
		textEvenCells.textSize = cellTextSize
		textEvenGivenValueCells.textSize = cellTextSize
		textTouched.textSize = cellTextSize
		textFocused.textSize = cellTextSize
		textHighlighted.textSize = cellTextSize

		// compute offsets in each cell to center the rendered number
		numberTop = ((cellHeight - textValue.textSize) / 2).toInt()

		// add some offset because in some resolutions marks are cut-off in the top
		marksTop = cellHeight / 50.0f
		// shrink if char is too wide
		marksTextSize = (cellHeight - marksTop * 2) / 3.0f * fontSet.requiredRatio()

		sectorLineWidth = computeSectorLineWidth(width, height)
		selectedCellFrame.style = Paint.Style.STROKE
		selectedCellFrame.strokeWidth = sectorLineWidth.toFloat()
	}

	private fun computeSectorLineWidth(widthInPx: Int, heightInPx: Int): Int {
		val sizeInPx = min(widthInPx, heightInPx)
		val dipScale = context.resources.displayMetrics.density
		val sizeInDip = sizeInPx / dipScale
		var sectorLineWidthInDip = 2.0f
		if (sizeInDip > 150) {
			sectorLineWidthInDip = 3.0f
		}
		return (sectorLineWidthInDip * dipScale).toInt()
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (isReadOnlyPreview || !isInitialized) return false

		val x = event.x.toInt()
		val y = event.y.toInt()
		when (event.action) {
			MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> touchedCell = getCellAtPoint(x, y)
			MotionEvent.ACTION_UP -> {
				selectedCell = getCellAtPoint(x, y)
				performClick()
			}

			MotionEvent.ACTION_CANCEL -> touchedCell = null
		}
		postInvalidate()
		return true
	}

	override fun performClick(): Boolean {
		invalidate() // selected cell has changed, update board as soon as you can
		selectedCell?.let(onCellTappedListener)
		if (autoHideTouchedCellHint) {
			touchedCell = null
		}
		return super.performClick()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		if (isReadOnlyPreview) return false

		val selectedCell = selectedCell

		when (keyCode) {
			KeyEvent.KEYCODE_DPAD_UP -> return moveCellSelection(0, -1)
			KeyEvent.KEYCODE_DPAD_RIGHT -> return moveCellSelection(1, 0)
			KeyEvent.KEYCODE_DPAD_DOWN -> return moveCellSelection(0, 1)
			KeyEvent.KEYCODE_DPAD_LEFT -> return moveCellSelection(-1, 0)
			KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DEL -> {
				// clear value in selected cell
				if (selectedCell != null) {
					if (event.isAltPressed) {
						game.setCellPrimaryMarks(selectedCell, CellMarks.EMPTY, true)
					} else if (event.isShiftPressed) {
						game.setCellSecondaryMarks(selectedCell, CellMarks.EMPTY, true)
					} else {
						setCellValue(selectedCell, 0)
					}
				}
				return true
			}

			KeyEvent.KEYCODE_DPAD_CENTER -> {
				selectedCell?.let(onCellTappedListener)
				return true
			}
		}

		if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9 && selectedCell != null) {
			val selNumber = keyCode - KeyEvent.KEYCODE_0
			if (event.isAltPressed) {    // add or remove number in cell's primary marks
				game.setCellPrimaryMarks(selectedCell, selectedCell.primaryMarks.toggleNumber(selNumber), true)
			} else if (doubleMarksMode != DoubleMarksMode.SINGLE && event.isShiftPressed) {    // add or remove number in secondary marks
				game.setCellSecondaryMarks(selectedCell, selectedCell.secondaryMarks.toggleNumber(selNumber), true)
			} else {  // enter number in cell
				setCellValue(selectedCell, selNumber)
			}
			return true
		}

		return false
	}

	/**
	 * Sets the cell value, with confirmation if it's an overwrite in play mode.
	 */
	private fun setCellValue(cell: Cell, value: Int) {
		if (value != 0 && cell.value != 0 && cell.value != value && !board.isEditMode && !shouldHighlightAsInvalid(cell)) {
			AlertDialog.Builder(context)
				.setMessage(R.string.overwrite_confirmation)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					game.setCellValue(cell, value, true)
					onValueEntered(value)
				}
				.setNegativeButton(android.R.string.cancel, null)
				.show()
		} else {
			val valToSet = if (value != 0 && value == cell.value) 0 else value
			game.setCellValue(cell, valToSet, true)
			onValueEntered(valToSet)
		}
	}

	private fun onValueEntered(value: Int) {
		highlightedValue = value
		if (moveCellSelectionOnPress && value != 0) {
			moveCellSelectionRight()
		}
	}

	/**
	 * Moves selected cell by one cell to the right. If edge is reached, selection
	 * skips on beginning of another line.
	 */
	fun moveCellSelectionRight() {
		val selectedCell = selectedCell ?: return

		if (!moveCellSelection(1, 0)) {
			var selRow = selectedCell.rowIndex
			selRow++
			if (!moveCellSelectionTo(selRow, 0)) {
				moveCellSelectionTo(0, 0)
			}
		}
		postInvalidate()
	}

	/**
	 * Moves selected by vx cells right and vy cells down. vx and vy can be negative. Returns true,
	 * if new cell is selected.
	 *
	 * @param vx Horizontal offset, by which move selected cell.
	 * @param vy Vertical offset, by which move selected cell.
	 */
	private fun moveCellSelection(vx: Int, vy: Int): Boolean {
		val selectedCell = selectedCell ?: return false
		val newRow = selectedCell.rowIndex + vy
		val newCol = selectedCell.columnIndex + vx
		return moveCellSelectionTo(newRow, newCol)
	}

	/**
	 * Moves selection to the cell given by row and column index.
	 *
	 * @param row Row index of cell which should be selected.
	 * @param col Column index of cell which should be selected.
	 * @return True, if cell was successfully selected.
	 */
	fun moveCellSelectionTo(row: Int, col: Int): Boolean {
		if ((col >= 0) && (col < SudokuBoard.SUDOKU_SIZE) && (row >= 0) && (row < SudokuBoard.SUDOKU_SIZE)) {
			selectedCell = board.getCell(row, col)
			postInvalidate()
			return true
		}
		return false
	}

	fun clearCellSelection() {
		selectedCell = null
		postInvalidate()
	}

	/**
	 * Returns cell at given screen coordinates. Returns null if no cell is found.
	 */
	private fun getCellAtPoint(x: Int, y: Int): Cell? {
		// take into account padding
		val lx = x - paddingLeft
		val ly = y - paddingTop
		val row = (ly / cellHeight).toInt()
		val col = (lx / cellWidth).toInt()
		return if (col >= 0 && col < SudokuBoard.SUDOKU_SIZE && row >= 0 && row < SudokuBoard.SUDOKU_SIZE) {
			board.getCell(row, col)
		} else {
			null
		}
	}

	fun blinkValue(digit: Int) {
		val anim: Animation = AlphaAnimation(0.0f, 1.0f)
		anim.duration = 50 //You can manage the blinking time with this parameter
		anim.startOffset = 20
		anim.repeatMode = Animation.REVERSE
		anim.repeatCount = 4
		blinker.blink(digit)
	}

	companion object {
		const val DEFAULT_BOARD_SIZE = 100

		private class Blinker(private val sudokuBoard: SudokuBoardView) : Timer(250) {
			private var blinkCount = 0
			private var blinkingDigit = 0

			fun blink(newDigit: Int) {
				blinkingDigit = newDigit
				blinkCount = 0
				start()
			}

			override fun step(): Boolean {
				if (sudokuBoard.blinkingDigit == 0) {
					sudokuBoard.blinkingDigit = blinkingDigit
					blinkCount += 1
				} else {
					sudokuBoard.blinkingDigit = 0
					if (blinkCount >= 2) { // stop blinking
						sudokuBoard.postInvalidate()
						return true
					}
				}
				sudokuBoard.postInvalidate()
				return false
			}
		}
	}
}
