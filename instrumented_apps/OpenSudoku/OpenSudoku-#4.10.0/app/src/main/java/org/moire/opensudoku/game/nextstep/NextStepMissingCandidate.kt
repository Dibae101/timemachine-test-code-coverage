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

package org.moire.opensudoku.game.nextstep

import android.content.Context
import org.moire.opensudoku.R
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SudokuBoard

/** NextStepMissingCandidate
 *
 * Check all cells in the grid for a missing candidate.
 * A non solved cell should contain the solution value as a candidate.
 *
 * We use the same structure to check for problems as to find a next step.
 * The result of the check will be stored in nextStepFound => problemFound!
 *
 */
class NextStepMissingCandidate(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context)
{

	override fun search(): Boolean {
		return checkCellsForMissingCandidate()
	}

	/** checkCellsForMissingCandidate()
	 *
	 * Check the primary marks for every empty cell.
	 * The marks must contain the solution value as a candidate.
	 *
	 * Message:
	 * 			"Missing Candidate: {5}"
	 * 			" ⊞ Cell ➠ [ r3c6 ]"
	 * 			" ✎ add candidate {5} into [ r3c6 ]"
	 * 		    "Are your primary pencil marks OK?"
	 */
	private fun checkCellsForMissingCandidate(): Boolean {
		board.cells.forEach { row ->
			row.forEach forCells@{ cell ->
				if (cell.value > 0) return@forCells // cell has already a value
				if (cell.primaryMarks.hasNumber(cell.solution)) return@forCells // candidate ok
				// missing candidate found
				val missingCell = cell
				val missingCandidate = missingCell.solution
				nextStepState = NextStepStates.PROBLEM_FOUND
				nextStepStrategyId = StrategyIds.MISSING_CANDIDATES
				nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
				when (hintLevel) {
					HintLevels.LEVEL1 -> {
						nextStepText = "$nextStepStrategyName\n\n"
						nextStepText += context.getString(R.string.hint_strategy_missing_candidate_note)
					}

					HintLevels.LEVEL2 -> {
						nextStepText = "$nextStepStrategyName: {${missingCandidate}}\n\n"
						nextStepText += context.getString(R.string.hint_strategy_missing_candidate_note)
					}

					HintLevels.LEVEL3 -> {
						nextStepText = "$nextStepStrategyName: {${missingCandidate}}\n"
						nextStepText +=
							context.getString(
								R.string.hint_strategy_missing_candidate_cell,
								"[${missingCell.gridAddress}]"
							) + "\n\n"
						nextStepText += context.getString(R.string.hint_strategy_missing_candidate_note)
						cellsToHighlight[HintHighlight.CAUSE] =
							listOf(missingCell.rowIndex to missingCell.columnIndex)
					}

					HintLevels.LEVEL4 -> {
						nextStepText = "$nextStepStrategyName: {$missingCandidate}\n"
						nextStepText +=
							context.getString(
								R.string.hint_strategy_missing_candidate_cell,
								"[${missingCell.gridAddress}]"
							) + "\n"
						nextStepText +=
							context.getString(
								R.string.hint_add_candidate_into,
								"{$missingCandidate}","[${missingCell.gridAddress}]"
							) + "\n\n"
						nextStepText += context.getString(R.string.hint_strategy_missing_candidate_note)
						cellsToHighlight[HintHighlight.CAUSE] =
							listOf(missingCell.rowIndex to missingCell.columnIndex)
						cellsToHighlight[HintHighlight.TARGET] =
							listOf(missingCell.rowIndex to missingCell.columnIndex)
					}
				}
				return@checkCellsForMissingCandidate true
			}
		}
		// no missing candidate found
		return false
	}

}

