/*
 * This file is part of Open S udoku - an open-source Sudoku game.
 * Copyright (C) 2009-2025 by Open Sudoku authors.
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
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import org.moire.opensudoku.R

/**
 * A button that displays a number the user can enter in to the grid.
 *
 * The display of the number on the button varies depending on the current edit mode.
 *
 * Exposes a state, app:all_numbers_placed, that is true if all 9 copies of this number
 * have been entered in to the grid. This can be used in a ColorStateList to adjust
 * the button's background/foreground colors if all 9 copies of a digit are entered.
 */
class NumberButton(context: Context, attrs: AttributeSet?) : MaterialButton(context, attrs) {
	/** Paint when entering main numbers  */
	private val enterNumberPaint: Paint = Paint()

	/** Paint for "numbers placed" count  */
	private val numbersPlacedPaint: Paint = Paint()

	/** True if the count of times the number is placed should be shown on the button  */
	internal var showNumbersPlaced = false
		set(value) {
			if (field != value) {
				field = value
				invalidate()
			}
		}

	/** the widest char in the set */
	internal var maxChar = 'X'

	/** Display SYMBOL instead of number in TAG */
 	internal var symbolToDisplay: String = "X"
		set(value) {
			if (field != value) {
				field = value
				invalidate()
			}
		}

	/** textSize multiplier to fit into the cell */
	internal var scalingMultiplier: Float

	/** Count of the number of times this number is placed in the puzzle  */
	private var numbersPlaced = 0

	/** True if the all_numbers_placed attribute is enabled  */
	internal var enableAllNumbersPlaced = false

	/** Bounds of the text to display  */
	private val textBounds = Rect()


	init {
		enterNumberPaint.isAntiAlias = true
		numbersPlacedPaint.isAntiAlias = true
		scalingMultiplier = 1f
	}

	override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
		super.onSizeChanged(w, h, oldWidth, oldHeight)

		// Adjust key text size, shrink if too wide
		val third = h / 3f * scalingMultiplier
		enterNumberPaint.textSize = third * 2
		numbersPlacedPaint.textSize = third / 1.5f
	}

	override fun onDraw(canvas: Canvas) {
		val left = paddingLeft
		val top = paddingTop
		val right = width - paddingRight
		val bottom = height - paddingBottom
		val midX = ((right + left) / 2.0).toFloat()
		val midY = ((bottom + top) / 2.0).toFloat()
		var textHeight: Float
		var textWidth: Float

		// Large numbers, vertically/horizontally centered, with optional small number at
		// the right showing the placed count.
		enterNumberPaint.color = currentTextColor
		enterNumberPaint.getTextBounds(symbolToDisplay, 0, 1, textBounds)
		textHeight = textBounds.height().toFloat()
		textWidth = enterNumberPaint.measureText(symbolToDisplay, 0, 1)
		canvas.drawText(symbolToDisplay, 0, 1, midX - textWidth / 2, midY + textHeight / 2, enterNumberPaint)

		if (showNumbersPlaced) {
			// Initial offset is immediately to the right of the largest number
			textWidth = enterNumberPaint.measureText(maxChar.toString(), 0, 1)
			val initialXOffset = midX + textWidth / 2

			// It's possible to enter more than 9 copies of a number in to the grid. Rather
			// than try and scale a 2 digit string, set it to "X", to both indicate an
			// error, and because "X" is the Roman numeral for 10.
			val btnNumberCnt = if (numbersPlaced <= 9) "$numbersPlaced" else "X"
			numbersPlacedPaint.color = currentTextColor
			if (isEnabled) numbersPlacedPaint.alpha = (255 * 0.68).toInt()
			numbersPlacedPaint.getTextBounds(maxChar.toString(), 0, 1, textBounds)
			textHeight = textBounds.height().toFloat()
			textWidth = numbersPlacedPaint.measureText(maxChar.toString(), 0, 1)

			// Draw the smaller number 1/4 of its width to the right of the large number
			canvas.drawText(btnNumberCnt, initialXOffset + textWidth / 4, midY + textHeight / 2, numbersPlacedPaint)
		}
	}

	override fun onCreateDrawableState(extraSpace: Int): IntArray {
		val state = super.onCreateDrawableState(extraSpace + 1)
		if (numbersPlaced == 9 && enableAllNumbersPlaced) {
			mergeDrawableStates(state, ALL_NUMBERS_PLACED_STATE)
		}
		return state
	}

	/** Sets the value to use for the count of placed numbers  */
	fun setNumbersPlaced(numbersPlaced: Int) {
		if (this.numbersPlaced != numbersPlaced) {
			this.numbersPlaced = numbersPlaced
			if (enableAllNumbersPlaced) {
				refreshDrawableState()
			}
			invalidate()
		}
	}

	override fun setTag(tag: Any) {
		@Suppress("UsePropertyAccessSyntax")
		super.setTag(tag)
		invalidate()
	}

	companion object {
		/** Attribute that corresponds to setting app:all_numbers_placed  */
		private val ALL_NUMBERS_PLACED_STATE = intArrayOf(R.attr.all_numbers_placed)
	}
}
