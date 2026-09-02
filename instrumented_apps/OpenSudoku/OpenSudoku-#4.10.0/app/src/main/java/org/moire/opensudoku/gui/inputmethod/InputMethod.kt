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
package org.moire.opensudoku.gui.inputmethod

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.button.MaterialButton
import net.margaritov.preference.colorpicker.ColorPickerDialog
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.gui.NumberButton
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.utils.BackgroundColorPrefs
import org.moire.opensudoku.utils.FontSelector
import org.moire.opensudoku.utils.ThemeUtils.dimmedColor

/**
 * Base class for several input methods used to edit puzzle contents.
 */
abstract class InputMethod {
	abstract var switchModeButton: Button?
	abstract var primaryMarksButton: MaterialButton?
	abstract var secondaryMarksButton: MaterialButton?
	abstract var backgroundColorButton: MaterialButton?
	var sideBarBackgroundColorButton: MaterialButton? = null

	protected lateinit var context: Context
	protected lateinit var controlPanel: IMControlPanel
	protected lateinit var game: SudokuGame
	protected lateinit var boardView: SudokuBoardView
	protected var isActive = false
	var inputMethodView: View? = null
	internal var digitButtons: MutableMap<Int, NumberButton>? = null

	/**
	 * This should be unique name of input method.
	 */
	var inputMethodName: String? = null
		private set

	open fun initialize(context: Context, controlPanel: IMControlPanel, game: SudokuGame, board: SudokuBoardView) {
		this.context = context
		this.controlPanel = controlPanel
		this.game = game
		boardView = board
		inputMethodName = this.javaClass.simpleName
	}

	fun createInputMethodView(): View {
		val inputMethodView = createControlPanelView(abbrName)
		this.inputMethodView = inputMethodView
		return inputMethodView
	}

	/**
	 * This should be called when activity is paused (InputMethod can do some cleanup for example properly dismiss dialogs preventing WindowLeaked exception).
	 */
	open fun onPause() {}

	abstract val nameResID: Int
	abstract val helpResID: Int

	/**
	 * Gets abbreviated name of input method, which will be displayed on input method switch button.
	 */
	abstract val abbrName: String
	var isEnabled: Boolean = false
		set(enabled) {
			field = enabled
			if (!enabled) {
				controlPanel.activateNextInputMethod()
			}
			controlPanel.onImEnabledChange()
		}

	fun activate() {
		isActive = true
		onActivated()
	}

	fun deactivate() {
		isActive = false
		onDeactivated()
	}

	protected abstract fun createControlPanelView(abbrName: String): View
	protected open fun onActivated() {}
	protected open fun onDeactivated() {}
	abstract fun update()

	fun onBackgroundColorEnabledChanged(enabled: Boolean) {
		backgroundColorButton?.isVisible = enabled
		sideBarBackgroundColorButton?.isVisible = enabled

		val switchButton = switchModeButton ?: return
		val params = switchButton.layoutParams as? GridLayout.LayoutParams ?: return

		val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
		if (enabled) {
			if (isLandscape) {
				params.columnSpec = GridLayout.spec(2, 1, GridLayout.FILL, 1f)
				params.rowSpec = GridLayout.spec(4, 1, GridLayout.FILL, 1f)
			} else {
				params.rowSpec = GridLayout.spec(2, 1, GridLayout.FILL, 1f)
				params.columnSpec = GridLayout.spec(4, 1, GridLayout.FILL, 1f)
			}
		} else {
			if (isLandscape) {
				params.columnSpec = GridLayout.spec(1, 2, GridLayout.FILL, 2f)
				params.rowSpec = GridLayout.spec(4, 1, GridLayout.FILL, 1f)
			} else {
				params.rowSpec = GridLayout.spec(1, 2, GridLayout.FILL, 2f)
				params.columnSpec = GridLayout.spec(4, 1, GridLayout.FILL, 1f)
			}
		}
		switchButton.layoutParams = params
	}

	/**
	 * Called when cell is selected. Please note that cell selection can change without direct user interaction.
	 */
	open fun onCellSelected(cell: Cell?) {}

	/**
	 * Called when cell is tapped.
	 */
	open fun onCellTapped(cell: Cell) {}

