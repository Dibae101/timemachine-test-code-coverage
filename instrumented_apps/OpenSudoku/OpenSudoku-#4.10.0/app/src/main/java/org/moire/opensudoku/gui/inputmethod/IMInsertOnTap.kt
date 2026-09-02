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

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.CellMarks
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.gui.NumberButton
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.utils.BackgroundColorPrefs

/**
 * This class represents following type of number input workflow: Number buttons are displayed
 * in the sidebar, user selects one number and then fill values by tapping the cells.
 */
open class IMInsertOnTap(open val parent: ViewGroup) : InputMethod() {
	/**
	 * If set to true, buttons for numbers, which occur in [SudokuBoard]
	 * more than [SudokuBoard.SUDOKU_SIZE]-times, will be highlighted.
	 */
	internal var bidirectionalSelection = true
	internal var highlightSimilar = true

	// Conceptually these behave like RadioButtons. However, it's difficult to style a RadioButton
	// without re-implementing all the drawables, and they would require a custom parent layout
	// to work properly in a ConstraintLayout, so it's simpler and more consistent in the UI to
	// handle the toggle logic in the code here.
	protected lateinit var clearButton: MaterialButton
	protected lateinit var enterNumberButton: MaterialButton
	internal var onSelectedNumberChangedListener: ((Int) -> Unit)? = null
	override var switchModeButton: Button? = null
	override var primaryMarksButton: MaterialButton? = null
	override var secondaryMarksButton: MaterialButton? = null
	override var backgroundColorButton: MaterialButton? = null

	private val numberButtonClicked = View.OnClickListener { v: View ->
		val newValue = v.tag as Int
		if (controlPanel.selectedNumber != newValue) {
			controlPanel.selectedNumber = newValue
		} else {
			controlPanel.selectedNumber = 0
		}
		onSelectedNumberChanged()
		update()
	}

	private val numberButtonLongClicked by lazy {
		createColorLongClickListener(context, controlPanel) {
			update()
			boardView.reloadCustomColors()
		}
	}

	private val clearButtonLongClicked = View.OnLongClickListener {
		when (controlPanel.editMode) {
			MODE_EDIT_PRIMARY_MARKS -> game.clearAllPrimaryMarksManual()
			MODE_EDIT_SECONDARY_MARKS -> game.clearAllSecondaryMarksManual()
			MODE_EDIT_COLOR -> {
				game.clearAllColorsManual()
				boardView.reloadCustomColors()
			}
			else -> return@OnLongClickListener false
		}
		update()
		true
	}

	private val onCellsChangeListener = {
		if (isActive) {
			update()
		}
	}

	private val modeButtonClicked = View.OnClickListener { v: View ->
		controlPanel.editMode = v.tag as Int
		update()
	}

	override fun initialize(context: Context, controlPanel: IMControlPanel, game: SudokuGame, board: SudokuBoardView) {
		super.initialize(context, controlPanel, game, board)
		game.board.ensureOnChangeListener(onCellsChangeListener)
	}

	override val nameResID: Int
		get() = R.string.insert_on_tap
	override val helpResID: Int
		get() = R.string.im_insert_on_tap_hint
	override val abbrName: String
		get() = context.getString(R.string.insert_on_tap_abbr)

