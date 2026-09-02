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
import org.moire.opensudoku.game.CellGroup
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SudokuBoard

/** Strategy: Fish Basic
 *
 *  This strategy package 'Hidden Group' contains the following strategies:
 *  - x-wing
 *  - swordfish
 *  - jellyfish
 *  - squirmbag or starfish
 *  - whale
 *  - leviathan
 *
 */
open class NextStepFishBasicGroup(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		// This methode is not implemented in this class!
		return false
	}

	/** Fish Basic Group
	 *
	 * Fish sizes from 2 to 7
	 *
	 *  	1n ... cyclone fish => use hidden single / naked single
	 *  	2n ... x-wing
	 *  	3n ... swordfish
	 *  	4n ... jellyfish
	 *  	5n ... squirmbag or starfish
	 *  	6n ... whale
	 *  	7n ... leviathan
	 *
	 *  ! The fish size 1 is not part of this strategy. It is already implemented
	 *  ! as Hidden Single and Naked Single.
	 *
	 * Messages:
	 *
	 * 	- Fish 2N - X-Wing, row (R) and column (C)
	 * 		"X-WING (R)"
	 * 	    " ⊞ Candidate ➠ {5}"
	 * 	    " ⊞ Cells ➠ [ r2c5,r2c8,r5c5,r5c8 ]"
	 * 	    " ⊞ BaseSet ➠ (r2,r5)"
	 * 	    " ⊞ CoverSet ➠ (c5,c8)"
	 * 	    " ✎ remove candidate {5} from [ r4c5 ]"
	 *
	 * 	- Fish 3N - Swordfish, row (R) and column (C)
	 * 			-> similar to (fish 2N) X-Wing
	 * 	- Fish 4N - Jellyfish, row (R) and column (C)
	 * 			-> similar to (fish 3N) Swordfish
	 * 	- Fish 5N - Starfish, row (R) and column (C)
	 * 			-> similar to (fish 3N) Swordfish
	 * 	- Fish 6N - Wale, row (R) and column (C)
	 * 			-> similar to (fish 3N) Swordfish
	 * 	- Fish 7N - Leviathan, row (R) and column (C)
	 * 			-> similar to (fish 3N) Swordfish
	 *
	 *
	 * Logic:
	 *
	 * - the focus in this strategy is to work with one digit (candidate) X
	 *   	- X is a valid digit for a cell
	 * - the strategy in two orientations:
	 * 		- vertical
	 * 		- horizontal
	 * - basis is a grid of rows and columns ,
	 *   with nodes as cells with the candidate X with the dimension N
	 * - orientation: horizontal
	 * 		- the base set contains N rows
	 * 		- the cover set contains N columns
	 * 		- each row in the base set should contain:
	 * 			- in minimum 2 cells with the candidate X
	 * 			- in maximum N cells with the candidate X
	 * 		- based on the rows in the base set you will get columns for the cover set
	 * 		- the cover set should contain exactly N columns
	 * 		- each column in the cover set contains:
	 * 			- cells as part of the grid (intersection with the rows)
	 * 				- in minimum 2 cells with the candidate X
	 * 				- in maximum N cells with the candidate X
	 * 			- cells not part of the grid:
	 * 				- in minimum one cells with the candidate X,
	 * 			      which can be removed as part of the strategy
	 * - orientation: vertical
	 * 	... change rows with columns and columns with rows
	 * 	... same rules
	 *
 	 */
	protected fun checkForFishBasicGroup(fishSize: Int): Boolean {

		if (fishSize !in listOf(2,3,4,5,6,7)) { return false } // allowed sizes

		nextStepStrategyId =
			when(fishSize) {
				2 -> StrategyIds.BASIC_FISH_2N_X_WING
				3 -> StrategyIds.BASIC_FISH_3N_SWORDFISH
				4 -> StrategyIds.BASIC_FISH_4N_JELLYFISH
				5 -> StrategyIds.BASIC_FISH_5N_STARFISH
				6 -> StrategyIds.BASIC_FISH_6N_WHALE
				7 -> StrategyIds.BASIC_FISH_7N_LEVIATHAN
				else -> StrategyIds.BASIC_FISH_GROUP
			}
		nextStepStrategyName =
			when(fishSize) {
				2,3,4,5,6,7 -> nextStepStrategyId.getStrategyName(context)
				else -> context.getString(R.string.hint_strategy_fish_basic_group,"${fishSize}")
			}
		var strategyName: String

		var orientationRow = false

		fun getHousesForBaseSet(): Array<CellGroup> {
			return if (orientationRow)
				board.getHousesRows()
			else
				board.getHousesColumns()
		}

		fun getHousesForCoverSet(): Array<CellGroup> {
			return if (orientationRow)
				board.getHousesColumns()
			else
				board.getHousesRows()
		}

		// loop over orientations Horizontal/Vertical
		forOrientation@ for (orientation in listOf(HouseTypes.ROW, HouseTypes.COL)) {
			if (orientation == HouseTypes.ROW) {
				orientationRow = true
				strategyName = "$nextStepStrategyName (R)"
			} else {
				orientationRow = false
				strategyName = "$nextStepStrategyName (C)"
			}
			// loop over all possible candidates
			forCandidateX@ for (candidateX in allowedDigits) {

				val houses = getHousesForBaseSet()

				// get possible coverSetLines
				val baseSetLines: ArrayList<List<Cell>> = ArrayList()
				var cntLinesWithX = 0
				for (house in houses) {
					var lineCells = house.cells.toList()
					lineCells = lineCells
						.filter { cell -> cell.value == 0 }
						.filter { cell -> candidateX in cell.primaryMarks.marksValues }
					if ( lineCells.isNotEmpty() ) cntLinesWithX += 1
					if (lineCells.size in 2..fishSize) baseSetLines.add(lineCells)
				}

				if ( cntLinesWithX <= fishSize) continue@forCandidateX // not enough lines with X
				if ( baseSetLines.size < fishSize) continue@forCandidateX // not enough base lines

				// create a list with all combinations of the lines
				val baseSets = combinations(baseSetLines,fishSize)

				// loop over the combinations
				forBaseSet@ for (baseSet in baseSets) {
					val baseSetCells = baseSet.flatten()
					val baseSetLineCellsMap = baseSetCells
						.groupBy { if (orientationRow) it.rowIndex else it.columnIndex }
						.toSortedMap()
					val baseSetLineIndexList = baseSetLineCellsMap.keys.toList()
					val coverSetLineCellsMap = baseSetCells
						.groupBy { if (orientationRow) it.columnIndex else it.rowIndex }
						.toSortedMap()
					val coverSetLineIndexList = coverSetLineCellsMap.keys.toList()

					// only coverSets with fishSize lines are valid
					if ( coverSetLineIndexList.size != fishSize ) continue@forBaseSet

					// each coverSetLine should contain in minimum 2 cells
					for ((_,v) in coverSetLineCellsMap.iterator()) {
						if ( v.size < 2) continue@forBaseSet
					}

					// create list with cells where X can be removed
					val cellsToRemoveX = mutableListOf<Cell>()
					val coverSetHouses = getHousesForCoverSet()
					for (lineIndex in coverSetLineIndexList) {
						var cells = coverSetHouses[lineIndex].cells.toList()
						cells = cells
							.filter { cell -> cell.value == 0 }
							.filter { cell -> candidateX in cell.primaryMarks.marksValues }
							.filter { cell -> cell !in baseSetCells }
						cellsToRemoveX.addAll(cells)
					}
					if ( cellsToRemoveX.isEmpty() ) continue@forBaseSet

					// *** fish with action found ***
					nextStepState = NextStepStates.STEP_FOUND
					nextStepActionRemoveCandidates[candidateX] = cellsToRemoveX

					var regionCells = mutableListOf<Cell>()
					for (k in baseSetLineCellsMap.keys)
						if (orientationRow)
							regionCells.addAll(board.getHousesRows()[k].cells)
						else
							regionCells.addAll(board.getHousesColumns()[k].cells)
					for (k in coverSetLineCellsMap.keys)
						if (orientationRow)
							regionCells.addAll(board.getHousesColumns()[k].cells)
						else
							regionCells.addAll(board.getHousesRows()[k].cells)
					regionCells = regionCells.toSet().toList() as MutableList

					when (hintLevel) {
						HintLevels.LEVEL1 -> {
							nextStepText = strategyName
						}
						HintLevels.LEVEL2 -> {
							nextStepText = strategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_fish_basic_candidate
								, "{$candidateX}")
						}
						HintLevels.LEVEL3 -> {
							nextStepText = strategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_fish_basic_candidate
								, "{$candidateX}")
							nextStepText += "\n" + context.getString(R.string.hint_strategy_fish_basic_cells
								, getCellsGridAddress(baseSetCells)	)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_fish_basic_base_set,
								(baseSetLineIndexList.joinToString(",", "(", ")") { n -> if (orientationRow) "r${n + 1}" else "c${n + 1}" })
							)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_fish_basic_cover_set,
								(coverSetLineIndexList.joinToString(",", "(", ")") { n -> if (orientationRow) "c${n + 1}" else "r${n + 1}" })
							)
							cellsToHighlight[HintHighlight.REGION] =
								regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] =
								baseSetCells.map { it.rowIndex to it.columnIndex }
						}
						HintLevels.LEVEL4 -> {
							nextStepText = strategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_fish_basic_candidate
								, "{$candidateX}")
							nextStepText += "\n" + context.getString(R.string.hint_strategy_fish_basic_cells
								, getCellsGridAddress(baseSetCells)	)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_fish_basic_base_set,
								(baseSetLineIndexList.joinToString(",", "(", ")") { n -> if (orientationRow) "r${n + 1}" else "c${n + 1}" })
							)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_fish_basic_cover_set,
								(coverSetLineIndexList.joinToString(",", "(", ")") { n -> if (orientationRow) "c${n + 1}" else "r${n + 1}" })
							)
							nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
							cellsToHighlight[HintHighlight.REGION] =
								regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] =
								baseSetCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.TARGET] =
								cellsToRemoveX.map { it.rowIndex to it.columnIndex }
						}
					}
					if (fishSize > 4) // TODO remove after testing the strategy for fishSize 5,6,7
						nextStepText += "\nPlease send us this puzzle to optimize the strategy $strategyName !"
					return true
				} // forBaseSet@
			} // forCandidateX@
		} // forOrientation@
		return false
	}

}
