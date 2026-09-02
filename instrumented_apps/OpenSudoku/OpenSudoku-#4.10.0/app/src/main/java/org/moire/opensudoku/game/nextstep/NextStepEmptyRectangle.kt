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

/** Strategy: Empty Rectangle
 */
class NextStepEmptyRectangle(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForEmptyRectangle()
	}


	/*** Empty Rectangle
	 *
	 *  - single candidate elimination
	 *  - conjugate pair = two instances of the candidate in a house
	 *
	 *  - conjugate pair (A,B) in row
	 *  	- box with Empty Rectangle x
	 *  	- candidate in C can be removed
	 *
	 *     .  C  .  x  x  x  .  .  .
	 *     .  .  .  .  .  x  .  .  .
	 *     .  .  .  .  .  x  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  B  .  .  .  A  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *
	 *  - conjugate pair (A,B) in column
	 *  	- box with Empty Rectangle x
	 *  	- candidate in C can be removed
	 *
	 *     .  A  .  x  x  x  .  .  .
	 *     .  .  .  .  .  x  .  .  .
	 *     .  .  .  .  .  x  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  B  .  .  .  C  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *     .  .  .  .  .  .  .  .  .
	 *
	 *
	 * Messages:
	 *
	 *		"Empty Rectangle (R)"
	 *      " ⊞ Candidate ➠ {7}"
	 *      " ⊞ Pair ➠ (r8) ➠ [r8c2,r8c6]"
	 *      " ⊞ Box ➠ (c2) ➠ (b1) ➠ (r2)"
	 *      " ⊞ Cell ➠ (r2) ➠ [ r2c6 ]"
	 *      " ✎ remove candidate {7} from [ r2c6 ]"
	 *
	 *		"Empty Rectangle (C): {9}"
	 *      " ⊞ Candidate ➠ {9}"
	 *      " ⊞ Pair ➠ (c2) ➠ [r5c2,r8c2]"
	 *      " ⊞ Box ➠ (r5) ➠ (b6) ➠ (c7)"
	 *      " ⊞ Cell ➠ (c7) ➠ [ r8c7 ]"
	 *      " ✎ remove candidate {9} from [ r8c7 ]"
	 *
	 *
	 * Logic:
	 *
	 *  *** Search starting with ER-box
	 *
	 *  - search for each possible candidate X
	 *  - identify an ER-box with candidates only in one row and one column
	 *  	- each line must contain in minimum one candidate X,
	 *        the candidate in the cross point of row and column did not count
	 *  - search for a conjugate pair (cell A & cell B) with candidate X
	 *  	- should be outside of the ER-box
	 *  	- the line with A & B can be a row or column
	 *  	- A should be:
	 *  		- in the same column of the ER-box candidates
	 *  		- or in the same row of the ER-box candidates
	 *  	- B should not be in the same box as A
	 *  	  B should not see any cell in the ER box
	 *  - search for cell C with candidate X
	 *  	- the position of cell C depends of:
	 *  		- A is in the column of the ER-box
	 *  			= A & B is in a row
	 *  			- C is in the cross point of B column and ER-box row
	 *  			- C sees the ER-box row and B
	 *  		- A is in the row of the ER-box
	 *  			= A & B is in a column
	 *  			- C is in the cross point of B row and ER-box column
	 *  			- C sees the ER-box column and B
	 *  - if C contains candidate X, then the candidate X can be removed from C
	 *
	 *  *** Search starting with conjugate pair - ! that's faster !
	 *
	 *  - loop over allowed candidates
	 * 	- loop over orientation of the conjugate pair line
	 * 		pair in row = Yes or No
	 * 	- loop over lines (orientation -> row/column)
	 * 	- find a conjugate pair A & B in the line
	 * 	- loop over pair cells A & B
	 * 	- find crossLineCells for pair cells
	 *	- find ER box for pair cell A
	 *  - check if box is a ER box
	 *	- find in ER box line to C
	 *	- use row/col from cell B and from one cell in line to C
	 *	- check cell C for candidate
	 *
	 */
	private fun checkForEmptyRectangle(): Boolean {

		// loop over all possible candidates
		forCandidateX@ for (candidateX in allowedDigits) {
			// loop over orientation
			forOrientation@ for (pairInRow in listOf(true,false)) {
				val houses =  if (pairInRow) board.getHousesRows() else board.getHousesColumns()
				// loop over all lines (row/column)
				forPairLine@ for (pairLine in houses ){
					val pairLineCells = pairLine.cells
						.filter { cell -> cell.value == 0 }
						.filter { cell -> candidateX in cell.primaryMarks.marksValues }
						.toList()
					if (pairLineCells.size != 2) continue@forPairLine // a pair of cells needed
					if (pairLineCells[0].sectorIndex == pairLineCells[1].sectorIndex) continue@forPairLine

					// find cells in cross line for each pair cells
					val crossLinesCells =  ArrayList<List<Cell>>()
					for (pairCell in pairLineCells) {
						val lineCells = (if (pairInRow) pairCell.column!!.cells else pairCell.row!!.cells)
							.filter { cell -> cell.value == 0 }
							.filter { cell -> candidateX in cell.primaryMarks.marksValues }
							.filter { cell -> cell.sectorIndex != pairCell.sectorIndex }
						if (lineCells.isEmpty()) continue@forPairLine
						crossLinesCells.add(lineCells)
					}

					// search ER box
					forPairCell@ for ( pairCellIndex in pairLineCells.indices ) {
						val pairCellA = pairLineCells[pairCellIndex]
						val pairCellB = pairLineCells.first { it != pairCellA }

						// get boxCellMap
						val boxCellsMap = crossLinesCells[pairCellIndex]
							.groupBy { it.sectorIndex }
							.toSortedMap()

						// loop over boxes
						forBox@ for ( boxIndex in boxCellsMap.keys ) {
							val boxCellsLineA = boxCellsMap[boxIndex]!!.toList()
							val boxLineCellsMap = boxCellsLineA.first().sector!!.cells
								.filter { cell -> cell.value == 0 }
								.filter { cell -> candidateX in cell.primaryMarks.marksValues }
								.filter { cell -> cell !in boxCellsLineA}
								.groupBy { if (pairInRow) it.rowIndex else it.columnIndex }
								.toSortedMap()
							if (boxLineCellsMap.size != 1) continue@forBox
							val boxCellsLineC = boxLineCellsMap[boxLineCellsMap.firstKey()]!!.toList()
							val cellC = if (pairInRow) {
								board.getCell(boxCellsLineC.first().rowIndex,pairCellB.columnIndex)
							} else {
								board.getCell(pairCellB.rowIndex,boxCellsLineC.first().columnIndex)
							}
							if (cellC.value != 0) continue@forBox
							if (!cellC.primaryMarks.hasNumber(candidateX)) continue@forBox

							// Empty Rectangle with action found
							nextStepState = NextStepStates.STEP_FOUND
							nextStepStrategyId = StrategyIds.EMPTY_RECTANGLE
							nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
							val strategyName = nextStepStrategyName + if (pairInRow) " (R)" else " (C)"
							nextStepActionRemoveCandidates[candidateX] = listOf(cellC).toMutableList()

							// setup regionCells
							var regionCells = mutableListOf<Cell>()
							regionCells.addAll(board.getHousesSectors()[boxIndex].cells.toList())
							if (pairInRow) {
								var mm: Set<Int> =
									listOf(pairCellA,pairCellB).map{ it.columnIndex }.toSortedSet()
								regionCells.addAll(pairCellA.row!!.cells
									.filter { it.columnIndex >= mm.min() }
									.filter { it.columnIndex <= mm.max() }
								)
								mm = listOf(pairCellA,boxCellsLineA.first()).map{ it.rowIndex }.toSortedSet()
								regionCells.addAll(pairCellA.column!!.cells
									.filter { it.rowIndex >= mm.min() }
									.filter { it.rowIndex <= mm.max() }
								)
								mm = listOf(pairCellB,cellC).map{ it.rowIndex }.toSortedSet()
								regionCells.addAll(pairCellB.column!!.cells
									.filter { it.rowIndex >= mm.min() }
									.filter { it.rowIndex <= mm.max() }
								)
								mm = listOf(cellC,boxCellsLineC.first()).map{ it.columnIndex }.toSortedSet()
								regionCells.addAll(cellC.row!!.cells
									.filter { it.columnIndex >= mm.min() }
									.filter { it.columnIndex <= mm.max() }
								)
							} else {
								var mm: Set<Int> =
									listOf(pairCellA,pairCellB).map{ it.rowIndex }.toSortedSet()
								regionCells.addAll(pairCellA.column!!.cells
									.filter { it.rowIndex >= mm.min() }
									.filter { it.rowIndex <= mm.max() }
								)
								mm = listOf(pairCellA,boxCellsLineA.first()).map{ it.columnIndex }.toSortedSet()
								regionCells.addAll(pairCellA.row!!.cells
									.filter { it.columnIndex >= mm.min() }
									.filter { it.columnIndex <= mm.max() }
								)
								mm = listOf(pairCellB,cellC).map{ it.columnIndex }.toSortedSet()
								regionCells.addAll(pairCellB.row!!.cells
									.filter { it.columnIndex >= mm.min() }
									.filter { it.columnIndex <= mm.max() }
								)
								mm = listOf(cellC,boxCellsLineC.first()).map{ it.rowIndex }.toSortedSet()
								regionCells.addAll(cellC.column!!.cells
									.filter { it.rowIndex >= mm.min() }
									.filter { it.rowIndex <= mm.max() }
								)
							}
							regionCells = regionCells.toSet().toList() as MutableList

							// setup regionCells
							var causeCells = mutableListOf<Cell>()
							causeCells.add(pairCellA)
							causeCells.add(pairCellB)
							causeCells.addAll(boxCellsLineA)
							causeCells.addAll(boxCellsLineC)
							causeCells.add(cellC)
							causeCells = causeCells.toSet().toList() as MutableList

							// setup message
							when (hintLevel) {
								HintLevels.LEVEL1 -> {
									nextStepText = strategyName
								}
								HintLevels.LEVEL2 -> {
									nextStepText = strategyName
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_candidate,
										"{$candidateX}")
								}
								HintLevels.LEVEL3 -> {
									nextStepText = strategyName
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_candidate,
										"{$candidateX}")
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_pair,
										"(${(if (pairInRow) pairCellA.rowAddress else pairCellA.columnAddress)})",
										getCellsGridAddress(listOf(pairCellA,pairCellB))
									)
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_box,
										"(${(if (pairInRow) pairCellA.columnAddress else pairCellA.rowAddress)})",
										"(${boxCellsLineA.first().sectorAddress})",
										"(${(if (pairInRow) cellC.rowAddress else cellC.columnAddress)})",
									)
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_cell,
										"(${(if (pairInRow) cellC.rowAddress else cellC.columnAddress)})",
										"[${cellC.gridAddress}]"
									)
									cellsToHighlight[HintHighlight.REGION] =
										regionCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] =
										causeCells.map { it.rowIndex to it.columnIndex }
								}
								HintLevels.LEVEL4 -> {
									nextStepText = strategyName
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_candidate,
										"{$candidateX}")
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_pair,
										"(${(if (pairInRow) pairCellA.rowAddress else pairCellA.columnAddress)})",
										getCellsGridAddress(listOf(pairCellA,pairCellB))
									)
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_box,
										"(${(if (pairInRow) pairCellA.columnAddress else pairCellA.rowAddress)})",
										"(${boxCellsLineA.first().sectorAddress})",
										"(${(if (pairInRow) cellC.rowAddress else cellC.columnAddress)})",
									)
									nextStepText += "\n" + context.getString(
										R.string.hint_strategy_empty_rectangle_cell,
										"(${(if (pairInRow) cellC.rowAddress else cellC.columnAddress)})",
										"[${cellC.gridAddress}]"
									)
									nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
									cellsToHighlight[HintHighlight.REGION] =
										regionCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] =
										causeCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.TARGET] =
										listOf(cellC).map { it.rowIndex to it.columnIndex }
								}
							} // when (hintLevel)
							return true
						} // forBox@
					} // forPairCell@
				} // forPairLine@
			} // forOrientation@
		} // forCandidateX@
		return false
	}

}

