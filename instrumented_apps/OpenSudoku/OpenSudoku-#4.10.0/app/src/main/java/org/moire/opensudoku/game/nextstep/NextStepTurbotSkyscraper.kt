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

/** Strategy: Turbot Skyscraper
 */
class NextStepTurbotSkyscraper(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForTurbotSkyscraper()
	}

	/** Strategy: Turbot Skyscraper
	 *
	 * Messages:
	 *
	 *  "Turbot - Skyscraper (R)"
	 *  " ⊞ Candidate ➠ {5}"
	 *  " ⊞ Base ➠ (r8) ➠ [ r8c1.r8c4 ]"
	 *  " ⊞ Walls ➠ (c1,c4)
	 *  " ⊞ Roofs ➠ [r5c1,r4c4]"
	 *	" ✎ remove {5} from (r4) ➠ [ r4c2 ]"
	 * 	" ✎ remove {5} from (r5) ➠ [ r5c6 ]"
	 *
	 *  "Turbot - Skyscraper (C)"
	 *  " ⊞ Candidate ➠ {1}"
	 *  " ⊞ Base ➠ (c9) ➠ [ r2c9,r7c9 ]"
	 *  " ⊞ Walls ➠ (r2r7)
	 *  " ⊞ Roofs ➠ [ r2c4,r7c5 ]"
	 * 	" ✎ remove {1} from (c4) ➠ [ r8c4 ]"
	 * 	" ✎ remove {1} from (c5) ➠ [ r3c5 ]"
	 *
	 * Logic:
	 *
	 * The skyscraper can have two(+two) orientations:
	 * 		- base line is ROW
	 * 			- walls up or down
	 * 		- base line is COLUMN
	 * 			- walls right or left
	 *
	 * In both cases the strategy is the same!
	 *
	 * names:
	 *   roof                r2
	 *   roof     r1      X  w
	 *            w          w
	 *   wall     w1         w2
	 *            w          w
	 *   base   . b1 . . . . b2 . .
	 *
	 *  - common candidate X , in cell b1/b2/r1/r2
	 *
	 *  - one base line
	 *  	- only one line
	 *  	- includes cell b1 and b2
	 *  	- can include the candidate X more than 2 times
	 *
	 *  - two wall lines
	 *  	- one line for each pair of base cell and roof cell
	 *  	- in each line we have the candidate X only 2 times, in the base cell and the roof cell
	 *  	- the roof cells must be in the same chute, 3 boxes, parallel to the base line
	 *  	- the roof cells can not be in the same line, because this is an "X-Wing"
	 *  	- the pair of base cell and roof cell should not be in one box, because this is an
	 *  	  "Locked Candidates Type II"
	 *  	- the roof cells should be in the same chute (chute parallel to the base line),
	 *        if not, they do not have common cells where X can be removed
	 *
	 *  - links between cells:
	 *    r1 <strong> b1 <weak> b2 <strong> r2
	 *
	 * 	- region with obsolete candidates X
	 * 		- cells seen by both roof cells (excluding roof cells)
	 * 			- max. 4 possible cells if roof cells not in the same box
	 * 			- max. 3 possible cells if roof cells in the same box
	 * 		    - 0 cells if they are not in the same chute
	 */
	private fun checkForTurbotSkyscraper(): Boolean {

		// loop over orientations Horizontal/Vertical
		forOrientation@ for (orientation in listOf(HouseTypes.ROW, HouseTypes.COL)) {

			val baseInRow = orientation == HouseTypes.ROW

			// loop over all possible candidates
			forCandidateX@ for (candidateX in allowedDigits) {
				// get all possible wall lines
				val wallLines: ArrayList<List<Cell>> = ArrayList()
				var cntLinesWithX = 0
				val houses = if (baseInRow)
					board.getHousesColumns()
				else
					board.getHousesRows()
				for (house in houses) {
					var lineCells = house.cells.toList()
					lineCells = lineCells
						.filter { cell -> cell.value == 0 }
						.filter { cell -> candidateX in cell.primaryMarks.marksValues }
					if ( lineCells.isNotEmpty() ) cntLinesWithX += 1
					if ( lineCells.size != 2 ) continue // strong link needed
					if ( (lineCells.map{cell -> cell.sectorIndex}.toSet().size) == 1 ) continue // in the same box
					wallLines.add(lineCells)
				}
				if (cntLinesWithX < 3) continue@forCandidateX // in minimum 3 lines with X needed
				if (wallLines.size < 2) continue@forCandidateX // in minimum 2 wallLines needed

				// wallLines found, construct the skyscraper
				forWallLine1@ for ( i in 0..<(wallLines.size-1)) {
					val wallLine1 = wallLines[i]
					forWallLine2@ for ( j in (i+1)..<wallLines.size ) {
						val wallLine2 = wallLines[j]
						// search base and roof cells
						var b1 = Cell()
						var b2 = Cell()
						var r1 = Cell()
						var r2 = Cell()
						var cellsOK = false
						forBC@ for (bc in 0..1) {
							for (rc in 0 .. 1) {
								b1 = wallLine1[bc]
								r1 = wallLine1.filterNot { it == b1 }.first()
								b2 = wallLine2[rc]
								r2 = wallLine2.filterNot { it == b2 }.first()
								if (baseInRow) {
									// b1 and b2 must be in the same baseline
									if (b1.rowIndex != b2.rowIndex) continue
									// r1 and r2 must not be in the same baseline
									if (r1.rowIndex == r2.rowIndex) continue
									// r1 and r2 should be in the same chute
									if (r1.sectorIndex/3 != r2.sectorIndex/3) continue
								}
								else {
									// b1 and b2 must be in the same baseline
									if (b1.columnIndex != b2.columnIndex) continue
									// r1 and r2 must not be in the same baseline
									if (r1.columnIndex == r2.columnIndex) continue
									// r1 and r2 should be in the same chute
									if (r1.sectorIndex%3 != r2.sectorIndex%3) continue
								}
								cellsOK = true
								break@forBC
							}
						}
						if (!cellsOK) continue@forWallLine2

						// search for cells with removable candidate X
						// cells seen by r1 and r2, without r1 and r2
						val cellsToRemoveX = getUnsolvedCellsSeenByAll(listOf(r1,r2))
							.filter { cell -> cell.primaryMarks.hasNumber(candidateX) }
						if (cellsToRemoveX.isEmpty()) continue@forWallLine2
						nextStepActionRemoveCandidates[candidateX] = cellsToRemoveX as MutableList

						// skyscraper with action found
						nextStepState = NextStepStates.STEP_FOUND
						nextStepStrategyId = StrategyIds.TURBOT_SKYSCRAPER
						nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
						val strategyName = if (orientation == HouseTypes.ROW) {
							"$nextStepStrategyName (R)"
						} else {
							"$nextStepStrategyName (C)"
						}

						var regionCells = mutableListOf<Cell>()
						if (baseInRow) {
							var mm: Set<Int> = listOf(b1,b2).map{ it.columnIndex }.toSortedSet()
							regionCells.addAll(b1.row!!.cells
								.filter { it.columnIndex >= mm.min() }
								.filter { it.columnIndex <= mm.max() }
							)
							mm = listOf(b1,r1).map{ it.rowIndex }.toSortedSet()
							regionCells.addAll(r1.column!!.cells
								.filter { it.rowIndex >= mm.min() }
								.filter { it.rowIndex <= mm.max() }
							)
							mm = listOf(b2,r2).map{ it.rowIndex }.toSortedSet()
							regionCells.addAll(r2.column!!.cells
								.filter { it.rowIndex >= mm.min() }
								.filter { it.rowIndex <= mm.max() }
							)
						} else {
							var mm: Set<Int> = listOf(b1,b2).map{ it.rowIndex }.toSortedSet()
							regionCells.addAll(b1.column!!.cells
								.filter { it.rowIndex >= mm.min() }
								.filter { it.rowIndex <= mm.max() }
							)
							mm = listOf(b1,r1).map{ it.columnIndex }.toSortedSet()
							regionCells.addAll(r1.row!!.cells
								.filter { it.columnIndex >= mm.min() }
								.filter { it.columnIndex <= mm.max() }
							)
							mm = listOf(b2,r2).map{ it.columnIndex }.toSortedSet()
							regionCells.addAll(r2.row!!.cells
								.filter { it.columnIndex >= mm.min() }
								.filter { it.columnIndex <= mm.max() }
							)
						}
						regionCells = regionCells.toSet().toList() as MutableList

						//val cellsToRemoveXLineMap = cellsToRemoveX
						//	.groupBy { if (baseInRow) "(r${it.rowIndex+1})" else "(c${it.columnIndex+1})" }
						//	.toSortedMap()

						when (hintLevel) {
							HintLevels.LEVEL1 -> {
								nextStepText = strategyName
							}

							HintLevels.LEVEL2 -> {
								nextStepText = strategyName
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_candidate,
									"{$candidateX}")
							}

							HintLevels.LEVEL3 -> {
								nextStepText = strategyName
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_candidate,
									"{$candidateX}")
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_base,
									if (baseInRow) "(r${b1.rowIndex+1})" else "(c${b1.columnIndex+1})",
									getCellsGridAddress(listOf(b1,b2))
								)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_walls,
									if (baseInRow)
										"(c${r1.columnIndex+1},c${r2.columnIndex+1})"
									else
										"(r${r1.rowIndex+1},r${r2.rowIndex+1})",
								)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_roofs,
									getCellsGridAddress(listOf(r1,r2))
								)
								cellsToHighlight[HintHighlight.REGION] =
									regionCells.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.CAUSE] =
									listOf(b1,b2,r1,r2).map { it.rowIndex to it.columnIndex }
							}

							HintLevels.LEVEL4 -> {
								nextStepText = strategyName
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_candidate,
									"{$candidateX}")
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_base,
									if (baseInRow) "(r${b1.rowIndex+1})" else "(c${b1.columnIndex+1})",
									getCellsGridAddress(listOf(b1,b2))
								)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_walls,
									if (baseInRow)
										"(c${r1.columnIndex+1},c${r2.columnIndex+1})"
									else
										"(r${r1.rowIndex+1},r${r2.rowIndex+1})",
								)
								nextStepText += "\n" + context.getString(
									R.string.hint_strategy_turbot_skyscraper_roofs,
									getCellsGridAddress(listOf(r1,r2))
								)
								nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
								cellsToHighlight[HintHighlight.REGION] =
									regionCells.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.CAUSE] =
									listOf(b1,b2,r1,r2).map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.TARGET] =
									cellsToRemoveX.map { it.rowIndex to it.columnIndex }
							}
						}
						return true
					} // forWallLine2@
				} // forWallLine1@
			} // forCandidateX@
		} // forOrientation@
		return false
	}

}

