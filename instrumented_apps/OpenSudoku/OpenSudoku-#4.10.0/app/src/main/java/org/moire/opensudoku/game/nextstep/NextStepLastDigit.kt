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

/** Strategy: Last Digit (Full House)
 */
class NextStepLastDigit(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForLastDigit()
	}

	/** Strategy: Last Digit (Full House)
	 *
	 * Find last cell in a house (row, column or box) without a value
	 *
	 * Message:
	 * 		"Last Digit"
	 *  	" ⊞ Value ➠ {4}"
	 *  	" ⊞ House ➠ (b5)"
	 * 		" ✎ enter value {4} into [ r5c5 ]"
	 */
	private fun checkForLastDigit(): Boolean {

		// loop over all house types
		forHouseType@ for (houseType in HouseTypes.entries) {

			// get all houses for the house type
			val houseCellsArray = when (houseType) {
				HouseTypes.ROW -> board.getHousesRows()
				HouseTypes.COL -> board.getHousesColumns()
				HouseTypes.BOX -> board.getHousesSectors()
			}

			// loop over all houses
			forHouse@ for (houseCells in houseCellsArray) {
				val cellsToCheck = houseCells.cells.filter { it.value == 0 }
				if (cellsToCheck.size != 1) continue@forHouse
				val emptyCell = cellsToCheck.elementAt(0)
				val setValue = emptyCell.solution
				nextStepState = NextStepStates.STEP_FOUND
				nextStepStrategyId = StrategyIds.LAST_DIGIT
				nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
				nextStepActionSetValues[setValue] = mutableListOf(emptyCell)
				when(hintLevel) {
					HintLevels.LEVEL1 -> {
						nextStepText = nextStepStrategyName
					}
					HintLevels.LEVEL2 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_last_digit_value,
							"{$setValue}")
					}
					HintLevels.LEVEL3 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_last_digit_value,
							"{$setValue}")
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_last_digit_house,
							"(${houseType.houseAdr(emptyCell)})")
						cellsToHighlight[HintHighlight.REGION] = houseCells.cells.map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.CAUSE] = listOf(emptyCell).map { it.rowIndex to it.columnIndex }
					}
					HintLevels.LEVEL4 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_last_digit_value,
							"{$setValue}")
						nextStepText += "\n" + context.getString(
							R.string.hint_strategy_last_digit_house,
							"(${houseType.houseAdr(emptyCell)})")
						nextStepText += "\n" + getNextStepActionSetValuesAsText()
						cellsToHighlight[HintHighlight.REGION] = houseCells.cells.map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.CAUSE] = listOf(emptyCell).map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.TARGET] = listOf(emptyCell).map { it.rowIndex to it.columnIndex }
					}
				}
				return true
			} // forHouse@
		} // forHouseType@
		return false
	}

}

