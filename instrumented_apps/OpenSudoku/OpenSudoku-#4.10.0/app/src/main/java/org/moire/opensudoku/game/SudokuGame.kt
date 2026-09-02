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
package org.moire.opensudoku.game

import android.content.ContentValues
import android.os.SystemClock
import org.moire.opensudoku.db.PuzzlesColumn
import org.moire.opensudoku.game.command.AbstractCommand
import org.moire.opensudoku.game.command.ClearAllColorsCommand
import org.moire.opensudoku.game.command.ClearAllMarksCommand
import org.moire.opensudoku.game.command.ClearAllPrimaryMarksCommand
import org.moire.opensudoku.game.command.ClearAllSecondaryMarksCommand
import org.moire.opensudoku.game.command.CommandStack
import org.moire.opensudoku.game.command.EditCellPrimaryMarksCommand
import org.moire.opensudoku.game.command.EditCellSecondaryMarksCommand
import org.moire.opensudoku.game.command.FillInMarksCommand
import org.moire.opensudoku.game.command.FillInMarksWithAllValuesCommand
import org.moire.opensudoku.game.command.SetCellColorCommand
import org.moire.opensudoku.game.command.SetCellValueAndRemoveMarksCommand
import org.moire.opensudoku.game.command.SetCellValueCommand
import java.time.Instant

class SudokuGame(board: SudokuBoard) {
	init {
		board.game = this
	}
	var id: Long = -1
	var folderId: Long = -1
	var created: Long = 0
	var state: Int
	var mistakeCounter: Int? = 0

	/** hintUsage
	 * This variable is used to count the usage of the hint function to get a next step.
	 * Depending on the hint level the count will be increased by 1,2,4 or 8.
	 * 1 -> only the strategy name ... 8 -> the complete solution including the needed actions
	 */
	var hintUsage: Int? = 0

	var userNote: String = ""

	var suid: String? = null

	var solution: String? = null

	// rating of the puzzle
	var ratingLevel: Int = 0
	var ratingValue: Int = 0

	private var isSolverUsed = false
	internal var isRemoveMarksOnEntry = true
	internal var isCheckIndirectErrors: Boolean = true
	internal var isCheckDirectErrors: Boolean = true
	internal var onPuzzleSolvedListener: (() -> Unit)? = null
	internal var onDigitFinishedManuallyListener: ((Int) -> Unit)? = null
	internal var onInvalidDigitEnteredListener: (() -> Unit)? = null
	private var activeFromTime: Long = -1 // time when current activity has become active.

	var board: SudokuBoard = board
		internal set(newCells) {
			field = newCells
			newCells.validateAllCells()
			commandStack = CommandStack(newCells)
			field.ensureOnChangeListener {
				if (field.isEditMode) {
					suid = null
					solution = null
				}
			}
		}

	var onHasUndoChangedListener: (() -> Unit)? = null
		set(newValue) {
			field = newValue
			commandStack.onEmptyChangeListener = newValue
		}

	var commandStack: CommandStack = CommandStack(this.board)
		set(value) {
			field = value
			field.onEmptyChangeListener = onHasUndoChangedListener
		}

	private val isPaused: Boolean
		get() = activeFromTime == -1L

	var lastPlayed: Long = 0
		get() = (if (isPaused) field else Instant.now().toEpochMilli())

	/**
	 * Time of game-play in milliseconds.
	 */
	var playingDuration: Long = 0
		get() = (if (isPaused) field else field + SystemClock.uptimeMillis() - activeFromTime)
		set(value) {
			field = value
			if (!isPaused) {
				activeFromTime = SystemClock.uptimeMillis()
			}
		}

