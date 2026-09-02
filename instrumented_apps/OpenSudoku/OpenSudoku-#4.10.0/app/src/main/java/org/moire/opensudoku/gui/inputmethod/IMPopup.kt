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

import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.CellMarks
import org.moire.opensudoku.game.SameValueHighlightMode
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.gui.NumberButton

class IMPopup(override val parent: ViewGroup) : IMInsertOnTap(parent) {

	private var highlightKeypadButtons: MutableMap<Int, NumberButton>? = null

	/**
	 * If set to true, buttons for numbers, which occur in [SudokuBoard]
	 * more than [SudokuBoard.SUDOKU_SIZE]-times, will be highlighted.
	 */
	private var editCellDialog: IMPopupDialog? = null
	private lateinit var selectedCell: Cell
	override var switchModeButton: Button? = null

	override var primaryMarksButton: MaterialButton? = null
		get() = editCellDialog?.primaryMarksButton

	override var secondaryMarksButton: MaterialButton? = null
		get() = editCellDialog?.secondaryMarksButton

	override var backgroundColorButton: MaterialButton? = null
		get() = editCellDialog?.backgroundColorButton

	/**
	 * Occurs when user selects number in EditCellDialog.
	 * Occurs when user edits marks in EditCellDialog
	 */
	private fun onCellUpdate(value: Int, primaryMarks: Array<Int>, secondaryMarks: Array<Int>) {
		var manualRecorded = game.setCellPrimaryMarks(selectedCell, CellMarks.fromIntArray(primaryMarks), true)
		manualRecorded = game.setCellSecondaryMarks(selectedCell, CellMarks.fromIntArray(secondaryMarks), !manualRecorded) || manualRecorded
		if (value != -1) {
			setCellValue(selectedCell, value, !manualRecorded)
		}
	}

	override fun onValueEntered(cell: Cell, value: Int) {
		// In Popup mode we don't want to clear selection or do anything else that InsertOnTap does
	}

	/**
	 * Occurs when popup dialog is closed.
	 */
	private val onPopupDismissedListener = DialogInterface.OnDismissListener { _: DialogInterface? -> boardView.hideTouchedCellHint() }

	private fun ensureEditCellDialog() {
		if (editCellDialog == null) {
			editCellDialog = IMPopupDialog(parent, context, boardView, controlPanel).apply {
				cellUpdateCallback = ::onCellUpdate
				colorUpdateCallback = { color -> game.setCellColor(selectedCell, color, true) }
				setOnDismissListener(onPopupDismissedListener)
				setShowNumberTotals(controlPanel.showDigitCount)
				setHighlightCompletedValues(controlPanel.highlightCompletedValues)
				digitButtons = numberButtons
				secondaryMarksButton.isVisible = controlPanel.isDoubleMarksEnabled
				backgroundColorButton?.isVisible = controlPanel.isBackgroundColorEnabled
			}
		}
	}

	override fun onActivated() {
		boardView.autoHideTouchedCellHint = false
		update()
	}

	override fun onDeactivated() {
		boardView.autoHideTouchedCellHint = true
	}

	override fun update() {
		val valuesUseCount = game.board.valuesUseCount
		highlightKeypadButtons?.values?.forEach { button ->
			val tag = button.tag as Int
			button.isChecked = controlPanel.selectedNumber == tag

			// Update the count of numbers placed
			button.setNumbersPlaced(valuesUseCount[tag])
		}
		boardView.highlightedValue = if (boardView.isReadOnlyPreview) 0 else controlPanel.selectedNumber
	}

	override fun onCellTapped(cell: Cell) {
		selectedCell = cell
		if (cell.isEditable || boardView.board.isEditMode) {
			ensureEditCellDialog()
			editCellDialog!!.setHighlightedValue(cell.value)
			editCellDialog!!.setHighlightedColor(cell.color)
			editCellDialog!!.setPrimaryMarks(cell.primaryMarks.marksValues)
			editCellDialog!!.setSecondaryMarks(cell.secondaryMarks.marksValues)
			val valuesUseCount = game.board.valuesUseCount
			editCellDialog!!.setValueCount(valuesUseCount)
			editCellDialog!!.show()
		} else {
			boardView.hideTouchedCellHint()
		}
	}

	override fun onPause() {
		// release dialog resource (otherwise WindowLeaked exception is logged)
		editCellDialog?.cancel()
	}

	override val nameResID: Int
		get() = R.string.popup
	override val helpResID: Int
		get() = R.string.im_popup_hint
	override val abbrName: String
		get() = context.getString(R.string.popup_abbr)

	override fun createControlPanelView(abbrName: String): View {
		val imPanelView = super.createControlPanelView(abbrName)
		highlightKeypadButtons = digitButtons!!.toMutableMap() // copy map as it will be overwritten by popup dialog later

		// In Popup mode, the sidebar keypad should not react to long clicks (only the popup dialog should).
		digitButtons?.values?.forEach { it.setOnLongClickListener(null) }

		switchModeButton = imPanelView.findViewById<Button>(R.id.switch_input_mode).apply {
			text = abbrName
			isEnabled = controlPanel.isSwitchModeButtonEnabled
			setTextColor(makeTextColorStateList(boardView))
			backgroundTintList = makeBackgroundColorStateList(boardView)
		}
		sideBarBackgroundColorButton = imPanelView.findViewById(R.id.background_color)
		onBackgroundColorEnabledChanged(this.controlPanel.isBackgroundColorEnabled)

		listOf(R.id.enter_number, R.id.primary_mark, R.id.secondary_mark, R.id.background_color, R.id.button_clear).forEach { imPanelView.findViewById<MaterialButton>(it).hide() }
		if (boardView.sameValueHighlightMode  == SameValueHighlightMode.NONE || boardView.sameValueHighlightMode == SameValueHighlightMode.HINTS_ONLY) {
			listOf(R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7, R.id.button_8, R.id.button_9)
				.forEach { id -> imPanelView.findViewById<MaterialButton>(id).hide() }
		}

		return imPanelView
	}
}

// Extension function to hide element but preserve it's space. Changing .isVisible for whole row/column makes reuse the space by other cells grid layout,
// this way we prevent it.
private fun MaterialButton.hide() {
	isEnabled = false
	alpha = 0.0.toFloat()
}
