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
import org.moire.opensudoku.gui.IconButton
import org.moire.opensudoku.gui.NumberButton
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.utils.BackgroundColorPrefs


class IMSelectOnTap(val parent: ViewGroup) : InputMethod() {
	var isMoveCellSelectionOnPress = true

	/**
	 * If set to true, buttons for numbers, which occur in [SudokuBoard]
	 * more than [SudokuBoard.SUDOKU_SIZE]-times, will be highlighted.
	 */
	private var selectedCell: Cell? = null
	private lateinit var clearButton: IconButton

	// Conceptually these behave like RadioButtons. However, it's difficult to style a RadioButton
	// without re-implementing all the drawables, and they would require a custom parent layout
	// to work properly in a ConstraintLayout, so it's simpler and more consistent in the UI to
	// handle the toggle logic in the code here.
	private lateinit var enterNumberButton: MaterialButton
	override var primaryMarksButton: MaterialButton? = null
	override var secondaryMarksButton: MaterialButton? = null
	override var backgroundColorButton: MaterialButton? = null
	override var switchModeButton: Button? = null

	override val nameResID: Int
		get() = R.string.select_on_tap
	override val helpResID: Int
		get() = R.string.im_select_on_tap_hint
	override val abbrName: String
		get() = context.getString(R.string.select_on_tap_abbr)

	private val numberButtonClicked = View.OnClickListener { v: View ->
		var selectedDigit = v.tag as Int
		val selCell = selectedCell
		if (selCell != null) {
			when (controlPanel.editMode) {
				MODE_EDIT_VALUE -> if (selectedDigit in 0..9) {
					setCellValue(selCell, selectedDigit, true)
				}

				MODE_EDIT_PRIMARY_MARKS -> if (selectedDigit == 0) {
					game.setCellPrimaryMarks(selCell, CellMarks.EMPTY, true)
				} else if (selectedDigit in 1..9) {
					game.setCellPrimaryMarks(selCell, selCell.primaryMarks.toggleNumber(selectedDigit), true)
				}

				MODE_EDIT_SECONDARY_MARKS -> if (selectedDigit == 0) {
					game.setCellSecondaryMarks(selCell, CellMarks.EMPTY, true)
				} else if (selectedDigit in 1..9) {
					game.setCellSecondaryMarks(selCell, selCell.secondaryMarks.toggleNumber(selectedDigit), true)
				}

				MODE_EDIT_COLOR -> if (selectedDigit in 0..9) {
					val newColor = if (selectedDigit == selCell.color) 0 else selectedDigit
					game.setCellColor(selCell, newColor, true)
				}
			}
		}
	}