	val contentValues: ContentValues
		get() {
			return ContentValues().apply {
				put(PuzzlesColumn.ORIGINAL_VALUES.nme, board.originalValues)
				put(PuzzlesColumn.CELLS_DATA.nme, board.serialize())
				put(PuzzlesColumn.CREATED.nme, created)
				put(PuzzlesColumn.LAST_PLAYED.nme, lastPlayed)
				put(PuzzlesColumn.STATE.nme, state)
				put(PuzzlesColumn.MISTAKE_COUNTER.nme, mistakeCounter)
				put(PuzzlesColumn.HINT_USAGE.nme, hintUsage)
				put(PuzzlesColumn.TIME.nme, playingDuration)
				put(PuzzlesColumn.USER_NOTE.nme, userNote)
				put(PuzzlesColumn.COMMAND_STACK.nme, commandStack.serialize())
				put(PuzzlesColumn.FOLDER_ID.nme, folderId)
				put(PuzzlesColumn.RATING_LEVEL.nme, ratingLevel)
				put(PuzzlesColumn.RATING_VALUE.nme, ratingValue)
				put(PuzzlesColumn.SUID.nme, suid)
				put(PuzzlesColumn.SOLUTION.nme, solution)
			}
		}

	init {
		state = GAME_STATE_NOT_STARTED
		this.board.ensureOnChangeListener {
			if (this.board.isEditMode) {
				suid = null
				solution = null
			}
		}
	}

	/**
	 * Sets value for the given cell. 0 means empty cell.
	 */
	fun setCellValue(cell: Cell, value: Int, isManual: Boolean) {
		if (cell.value == value || (!cell.isEditable && !board.isEditMode)) return
		require(!(value < 0 || value > 9)) { @Suppress("HardCodedStringLiteral") "Value must be between 0-9." }

		if (isRemoveMarksOnEntry) {
			executeCommand(SetCellValueAndRemoveMarksCommand(cell, value), isManual)
		} else {
			executeCommand(SetCellValueCommand(cell, value), isManual)
		}

		if (board.isEditMode) {
			cell.isEditable = value == 0
		}

		board.validateAllCells()

		if (isManual && value > 0) {
			val isErrorVisibleForCell = (isCheckIndirectErrors && !cell.isCorrect) || (isCheckDirectErrors && !cell.isValid)
			if (isErrorVisibleForCell) {
				onInvalidDigitEnteredListener?.invoke()
				mistakeCounter = mistakeCounter?.plus(1)
			} else if (board.valuesUseCount[value] == 9) {
				onDigitFinishedManuallyListener?.invoke(value)
			}
		}

		if (isSolved) {
			markGameAsCompletedAndPause()
			onPuzzleSolvedListener?.invoke()
		}
	}

	/**
	 * Sets primary marks attached to the given cell.
	 */
	fun setCellPrimaryMarks(cell: Cell, marks: CellMarks, isManual: Boolean): Boolean {
		if (cell.isEditable && cell.primaryMarks != marks) {
			executeCommand(EditCellPrimaryMarksCommand(cell, marks), isManual)
			return true
		}
		return false
	}

	/**
	 * Remove number from primary marks attached to the given cell.
	 */
	fun removeNumberFromCellPrimaryMarks(cell: Cell, number: Int, isManual: Boolean): Boolean {
		val marks = cell.primaryMarks.removeNumber(number)
		if (cell.isEditable && cell.primaryMarks != marks) {
			executeCommand(EditCellPrimaryMarksCommand(cell, marks), isManual)
			return true
		}
		return false
	}

	/**
	 * Sets secondary marks attached to the given cell.
	 */
	fun setCellSecondaryMarks(cell: Cell, marks: CellMarks, isManual: Boolean): Boolean {
		if (cell.isEditable && cell.secondaryMarks != marks) {
			executeCommand(EditCellSecondaryMarksCommand(cell, marks), isManual)
			return true
		}
		return false
	}

	fun setCellColor(cell: Cell, color: Int, isManual: Boolean): Boolean {
		if (cell.color != color) {
			executeCommand(SetCellColorCommand(cell, color), isManual)
			return true
		}
		return false
	}

	private fun executeCommand(c: AbstractCommand, isManual: Boolean) {
		commandStack.execute(c, isManual)
	}

	/**
	 * Undo last command.
	 */
	fun undo(): Cell? = commandStack.undo()

	fun hasSomethingToUndo(): Boolean = !commandStack.isEmpty

	fun setUndoCheckpoint() {
		commandStack.setCheckpoint()
	}

