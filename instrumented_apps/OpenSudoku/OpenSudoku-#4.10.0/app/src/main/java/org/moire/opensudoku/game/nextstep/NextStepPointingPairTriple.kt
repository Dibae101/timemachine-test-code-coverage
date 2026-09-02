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
import kotlin.collections.addAll

/** Strategy: Pointing Pair / Pointing Triple
 */
class NextStepPointingPairTriple(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForPointingPairTriple()
	}

	/** Strategy: Pointing Pair / Pointing Triple
	 *
	 * Pointing Pair and Pointing Triple
	 *  - Intersection Removal
	 * 	- BOX -> LINE Reduction / Interaction
	 *  - Locked Candidates Type 1
	 *
	 *  Check if an candidate number exits in a box only in one row or column.
	 *  If this is the case, these means that this candidate number will
	 *  block the row or column outside of the box.
	 *  You can delete all candidate with this number from the cells of
	 *  the row or column outside of the box.
	 *
	 * Message:
	 *
	 * 	"Locked Candidate Type 1 - Pointing Pair"
	 *  " ⊞ Candidate ➠ {8}"
	 *  " ⊞ Cells ➠ [ r1c4,r3c4 ]"
	 *  " ⊞ Box ➠ (b2) ➠ Line (c4)"
	 * 	" ✎ remove candidate {8} from [ r6c4 ]"
	 *
	 * 	"Locked Candidate Type 1 - Pointing Triple"
	 *  " ⊞ Candidate ➠ {8}"
	 *  " ⊞ Cells ➠ [ r1c4,r2c4,r3c4 ]"
	 *  " ⊞ Box ➠ (b2) ➠ Line (c4)"
	 * 	" ✎ remove candidate {8} from [ r4c4,r5c4,r6c4 ]"
	 *
	 */
	private fun checkForPointingPairTriple(): Boolean {

		// get all houses from type BOX
		val houseType = HouseTypes.BOX
		val houseCellsArray = board.getHousesSectors()

		// loop over all houses in the house type
		forHouse@ for (houseCells in houseCellsArray) {
			val regionCells = arrayListOf<Cell>()
			regionCells.addAll(houseCells.cells)

			// loop over all cells in the house and create a number-cell-map
			var numberCellsMap = mutableMapOf<Int, MutableList<Cell>>()
			forCellInHouse@ for (cell in houseCells.cells) {
				if (cell.value > 0) continue // already solved
				val marksValues = cell.primaryMarks.marksValues
				for (number in marksValues) {
					val numberCells =
						numberCellsMap.getOrDefault(number, mutableListOf())
					numberCells.add(cell)
					numberCellsMap[number] = numberCells
				}
			}
			if (numberCellsMap.isEmpty()) continue@forHouse // all cells are solved
			// filter list by count < 4 and sort to make it easier for the user ...
			numberCellsMap = numberCellsMap
				.filter { it.value.size in listOf(2, 3) } as MutableMap<Int, MutableList<Cell>>
			if (numberCellsMap.isEmpty()) continue@forHouse // no cells found
			numberCellsMap = numberCellsMap
				.toSortedMap()
				.toList()
				.sortedBy { (_, value) -> value.size }
				.toMap() as MutableMap<Int, MutableList<Cell>>

			// loop over number sorted by count (single,pair,triple)
			forCount@ for ((pointingCandidate, pointingCells) in numberCellsMap) {
				nextStepState = NextStepStates.STEP_FOUND
				nextStepStrategyId = when (pointingCells.size) {
					2 -> StrategyIds.POINTING_PAIR
					3 -> StrategyIds.POINTING_TRIPLE
					else-> StrategyIds.POINTING_GROUP
				}
				nextStepStrategyName = when (pointingCells.size) {
					2,3 -> nextStepStrategyId.getStrategyName(context)
					else-> context.getString(R.string.hint_strategy_pointing_group,"${pointingCells.size}")
				}

				var stepFound = false
				val msgBox = "(${houseType.houseAdr(pointingCells[0])})"
				var msgLine = ""
				var cellsToWorkOn = listOf<Cell>()

				// all cells in the same row?
				if (pointingCells.map(Cell::rowIndex).toSet().size == 1) {
					msgLine = "(${HouseTypes.ROW.houseAdr(pointingCells[0])})"
					cellsToWorkOn = pointingCells[0].row!!.cells
						.filter { it !in pointingCells }
						.filter { it.value == 0 }
						.filter { it.primaryMarks.hasNumber(pointingCandidate) }
					if (cellsToWorkOn.isNotEmpty()) {
						pointingCells.size
						regionCells.addAll(pointingCells[0].row!!.cells)
						nextStepActionRemoveCandidates[pointingCandidate] = cellsToWorkOn as MutableList
						stepFound = true
					}
				}

				// all cells in the same column?
				if (pointingCells.map(Cell::columnIndex).toSet().size == 1) {
					msgLine = "(${HouseTypes.COL.houseAdr(pointingCells[0])})"
					cellsToWorkOn = pointingCells[0].column!!.cells
						.filter { it !in pointingCells }
						.filter { it.value == 0 }
						.filter { it.primaryMarks.hasNumber(pointingCandidate) }
					if (cellsToWorkOn.isNotEmpty()) {
						pointingCells.size
						regionCells.addAll(pointingCells[0].column!!.cells)
						nextStepActionRemoveCandidates[pointingCandidate] = cellsToWorkOn as MutableList
						stepFound = true
					}
				}

				if (stepFound) {
					when (hintLevel) {
						HintLevels.LEVEL1 -> {
							nextStepText = nextStepStrategyName
						}
						HintLevels.LEVEL2 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_candidate,
								"{$pointingCandidate}")
						}
						HintLevels.LEVEL3 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_candidate,
								"{$pointingCandidate}")
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_cells,
								getCellsGridAddress(pointingCells))
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_extra,
								msgBox,msgLine)
							cellsToHighlight[HintHighlight.REGION] = regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] = pointingCells.map { it.rowIndex to it.columnIndex }
						}
						HintLevels.LEVEL4 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_candidate,
								"{$pointingCandidate}")
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_cells,
								getCellsGridAddress(pointingCells))
							nextStepText += "\n" + context.getString(R.string.hint_strategy_pointing_extra,
								msgBox,msgLine)
							nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
							cellsToHighlight[HintHighlight.REGION] = regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] = pointingCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.TARGET] = cellsToWorkOn.map { it.rowIndex to it.columnIndex }
						}
					}
					return true
				}
			} // forCount@
		} // forHouse@
		return false
	}

}