	override fun onValueEntered(cell: Cell, value: Int) {
		boardView.highlightedValue = value
		if (isMoveCellSelectionOnPress && value != 0) {
			boardView.moveCellSelectionRight()
		}
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

	private val modeButtonClicked = View.OnClickListener { v: View ->
		controlPanel.editMode = v.tag as Int
		update()
	}

	private val onCellsChangeListener = { if (isActive) update() }

	override fun initialize(context: Context, controlPanel: IMControlPanel, game: SudokuGame, board: SudokuBoardView) {
		super.initialize(context, controlPanel, game, board)
		game.board.ensureOnChangeListener(onCellsChangeListener)
	}

	override fun createControlPanelView(abbrName: String): View {
		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val imPanelView = inflater.inflate(R.layout.im_keypad, parent, false)

		val numberButtons = HashMap<Int, NumberButton>()
		numberButtons[1] = imPanelView.findViewById(R.id.button_1)
		numberButtons[2] = imPanelView.findViewById(R.id.button_2)
		numberButtons[3] = imPanelView.findViewById(R.id.button_3)
		numberButtons[4] = imPanelView.findViewById(R.id.button_4)
		numberButtons[5] = imPanelView.findViewById(R.id.button_5)
		numberButtons[6] = imPanelView.findViewById(R.id.button_6)
		numberButtons[7] = imPanelView.findViewById(R.id.button_7)
		numberButtons[8] = imPanelView.findViewById(R.id.button_8)
		numberButtons[9] = imPanelView.findViewById(R.id.button_9)
		val colorValueText: ColorStateList = makeTextColorStateList(boardView)
		val colorBackground: ColorStateList = makeBackgroundColorStateList(boardView)
		for (num in numberButtons.keys) {
			val button = numberButtons[num]!!
			button.tag = num
			button.symbolToDisplay = boardView.fontSet.convert(num)
			button.scalingMultiplier = boardView.fontSet.requiredRatio()
			button.maxChar = boardView.fontSet.maxChar
			button.setOnClickListener(numberButtonClicked)
			button.setOnLongClickListener(numberButtonLongClicked)
			button.showNumbersPlaced = this.controlPanel.showDigitCount
			button.enableAllNumbersPlaced = this.controlPanel.highlightCompletedValues
			button.backgroundTintList = colorBackground
			button.setTextColor(colorValueText)
		}
		digitButtons = numberButtons

		val clearButton = imPanelView.findViewById<IconButton>(R.id.button_clear)
		clearButton.tag = 0
		clearButton.setOnClickListener(numberButtonClicked)
		clearButton.setOnLongClickListener(clearButtonLongClicked)
		clearButton.backgroundTintList = colorBackground
		clearButton.iconTint = colorValueText
		this.clearButton = clearButton

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
			isVisible = this@IMSelectOnTap.controlPanel.isDoubleMarksEnabled
		}

		backgroundColorButton = imPanelView.findViewById<MaterialButton>(R.id.background_color).apply {
			tag = MODE_EDIT_COLOR
			setOnClickListener(modeButtonClicked)
			backgroundTintList = colorBackground
			isEnabled = !boardView.board.isEditMode
			isVisible = this@IMSelectOnTap.controlPanel.isBackgroundColorEnabled
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

	override fun onActivated() {
		onCellSelected(if (boardView.isReadOnlyPreview) null else boardView.selectedCell)
	}

	override fun onCellSelected(cell: Cell?) {
		boardView.highlightedValue = cell?.value ?: 0
		selectedCell = cell
		update()
	}

	override fun update() {
		val isColorMode = controlPanel.editMode == MODE_EDIT_COLOR
		val colorBackground: ColorStateList = makeBackgroundColorStateList(boardView)
		val isInputAllowed = selectedCell?.isEditable == true || boardView.board.isEditMode
		clearButton.isEnabled = isInputAllowed

		// Determine which buttons to check, based on the value / marks in the selected cell
		var buttonsToCheck: MutableList<Int> = ArrayList()
		when (controlPanel.editMode) {
			MODE_EDIT_VALUE -> {
				enterNumberButton.isChecked = true
				primaryMarksButton?.isChecked = false
				secondaryMarksButton?.isChecked = false
				backgroundColorButton?.isChecked = false
				selectedCell?.let { buttonsToCheck.add(it.value) }
			}

			MODE_EDIT_PRIMARY_MARKS -> {
				enterNumberButton.isChecked = false
				primaryMarksButton?.isChecked = true
				secondaryMarksButton?.isChecked = false
				backgroundColorButton?.isChecked = false
				selectedCell?.let { buttonsToCheck = it.primaryMarks.marksValues }
			}

			MODE_EDIT_SECONDARY_MARKS -> {
				enterNumberButton.isChecked = false
				primaryMarksButton?.isChecked = false
				secondaryMarksButton?.isChecked = true
				backgroundColorButton?.isChecked = false
				selectedCell?.let { buttonsToCheck = it.secondaryMarks.marksValues }
			}

			MODE_EDIT_COLOR -> {
				enterNumberButton.isChecked = false
				primaryMarksButton?.isChecked = false
				secondaryMarksButton?.isChecked = false
				backgroundColorButton?.isChecked = true
				selectedCell?.let { buttonsToCheck = if (it.color != 0) mutableListOf(it.color) else mutableListOf() }
			}
		}
		val valuesUseCount = game.board.valuesUseCount
		digitButtons?.values?.forEach { button ->
			val tag = button.tag as Int

			// Enable / disable the button, depending on the editable state of the selected cell
			// This has to come first, calling setChecked() later doesn't work if the button is
			// not editable when setChecked() is called.
			button.isEnabled = isInputAllowed

			// Check the button if necessary
			button.isChecked = buttonsToCheck.contains(tag)

			if (isColorMode) {
				button.backgroundTintList = ColorStateList.valueOf(BackgroundColorPrefs.getColor(context, tag))
			} else {
				button.backgroundTintList = colorBackground
			}

			// Update the count of numbers placed
			val valueCount = valuesUseCount[tag]
			button.setNumbersPlaced(valueCount)
		}
	}
}
