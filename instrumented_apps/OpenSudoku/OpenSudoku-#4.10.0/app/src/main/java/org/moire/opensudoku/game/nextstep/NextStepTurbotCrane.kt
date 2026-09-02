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

/** Strategy: Turbot Crane
 */
class NextStepTurbotCrane(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForTurbotCrane()
	}

	/*** Turbot Crane
	 *
	 * Messages:
	 *
	 *  	"Turbot - Crane"
	 *  	" ⊞ Candidate ➠ {2}"
	 *  	" ⊞ Box ➠ (b9) ➠ [ r9c9,r8c7 ]"
	 *  	" ⊞ Row ➠ (r8) ➠ [ r8c7,r8c4 ]"
	 *  	" ⊞ Column ➠ (c4) ➠ [ r8c4,r3c4 ]"
	 *  	" ✎ remove candidate {2} from [ r3c9 ]"
	 *
	 * Logic:
	 *
	 * 	- family: turbot fish (X-chain)
	 *
	 * 	- single candidate elimination
	 *
	 * 	- chain contains 4 node cells
	 *
	 * 	- chain of strong link - weak link - strong link
	 * 		- variant ROW
	 * 			- strong link in a box
	 * 			- weak link from box to row (a strong link is also ok)
	 * 			- strong link from row to column
	 * 		- variant COL
	 * 			- strong link in a box
	 * 			- weak link from box to column (a strong link is also ok)
	 * 			- strong link from column to row
	 * 	- candidate can be removed from cells seen by the endpoint cells
	 *
	 */
	private fun checkForTurbotCrane(): Boolean {

		// loop over all possible candidates
		forCandidateX@ for (candidateX in allowedDigits) {
			// loop over all boxes
			forBox@ for (box in board.getHousesSectors()) {
				val boxCells = box.cells
					.filter { cell -> cell.value == 0 }
					.filter { cell -> candidateX in cell.primaryMarks.marksValues }
					.toList()
				if (boxCells.size != 2) continue@forBox // only 2 cells for the strong link
				if (boxCells[0].rowIndex == boxCells[1].rowIndex) continue@forBox
				if (boxCells[0].columnIndex == boxCells[1].columnIndex) continue@forBox
				// search for weak link in row/column
				forWeakLinkInRow@ for (weakLinkInRow in listOf(true,false)) {
					var cellNode1: Cell
					var cellNode2: Cell
					forNode2@ for (cell in boxCells) {
						cellNode1 = cell
						cellNode2 = boxCells.filterNot { it == cellNode1 }.first()
						val line1 = if (weakLinkInRow) cellNode2.row else cellNode2.column
						val weakLinkCells = line1!!.cells
							.filter { it != cellNode2 }
							.filter { it.value == 0 }
							.filter { candidateX in it.primaryMarks.marksValues }
							.toList()
						if (weakLinkCells.isEmpty()) continue@forNode2 // 2x == special case
						// search for second strong link in column/row
						forStrongLink2@ for (cellNode3 in weakLinkCells) {
							val line2 = if (weakLinkInRow) cellNode3.column else cellNode3.row
							val strongLink2Cells = line2!!.cells
								.filter { it != cellNode3 }
								.filter { it.value == 0 }
								.filter { candidateX in it.primaryMarks.marksValues }
								.toList()
							if (strongLink2Cells.size != 1) continue@forStrongLink2
							val cellNode4 = strongLink2Cells.first()
							// search for cells with removable candidate X
							// cells seen by cellNode1 and cellNode4, without cellNode2 and cellNode3
							val cellsToRemoveX = getUnsolvedCellsSeenByAll(listOf(cellNode1,cellNode4))
								.filter { it != cellNode1 }
								.filter { it != cellNode2 }
								.filter { it != cellNode3 }
								.filter { it != cellNode4 }
								.filter { it.primaryMarks.hasNumber(candidateX) }
							if (cellsToRemoveX.isEmpty()) continue@forStrongLink2

							// crane with needed user action found
							nextStepState = NextStepStates.STEP_FOUND
							nextStepStrategyId = StrategyIds.TURBOT_CRANE
							nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
							nextStepActionRemoveCandidates[candidateX] = cellsToRemoveX.toMutableList()

							// setup regionCells
							var regionCells = mutableListOf<Cell>()
							regionCells.addAll(cellNode1.sector!!.cells)
							if (weakLinkInRow) {
								var mm: Set<Int> =
									listOf(cellNode2,cellNode3).map{ it.columnIndex }.toSortedSet()
								regionCells.addAll(cellNode2.row!!.cells
									.filter { it.columnIndex >= mm.min() }
									.filter { it.columnIndex <= mm.max() }
								)
								mm = listOf(cellNode3,cellNode4).map{ it.rowIndex }.toSortedSet()
								regionCells.addAll(cellNode3.column!!.cells
									.filter { it.rowIndex >= mm.min() }
									.filter { it.rowIndex <= mm.max() }
								)
							} else {
								var mm: Set<Int> = listOf(cellNode2,cellNode3).map{ it.rowIndex }.toSortedSet()
								regionCells.addAll(cellNode2.column!!.cells
									.filter { it.rowIndex >= mm.min() }
									.filter { it.rowIndex <= mm.max() }
								)
								mm = listOf(cellNode3,cellNode4).map{ it.columnIndex }.toSortedSet()
								regionCells.addAll(cellNode3.row!!.cells
									.filter { it.columnIndex >= mm.min() }
									.filter { it.columnIndex <= mm.max() }
								)
							}
							regionCells = regionCells.toSet().toList() as MutableList

							// setup message
							when (hintLevel) {
								HintLevels.LEVEL1 -> {
									nextStepText = nextStepStrategyName
								}
								HintLevels.LEVEL2 -> {
									nextStepText = nextStepStrategyName
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_turbot_crane_candidate,
										"{$candidateX}")
								}
								HintLevels.LEVEL3 -> {
									nextStepText = nextStepStrategyName
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_turbot_crane_candidate,
										"{$candidateX}")
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_turbot_crane_box,
										"(b${cellNode1.sectorIndex+1})",
										getCellsGridAddress(listOf(cellNode1,cellNode2))
									)
									if (weakLinkInRow) {
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_row,
											"(r${cellNode2.rowIndex + 1})",
											getCellsGridAddress(listOf(cellNode2,cellNode3))
										)
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_col,
											"(c${cellNode3.columnIndex + 1})",
											getCellsGridAddress(listOf(cellNode3,cellNode4))
										)
									} else {
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_col,
											"(c${cellNode2.columnIndex + 1})",
											getCellsGridAddress(listOf(cellNode2,cellNode3))
										)
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_row,
											"(r${cellNode3.rowIndex + 1})",
											getCellsGridAddress(listOf(cellNode3,cellNode4))
										)
									}
									cellsToHighlight[HintHighlight.REGION] =
										regionCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] =
										listOf(cellNode1,cellNode2,cellNode3,cellNode4).map { it.rowIndex to it.columnIndex }
								}
								HintLevels.LEVEL4 -> {
									nextStepText = nextStepStrategyName
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_turbot_crane_candidate,
										"{$candidateX}")
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_turbot_crane_box,
										"(b${cellNode1.sectorIndex+1})",
										getCellsGridAddress(listOf(cellNode1,cellNode2))
									)
									if (weakLinkInRow) {
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_row,
											"(r${cellNode2.rowIndex + 1})",
											getCellsGridAddress(listOf(cellNode2,cellNode3))
										)
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_col,
											"(c${cellNode3.columnIndex + 1})",
											getCellsGridAddress(listOf(cellNode3,cellNode4))
										)
									} else {
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_col,
											"(c${cellNode2.columnIndex + 1})",
											getCellsGridAddress(listOf(cellNode2,cellNode3))
										)
										nextStepText += "\n" + context.getString(
											R.string.hint_strategy_turbot_crane_row,
											"(r${cellNode3.rowIndex + 1})",
											getCellsGridAddress(listOf(cellNode3,cellNode4))
										)
									}
									nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
									cellsToHighlight[HintHighlight.REGION] =
										regionCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] =
										listOf(cellNode1,cellNode2,cellNode3,cellNode4).map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.TARGET] =
										cellsToRemoveX.map { it.rowIndex to it.columnIndex }
								}
							} // when (hintLevel)
							return true
						} // forStrongLink2@
					} // forNode2@
				} // forWeakLinkInRow@
			} // forBox@
		} // forCandidateX@
		return false
	}

}

