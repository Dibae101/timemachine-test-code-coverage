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

/** Strategy: WXYZ Wing
 */
class NextStepWXYZWing(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForWXYZWing()
	}

	/** WXYZ-Wing (basic, not extended)
	 *
	 *  - The WXYZ-Wing is an extension of the XYZ-Wing, using one cell and candidate more.
	 *  - The WXYZ-Wing consists of a group of 4 cells.
	 *
	 *  - The one central cell is named 'pivot' or 'hinge'.
	 *  - The three wing cells are named 'pincer' or 'wing'.
	 *
	 *  - The 4 cells are located in an region consisting of box and row or box and column.
	 *      - The pivot cell is in an box
	 *      - The wing cells are in the same box and
	 *         - in the row with the pivot cell
	 *         OR
	 *         - in the column with the pivot cell
	 *         - WZ is always in the box
	 *  - The pivot cell must have 4 candidates and the wing cells must have 2 candidates each.
	 *  - Each cell must share 1 candidate (Z) with the other cells.
	 *  - In total there are only 4 candidates (WXYZ) in the 4 cells.
	 *
	 *  - Are the 4 cells only in the box, we have a Naked Quad!
	 *    So, only max 3 cells in the box.
	 *
	 *  - The pivot cell must see all wing cells.
	 * 	- There is no need for the wing cells to see each other wing cell.
	 *
	 *  - In all cells seen by the 4 cells the one shared candidate (Z) can be removed.
	 *  - The candidate Z can be removed from all cells seen by the pivot and wing cells,
	 * 	  except the pivot and wing cell.
	 *
	 *  - The pivot cell contains the candidates W,X,Y and Z.
	 * 	- The wing cells contains the candidates
	 * 		- wingW: W and Z
	 * 		- wingX: X and Z
	 * 		- wingY: Y and Z
	 *
	 * Messages:
	 *
	 *   "WXYZ-Wing"
	 *  " ⊞ Candidates Pivot , Z ➠ {2,3,4,5} , {2}"
	 *  " ⊞ Pivot WXYZ ➠ {3,5,4,2} ➠ [ r5c6 ]"
	 *  " ⊞ Wing W ➠ {3,2} ➠ [ r4c6 ]"
	 *  " ⊞ Wing X ➠ {5,2} ➠ [ r1c6 ]"
	 *  " ⊞ Wing Y ➠ {4,2} ➠ [ r6c5 ]"
	 *  " ✎ remove candidate {2} from [ r6c6 ]"
	 *
	 */
	private fun checkForWXYZWing(): Boolean {

		val cells = board.cells

		val pivotCells = mutableListOf<Cell>()
		cells.forEach { row ->
			row.forEach forCells@{ cell ->
				if (cell.value > 0) return@forCells // cell has already a value
				if (cell.primaryMarks.marksValues.size != 4) return@forCells // not 4 candidates
				pivotCells.add(cell)
			}
		}

		// loop over all possible pivot cells
		forPivotCells@ for (pivotCell in pivotCells) {

			// search in the box of the pivot cell for possible wing cells
			val wingCellsBox = mutableListOf<Cell>()
			forCellInPivotBox@ for (wingCell in pivotCell.sector!!.cells) {
				if (wingCell.value > 0)
					continue@forCellInPivotBox // cell has already a value
				if (wingCell.primaryMarks.marksValues.size != 2)
					continue@forCellInPivotBox // not 2 candidates
				if (! (pivotCell.primaryMarks.marksValues.containsAll(wingCell.primaryMarks.marksValues)))
					continue@forCellInPivotBox // both marks of cell not in pivotCell
				wingCellsBox.add(wingCell)
			}
			if (wingCellsBox.isEmpty() or (wingCellsBox.size > 3))
				continue@forPivotCells // no possible wing cell or to much cells found

			// search in the row of the pivot cell for possible wing cells
			val wingCellsRow = mutableListOf<Cell>()
			forCellInPivotRow@ for (rowCell in pivotCell.row!!.cells) {
				if (rowCell.value > 0)
					continue@forCellInPivotRow // cell has already a value
				if (pivotCell.sectorIndex == rowCell.sectorIndex)
					continue@forCellInPivotRow // cell in pivot box
				if (rowCell.primaryMarks.marksValues.size != 2)
					continue@forCellInPivotRow // not 2 candidates
				if (!(pivotCell.primaryMarks.marksValues.containsAll(rowCell.primaryMarks.marksValues)))
					continue@forCellInPivotRow // both marks of cell not in pivotCell
				wingCellsRow.add(rowCell)
			}

			// search in the column of the pivot cell for possible wing cells
			val wingCellsCol = mutableListOf<Cell>()
			forCellInPivotCol@ for (colCell in pivotCell.column!!.cells) {
				if (colCell.value > 0)
					continue@forCellInPivotCol // cell has already a value
				if (pivotCell.sectorIndex == colCell.sectorIndex)
					continue@forCellInPivotCol // cell in pivot box
				if (colCell.primaryMarks.marksValues.size != 2)
					continue@forCellInPivotCol // not 2 candidates
				if (!(pivotCell.primaryMarks.marksValues.containsAll(colCell.primaryMarks.marksValues)))
					continue@forCellInPivotCol // both marks of cell not in pivotCell
				wingCellsCol.add(colCell)
			}

			// loop over possible wing cell W, should be in the pivot box
			forWingCellW@ for (wingCellW in wingCellsBox) {
				forCandidateZ@ for (candidateZ in wingCellW.primaryMarks.marksValues) {
					// wingCellW
					// candidateZ
					val candidateW = wingCellW.primaryMarks.marksValues.first { it != candidateZ }

					// cellList for wingX, could be in the box or row/column
					val wingCellsRowBoxX = (wingCellsRow + wingCellsBox)
						.toSet().toList()
						.filter { it != pivotCell}
						.filter { it != wingCellW}
						.filter { candidateZ in it.primaryMarks.marksValues}
						.filterNot { candidateW in it.primaryMarks.marksValues}
					val wingCellsColBoxX = (wingCellsCol + wingCellsBox)
						.toSet().toList()
						.filter { it != pivotCell}
						.filter { it != wingCellW}
						.filter { candidateZ in it.primaryMarks.marksValues}
						.filterNot { candidateW in it.primaryMarks.marksValues}
					val wingCellsMapX = mapOf(
							HouseTypes.ROW to wingCellsRowBoxX,
							HouseTypes.COL to wingCellsColBoxX )

					forListInWingCellsLists@ for (entry in wingCellsMapX.iterator()) {
						val wingCellsHouseType = entry.key
						val wingCellsX = entry.value
						if (wingCellsX.isEmpty())
							continue@forListInWingCellsLists
						if (wingCellsX.size < 2)
							continue@forListInWingCellsLists // we need cells for X and Y

						// loop over cells for Wing X
						forWingCellX@ for (wingCellX in wingCellsX) {
							// wingCellW, candidateZ, candidateW, wingCellX
							val candidateX =
								wingCellX.primaryMarks.marksValues.first { it != candidateZ }

							val wingCellsY = wingCellsX
								.filter { it != wingCellX}
								.filterNot { candidateX in it.primaryMarks.marksValues}
							if (wingCellsY.isEmpty())
								continue@forWingCellX // we need a for cell Y

							// loop over cells for Wing Y
							forWingCellY@ for (wingCellY in wingCellsY) {
								// wingCellW, candidateZ, candidateW, wingCellX, candidateX, wingCellY
								val candidateY =
									wingCellY.primaryMarks.marksValues.first { it != candidateZ }

								// pivot and wings W,X,Y found with common Z
								val groupCells = listOf(pivotCell, wingCellW, wingCellX, wingCellY)

								// check for cells where Z can be removed
								val cellsToWorkOn = getUnsolvedCellsSeenByAll(groupCells)
									.filter { it.primaryMarks.hasNumber(candidateZ) }
									.toSet().toList()
								if (cellsToWorkOn.isEmpty()) continue@forWingCellY
								nextStepActionRemoveCandidates[candidateZ] = cellsToWorkOn as MutableList

								// found cells where Z can be removed
								nextStepState = NextStepStates.STEP_FOUND
								nextStepStrategyId = StrategyIds.WXYZ_WING
								nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

								//var regionCells = listOf<Cell>()
								val regionCells = if (wingCellsHouseType  == HouseTypes.ROW) {
									(pivotCell.sector!!.cells + pivotCell.row!!.cells).toSet()
										.toList()
								} else {
									(pivotCell.sector!!.cells + pivotCell.column!!.cells).toSet()
										.toList()
								}

								// Cells with there candidates in 'right' order
								val candidatesPivot = formatCellMarks(pivotCell)
								val candidatesPivotWXYZ = formatCellMarks(listOf(candidateW,candidateX,candidateY,candidateZ))
								val candidatesWingW = formatCellMarks(listOf(candidateW,candidateZ))
								val candidatesWingX = formatCellMarks(listOf(candidateX,candidateZ))
								val candidatesWingY = formatCellMarks(listOf(candidateY,candidateZ))

								when (hintLevel) {
									HintLevels.LEVEL1 -> {
										nextStepText = nextStepStrategyName
									}

									HintLevels.LEVEL2 -> {
										nextStepText = nextStepStrategyName
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_candidates
											,"$candidatesPivot , {$candidateZ}")
									}

									HintLevels.LEVEL3 -> {
										nextStepText = nextStepStrategyName
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_candidates
											,"$candidatesPivot , {$candidateZ}")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_pivot
											,candidatesPivotWXYZ , "[${pivotCell.gridAddress}]")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_wing_w
											,candidatesWingW, "[${wingCellW.gridAddress}]")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_wing_x
											,candidatesWingX, "[${wingCellX.gridAddress}]")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_wing_y
											,candidatesWingY, "[${wingCellY.gridAddress}]")
										cellsToHighlight[HintHighlight.REGION] =
											regionCells.map { it.rowIndex to it.columnIndex }
										cellsToHighlight[HintHighlight.CAUSE] =
											groupCells.map { it.rowIndex to it.columnIndex }
									}

									HintLevels.LEVEL4 -> {
										nextStepText = nextStepStrategyName
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_candidates
											,"$candidatesPivot , {$candidateZ}")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_pivot
											,candidatesPivotWXYZ , "[${pivotCell.gridAddress}]")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_wing_w
											,candidatesWingW, "[${wingCellW.gridAddress}]")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_wing_x
											,candidatesWingX, "[${wingCellX.gridAddress}]")
										nextStepText += "\n" + context.getString(R.string.hint_strategy_wxyz_wing_wing_y
											,candidatesWingY, "[${wingCellY.gridAddress}]")
										nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
										cellsToHighlight[HintHighlight.REGION] =
											regionCells.map { it.rowIndex to it.columnIndex }
										cellsToHighlight[HintHighlight.CAUSE] =
											groupCells.map { it.rowIndex to it.columnIndex }
										cellsToHighlight[HintHighlight.TARGET] =
											cellsToWorkOn.map { it.rowIndex to it.columnIndex }
									}
								}
								return true
							} // forWingCellY@
						} // forWingCellX@
					} // forListInWingCellsLists@
				} // forCandidateZ@
			} // forWingCellW@
		} // forPivotCells@
		return false
	}

}
