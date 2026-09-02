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
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.gui.screen.game_play.HintsQueue
import org.moire.opensudoku.gui.SudokuBoardView
import java.util.Collections

class IMControlPanel : LinearLayout {
	private var context: Context
	private lateinit var boardView: SudokuBoardView
	private lateinit var game: SudokuGame
	val imPopup: IMPopup = IMPopup(this)
	val imInsertOnTap: IMInsertOnTap = IMInsertOnTap(this)
	val imSelectOnTap: IMSelectOnTap = IMSelectOnTap(this)
	private val _inputMethods: MutableList<InputMethod> = ArrayList()
	internal var highlightCompletedValues = true
	private var hintsQueue: HintsQueue? = null
	internal var selectedNumber = 0

	var activeMethodIndex = -1
		private set

	val currentInputMethod: InputMethod? get() {
		return if (activeMethodIndex >= 0) inputMethods[activeMethodIndex] else null
	}

	internal var editMode = InputMethod.MODE_EDIT_VALUE
		set(value) {
			field = if (boardView.board.isEditMode && value != InputMethod.MODE_EDIT_COLOR) InputMethod.MODE_EDIT_VALUE
				else if (value == InputMethod.MODE_EDIT_SECONDARY_MARKS && !isDoubleMarksEnabled) InputMethod.MODE_EDIT_PRIMARY_MARKS
				else if (value == InputMethod.MODE_EDIT_COLOR && !isBackgroundColorEnabled) InputMethod.MODE_EDIT_VALUE
			 	else value
		}

	internal var showDigitCount = false
		set(value) {
			if (field != value) {
				field = value
				_inputMethods.forEach { m -> m.digitButtons?.values?.forEach { b -> b.showNumbersPlaced = value } }
			}
		}

	var isSwitchModeButtonEnabled: Boolean = true
		set(value) {
			if (field != value) {
				field = value
				_inputMethods.forEach {
					it.switchModeButton?.isEnabled = value
				}
			}
		}
	var isDoubleMarksEnabled: Boolean = true
		set(value) {
			if (value != field) {
				field = value
				if (editMode == InputMethod.MODE_EDIT_SECONDARY_MARKS) {
					editMode = InputMethod.MODE_EDIT_PRIMARY_MARKS
				}
				_inputMethods.forEach {
					it.secondaryMarksButton?.isVisible = value
				}
			}
		}

	var isBackgroundColorEnabled: Boolean = true
		set(value) {
			if (value != field) {
				field = value
				if (editMode == InputMethod.MODE_EDIT_COLOR && !value) {
					editMode = InputMethod.MODE_EDIT_VALUE
				}
				_inputMethods.forEach {
					it.onBackgroundColorEnabledChanged(value)
				}
				currentInputMethod?.update()
			}
		}
	private val onCellTapListener = { cell: Cell ->
		if (activeMethodIndex != -1) {
			_inputMethods[activeMethodIndex].onCellTapped(cell)
		}
	}
	private val onCellSelected = { cell: Cell? ->
		if (activeMethodIndex != -1) {
			_inputMethods[activeMethodIndex].onCellSelected(cell)
		}
	}
	private val switchModeListener = OnClickListener { _: View? -> activateNextInputMethod() }

	constructor(context: Context) : super(context) {
		this.context = context
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		this.context = context
	}

	fun initialize(board: SudokuBoardView, game: SudokuGame, hintsQueue: HintsQueue?) {
		board.onCellTappedListener = onCellTapListener
		board.onSelectedCellUpdate = onCellSelected
		boardView = board
		this.game = game
		this.hintsQueue = hintsQueue
		createInputMethods()
	}

	/**
	 * Activates first enabled input method. If such method does not exists, nothing
	 * happens.
	 */
	fun activateFirstInputMethod() {
		ensureInputMethods()
		if (activeMethodIndex == -1 || !_inputMethods[activeMethodIndex].isEnabled) {
			activateInputMethod(0)
		}
	}

