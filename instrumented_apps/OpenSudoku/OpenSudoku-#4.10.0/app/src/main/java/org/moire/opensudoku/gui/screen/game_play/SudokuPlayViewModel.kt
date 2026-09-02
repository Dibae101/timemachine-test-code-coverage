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

package org.moire.opensudoku.gui.screen.game_play

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.nextstep.StrategyLevelIds
import org.moire.opensudoku.gui.DbKeeperViewModel
import org.moire.opensudoku.gui.inputmethod.IMControlPanel
import org.moire.opensudoku.gui.rating.PuzzleRating
import org.moire.opensudoku.gui.rating.RatingResultState

data class SudokuPlayUiState(
	var playedGame: SudokuGame? = null
)

class SudokuPlayViewModel(application: Application) : DbKeeperViewModel(application) {
	private val _uiState = MutableStateFlow(SudokuPlayUiState())
	val uiState = _uiState.asStateFlow()
	var initStarted: Boolean = false
		private set
	private val _initFinishedState = MutableStateFlow(false)
	val initFinished = _initFinishedState.asStateFlow()

	lateinit var settings: GameSettings

	var game: SudokuGame? = null
		private set
	val lastCommandCell get() = game?.lastCommandCell
	var highlightedCells: Map<HintHighlight, List<Pair<Int, Int>>>? = null

	fun init(puzzleID: Long) {
		initStarted = true
		settings = GameSettings(getApplication())
		super.initDb(false) {
			db!!.getPuzzle(puzzleID, false)?.let { game ->
				this.game = game
				settings.lastGamePuzzleId = game.id
				_uiState.update {
					SudokuPlayUiState(
						playedGame = game,
					)
				}
				_initFinishedState.update { true }
				if (game.ratingLevel < 1) {
					ratePuzzle()
				}
			}
		}
	}

	private fun ratePuzzle() {
		viewModelScope.launch(Dispatchers.Default) {
			val currentGame = game ?: return@launch
			val tempBoard = currentGame.board.copyOriginal(false)
			val tempGame = SudokuGame(tempBoard)
			val puzzleRating = PuzzleRating(tempGame, getApplication())
			val state = puzzleRating.startRatingOriginal()
			if (state == RatingResultState.RATING_FINISHED) {
				currentGame.ratingLevel = puzzleRating.getRatingLevel()
				currentGame.ratingValue = puzzleRating.getRatingValue()
				updatePuzzleInDB()
			} else if (state == RatingResultState.RATING_INCOMPLETE) {
				currentGame.ratingLevel = StrategyLevelIds.MASTER.getStrategyLevelId()
				currentGame.ratingValue = 999
				updatePuzzleInDB()
			}
		}
	}

	fun updatePuzzleInDB() {
		game?.let { db?.updatePuzzle(it) }
	}

	fun getFolderName(): String? {
		return game?.let { db?.getFolderInfo(it.folderId)?.name }
	}

	override fun onCleared() {
		updatePuzzleInDB()
		super.onCleared()
	}

	fun saveInputState(imControlPanel: IMControlPanel) {
		imControlPanel.saveState(settings)
	}

	fun restoreInputState(imControlPanel: IMControlPanel) {
		imControlPanel.restoreState(settings)
	}
}
