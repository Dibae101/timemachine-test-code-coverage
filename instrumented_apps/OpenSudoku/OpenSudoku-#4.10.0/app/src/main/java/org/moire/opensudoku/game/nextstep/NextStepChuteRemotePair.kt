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

/** Strategy: Chute Remote Pair
 */
class NextStepChuteRemotePair(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForChuteRemotePair()
	}

	/** Strategy: Chute Remote Pair
	 *
	 * ** Chute Remote Pair
	 *
	 *  Info:
	 *
	 *  	- Chute
	 *  		- horizontal chute = floor chute
	 *  			- box 1,2,3 | box 4,5,6 | box 7,8,9
	 *  		- vertical chute = tower chute
	 *  			- box 1,3,7 | box 2,5,6 | box 3,6,9
	 *
	 * 	Logic:
	 *
	 * 		- viewpoint is a chute (floor or tower)
	 * 		- the chute should contain to cells with the same bi-values
	 * 		- both cells should not see each other
	 * 			- the cells should in different boxes and lines (row/column)
	 * 		- Case SINGLE
	 * 			- there are three cells in the unused box and unused line
	 * 				- these cells contains only one of the two bi-values
	 * 			  	  as candidate list or value
	 * 			- the existing bi-value can be removed from the cells seen by the two bi-value cells,
	 * 		      except the bi-value cells
	 * 		- Case DOUBLE
	 * 			- there are three cells in the unused box and unused line
	 * 				- these cells contains none of the two bi-values
	 * 			  	  as candidate list or value
	 * 			- both bi-values can be removed from the cells seen by the two bi-value cells,
	 * 		      except the bi-value cells
	 *			- Case BONUS
	 *				- it is an extension of the case DOUBLE
	 *				- check in addition the two cells next by each bi-value cell too
	 *					- they are in the same box as the bi-value cell
	 *					- they are in the same line (row/column) as the be-value cell
	 *				- each bi-value can be removed from these cells
	 *
	 * 	 Action:
	 *
	 *      - Case SINGLE: the existing bi-value can be removed ...
	 *      - Case DOUBLE: both bi-values can be removed ...
	 *        ... from the cells seen by the two bi-value cells, but not from the bi-value cells
	 *      - Case BONUS for DOUBLE : both bi-values can be removed from the bonus cells
	 *
	 * 	 Messages:
	 *
	 *  	"Chute Remote Pair (Single)"
	 *  	" ⊞ Candidates ➠ {6,9}"
	 *  	" ⊞ Chute horizontal ➠ (b1,b2,b3)"
	 *  	" ⊞ Remote Pair ➠ [ r4c2,r5c5 ]"
	 *  	" ⊞ Check cells ➠ [ r4c2,r4c2,r4c2 ]"
	 * 		" ✎ remove candidate {6} from [ r4c2 ]"
	 *
	 */
	private fun checkForChuteRemotePair(): Boolean {
		// create a list with all Bi-Values of the board
		val biValues = getBoardBiValues(board)
		if (biValues.isEmpty()) return false
		// check for each Bi-Value (Pair)
		forBiValue@ for ( biValue in biValues ) {
			val cellPairCombinations = generateCellPairCombinations(biValue)
			if (cellPairCombinations.isEmpty()) continue@forBiValue
			forCellPair@ for (cellPair in cellPairCombinations) {
				// check for valid chute remote pair
				if(cellPair[0].sectorIndex == cellPair[1].sectorIndex) continue@forCellPair
				if(cellPair[0].columnIndex == cellPair[1].columnIndex) continue@forCellPair
				if(cellPair[0].rowIndex == cellPair[1].rowIndex) continue@forCellPair
				// chute and checkBox, checkCells
				var chuteType: String // H or V
				var chuteIndex: Int // 1,2,3
				var chuteBoxes: List<Int>
				var checkBoxIndex: Int
				var checkCells: List<Cell>
				if ( (cellPair[0].sectorIndex/3) == (cellPair[1].sectorIndex/3) ) {
					chuteType = "H"
					chuteIndex = cellPair[0].sectorIndex/3
					chuteBoxes = listOf(chuteIndex*3,chuteIndex*3+1,chuteIndex*3+2)
					checkBoxIndex = chuteBoxes
						.filter { it != cellPair[0].sectorIndex }
						.filter { it != cellPair[1].sectorIndex }
						.first()
					checkCells = board.getHousesSectors()[checkBoxIndex].cells.toList()
						.filter { it.rowIndex != cellPair[0].rowIndex }
						.filter { it.rowIndex != cellPair[1].rowIndex }
				} else if ( (cellPair[0].sectorIndex.mod(3)) == (cellPair[1].sectorIndex.mod(3)) ) {
					chuteType = "V"
					chuteIndex = cellPair[0].sectorIndex.mod(3)
					chuteBoxes = listOf(chuteIndex,chuteIndex+3,chuteIndex+6)
					checkBoxIndex = chuteBoxes
						.filter { it != cellPair[0].sectorIndex }
						.filter { it != cellPair[1].sectorIndex }
						.first()
					checkCells = board.getHousesSectors()[checkBoxIndex].cells.toList()
						.filter { it.columnIndex != cellPair[0].columnIndex }
						.filter { it.columnIndex != cellPair[1].columnIndex }
				} else {
					// cell not in the same chute
					continue@forCellPair
				}
				// create list with all values / candidates in the check area
				var checkValues = mutableListOf<Int>()
				for (cell in checkCells) {
					if (cell.value != 0) { // Cell has already a value
						checkValues.add(cell.value)
					} else {
						checkValues.addAll(cell.primaryMarks.marksValues)
					}
				}
				checkValues = checkValues.toSet().sorted().toList() as MutableList
				// create list with candidates which can be removed
				val foundCandidates = checkValues.filter { it in biValue }
				var crpType: String // Single,Double,Bonus
				val actionCandidates = mutableListOf<Int>()
				if (foundCandidates.size == 1) {
					crpType ="S"
					actionCandidates.add(foundCandidates.first())
				} else if (foundCandidates.isEmpty()) {
					crpType ="D" // or "B" ... will be later checked
					actionCandidates.addAll(biValue)
				} else {
					continue@forCellPair
				}
				// create list with of cell where actionCandidates can be removed
				val actionCellsInFocus = getUnsolvedCellsSeenByAll(cellPair)
					.filter { it !in cellPair }
				// filter list for single and double
				var actionCells = mutableListOf<Cell>()
				if (crpType == "S") {
					actionCells = actionCellsInFocus
						.filter { it.primaryMarks.hasNumber(actionCandidates.first()) }
						.toMutableList()
				}
				if (crpType == "D") {
					actionCells = actionCellsInFocus
						.filter { it.primaryMarks.hasNumber(actionCandidates[0]) ||
							it.primaryMarks.hasNumber(actionCandidates[1]) }
						.toMutableList()
				}
				// check for bonus
				if (crpType == "D") {
					var bonusCells = mutableListOf<Cell>()
					bonusCells.addAll(board.getHousesSectors()[cellPair[0].sectorIndex].cells)
					bonusCells.addAll(board.getHousesSectors()[cellPair[1].sectorIndex].cells)
					bonusCells = bonusCells
						.filter { it.value == 0 }.toMutableList()
					if (chuteType == "H") {
						bonusCells = bonusCells
							.filter { it.rowIndex == cellPair[0].rowIndex
								|| it.rowIndex == cellPair[1].rowIndex }.toMutableList()
					}
					if (chuteType == "V") {
						bonusCells = bonusCells
							.filter { it.columnIndex == cellPair[0].columnIndex
								|| it.columnIndex == cellPair[1].columnIndex }.toMutableList()
					}
					bonusCells = bonusCells
						.filter { it !in cellPair }.toMutableList()
					bonusCells = bonusCells
						.filter { it !in actionCellsInFocus }.toMutableList()
					bonusCells = bonusCells
						.filter { it.primaryMarks.hasNumber(actionCandidates[0]) ||
							it.primaryMarks.hasNumber(actionCandidates[1]) }
						.toMutableList()
					if (bonusCells.isNotEmpty()) {
						crpType = "B"
						actionCells.addAll(bonusCells)
					}
				}
				if (actionCells.isEmpty()) continue@forCellPair
				// fill structure with candidates to remove
				for (candidate in actionCandidates) {
					nextStepActionRemoveCandidates[candidate] = actionCells
						.filter { it.primaryMarks.hasNumber(candidate) }
						.toMutableList()
				}
				// CRP with action found
				nextStepState = NextStepStates.STEP_FOUND
				nextStepStrategyId = when (crpType) {
					"S" -> StrategyIds.CHUTE_REMOTE_PAIR_SINGLE
					"D" -> StrategyIds.CHUTE_REMOTE_PAIR_DOUBLE
					"B" -> StrategyIds.CHUTE_REMOTE_PAIR_BONUS
					else -> StrategyIds.CHUTE_REMOTE_PAIR
				}
				nextStepStrategyName = when (crpType) {
					"S","D","B" -> nextStepStrategyId.getStrategyName(context)
					else -> "Chute Remote Pair (unknown)"
				}

				val chuteBoxesText = "(${chuteBoxes.joinToString { "b${it+1}" }.replace(" ","")})"
				val regionCells = mutableListOf<Cell>()
				for (box in chuteBoxes)
					regionCells.addAll(board.getHousesSectors()[box].cells)
				val causeCells = mutableListOf<Cell>()
				causeCells.addAll(cellPair)
				causeCells.addAll(checkCells)
				val targetCells = mutableListOf<Cell>()
				targetCells.addAll(actionCells)
				when (hintLevel) {
					HintLevels.LEVEL1 -> {
						nextStepText = nextStepStrategyName
					}
					HintLevels.LEVEL2 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_candidates
							,formatCellMarks(biValue))
					}
					HintLevels.LEVEL3 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_candidates
							,formatCellMarks(biValue))
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_chute
							,chuteBoxesText)
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_pair
							, getCellsGridAddress(cellPair) )
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_check
							, getCellsGridAddress(checkCells) )
						cellsToHighlight[HintHighlight.REGION] = regionCells
							.map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.CAUSE] = causeCells
							.map { it.rowIndex to it.columnIndex }
					}
					HintLevels.LEVEL4 -> {
						nextStepText = nextStepStrategyName
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_candidates
							,formatCellMarks(biValue))
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_chute
							,chuteBoxesText)
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_pair
							, getCellsGridAddress(cellPair) )
						nextStepText += "\n" + context.getString(R.string.hint_strategy_chute_remote_pair_check
							, getCellsGridAddress(checkCells) )
						nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
						cellsToHighlight[HintHighlight.REGION] = regionCells
							.map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.CAUSE] = causeCells
							.map { it.rowIndex to it.columnIndex }
						cellsToHighlight[HintHighlight.TARGET] = targetCells
							.map { it.rowIndex to it.columnIndex }
					}
				} // HintLevel
				return true
			} // forCellPair@
		} // forBiValue@
		return false
	}

	/** generateCellPairCombinations()
	 *
	 * This function generates a list with all possible combinations of two cells in the board
	 * containing the BiValue.
	 *
	 */
	private fun generateCellPairCombinations(biValue: List<Int>): List<List<Cell>> {
		// create a list with of cells containing both Bi-Values
		val possibleCells = mutableListOf<Cell>()
		forRow@ for ( row in board.getHousesRows()) {
			forCellInRow@ for (cell in row.cells) {
				if (cell.value != 0) continue@forCellInRow // only empty cells
				if (cell.primaryMarks.marksValues.size != 2) continue@forCellInRow
				if ( !cell.primaryMarks.marksValues.containsAll(biValue) ) continue@forCellInRow
				possibleCells.add(cell)
			}
		}
		if ( possibleCells.size < 2 ) return listOf()
		return combinations(possibleCells,2).toList()
	}

}