	/**
	 * Activates given input method (see INPUT_METHOD_* constants). If the given method is
	 * not enabled, activates first available method after this method.
	 *
	 * @param methodID ID of method input to activate.
	 */
	fun activateInputMethod(methodID: Int) {
		@Suppress("HardCodedStringLiteral")
		require(!(methodID < -1 || methodID >= _inputMethods.size)) { "Invalid method id: $methodID." }
		ensureInputMethods()
		if (activeMethodIndex != -1) {
			_inputMethods[activeMethodIndex].deactivate()
		}
		var idFound = false
		var id = methodID
		var numOfCycles = 0
		if (id != -1) {
			while (numOfCycles <= _inputMethods.size) {
				if (_inputMethods[id].isEnabled) {
					ensureInputMethod(id)
					idFound = true
					break
				}
				id++
				if (id == _inputMethods.size) {
					id = 0
				}
				numOfCycles++
			}
		}
		if (!idFound) {
			id = -1
		}
		for (i in _inputMethods.indices) {
			_inputMethods[i].inputMethodView?.visibility = if (i == id) VISIBLE else GONE
		}
		activeMethodIndex = id
		if (activeMethodIndex != -1) {
			val activeMethod = _inputMethods[activeMethodIndex]
			if (boardView.board.isEditMode) editMode = InputMethod.MODE_EDIT_VALUE
			activeMethod.primaryMarksButton?.isEnabled = !boardView.board.isEditMode
			activeMethod.secondaryMarksButton?.isEnabled = !boardView.board.isEditMode && isDoubleMarksEnabled
			activeMethod.backgroundColorButton?.isEnabled = !boardView.board.isEditMode
			activeMethod.activate()
			hintsQueue?.showOneTimeHint(activeMethod.inputMethodName!!, activeMethod.nameResID, activeMethod.helpResID)
		}
	}

	fun activateNextInputMethod() {
		ensureInputMethods()
		var id = activeMethodIndex + 1
		if (id >= _inputMethods.size) {
			hintsQueue?.showOneTimeHint("thatIsAll", R.string.that_is_all, R.string.im_disable_modes_hint)
			id = 0
		}
		activateInputMethod(id)
	}

	val inputMethods: List<InputMethod>
		get() = Collections.unmodifiableList(_inputMethods)

	/**
	 * This should be called when activity is paused (so Input Methods can do some cleanup,
	 * for example properly dismiss dialogs because of WindowLeaked exception).
	 */
	fun pause() {
		for (im in _inputMethods) {
			im.onPause()
		}
	}

	/**
	 * Ensures that all input method objects are created.
	 */
	private fun ensureInputMethods() {
		@Suppress("HardCodedStringLiteral")
		check(_inputMethods.isNotEmpty()) { "Input methods are not created yet. Call initialize() first." }
	}

	private fun createInputMethods() {
		if (_inputMethods.isEmpty()) {
			addInputMethod(INPUT_METHOD_POPUP, imPopup)
			addInputMethod(INPUT_METHOD_INSERT_ON_TAP, imInsertOnTap)
			addInputMethod(INPUT_METHOD_SELECT_ON_TAP, imSelectOnTap)
		}
	}

	private fun addInputMethod(methodIndex: Int, im: InputMethod) {
		im.initialize(context, this, game, boardView)
		_inputMethods.add(methodIndex, im)
	}

	/**
	 * Ensures that control panel for given input method is created.
	 */
	private fun ensureInputMethod(methodID: Int) {
		val im = _inputMethods[methodID]
		if (im.inputMethodView == null) {
			im.createInputMethodView()
			im.switchModeButton?.setOnClickListener(switchModeListener)
			this.addView(im.inputMethodView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
		}
	}

	fun onImEnabledChange() {
		isSwitchModeButtonEnabled = _inputMethods.count(InputMethod::isEnabled) > 1
	}

	fun saveState(settings: GameSettings) {
		settings.selectedInputMethod = activeMethodIndex
		settings.lastGameKeyboardSelectedNumber = selectedNumber
		settings.lastGameKeyboardEditMode = editMode
	}

	fun restoreState(settings: GameSettings) {
		// restore state of control panel itself
		val methodId = settings.selectedInputMethod
		if (methodId != -1) {
			activateInputMethod(methodId)
		}

		if (game.id != settings.lastGamePuzzleId) { // don't restore if it's a different game
			return
		}
		selectedNumber = settings.lastGameKeyboardSelectedNumber
		editMode = settings.lastGameKeyboardEditMode
		currentInputMethod?.update()
	}

	companion object {
		const val INPUT_METHOD_POPUP = 0
		const val INPUT_METHOD_INSERT_ON_TAP = 1
		const val INPUT_METHOD_SELECT_ON_TAP = 2
	}
}
