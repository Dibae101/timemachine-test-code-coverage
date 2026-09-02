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
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SudokuBoard

/** Strategy: Hidden Single
 */
class NextStepHiddenSingle(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForHiddenSingle()
	}

	/** Strategy: Hidden Single
	 *
	 * Find in the empty cells of a house a candidate with count 1 (single).
	 * The cell with this single candidate can contain more than one other candidates.
	 *
	 * Message:
	 * 		"Hidden Single"
	 *  	" ⊞ Candidate ➠ {5}"
	 *  	" ⊞ House ➠ (r5)"
	 *  	" ⊞ Cell ➠ [ r5c2 ]"
	 * 		" ✎ enter value {5} into [ r5c2 ]"
	 *
	 * How to search:
	 *  Create a list with the count of all candidate numbers for a house.
	 *  Filter the counts for all numbers with the count 1.
	 *  It could be that there are more than one hidden single in a house.
	 *  We take the first one ...
	 */

	private fun checkForHiddenSingle(): Boolean {

		// loop over all house types
		for (houseType in HouseTypes.entries) {

			// get all houses for the house type
			val houseCellsArray = when (houseType) {
				HouseTypes.ROW -> board.getHousesRows()
				HouseTypes.COL -> board.getHousesColumns()
				HouseTypes.BOX -> board.getHousesSectors()
			}

			// loop over all houses
			for (houseCells in houseCellsArray) {
				val numberCountMap = mutableMapOf<Int, Int>()
				val numberCellMap = mutableMapOf<Int, Cell>()
				for (cell in houseCells.cells) {
					if (cell.value > 0) continue
					val candidates = cell.primaryMarks.marksValues
					for (candidate in candidates) {
						val count = numberCountMap.getOrDefault(candidate, 0)
						numberCountMap[candidate] = count + 1
						numberCellMap[candidate] = cell
					}
				}
				val numberCountMapSingle = numberCountMap.filter { it.value == 1 }
				if (numberCountMapSingle.isNotEmpty()) {
					val actionCandidate = numberCountMapSingle.keys.toIntArray()[0]
					val actionCell = numberCellMap[actionCandidate]!!
					nextStepState = NextStepStates.STEP_FOUND
					nextStepStrategyId = StrategyIds.HIDDEN_SINGLE
					nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
					nextStepActionSetValues[actionCandidate] = mutableListOf(actionCell)
					when(hintLevel) {
						HintLevels.LEVEL1 -> {
							nextStepText = nextStepStrategyName
						}
						HintLevels.LEVEL2 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_candidate,
								"{$actionCandidate}")
						}
						HintLevels.LEVEL3 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_candidate,
								"{$actionCandidate}")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_house,
								"(${houseType.houseAdr(actionCell)})")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_cell,
								"[${actionCell.gridAddress}]")
							cellsToHighlight[HintHighlight.REGION] = houseCells.cells
								.filterNot { it == actionCell }
								.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] = listOf(actionCell).map { it.rowIndex to it.columnIndex }
						}
						HintLevels.LEVEL4 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_candidate,
								"{$actionCandidate}")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_house,
								"(${houseType.houseAdr(actionCell)})")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_hidden_single_cell,
								"[${actionCell.gridAddress}]")
							nextStepText += "\n" + getNextStepActionSetValuesAsText()
							cellsToHighlight[HintHighlight.REGION] = houseCells.cells
								.filterNot { it == actionCell }
								.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] = listOf(actionCell).map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.TARGET] = listOf(actionCell).map { it.rowIndex to it.columnIndex }
						}
					}
					return true
				}
			}
		}
		return false
	}

}
