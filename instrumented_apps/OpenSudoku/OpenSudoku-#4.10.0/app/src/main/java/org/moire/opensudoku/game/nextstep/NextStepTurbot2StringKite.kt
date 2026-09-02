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

/** Strategy: Turbot 2-String-Kite
 */
class NextStepTurbot2StringKite(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForTurbot2StringKite()
	}

	/*** Turbot - 2-String-Kite
	 *
	 * Messages:
	 *
	 *  	"Turbot - 2-string kite"
	 *  	" ⊞ Candidate ➠ {3}"
	 *  	" ⊞ Row ➠ (r8) ➠ [ r9c1.r9c5 ]"
	 *  	" ⊞ Box ➠ (b8) ➠ [ r9c5,r8c4 ]"
	 *  	" ⊞ Column ➠ (c4) ➠ [ r8c4,r2c4 ]"
	 *   	" ✎ remove candidate {3} from [ r2c1 ]"
	 *
	 * Logic:
	 *
	 * 	- single candidate elimination
	 * 	- chain of strong link - weak link - strong link
	 * 		- the weak link joins twos cells in one box containing the candidate
	 * 		- one strong link joins one of the cells in the box with another cell in the row
	 * 	    - the other strong link joins the other cells in the box with another cell in the row
	 * 	- candidate can be removed from cells seen by the endpoint cells
	 *
	 * 	conditions:
	 * 		- the box B should contain two cells BR and BC with the candidate X
	 * 			- the cells BR and BC should be in different rows and cols
	 * 		- the row R should contain only two cells BR,OR with the candidate X
	 * 			- one cell BR in the box and the other cell OR outside the box
	 * 		- the column C should contain only two cells BC,OC with the candidate X
	 * 			- one cell BC in the box and the other cell OC outside the box
	 * 		- there is only one cell XX seen by the cells OR and OC where the candidate X
	 * 	      can be removed
	 *
	 */
	private fun checkForTurbot2StringKite(): Boolean {

		// loop over all possible candidates
		forCandidateX@ for (candidateX in allowedDigits) {
			// loop over all rows
			forRow@ for (row in board.getHousesRows()) {
				val rowCells = row.cells
					.filter { cell -> cell.value == 0 }
					.filter { cell -> candidateX in cell.primaryMarks.marksValues }
					.toList()
				if (rowCells.size != 2) continue@forRow // only 2 cells for the strong link
				if (rowCells[0].sectorIndex == rowCells[1].sectorIndex) continue@forRow // not in the same box
				forCol@ for (col in board.getHousesColumns()) {
					val colCells = col.cells
						.filter { cell -> cell.value == 0 }
						.filter { cell -> candidateX in cell.primaryMarks.marksValues }
						.toList()
					if (colCells.size != 2) continue@forCol // only 2 cells for the strong link
					if (colCells[0].sectorIndex == colCells[1].sectorIndex) continue@forCol // not in the same box
					if (rowCells.intersect(colCells.toSet()).toList().isNotEmpty()) continue@forCol // no common cells
					// search for cells in box and outside
					var cellBR = Cell()
					var cellBC = Cell()
					var cellOR = Cell()
					var cellOC = Cell()
					var cellsOK = false
					forBR@ for (br in 0..1) {
						for (bc in 0 .. 1) {
							cellBR = rowCells[br]
							cellOR = rowCells.filterNot { it == cellBR }.first()
							cellBC = colCells[bc]
							cellOC = colCells.filterNot { it == cellBC }.first()
							// cellBR and cellBC must be in one box
							if (cellBR.sectorIndex != cellBC.sectorIndex) continue
							cellsOK = true
							break@forBR
						}
					}
					if (!cellsOK) continue@forCol
					// search for cells with removable candidate X
					// cells seen by OR and OC, without OR and OC
					val cellsToRemoveX = getUnsolvedCellsSeenByAll(listOf(cellOR,cellOC))
						.filter { cell -> cell.primaryMarks.hasNumber(candidateX) }
					if (cellsToRemoveX.isEmpty()) continue@forCol
					nextStepActionRemoveCandidates[candidateX] = cellsToRemoveX as MutableList

					// 2-string kite with action found
					nextStepState = NextStepStates.STEP_FOUND
					nextStepStrategyId = StrategyIds.TURBOT_2_STRING_KITE
					nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

					var regionCells = mutableListOf<Cell>()
					regionCells.addAll(cellBR.row!!.cells)
					regionCells.addAll(cellBC.column!!.cells)
					regionCells.addAll(cellBR.sector!!.cells)
					regionCells = regionCells.toSet().toList() as MutableList

					when (hintLevel) {
						HintLevels.LEVEL1 -> {
							nextStepText = nextStepStrategyName
						}

						HintLevels.LEVEL2 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_candidate,
								"{$candidateX}")
						}

						HintLevels.LEVEL3 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_candidate,
								"{$candidateX}")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_row,
								"(r${cellOR.rowIndex+1})",
								getCellsGridAddress(listOf(cellOR,cellBR))
							)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_box,
								"(b${cellBR.sectorIndex+1})",
								getCellsGridAddress(listOf(cellBR,cellBC))
							)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_col,
								"(c${cellOC.columnIndex+1})",
								getCellsGridAddress(listOf(cellBC,cellOC))
							)
							cellsToHighlight[HintHighlight.REGION] =
								regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] =
								listOf(cellBR,cellBC,cellOR,cellOC).map { it.rowIndex to it.columnIndex }
						}

						HintLevels.LEVEL4 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_candidate,
								"{$candidateX}")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_row,
								"(r${cellOR.rowIndex+1})",
								getCellsGridAddress(listOf(cellOR,cellBR))
							)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_box,
								"(b${cellBR.sectorIndex+1})",
								getCellsGridAddress(listOf(cellBR,cellBC))
							)
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_turbot_2_string_kite_col,
								"(c${cellOC.columnIndex+1})",
								getCellsGridAddress(listOf(cellBC,cellOC))
							)
							nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
							cellsToHighlight[HintHighlight.REGION] =
								regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] =
								listOf(cellBR,cellBC,cellOR,cellOC).map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.TARGET] =
								cellsToRemoveX.map { it.rowIndex to it.columnIndex }
						}
					}
					return true
				}
			}
		}
		return false
	}

}

