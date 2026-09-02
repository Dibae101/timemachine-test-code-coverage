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

/** NextStepObsoleteCandidate
 *
 * Check that all values of solved cells in a house are not in use as a candidate in this house.
 *
 * We use the same structure to check for problems as to find a next step.
 * The result of the check will be stored in nextStepFound => problemFound!
 *
 */
class NextStepObsoleteCandidate(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context)
{

	override fun search(): Boolean {
		return checkCellsForObsoleteCandidate()
	}

	/** checkCellsForObsoleteCandidate()
	 *
	 * Check that a value of solved cell in not in use as a candidate
	 * in all houses for the solved cell.
	 *
	 * Message:
	 * 			"Obsolete Candidate: {5}"
	 * 			" ⊞ Cell ➠ [ r3c6 ]"
	 * 			" ✎ remove candidate {5} from [ r3c6 ]"
	 */
	private fun checkCellsForObsoleteCandidate(): Boolean {
		board.cells.forEach { row ->
			row.forEach forCells@{ cell ->
				if (cell.value > 0) return@forCells // cell has already a value
				val rowCells = cell.row
				val colCells = cell.column
				val boxCells = cell.sector
				for (candidate in cell.primaryMarks.marksValues) {
					for (group in arrayOf(rowCells, colCells, boxCells)) {
						if ((group ?: continue).contains(candidate)) {
							// obsolete candidate found
							val obsoleteCell = cell
							val obsoleteCandidate = candidate
							val obsoleteGroup = group
							nextStepState = NextStepStates.PROBLEM_FOUND
							nextStepStrategyId = StrategyIds.OBSOLETE_CANDIDATES
							nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
							nextStepActionRemoveCandidates[candidate] = mutableListOf(cell)
							when (hintLevel) {
								HintLevels.LEVEL1 -> {
									nextStepText = nextStepStrategyName
								}

								HintLevels.LEVEL2 -> {
									nextStepText = "$nextStepStrategyName: {$obsoleteCandidate}"
								}

								HintLevels.LEVEL3 -> {
									nextStepText = "$nextStepStrategyName: {${obsoleteCandidate}}\n"
									nextStepText +=
										context.getString(
											R.string.hint_strategy_wrong_value_cell,
											"[${obsoleteCell.gridAddress}]"
										)
									cellsToHighlight[HintHighlight.REGION] =
										obsoleteGroup.cells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] = obsoleteGroup.cells
										.filter { it.value == obsoleteCandidate }
										.map { it.rowIndex to it.columnIndex }
								}

								HintLevels.LEVEL4 -> {
									nextStepText = "$nextStepStrategyName: {${obsoleteCandidate}}\n"
									nextStepText +=
										context.getString(
											R.string.hint_strategy_obsolete_candidate_cell,
											"[${obsoleteCell.gridAddress}]\n"
										)
									nextStepText += getNextStepActionRemoveCandidatesAsText()
									cellsToHighlight[HintHighlight.REGION] =
										obsoleteGroup.cells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] = obsoleteGroup.cells
										.filter { it.value == obsoleteCandidate }
										.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.TARGET] =
										listOf(obsoleteCell).map { it.rowIndex to it.columnIndex }
								}
							}
							return@checkCellsForObsoleteCandidate true
						}
					}
				}
			}
		}
		// no obsolete candidate found
		return false
	}

}

