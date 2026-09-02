/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
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
package org.moire.opensudoku.gui.inputmethod

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import org.moire.opensudoku.R
import org.moire.opensudoku.game.SudokuBoard.Companion.SUDOKU_SIZE
import org.moire.opensudoku.gui.NumberButton
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.utils.BackgroundColorPrefs

/**
 * Dialog for selecting and entering values and marks.
 *
 * When entering a value, the dialog automatically closes.
 *
 * When entering a mark the dialog remains open, to allow multiple marks to be entered at once.
 */
class IMPopupDialog(val parent: ViewGroup, context: Context, private val boardView: SudokuBoardView, val controlPanel: IMControlPanel) : Dialog(context) {
	private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
	val numberButtons: MutableMap<Int, NumberButton> = HashMap()

	private var highlightedValue = 0
	private var highlightedColor = 0
	private val primaryMarksSelectedNumbers: MutableSet<Int> = HashSet()
	private val secondaryMarksSelectedNumbers: MutableSet<Int> = HashSet()
	private var showNumberTotals = false

	/** True if buttons with completed values should be highlighted  */
	private var highlightCompletedValues = false
	private var enterNumberButton: MaterialButton
	var primaryMarksButton: MaterialButton
	var secondaryMarksButton: MaterialButton
	var backgroundColorButton: MaterialButton? = null

	/**
	 * Callback to be invoked when user selects a new cell value and/or primary/secondary marks.
	 */
	internal lateinit var cellUpdateCallback: ((value: Int, primaryMarks: Array<Int>, secondaryMarks: Array<Int>) -> Unit)
	internal var colorUpdateCallback: ((color: Int) -> Unit)? = null

	private val valueCount: Array<Int> = Array(SUDOKU_SIZE + 1) { 0 }

	private val numberButtonClicked = View.OnClickListener { v: View ->
		val number = v.tag as Int
		when (controlPanel.editMode) {
			InputMethod.MODE_EDIT_VALUE -> {
				syncAndDismiss(number) // close the dialog in the value edit mode only
			}

			InputMethod.MODE_EDIT_PRIMARY_MARKS -> if ((v as MaterialButton).isChecked) {
				primaryMarksSelectedNumbers.add(number)
			} else {
				primaryMarksSelectedNumbers.remove(number)
			}

			InputMethod.MODE_EDIT_SECONDARY_MARKS -> if ((v as MaterialButton).isChecked) {
				secondaryMarksSelectedNumbers.add(number)
			} else {
				secondaryMarksSelectedNumbers.remove(number)
			}

			InputMethod.MODE_EDIT_COLOR -> {
				val newColor = if (number == highlightedColor) 0 else number
				syncAndDismissColor(newColor)
			}
		}
	}

	private val numberButtonLongClicked by lazy {
		InputMethod.createColorLongClickListener(context, controlPanel) {
			update()
			boardView.reloadCustomColors()
		}
	}

	/**
	 * Occurs when user presses "Clear" button.
	 */
	private val clearButtonListener = View.OnClickListener { v ->
		(v as MaterialButton).isChecked = false
		when (controlPanel.editMode) {
			InputMethod.MODE_EDIT_VALUE -> {
				syncAndDismiss(0) // close the dialog only in the value edit mode
			}

			InputMethod.MODE_EDIT_PRIMARY_MARKS ->                     // Clear the primary marks. Dialog should stay visible
				setPrimaryMarks(emptyList())

			InputMethod.MODE_EDIT_SECONDARY_MARKS ->                     // Clear the secondary marks. Dialog should stay visible
				setSecondaryMarks(emptyList())

			InputMethod.MODE_EDIT_COLOR ->
				syncAndDismissColor(0)
		}
		update() // don't close, update the view in the marks edit modes
	}

	/**
	 * Occurs when user presses "Close" button.
	 */
	private val closeButtonListener = View.OnClickListener { _: View? -> syncAndDismiss(-1) }

	/** Synchronises state with the hosting activity and dismisses the dialog  */
	private fun syncAndDismiss(newValue: Int) {
		cellUpdateCallback(newValue, primaryMarksSelectedNumbers.toTypedArray(), secondaryMarksSelectedNumbers.toTypedArray())
		dismiss()
	}

	private fun syncAndDismissColor(color: Int) {
		colorUpdateCallback?.invoke(color)
		dismiss()
	}

