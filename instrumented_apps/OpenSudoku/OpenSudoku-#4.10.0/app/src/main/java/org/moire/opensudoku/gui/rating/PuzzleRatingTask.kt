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

package org.moire.opensudoku.gui.rating

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.moire.opensudoku.R
import org.moire.opensudoku.db.SudokuDatabase
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.nextstep.StrategyLevelIds
import org.moire.opensudoku.gui.screen.folder_list.FolderTaskModel
import org.moire.opensudoku.gui.SuidGenerator3F

class PuzzleRatingTask(val puzzleRatingParams: PuzzleRatingTaskParams) : FolderTaskModel() {

	private fun ratePuzzle(puzzleRatingParams: PuzzleRatingTaskParams, context: Context): Boolean {

		val game: SudokuGame

		// set values for ProgressUiState
		corruptedCount = 0
		maxValue = SudokuBoard.SUDOKU_SIZE * SudokuBoard.SUDOKU_SIZE
		currentValue = 0
		/*folderName*/ titleParam = "${puzzleRatingParams.puzzleId}"
		puzzleRatingParams.fileName = "?" // placeholder for the rating result

		// use database to get the data for the puzzle
		SudokuDatabase(context, true).use { db ->
			game  = db.getPuzzle(puzzleRatingParams.puzzleId!!, false)!!
		}

		titleParam = SuidGenerator3F().encode(game.board)
		if (game.userNote.isNotEmpty())
			titleParam += "\n" + context.getString(R.string.note) + game.userNote

		currentValue = game.board.valuesCount

		val puzzleRating = PuzzleRating(game, context)
		val puzzleRatingState = puzzleRating.startRatingOriginal()

		when(puzzleRatingState) {
			RatingResultState.PROBLEM_FOUND -> {
				puzzleRatingParams.fileName = puzzleRating.getProblemText()
				return false
			}
			RatingResultState.RATING_FINISHED -> {
				// update rating for the puzzle in the database
				SudokuDatabase(context, false).use { db ->
					db.updatePuzzleRating(game.id,
						puzzleRating.getRatingLevel(),
						puzzleRating.getRatingValue())
				}
				puzzleRatingParams.fileName = puzzleRating.getRatingText()
				return true
			}
			RatingResultState.RATING_INCOMPLETE -> {
				// update rating for the puzzle in the database
				// -> currently: finished with "NO STEP FOUND"
				SudokuDatabase(context, false).use { db ->
					db.updatePuzzleRating(game.id,
						StrategyLevelIds.MASTER.getStrategyLevelId(),
						999)
				}
				puzzleRatingParams.fileName = puzzleRating.getIncompleteRatingText()
				return false
			}
			else -> {
				throw IllegalStateException("Illegal RatingResultState -> check coding!")
			}
		}

	}

	override suspend fun run(context: Context, onTaskFinished: (Long, Boolean, String) -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
		val ratingDone = ratePuzzle(puzzleRatingParams, context)
		onTaskFinished(0, ratingDone, puzzleRatingParams.fileName!!)
		}
	}

}
