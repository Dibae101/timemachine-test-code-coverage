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

/** Strategy: Hidden Group
 *
 *  the strategy package 'Hidden Group' contains the following strategies:
 *  - Hidden Pair
 *  - Hidden Triple
 *  - Hidden Quad
 *
 */
open class NextStepHiddenGroup(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		// This methode is not implemented in this class!
		return false
	}

	/** Hidden Pair / Hidden Triple / Hidden Quad
	 *
	 * If there are 2/3/4 cells in a house (in a row, column, or box) in which 2/3/4 candidates
	 * do not appear anywhere outside of those cells in the same house, those candidates
	 * must be inserted into those 2/3/4 cells. These cell should contain also other candidates.
	 * These other candidates can therefore be deleted from these two cells.
	 *
	 *  groupSize ... 2 = pair | 3 = triple | 4 = quad
	 *
	 * Messages:
	 *
	 * 		"Hidden Pair"
	 *  	" ⊞ Candidates ➠ {2,4}"
	 *  	" ⊞ House ➠ (c3)"
	 *  	" ⊞ Cells ➠ [ r4c3,r5c3 ]"
	 * 		" ✎ remove candidate {3} from [ r5c3 ]"
	 * 		" ✎ remove candidate {5} from [ r4c3 ]"
	 * 		" ✎ remove candidate {6} from [ r4c3,r5c3 ]"
	 * 		" ✎ remove candidate {7} from [ r5c3 ]"
	 *
	 * 		Hidden Triple ... see Hidden Pair
	 *		Hidden Quad ...  see Hidden Pair
	 *
	 */
	protected fun checkForHiddenGroup(groupSize: Int): Boolean {

		if (groupSize < 2 || groupSize > 4) {
			//error("groupSize must be between 2 and 4")
			return false
		}

		nextStepStrategyId =
			when(groupSize) {
				2 -> StrategyIds.HIDDEN_PAIR
				3 -> StrategyIds.HIDDEN_TRIPLE
				4 -> StrategyIds.HIDDEN_QUAD
				else -> StrategyIds.HIDDEN_GROUP
			}

		nextStepStrategyName =
			when(groupSize) {
				2,3,4 -> nextStepStrategyId.getStrategyName(context)
				else -> context.getString(R.string.hint_strategy_hidden_group,"$groupSize")
			}

		// loop over all house types
		for (houseType in HouseTypes.entries) {

			// get all houses for the house types
			val houseCellsArray = when (houseType) {
				HouseTypes.ROW -> board.getHousesRows()
				HouseTypes.COL -> board.getHousesColumns()
				HouseTypes.BOX -> board.getHousesSectors()
			}

			// loop over all houses
			forHouse@ for (houseCells in houseCellsArray) {

				// create a list with needed solution values (= allowed candidates)
				var allowedCandidates = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
				houseCells.cells
					.filter { it.value != 0 }
					.forEach { allowedCandidates = allowedCandidates.minus(it.value) }

				// enough candidates to have a hidden group ?
				if (allowedCandidates.size <= groupSize) continue@forHouse

				// create a list with all group combinations
				val groupCombinations = combinations(allowedCandidates.toList(), groupSize).toList()

				// loop over group combinations to find a hidden group
				forGroupCombination@ for (groupCombination in groupCombinations) {
					// loop over unsolved cells in a house
					// search for each number in the group the cells with such a candidate
					val numberCellsMap = mutableMapOf<Int, MutableList<Cell>>()
					for (cell in houseCells.cells) {
						if (cell.value != 0) continue // cell solved
						for (number in groupCombination) {
							if (cell.primaryMarks.hasNumber(number)) {
								val numberCells = numberCellsMap.getOrDefault(number, mutableListOf())
								numberCells.add(cell)
								numberCellsMap[number] = numberCells
								// a number should not be in more cells than group size
								if (numberCells.size > groupSize) continue@forGroupCombination
							}
						}
					}

					// create a list of the cells found
					val cellsFound = numberCellsMap.values.flatten().toSet().toList()

					// the count of cells found must be equal to the group size
					if (cellsFound.size != groupSize) continue@forGroupCombination

					// get actions
					var removeCellsMap = mutableMapOf<Int, MutableList<Cell>>()
					for (cell in cellsFound) {
						val candidates = cell.primaryMarks.marksValues.filter { it !in groupCombination }
						for (candidate in candidates) {
							val removeCells = removeCellsMap.getOrDefault(candidate, mutableListOf())
							removeCells.add(cell)
							removeCellsMap[candidate] = removeCells
							nextStepActionRemoveCandidates[candidate] = removeCells
						}
					}

					removeCellsMap = removeCellsMap.toSortedMap()

					if (removeCellsMap.isNotEmpty()) {
						// User can remove candidates
						nextStepState = NextStepStates.STEP_FOUND
						val msgValues = "{${groupCombination.joinToString(",")}}"
						val msgHouse = "(${houseType.houseAdr(houseCells.cells[0])})"
						val msgCells = getCellsGridAddress(cellsFound)
						when (hintLevel) {
							HintLevels.LEVEL1 -> {
								nextStepText = nextStepStrategyName
							}
							HintLevels.LEVEL2 -> {
								nextStepText = nextStepStrategyName
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_candidates,
									msgValues)
							}
							HintLevels.LEVEL3 -> {
								nextStepText = nextStepStrategyName
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_candidates,
									msgValues)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_house,
									msgHouse)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_cells,
									msgCells)
								cellsToHighlight[HintHighlight.REGION] = houseCells.cells.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.CAUSE] = cellsFound.map { it.rowIndex to it.columnIndex }
							}
							HintLevels.LEVEL4 -> {
								nextStepText = nextStepStrategyName
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_candidates,
									msgValues)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_house,
									msgHouse)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_hidden_group_cells,
									msgCells)
								nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
								cellsToHighlight[HintHighlight.REGION] = houseCells.cells.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.CAUSE] = cellsFound.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.TARGET] = removeCellsMap.values.flatten().map { it.rowIndex to it.columnIndex }
							}
						}
						return true
					}
				}
			}
		}
		return false
	}

}