	override fun createControlPanelView(abbrName: String): View {
		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val imPanelView = inflater.inflate(R.layout.im_keypad, parent, false)

		val colorValueText: ColorStateList = makeTextColorStateList(boardView)
		val colorBackground: ColorStateList = makeBackgroundColorStateList(boardView)

		digitButtons = HashMap<Int, NumberButton>().also { numberButtons ->
			numberButtons[1] = imPanelView.findViewById(R.id.button_1)
			numberButtons[2] = imPanelView.findViewById(R.id.button_2)
			numberButtons[3] = imPanelView.findViewById(R.id.button_3)
			numberButtons[4] = imPanelView.findViewById(R.id.button_4)
			numberButtons[5] = imPanelView.findViewById(R.id.button_5)
			numberButtons[6] = imPanelView.findViewById(R.id.button_6)
			numberButtons[7] = imPanelView.findViewById(R.id.button_7)
			numberButtons[8] = imPanelView.findViewById(R.id.button_8)
			numberButtons[9] = imPanelView.findViewById(R.id.button_9)
			for ((key, button) in numberButtons) {
				button.run {
					tag = key
					symbolToDisplay = boardView.fontSet.convert(key)
					scalingMultiplier = boardView.fontSet.requiredRatio()
					maxChar = boardView.fontSet.maxChar
					setOnClickListener(numberButtonClicked)
					setOnLongClickListener(numberButtonLongClicked)
					showNumbersPlaced = this@IMInsertOnTap.controlPanel.showDigitCount
					enableAllNumbersPlaced = this@IMInsertOnTap.controlPanel.highlightCompletedValues
					backgroundTintList = colorBackground
					setTextColor(colorValueText)
				}
			}
		}

		clearButton = imPanelView.findViewById<MaterialButton>(R.id.button_clear).apply {
			tag = 0
			setOnClickListener(numberButtonClicked)
			setOnLongClickListener(clearButtonLongClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
		}

		enterNumberButton = imPanelView.findViewById<MaterialButton>(R.id.enter_number).apply {
			tag = MODE_EDIT_VALUE
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
		}

		primaryMarksButton = imPanelView.findViewById<MaterialButton>(R.id.primary_mark).apply {
			tag = MODE_EDIT_PRIMARY_MARKS
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
			isEnabled = !boardView.board.isEditMode
		}

		secondaryMarksButton = imPanelView.findViewById<MaterialButton>(R.id.secondary_mark).apply {
			tag = MODE_EDIT_SECONDARY_MARKS
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			iconTint = colorValueText
			isEnabled = !boardView.board.isEditMode
			isVisible = this@IMInsertOnTap.controlPanel.isDoubleMarksEnabled
		}

		backgroundColorButton = imPanelView.findViewById<MaterialButton>(R.id.background_color).apply {
			tag = MODE_EDIT_COLOR
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			isEnabled = !boardView.board.isEditMode
			isVisible = this@IMInsertOnTap.controlPanel.isBackgroundColorEnabled
		}
		sideBarBackgroundColorButton = backgroundColorButton

		switchModeButton = imPanelView.findViewById<Button>(R.id.switch_input_mode).apply {
			text = abbrName
			isEnabled = controlPanel.isSwitchModeButtonEnabled
			setTextColor(colorValueText)
			backgroundTintList = colorBackground
		}

		onBackgroundColorEnabledChanged(this.controlPanel.isBackgroundColorEnabled)

		return imPanelView
	}

	override fun update() {
		val isColorMode = controlPanel.editMode == MODE_EDIT_COLOR
		val colorBackground: ColorStateList = makeBackgroundColorStateList(boardView)

		when (controlPanel.editMode) {
			MODE_EDIT_VALUE -> {
				enterNumberButton.isChecked = true
				primaryMarksButton?.isChecked = false
				secondaryMarksButton?.isChecked = false
				backgroundColorButton?.isChecked = false
			}

			MODE_EDIT_PRIMARY_MARKS -> {
				enterNumberButton.isChecked = false
				primaryMarksButton?.isChecked = true
				secondaryMarksButton?.isChecked = false
				backgroundColorButton?.isChecked = false
			}

			MODE_EDIT_SECONDARY_MARKS -> {
				enterNumberButton.isChecked = false
				primaryMarksButton?.isChecked = false
				secondaryMarksButton?.isChecked = true
				backgroundColorButton?.isChecked = false
			}

			MODE_EDIT_COLOR -> {
				enterNumberButton.isChecked = false
				primaryMarksButton?.isChecked = false
				secondaryMarksButton?.isChecked = false
				backgroundColorButton?.isChecked = true
			}
		}
		val valuesUseCount = game.board.valuesUseCount
		digitButtons?.values?.forEach { button ->
			val tag = button.tag as Int
			if (controlPanel.selectedNumber == tag) {
				button.isChecked = true
				button.requestFocus()
			} else {
				button.isChecked = false
			}

			if (isColorMode) {
				button.backgroundTintList = ColorStateList.valueOf(BackgroundColorPrefs.getColor(context, tag))
			} else {
				button.backgroundTintList = colorBackground
			}

			// Update the count of numbers placed
			button.setNumbersPlaced(valuesUseCount[tag])
		}
		clearButton.isChecked = controlPanel.selectedNumber == 0
		boardView.highlightedValue = if (boardView.isReadOnlyPreview) 0 else controlPanel.selectedNumber
	}

	override fun onActivated() {
		update()
	}

	override fun onCellSelected(cell: Cell?) {
		super.onCellSelected(cell)
		if (bidirectionalSelection && cell != null) {
			val v = cell.value
			if (v != 0 && v != controlPanel.selectedNumber) {
				controlPanel.selectedNumber = cell.value
				update()
			}
		}
		boardView.highlightedValue = controlPanel.selectedNumber
	}

	private fun onSelectedNumberChanged() {
		if (highlightSimilar && !boardView.isReadOnlyPreview) {
			onSelectedNumberChangedListener?.invoke(controlPanel.selectedNumber)
			boardView.highlightedValue = controlPanel.selectedNumber
		}
	}

	override fun onCellTapped(cell: Cell) {
		var selectedDigit = controlPanel.selectedNumber
		when (controlPanel.editMode) {
			MODE_EDIT_PRIMARY_MARKS -> if (selectedDigit == 0) {
				game.setCellPrimaryMarks(cell, CellMarks.EMPTY, true)
			} else if (selectedDigit in 1..9) {
				val newMark = cell.primaryMarks.toggleNumber(selectedDigit)
				game.setCellPrimaryMarks(cell, newMark, true)
				if (!newMark.hasNumber(selectedDigit)) {
					boardView.clearCellSelection()
				}
			}

			MODE_EDIT_SECONDARY_MARKS -> if (selectedDigit == 0) {
				game.setCellSecondaryMarks(cell, CellMarks.EMPTY, true)
			} else if (selectedDigit in 1..9) {
				val newMark = cell.secondaryMarks.toggleNumber(selectedDigit)
				game.setCellSecondaryMarks(cell, newMark, true)
				// if we toggled the mark off we want to de-select the cell
				if (!newMark.hasNumber(selectedDigit)) {
					boardView.clearCellSelection()
				}
			}

			MODE_EDIT_VALUE -> {
				// Normal flow, just set the value (or clear it if it is repeated click)
				setCellValue(cell, selectedDigit, true)
			}

			MODE_EDIT_COLOR -> {
				val newColor = if (selectedDigit == cell.color) 0 else selectedDigit
				game.setCellColor(cell, newColor, true)
			}
		}
	}

	override fun onValueEntered(cell: Cell, value: Int) {
		if (value == 0) {
			boardView.clearCellSelection()
		}
	}
}
