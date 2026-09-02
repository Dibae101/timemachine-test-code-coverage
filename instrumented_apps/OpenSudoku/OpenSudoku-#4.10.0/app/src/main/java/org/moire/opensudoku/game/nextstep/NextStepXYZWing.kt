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
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SudokuBoard

/** Strategy: XYZ Wing
 */
class NextStepXYZWing(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForXYZWing()
	}


	/** XYZ-Wing
	 *
	 * the central cell is named 'pivot' or 'hinge'
	 * the wing cells are named 'pincer' or 'wing'
	 *
	 * the pivot cell contains the candidates X,Y and Z
	 *
	 * the wing cells contains the candidates
	 * 		- wingX: X and Z
	 * 		- wingY: Y and Z
	 *
	 * the pivot cell must see wingX and wingY cells
	 *
	 * there is no need for the wingX cell to see the wingY cell and vice versa
	 *
	 * the candidate Z can be removed from all cells seen by the pivot, wingX and wingY cell.
	 * except the pivot, wingX and wingY cell
	 *
	 * optimization:
	 *
	 * If both cells, wingX and wingY, are outside the box of the pivot cell there is
	 * no common visible cell from which a candidate Z could be removed
	 *
	 * One wing cell must be in the box of the pivot cell and
	 * the other wing cell is on the row or column of the pivot cell
	 *
	 *  Messages:
	 *
	 *   "XYZ-Wing: {1,3,6} + {3}"
	 *   " XYZ = {1,6,3} ➠ [ r8c8 ]"
	 *   " XZ = {1,3} ➠ [ r7c8 ]"
	 *   " YZ = {6,3} ➠ [ r8c5 ]"
	 *   " ✎ remove {3} from [ r8c9 ]"
	 */
	private fun checkForXYZWing(): Boolean {

		val cells = board.cells

		// loop over all cells with three candidates
		cells.forEach { row ->
			row.forEach forCells@{ boardCell ->
				if (boardCell.value > 0) return@forCells // cell has already a value
				if (boardCell.primaryMarks.marksValues.size != 3) return@forCells // not 3 candidates
				// cell with 3 candidates found
				val pivotCell = boardCell
				// search in the box of the pivot cell for possible wing cells
				val wingXCells = mutableListOf<Cell>()
				forCellInPivotBox@ for (boxCell in pivotCell.sector!!.cells) {
					if (boxCell.value > 0)
						continue@forCellInPivotBox // cell has already a value
					if (boxCell.primaryMarks.marksValues.size != 2)
						continue@forCellInPivotBox // not 2 candidates
					if (! (pivotCell.primaryMarks.marksValues.containsAll(boxCell.primaryMarks.marksValues)))
						continue@forCellInPivotBox // both marks of cell not in pivotCell
					wingXCells.add(boxCell)
				}
				if (wingXCells.isEmpty())
					return@forCells // no possible wingX cell found

				// search in the row and column of the pivot cell for possible wing cells
				val wingYCells = mutableListOf<Cell>()
				forCellInPivotRow@ for (rowCell in pivotCell.row!!.cells) {
					if (rowCell.value > 0)
						continue@forCellInPivotRow // cell has already a value
					if (pivotCell.sectorIndex == rowCell.sectorIndex)
						continue@forCellInPivotRow // cell in pivot box
					if (rowCell.primaryMarks.marksValues.size != 2)
						continue@forCellInPivotRow // not 2 candidates
					if (! (pivotCell.primaryMarks.marksValues.containsAll(rowCell.primaryMarks.marksValues)))
						continue@forCellInPivotRow // both marks of cell not in pivotCell
					wingYCells.add(rowCell)
				}
				forCellInPivotCol@ for (colCell in pivotCell.column!!.cells) {
					if (colCell.value > 0)
						continue@forCellInPivotCol // cell has already a value
					if (pivotCell.sectorIndex == colCell.sectorIndex)
						continue@forCellInPivotCol // cell in pivot box
					if (colCell.primaryMarks.marksValues.size != 2)
						continue@forCellInPivotCol // not 2 candidates
					if (! (pivotCell.primaryMarks.marksValues.containsAll(colCell.primaryMarks.marksValues)))
						continue@forCellInPivotCol // both marks of cell not in pivotCell
					wingYCells.add(colCell)
				}
				if (wingYCells.isEmpty())
					return@forCells // no possible wingY cell found

				// loop over wingXCells
				forWingXCell@ for (wingXCell in wingXCells) {
					// compute Y candidate
					val candidateY = pivotCell.primaryMarks.marksValues
						.toSet().subtract(wingXCell.primaryMarks.marksValues.toSet()).first()
					// loop over possible Z candidates
					forCandidateZ@ for (candidateZ in wingXCell.primaryMarks.marksValues) {
						val candidatesYZ = setOf(candidateY, candidateZ)
						forWingYCell@ for (wingYCell in wingYCells) {
							if (wingYCell.primaryMarks.marksValues.toSet() != candidatesYZ) continue@forWingYCell
							// check for actions
							val visibleCellsPivot = mutableListOf<Cell>()
							visibleCellsPivot.addAll(pivotCell.row!!.cells.toList())
							visibleCellsPivot.addAll(pivotCell.column!!.cells.toList())
							visibleCellsPivot.addAll(pivotCell.sector!!.cells.toList())
							val visibleCellsWingX = mutableListOf<Cell>()
							visibleCellsWingX.addAll(wingXCell.row!!.cells.toList())
							visibleCellsWingX.addAll(wingXCell.column!!.cells.toList())
							visibleCellsWingX.addAll(wingXCell.sector!!.cells.toList())
							val visibleCellsWingY = mutableListOf<Cell>()
							visibleCellsWingY.addAll(wingYCell.row!!.cells.toList())
							visibleCellsWingY.addAll(wingYCell.column!!.cells.toList())
							visibleCellsWingY.addAll(wingYCell.sector!!.cells.toList())
							val visibleCells = visibleCellsPivot
								.intersect(visibleCellsWingX)
								.intersect(visibleCellsWingY)
								.toSet().toList()
							val cellsToWorkOn = visibleCellsPivot
								.intersect(visibleCellsWingX)
								.intersect(visibleCellsWingY)
								.filterNot { it in listOf(pivotCell, wingXCell, wingYCell) }
								.filter { it.value < 1 }
								.filter { it.primaryMarks.hasNumber(candidateZ) }
								.toSet().toList()
							if (cellsToWorkOn.isEmpty()) continue@forWingYCell
							// found cells where Z can be removed
							nextStepState = NextStepStates.STEP_FOUND
							nextStepStrategyId = StrategyIds.XYZ_WING
							nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
							nextStepActionRemoveCandidates[candidateZ] = cellsToWorkOn as MutableList
							val candidateX = pivotCell.primaryMarks.marksValues
								.toSet().subtract(wingYCell.primaryMarks.marksValues.toSet()).first()
							val candidatesPivot = formatCellMarks(pivotCell)
							val candidatesPivotXYZ = "{$candidateX,$candidateY,$candidateZ}"
							val candidatesWingX = "{$candidateX,$candidateZ}"
							val candidatesWingY = "{$candidateY,$candidateZ}"
							when (hintLevel) {
								HintLevels.LEVEL1 -> {
									nextStepText = nextStepStrategyName
								}
								HintLevels.LEVEL2 -> {
									nextStepText = "$nextStepStrategyName: $candidatesPivot + {$candidateZ}"
								}
								HintLevels.LEVEL3 -> {
									nextStepText = "$nextStepStrategyName: $candidatesPivot + {$candidateZ}\n"
									nextStepText += "  XYZ = $candidatesPivotXYZ ➠ [${pivotCell.gridAddress}]\n"
									nextStepText += "  XZ = $candidatesWingX ➠ [${wingXCell.gridAddress}]\n"
									nextStepText += "  YZ = $candidatesWingY ➠ [${wingYCell.gridAddress}]\n"
									cellsToHighlight[HintHighlight.REGION] = visibleCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] = listOf(wingXCell, wingYCell, pivotCell).map { it.rowIndex to it.columnIndex }
								}
								HintLevels.LEVEL4 -> {
									nextStepText = "$nextStepStrategyName: $candidatesPivot + {$candidateZ}\n"
									nextStepText += "  XYZ = $candidatesPivotXYZ ➠ [${pivotCell.gridAddress}]\n"
									nextStepText += "  XZ = $candidatesWingX ➠ [${wingXCell.gridAddress}]\n"
									nextStepText += "  YZ = $candidatesWingY ➠ [${wingYCell.gridAddress}]\n"
									nextStepText += getNextStepActionRemoveCandidatesAsText()
									cellsToHighlight[HintHighlight.REGION] = visibleCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] = listOf(wingXCell, wingYCell, pivotCell).map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.TARGET] = cellsToWorkOn.map { it.rowIndex to it.columnIndex }
								}
							}
							return true
						}
					}
				}
			}
		}
		return false
	}

}

