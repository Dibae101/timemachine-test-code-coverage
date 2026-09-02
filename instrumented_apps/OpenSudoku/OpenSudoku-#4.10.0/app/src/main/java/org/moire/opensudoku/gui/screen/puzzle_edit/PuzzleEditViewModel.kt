/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2025-2025 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.gui.screen.puzzle_edit

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.gui.DbKeeperViewModel
import org.moire.opensudoku.gui.SuidGenerator3F
import java.time.Instant

data class PuzzleEditUiState(
	var newPuzzle: SudokuGame? = null
)

class PuzzleEditViewModel(application: Application) : DbKeeperViewModel(application) {
	private val _uiState = MutableStateFlow(PuzzleEditUiState())
	val uiState = _uiState.asStateFlow()

	val solutionCount get() = _uiState.value.newPuzzle?.solutionCount ?: -1
	val serializedCells get() = newPuzzle?.board?.serialize(SudokuBoard.DATA_VERSION_PLAIN) ?: ""
	var initialized: Boolean = false
		private set

	private val newPuzzle get() = _uiState.value.newPuzzle
	private var savedOriginalValues: String = ""

	fun init(puzzleID: Long, targetFolderId: Long, initialBoard: SudokuBoard?) {
		initialized = true
		super.initDb(false) {
			_uiState.update {
				PuzzleEditUiState(
					newPuzzle = if (puzzleID != -1L) { // editing existing puzzle, read it from the database
						db!!.getPuzzle(puzzleID, true)!!.apply {
							savedOriginalValues = board.originalValues
							reset()
						}
					} else { // creating a new puzzle
						SudokuGame.createEmptyGame(true).apply {
							if (initialBoard != null) board = initialBoard
							folderId = targetFolderId
						}
					}
				)
			}
		}
	}

	fun isModified(): Boolean {
		val newPuzzle = newPuzzle ?: return false
		if (newPuzzle.board.isEmpty) {
			return false
		}
		if (newPuzzle.id < 0) {
			return true
		}
		val isOriginalModified = newPuzzle.board.originalValues != savedOriginalValues
		return isOriginalModified
	}

	// returns true if new puzzle was created, false if existing was updated
	fun savePuzzle(): Boolean {
		val newPuzzle = newPuzzle ?: return false
		savedOriginalValues = newPuzzle.board.originalValues

		newPuzzle.reset()
		newPuzzle.created = Instant.now().toEpochMilli()

		// board has changed, so new rating needed -> reset rating
		newPuzzle.ratingLevel = 0
		newPuzzle.ratingValue = 0

		if (newPuzzle.id < 0L) {
			newPuzzle.id = db!!.insertPuzzle(newPuzzle)
			return true
		} else {
			db!!.updatePuzzle(newPuzzle)
			return false
		}
	}

	fun getExistingPuzzleFolderName(): String? {
		val existingPuzzle = db!!.findPuzzle(newPuzzle!!.board.originalValues, false)
		if (existingPuzzle == null) return null
		return db!!.getFolderInfo(existingPuzzle.folderId)?.name
	}

	fun deserialize(clipDataText: String): SudokuBoard? {
		if (clipDataText.startsWith("SUID:")) {
			try {
				val plainData = SuidGenerator3F().decode(clipDataText.substring(5).trim())
				val (board, _) = SudokuBoard.deserialize(plainData, true)
				return board
			} catch (_: Exception) {
				return null
			}
		}
		val clipDataVersion = SudokuBoard.getDataVersion(clipDataText)
		if (clipDataVersion >= SudokuBoard.DATA_VERSION_PLAIN) {
			val board: SudokuBoard = try {
				val (board, _) = SudokuBoard.deserialize(clipDataText, true)
				board
			} catch (_: Exception) {
				return null
			}
			return board
		} else {
			return null
		}
	}

	fun setCells(cells: Array<Array<Cell>>) {
		newPuzzle!!.board.setCells(cells)
	}
}