	fun undoToCheckpoint() {
		commandStack.undoToCheckpoint()
	}

	fun hasUndoCheckpoint(): Boolean = commandStack.hasCheckpoint()

	fun undoToBeforeMistake() {
		commandStack.undoToSolvableState()
	}

	val lastCommandCell: Cell?
		get() = commandStack.lastCommandCell

	/**
	 * Start game-play.
	 */
	fun start() {
		mistakeCounter = 0
		hintUsage = 0
		state = GAME_STATE_PLAYING
		resume()
	}

	fun resume() {
		// reset time we have spent playing so far, so time when activity was not active
		// will not be part of the game play time
		activeFromTime = SystemClock.uptimeMillis()
	}

	/**
	 * Pauses game-play (for example if activity pauses).
	 */
	fun pause() {
		// save time, we have spent playing so far - it will be reset after resuming
		val lastPlayingDuration = playingDuration // getter calculated value
		activeFromTime = -1L
		playingDuration = lastPlayingDuration
		lastPlayed = Instant.now().toEpochMilli()
	}

	val solutionCount: Int
		get() = board.solutionCount

	/**
	 * Solves puzzle from original state
	 */
	fun solve(): Int {
		isSolverUsed = true
		if (board.solutionCount != 1) {
			return board.solutionCount
		}
		board.cells.forEach { row ->
			row.forEach { cell ->
				setCellValue(cell, cell.solution, false)
			}
		}
		commandStack.clean()
		return 1
	}

	fun usedSolver(): Boolean = isSolverUsed

	/**
	 * Solves puzzle and fills in correct value for selected cell
	 */
	fun solveCell(cell: Cell) {
		require(board.solutionCount == 1) {
			@Suppress("HardCodedStringLiteral")
			"This puzzle has " + board.solutionCount + " solutions"
		}
		setCellValue(cell, cell.solution, true)
	}

	/**
	 * Pauses game-play and sets state to "completed". Called when puzzle is solved.
	 */
	private fun markGameAsCompletedAndPause() {
		pause()
		state = GAME_STATE_COMPLETED
	}

	/**
	 * Resets game.
	 */
	fun reset() {
		board.cells.forEach { row ->
			row.forEach { cell ->
				if (cell.isEditable) {
					cell.value = 0
				}
				cell.primaryMarks = CellMarks()
				cell.secondaryMarks = CellMarks()
			}
		}
		commandStack = CommandStack(board)
		board.validateAllCells()
		mistakeCounter = 0
		hintUsage = 0
		playingDuration = 0
		lastPlayed = 0
		state = GAME_STATE_NOT_STARTED
		isSolverUsed = false
		//ratingLevel = 0  ... do not reset
		//ratingValue = 0  ... do not reset
	}

	/**
	 * Returns true, if puzzle is solved. In order to know the current state, you have to call validate first.
	 */
	private val isSolved: Boolean
		get() = board.isSolved

	fun clearAllMarksManual() = executeCommand(ClearAllMarksCommand(), true)

	fun clearAllPrimaryMarksManual() = executeCommand(ClearAllPrimaryMarksCommand(), true)

	fun clearAllSecondaryMarksManual() = executeCommand(ClearAllSecondaryMarksCommand(), true)

	fun clearAllColorsManual() = executeCommand(ClearAllColorsCommand(), true)

	/**
	 * Fills in possible values which can be entered in each cell.
	 */
	fun fillInMarksManual() = executeCommand(FillInMarksCommand(), true)

	/**
	 * Fills in all values which can be entered in each cell.
	 */
	fun fillInMarksWithAllValuesManual() = executeCommand(FillInMarksWithAllValuesCommand(), true)

	companion object {
		const val GAME_STATE_PLAYING = 0
		const val GAME_STATE_NOT_STARTED = 1
		const val GAME_STATE_COMPLETED = 2
		fun createEmptyGame(isEditMode: Boolean): SudokuGame {
			val game = SudokuGame(SudokuBoard.createEmpty(isEditMode))
			game.created = Instant.now().toEpochMilli() // set creation time
			return game
		}
	}
}
