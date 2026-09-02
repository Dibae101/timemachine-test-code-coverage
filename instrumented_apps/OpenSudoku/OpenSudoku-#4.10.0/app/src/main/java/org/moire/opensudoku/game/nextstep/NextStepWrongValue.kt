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

/** NextStepWrongValue
 *
 * Check all cells in the grid for a wrong value.
 *
 * We use the same structure to check for problems as to find a next step.
 * The result of the check will be stored in nextStepFound => problemFound!
 *
 */
class NextStepWrongValue(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context)
{

	override fun search(): Boolean {
		return checkCellsForWrongValue()
	}

	/** checkCellsForWrongValue()
	 *
	 * Check all cells in the grid for a wrong value
	 *
	 * Message:
	 * 			"Wrong value: {9}"
	 * 			" ⊞ Cell ➠ [ r5c7 ]"
	 * 			" ✎ replace {4} with {8}"
	 */
	private fun checkCellsForWrongValue(): Boolean {
		board.cells.forEach {
			it.forEach forCells@{ cell ->
				if (!cell.isEditable) return@forCells // cell was given
				if (cell.value == 0) return@forCells // cell is empty
				if (cell.value == cell.solution)  return@forCells // value is ok
				// wrong value found
				val wrongCell = cell
				val wrongValue = wrongCell.value
				val correctValue = wrongCell.solution
				nextStepState = NextStepStates.PROBLEM_FOUND
				nextStepStrategyId = StrategyIds.WRONG_VALUES
				nextStepStrategyName  = nextStepStrategyId.getStrategyName(context)
				when (hintLevel) {
				   HintLevels.LEVEL1 -> { nextStepText = nextStepStrategyName }
				   HintLevels.LEVEL2 -> { nextStepText = "$nextStepStrategyName: {${wrongValue}}" }
				   HintLevels.LEVEL3 -> {
					   nextStepText = "$nextStepStrategyName: {${wrongValue}}\n"
					   nextStepText +=
						   context.getString(R.string.hint_strategy_wrong_value_cell,
							   "[${wrongCell.gridAddress}]")
					   cellsToHighlight[HintHighlight.CAUSE] = listOf(wrongCell.rowIndex to wrongCell.columnIndex)
				   }
				   HintLevels.LEVEL4 -> {
					   nextStepText = "$nextStepStrategyName: {${wrongValue}}\n"
					   nextStepText +=
						   context.getString(R.string.hint_strategy_wrong_value_cell,
							   "[${wrongCell.gridAddress}]\n")
					   nextStepText +=
						   context.getString(R.string.hint_replace_value,
							   "{$wrongValue}","{$correctValue}")
					   cellsToHighlight[HintHighlight.CAUSE] = listOf(wrongCell.rowIndex to wrongCell.columnIndex)
					   cellsToHighlight[HintHighlight.TARGET] = listOf(wrongCell.rowIndex to wrongCell.columnIndex)
				   }
				}
				return@checkCellsForWrongValue true
			}
		}
		// no wrong cell found
		return false
	}

}

