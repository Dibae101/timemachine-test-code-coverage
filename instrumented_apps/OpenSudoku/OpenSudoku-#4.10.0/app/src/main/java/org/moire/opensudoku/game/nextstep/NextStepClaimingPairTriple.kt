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

/** Strategy: Claiming Pair / Claiming Triple
 * also: Locked Candidate Type 2 == Claiming
 */
class NextStepClaimingPairTriple(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForClaimingPairTriple()
	}

	/** Strategy: Claiming Pair / Claiming Triple
	 *
	 * Claiming Pair and Claiming Triple
	 *  - Intersection Removal
	 * 	- LINE -> BOX Reduction / Interaction
	 *  - Locked Candidates Type 2
	 *
	 * Check if an candidate number exits in an row or column only in one box.
	 * If this is the case, these means that this candidate number will
	 * at the end in this box only in this row or column.
	 * You can delete all candidate with this number from the cells
	 * from all other rows or columns inside of the box.
	 *
	 * Message:
	 *
	 *	"Locked Candidate Type 2 - Claiming Pair"
	 *  " ⊞ Candidate ➠ {4}"
	 *  " ⊞ Cells ➠ [ r1c3,r2c3 ]"
	 *  " ⊞ Line ➠ (c3) ➠ Box (b1)"
	 *  " ✎ remove candidate {4} from [ r1c2,r2c2 ]
	 *
	 * 	"Locked Candidate Type 2 - Claiming Triple"
	 *  " ⊞ Candidate ➠ {4}"
	 *  " ⊞ Cells ➠ [ r1c4,r1c5,r1c6 ]"
	 *  " ⊞ Line ➠ (r1) ➠ Box (b2)"
	 * 	" ✎ remove candidate {4} from [ r2c4,r3c4 ]
     *
	 */
	private fun checkForClaimingPairTriple(): Boolean {

		// loop over all house types , not BOX
		forHouseType@ for (houseType in HouseTypes.entries) {

			// get all houses for the house types
			val houseCellsArray = when (houseType) {
				HouseTypes.ROW -> board.getHousesRows()
				HouseTypes.COL -> board.getHousesColumns()
				HouseTypes.BOX ->
					//mCells.getHousesSectors()
					continue@forHouseType
			}

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
				forCount@ for ((number, cells) in numberCellsMap) {

					// all cells in the same box?
					val cellBoxes = cells.map { it.sectorIndex }.toSet()
					if (cellBoxes.size == 1) {
						val boxCells = cells[0].sector!!.cells.toList()
						val cellsToWorkOn = boxCells
							.filter { it !in cells }
							.filter { it.value == 0 }
							.filter { it.primaryMarks.hasNumber(number) }
						nextStepStrategyId = when (cells.size) {
							2 -> StrategyIds.CLAIMING_PAIR
							3 -> StrategyIds.CLAIMING_TRIPLE
							else-> StrategyIds.CLAIMING_GROUP
						}
						nextStepStrategyName = when (cells.size) {
							2,3 -> nextStepStrategyId.getStrategyName(context)
							else->  context.getString(R.string.hint_strategy_claiming_group,"${cells.size}")
						}
						if (cellsToWorkOn.isNotEmpty()) {
							nextStepState = NextStepStates.STEP_FOUND
							regionCells.addAll(cells[0].sector!!.cells)
							nextStepActionRemoveCandidates[number] = cellsToWorkOn as MutableList
							val msgCandidate = "{$number}"
							val msgLine = "(${houseType.houseAdr(cells[0])})"
							val msgBox = "(${HouseTypes.BOX.houseAdr(cells[0])})"
							when (hintLevel) {
								HintLevels.LEVEL1 -> {
									nextStepText = nextStepStrategyName
								}
								HintLevels.LEVEL2 -> {
									nextStepText = nextStepStrategyName
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_candidate,
										msgCandidate)
								}
								HintLevels.LEVEL3 -> {
									nextStepText = nextStepStrategyName
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_candidate,
										msgCandidate)
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_cells,
										getCellsGridAddress(cells))
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_extra,
										msgLine,msgBox)
									cellsToHighlight[HintHighlight.REGION] = regionCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] = cells.map { it.rowIndex to it.columnIndex }
								}
								HintLevels.LEVEL4 -> {
									nextStepText = nextStepStrategyName
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_candidate,
										msgCandidate)
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_cells,
										getCellsGridAddress(cells))
									nextStepText += "\n" + context.getString(R.string.hint_strategy_claiming_extra,
										msgLine,msgBox)
									nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
									cellsToHighlight[HintHighlight.REGION] = regionCells.map { it.rowIndex to it.columnIndex }
									cellsToHighlight[HintHighlight.CAUSE] = cells.map { it.rowIndex to it.columnIndex }
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