	/**
	 * Sets the cell value, with confirmation if it's an overwrite in play mode.
	 */
	protected fun setCellValue(cell: Cell, value: Int, record: Boolean = true) {
		val isInvalid = if (cell.value != 0 && (boardView.board.isEditMode || cell.isEditable)) {
			if (boardView.board.isEditMode || boardView.highlightIndirectlyWrongValues) {
				!cell.isCorrect
			} else if (boardView.highlightDirectlyWrongValues) {
				boardView.board.validateAllCells()
				!cell.isValid
			} else {
				false
			}
		} else {
			false
		}

		if (value != 0 && cell.value != 0 && cell.value != value && !boardView.board.isEditMode && !isInvalid) {
			AlertDialog.Builder(context)
				.setMessage(R.string.overwrite_confirmation)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					game.setCellValue(cell, value, record)
					onValueEntered(cell, value)
				}
				.setNegativeButton(android.R.string.cancel, null)
				.show()
		} else {
			val valToSet = if (value != 0 && value == cell.value) 0 else value
			game.setCellValue(cell, valToSet, record)
			onValueEntered(cell, valToSet)
		}
	}

	/**
	 * Called after cell value is set.
	 */
	protected open fun onValueEntered(cell: Cell, value: Int) {}

	companion object {
		const val MODE_EDIT_VALUE = 0
		const val MODE_EDIT_PRIMARY_MARKS = 1
		const val MODE_EDIT_SECONDARY_MARKS = 2
		const val MODE_EDIT_COLOR = 3

		/**
		 * Creates a long click listener for number buttons that allows changing the background color
		 * when in color edit mode.
		 */
		fun createColorLongClickListener(context: Context, controlPanel: IMControlPanel, updateCallback: () -> Unit): View.OnLongClickListener {
			return View.OnLongClickListener { v: View ->
				if (controlPanel.editMode == MODE_EDIT_COLOR) {
					val index = v.tag as Int
					val initialColor = BackgroundColorPrefs.getColor(context, index)
					val symbol = FontSelector(context).convert(index)
					val dialog = ColorPickerDialog(context, initialColor, context.getString(R.string.choose_background_color_for, symbol))
					dialog.setOnColorChangedListener { color ->
						BackgroundColorPrefs.setColor(context, index, color)
						updateCallback()
					}
					dialog.show()
					return@OnLongClickListener true
				}
				false
			}
		}

		/**
		 * Generates a [ColorStateList] using colors from boardView suitable
		 * for use as text colors on a button.
		 *
		 *
		 * An XML color state list file can not be used because the colors may be
		 * changed at runtime instead of coming from a fixed theme.
		 *
		 * @param boardView the view to derive colors from
		 * @return suitable colors
		 * @see .makeBackgroundColorStateList
		 */
		// Note: It's tempting to make this part of NumberButton, but it's useful for other buttons
		// that are not NumberButton (e.g., the delete and mode switch buttons).
		fun makeTextColorStateList(boardView: SudokuBoardView): ColorStateList {
			val states = arrayOf(
				intArrayOf(R.attr.all_numbers_placed, android.R.attr.state_enabled),
				intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
				intArrayOf(android.R.attr.state_enabled),
				intArrayOf()
			)

			// The number being entered, or highlighted, so use the same colour as highlighted digits
			val allNumbersPlacedText = dimmedColor(boardView.textGivenValue.color)
			val selectedText = boardView.textHighlighted.color
			val notSelectedText = boardView.textValue.color
			val disabledText: Int = dimmedColor(boardView.textGivenValue.color)
			val colors = intArrayOf(
				allNumbersPlacedText, selectedText, notSelectedText, disabledText
			)
			return ColorStateList(states, colors)
		}

		/**
		 * Generates a [ColorStateList] using colors from boardView suitable
		 * for use as background colors on a button.
		 *
		 *
		 * An XML color state list file can not be used because the colors may be
		 * changed at runtime instead of coming from a fixed theme.
		 *
		 * @param boardView the view to derive colors from
		 * @return suitable colors
		 * @see .makeTextColorStateList
		 */
		fun makeBackgroundColorStateList(boardView: SudokuBoardView): ColorStateList {
			val states = arrayOf(
				intArrayOf(R.attr.all_numbers_placed, android.R.attr.state_enabled, -android.R.attr.state_checked),
				intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
				intArrayOf(android.R.attr.state_enabled),
				intArrayOf()
			)

			// The number being entered, or highlighted, so use the same colour as highlighted digits
			val allNumbersPlacedBackground = boardView.backgroundGivenValue.color
			val selectedBackground = boardView.backgroundHighlighted.color
			val notSelectedBackground = boardView.backgroundGivenValue.color
			val disabledBackground = boardView.background.color
			val colors = intArrayOf(allNumbersPlacedBackground, selectedBackground, notSelectedBackground, disabledBackground)
			return ColorStateList(states, colors)
		}
	}
}