	init {
		val keypad = inflater.inflate(R.layout.im_popup_edit_value, parent, false)

		numberButtons[1] = keypad.findViewById(R.id.button_1)
		numberButtons[2] = keypad.findViewById(R.id.button_2)
		numberButtons[3] = keypad.findViewById(R.id.button_3)
		numberButtons[4] = keypad.findViewById(R.id.button_4)
		numberButtons[5] = keypad.findViewById(R.id.button_5)
		numberButtons[6] = keypad.findViewById(R.id.button_6)
		numberButtons[7] = keypad.findViewById(R.id.button_7)
		numberButtons[8] = keypad.findViewById(R.id.button_8)
		numberButtons[9] = keypad.findViewById(R.id.button_9)

		val colorValueText: ColorStateList = InputMethod.makeTextColorStateList(boardView)
		val colorBackground: ColorStateList = InputMethod.makeBackgroundColorStateList(boardView)

		for ((key, b) in numberButtons) {
			b.tag = key
			b.symbolToDisplay = boardView.fontSet.convert(key)
			b.maxChar = boardView.fontSet.maxChar
			b.scalingMultiplier = boardView.fontSet.requiredRatio()
			b.setOnClickListener(numberButtonClicked)
			b.setOnLongClickListener(numberButtonLongClicked)
			b.enableAllNumbersPlaced = highlightCompletedValues
			b.backgroundTintList = colorBackground
			b.setTextColor(colorValueText)
		}

		val clearButton = keypad.findViewById<MaterialButton>(R.id.button_clear)
		clearButton.tag = 0
		clearButton.setOnClickListener(clearButtonListener)
		clearButton.backgroundTintList = colorBackground
		clearButton.iconTint = colorValueText

		/* Switch mode, and update the UI */
		val modeButtonClicked = View.OnClickListener { v: View ->
			controlPanel.editMode = v.tag as Int
			update()
		}

		enterNumberButton = keypad.findViewById<MaterialButton>(R.id.enter_number).apply {
			tag = InputMethod.MODE_EDIT_VALUE
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
		}

		primaryMarksButton = keypad.findViewById<MaterialButton>(R.id.primary_mark).apply {
			tag = InputMethod.MODE_EDIT_PRIMARY_MARKS
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
		}

		secondaryMarksButton = keypad.findViewById<MaterialButton>(R.id.secondary_mark).apply {
			tag = InputMethod.MODE_EDIT_SECONDARY_MARKS
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
		}

		backgroundColorButton = keypad.findViewById<MaterialButton>(R.id.background_color).apply {
			tag = InputMethod.MODE_EDIT_COLOR
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			isVisible = this@IMPopupDialog.controlPanel.isBackgroundColorEnabled
		}

		val closeButton = keypad.findViewById<View>(R.id.button_close)
		closeButton.setOnClickListener(closeButtonListener)
		setContentView(keypad)
	}

	private fun update() {
		val isColorMode = controlPanel.editMode == InputMethod.MODE_EDIT_COLOR
		val colorBackground: ColorStateList = InputMethod.makeBackgroundColorStateList(boardView)
		// Determine which buttons to check, based on the value / marks in the selected cell
		val buttonsToHighlight: List<Int>
		when (controlPanel.editMode) {
			InputMethod.MODE_EDIT_VALUE -> {
				enterNumberButton.isChecked = true
				primaryMarksButton.isChecked = false
				secondaryMarksButton.isChecked = false
				backgroundColorButton?.isChecked = false
				buttonsToHighlight = listOf(highlightedValue)
			}

			InputMethod.MODE_EDIT_PRIMARY_MARKS -> {
				enterNumberButton.isChecked = false
				primaryMarksButton.isChecked = true
				secondaryMarksButton.isChecked = false
				backgroundColorButton?.isChecked = false
				buttonsToHighlight = ArrayList(primaryMarksSelectedNumbers)
			}

			InputMethod.MODE_EDIT_SECONDARY_MARKS -> {
				enterNumberButton.isChecked = false
				primaryMarksButton.isChecked = false
				secondaryMarksButton.isChecked = true
				backgroundColorButton?.isChecked = false
				buttonsToHighlight = ArrayList(secondaryMarksSelectedNumbers)
			}

			InputMethod.MODE_EDIT_COLOR -> {
				enterNumberButton.isChecked = false
				primaryMarksButton.isChecked = false
				secondaryMarksButton.isChecked = false
				backgroundColorButton?.isChecked = true
				buttonsToHighlight = listOf(highlightedColor)
			}

			else ->                 // Can't happen
				buttonsToHighlight = ArrayList()
		}
		for (button in numberButtons.values) {
			val tag = button.tag as Int

			// Check the button if necessary
			button.isChecked = buttonsToHighlight.contains(tag)

			if (isColorMode) {
				button.backgroundTintList = ColorStateList.valueOf(BackgroundColorPrefs.getColor(context, tag))
			} else {
				button.backgroundTintList = colorBackground
			}

			// Update the count of numbers placed
			if (valueCount.isNotEmpty()) {
				button.setNumbersPlaced(valueCount[tag])
			}
		}
	}

	fun setShowNumberTotals(newShowNumberTotals: Boolean) {
		if (showNumberTotals == newShowNumberTotals) {
			return
		}
		showNumberTotals = newShowNumberTotals
		for (button in numberButtons.values) {
			button.showNumbersPlaced = showNumberTotals
		}
	}

	fun setHighlightCompletedValues(highlightCompletedValues: Boolean) {
		if (this.highlightCompletedValues == highlightCompletedValues) {
			return
		}
		this.highlightCompletedValues = highlightCompletedValues
		for (b in numberButtons.values) {
			b.enableAllNumbersPlaced = this.highlightCompletedValues
		}
	}

	fun setHighlightedValue(value: Int) {
		highlightedValue = value
		update()
	}

	fun setHighlightedColor(color: Int) {
		highlightedColor = color
		update()
	}

	fun setPrimaryMarks(numbers: List<Int>) {
		primaryMarksSelectedNumbers.clear()
		primaryMarksSelectedNumbers.addAll(numbers)
		update()
	}

	fun setSecondaryMarks(numbers: List<Int>) {
		secondaryMarksSelectedNumbers.clear()
		secondaryMarksSelectedNumbers.addAll(numbers)
		update()
	}

	fun setValueCount(count: Array<Int>) {
		valueCount.fill(0)
		count.copyInto(valueCount)
		update()
	}
}
