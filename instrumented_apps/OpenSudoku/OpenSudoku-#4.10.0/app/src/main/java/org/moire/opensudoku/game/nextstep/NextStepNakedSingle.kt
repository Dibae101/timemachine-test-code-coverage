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

/** Strategy: Naked Single
 */
class NextStepNakedSingle(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForNakedSingle()
	}

	/** Strategy: Naked Single
	 *
	 * Find a cell with only one candidate
	 *
	 * Message:
	 *
	 * 		"Naked Single"
	 *  	" ⊞ Candidate ➠ {5}"
	 *  	" ⊞ Cell ➠ [ r5c2 ]"
	 * 		" ✎ enter value {5} into [ r5c2 ]"
	 *
	 */
	private fun checkForNakedSingle(): Boolean {

		board.cells.forEach { house ->
			house.forEach forEachCell@{ cell ->
				if (cell.value > 0) return@forEachCell // cell has already a value
				if (cell.primaryMarks.marksValues.size != 1) return@forEachCell // not only one candidate
				val actionCandidate = cell.primaryMarks.marksValues.first()
				nextStepState = NextStepStates.STEP_FOUND
				nextStepStrategyId = StrategyIds.NAKED_SINGLE
				nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
				nextStepActionSetValues[actionCandidate] = mutableListOf(cell)
				when(hintLevel) {
					HintLevels.LEVEL1 -> {
						nextStepText = nextStepStrategyName
					}
					HintLevels.LEVEL2 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_naked_single_candidate,
							"{$actionCandidate}")
					}
					HintLevels.LEVEL3 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_naked_single_candidate,
							"{$actionCandidate}")
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_naked_single_cell,
							"[${cell.gridAddress}]")
						cellsToHighlight[HintHighlight.REGION] = listOf(cell).map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.CAUSE] = listOf(cell).map { it.rowIndex to it.columnIndex }
					}
					HintLevels.LEVEL4 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_naked_single_candidate,
							"{$actionCandidate}")
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_naked_single_cell,
							"[${cell.gridAddress}]")
						nextStepText += "\n" + getNextStepActionSetValuesAsText()
						cellsToHighlight[HintHighlight.REGION] = listOf(cell).map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.CAUSE] = listOf(cell).map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.TARGET] = listOf(cell).map { it.rowIndex to it.columnIndex }
					}
				}
				return@checkForNakedSingle true
			} // forEachCell@
		} // forEach house
		return false
	}

}

