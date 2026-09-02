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

/** Strategy: XY Wing
 */
class NextStepXYWing(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForXYWing()
	}

	/** XY-Wing
	 *
	 * the central cell is named 'pivot' or 'hinge'
	 * thw wing cells are named 'pincer' or 'wing'
	 *
	 * all cell must have two candidate
	 *
	 * the pivot cell contains the candidates X and Y
	 *
	 * the wing cells contains the candidates
	 * 		- wingA: X and Z
	 * 		- wingB: Y and Z
	 *
	 * Messages:
	 *
	 *  "XY-Wing:"
	 *  " ⊞ Candidates XY+Z ➠ {6,9} + {4}"
	 *  " ⊞ Pivot XY ➠ {6,9} ➠ [ r2c3 ]"
	 *  " ⊞ Wing X ➠ {6,4} ➠ [ r2c8 ]"
	 *  " ⊞ Wing Y ➠ {9,4} ➠ [ r6c3 ]"
	 *  " ✎ remove candidate {4} from [ r6c8 ]"
	 *
	 * Logic:
	 *
	 * 	- create a list with all cells with two candidates
	 * 	- loop over the list for the pivot cell
	 * 	- search in the list the wing cell A with candidate X
	 * 	- get candidate Z from wing cell A
	 * 	- search in the list the wing cell B with candidate Y and Z
	 * 	- create a list with cells which are seen by wing cell A and B
	 * 	- filter the list, not pivot, not wings, must contain candidate `false`
	 * 	- if list is not empty then we have found a y-wing with work to do
	 *
	 */
	private fun checkForXYWing(): Boolean {

		val cells = board.cells

		// create a list with all cells with two candidates
		val cellsWithTwoCandidates = mutableListOf<Cell>()
		cells.forEach { row ->
			row.forEach { cell ->
				if (cell.value == 0 && cell.primaryMarks.marksValues.size == 2) {
					cellsWithTwoCandidates.add(cell)
				}
			}
		}

		// if more than 2 cell found start with search for a y-wing
		var candidateX: Int
		var candidateY: Int
		var candidateZ: Int
		if (cellsWithTwoCandidates.size > 2) {
			for (pivotCell in cellsWithTwoCandidates) {
				candidateX = pivotCell.primaryMarks.marksValues[0]
				candidateY = pivotCell.primaryMarks.marksValues[1]
				// search for first pincer cell with A
				forWingCellX@ for (wingCellX in cellsWithTwoCandidates) {
					if (wingCellX == pivotCell) continue@forWingCellX
					if (!(wingCellX.primaryMarks.hasNumber(candidateX))) continue@forWingCellX
					if (wingCellX.primaryMarks.hasNumber(candidateY)) continue@forWingCellX
					// check if wing X is visible for pivot
					if (!((pivotCell.rowIndex == wingCellX.rowIndex) ||
							(pivotCell.columnIndex == wingCellX.columnIndex) ||
							(pivotCell.sectorIndex == wingCellX.sectorIndex))
					) continue@forWingCellX
					candidateZ = (wingCellX.primaryMarks.marksValues.filter { it != candidateX })[0]
					// search for the second wing cell with X and Z
					forWingCellY@ for (wingCellY in cellsWithTwoCandidates) {
						if (wingCellY == pivotCell) continue@forWingCellY
						if (wingCellY == wingCellX) continue@forWingCellY
						if (!(wingCellY.primaryMarks.hasNumber(candidateY))) continue@forWingCellY
						if (!(wingCellY.primaryMarks.hasNumber(candidateZ))) continue@forWingCellY
						// check if wing Y is visible for pivot
						if (!((pivotCell.rowIndex == wingCellY.rowIndex) ||
								(pivotCell.columnIndex == wingCellY.columnIndex) ||
								(pivotCell.sectorIndex == wingCellY.sectorIndex))
						) continue@forWingCellY
						// Y-Wing found
						// search for cells to work on for Z witch can be removed
						var visibleCells = mutableListOf<Cell>()
						var visibleCellsWingX = mutableListOf<Cell>()
						visibleCellsWingX.addAll(wingCellX.row!!.cells.toList())
						visibleCellsWingX.addAll(wingCellX.column!!.cells.toList())
						visibleCellsWingX.addAll(wingCellX.sector!!.cells.toList())
						visibleCells.addAll(visibleCellsWingX)
						visibleCellsWingX = visibleCellsWingX
							.filterNot { it in listOf(wingCellX, wingCellY) }
							.filter { it.value < 1 }
							.filter { it.primaryMarks.hasNumber(candidateZ) }
							.toSet()
							.toMutableList()
						var visibleCellsWingY = mutableListOf<Cell>()
						visibleCellsWingY.addAll(wingCellY.row!!.cells.toList())
						visibleCellsWingY.addAll(wingCellY.column!!.cells.toList())
						visibleCellsWingY.addAll(wingCellY.sector!!.cells.toList())
						visibleCells = visibleCells.intersect(visibleCellsWingY.toSet()).toMutableList()
						visibleCellsWingY = visibleCellsWingY
							.filterNot { it in listOf(wingCellX, wingCellY) }
							.filter { it.value < 1 }
							.filter { it.primaryMarks.hasNumber(candidateZ) }
							.toSet()
							.toMutableList()
						// create one list from visibleCellsWingX and visibleCellsWingY
						val cellsToWorkOn = visibleCellsWingX.intersect(visibleCellsWingY.toSet()).toList()
						if (cellsToWorkOn.isEmpty()) continue@forWingCellY
						// we have found a y-wing with work to do
						nextStepState = NextStepStates.STEP_FOUND
						nextStepStrategyId = StrategyIds.XY_WING
						nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
						nextStepActionRemoveCandidates[candidateZ] = cellsToWorkOn as MutableList
						val candidatesPivot = formatCellMarks(pivotCell)
						val candidatesPivotXY = "{$candidateX,$candidateY}"
						val candidatesWingX = "{$candidateX,$candidateZ}"
						val candidatesWingY = "{$candidateY,$candidateZ}"
						when (hintLevel) {
							HintLevels.LEVEL1 -> {
								nextStepText = nextStepStrategyName
							}
							HintLevels.LEVEL2 -> {
								nextStepText = nextStepStrategyName
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_candidates
									,"$candidatesPivot + {$candidateZ}")
							}
							HintLevels.LEVEL3 -> {
								nextStepText = nextStepStrategyName
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_candidates
									,"$candidatesPivot + {$candidateZ}")
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_pivot
									,candidatesPivotXY, "[${pivotCell.gridAddress}]")
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_wing_x
									,candidatesWingX, "[${wingCellX.gridAddress}]")
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_wing_y
									,candidatesWingY, "[${wingCellY.gridAddress}]")
								cellsToHighlight[HintHighlight.REGION] = visibleCells.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.CAUSE] = listOf(wingCellY, wingCellX, pivotCell).map { it.rowIndex to it.columnIndex }
							}
							HintLevels.LEVEL4 -> {
								nextStepText = nextStepStrategyName
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_candidates
									,"$candidatesPivot + {$candidateZ}")
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_pivot
									,candidatesPivotXY, "[${pivotCell.gridAddress}]")
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_wing_x
									,candidatesWingX, "[${wingCellX.gridAddress}]")
								nextStepText += "\n" + context.getString(R.string.hint_strategy_xy_wing_wing_y
									,candidatesWingY, "[${wingCellY.gridAddress}]")
								nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
								cellsToHighlight[HintHighlight.REGION] = visibleCells.map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.CAUSE] = listOf(wingCellY, wingCellX, pivotCell).map { it.rowIndex to it.columnIndex }
								cellsToHighlight[HintHighlight.TARGET] = cellsToWorkOn.map { it.rowIndex to it.columnIndex }
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

