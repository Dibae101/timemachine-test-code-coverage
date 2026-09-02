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

/** Strategy: Naked Group
 *
 *  the strategy package 'naked group' contains the following strategies:
 *  - naked pair / locked pair
 *  - naked triples / locked triples
 *  - naked quads
 *
 */
open class NextStepNakedGroup(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		// This methode is not implemented in this class!
		return false
	}

	/** Strategy: Naked Group  ->  Naked pair / triple / quad  or  Locked pair / triple
	 *
	 * Find a naked group in a house.
	 * The groupSize could be: 2 = pair | 3 = triple | 4 = quad
	 *
	 * Message:
	 *
	 * 	- naked ...
	 * 		"Naked Pair"
	 *  	" ⊞ Candidates ➠ {1,3}"
	 *  	" ⊞ House ➠ (r4)"
	 *  	" ⊞ Cells ➠ [ r4c1,r4c4 ]"
	 *  	" ✎ remove {1} from [ r4c2,r4c9 ]"
	 *  	" ✎ remove {3} from [ r4c5,r4c9 ]"
	 *
	 * 	- locked ...
	 * 		"Locked Pair"
	 *  	" ⊞ Candidates ➠ {1,3}"
	 *  	" ⊞ Houses ➠ (r4,b4)"
	 *  	" ⊞ Cells ➠ [ r4c1,r4c3 ]"
	 *  	" ✎ remove {1} from [ r4c2,r4c9,r5c1 ]"
	 *  	" ✎ remove {3} from [ r4c5,r4c9 ]"
	 *
	 * Logic:
	 *  1. search all possible candidates for the unsolved cells in a house
	 *  2. build groups (combinations) depending on the group size
	 *  3. search with each group for a solution
	 *     3.1 search for cells (count = group size) which contains only candidates
	 *         from the group
	 *     3.2 each candidate from the group must be present in minimum in one found cell
	 *  4. we have found a naked group, now we must check if a user action is possible
	 *     4.1 search in the other unsolved cells which are not part of the naked group
	 *         for group candidates
	 *     4.2 found candidates can be removed by the user -> next step found
	 *
	 *  5. check for special case: LOCKED PAIR & LOCKED TRIPLE
	 *  	1. are the found cells of the house type ROW also in the same BOX
	 *  	2. are the found cells of the house type COL also in the same BOX
	 *  	3. are the found cells of the house type BOX also in the same ROW
	 *  	4. are the found cells of the house type BOX also in the same COL
	 *     -> then we have found LOCKED PAIR or LOCKED TRIPLE
	 *     -> in this case the candidates can also removed from the other house type
	 *     -> use the name LOCKED only if a candidate can be removed by the additional cells
	 */
	protected fun checkForNakedGroup(groupSize: Int): Boolean {

		if (groupSize !in 2..4) {
			error("groupSize must be between 2 and 4")
		}

		// loop over all house types
		forHouseType@ for (houseType in HouseTypes.entries) {

			// get all houses for the house types
			val houseCellsArray = when (houseType) {
				HouseTypes.ROW -> board.getHousesRows()
				HouseTypes.COL -> board.getHousesColumns()
				HouseTypes.BOX -> board.getHousesSectors()
			}

			// loop over all houses
			forHouse@ for (house in houseCellsArray) {
				val houseCells = house.cells
				var groupTypeName = "Naked"
				val houseAdr = houseType.houseAdr(houseCells.first())

				// create a list with needed solution values (= allowed candidates)
				var allowedCandidates = allowedDigits
				houseCells
					.filter { it.value != 0 }
					.forEach { allowedCandidates = allowedCandidates.minus(it.value) }

				// enough candidates to have a naked group ?
				if (allowedCandidates.size < groupSize) continue@forHouse

				// create a list with all group combinations
				val groupCombinations = combinations(allowedCandidates.toList(), groupSize).toList()

				// loop over group combinations to find a hidden group
				forGroupCombination@ for (groupCombination in groupCombinations) {
					// loop over unsolved cells to find a group
					val cellsFound = houseCells.filter { cell ->
						// cell solved?
						cell.value == 0
							// more candidates as needed for this naked group?
							&& cell.primaryMarks.marksValues.size <= groupSize
							// not only group candidates for this cell?
							&& cell.primaryMarks.marksValues.filterNot { it in groupCombination }.isEmpty()
							// candidate from the group present?
							&& !cell.primaryMarks.marksValues.none { it in groupCombination }
					}
					// needed count of cells for the group found?
					if (cellsFound.size != groupSize) continue@forGroupCombination

					// add cells from house which must be checked for remove of group candidates
					val cellsToCheck = arrayListOf<Cell>()
					houseCells
						.filter { it.value == 0 }
						.filter { it !in cellsFound }
						.forEach(cellsToCheck::add)

					// check if user action is needed for house
					var removeDigitCellsMap = mutableMapOf<Int, MutableList<Cell>>()
					for (cell in cellsToCheck) {
						val candidates = cell.primaryMarks.marksValues.filter { it in groupCombination }
						for (candidate in candidates) {
							val removeCells = removeDigitCellsMap.getOrDefault(candidate, mutableListOf())
							removeCells.add(cell)
							removeDigitCellsMap[candidate] = removeCells
						}
					}
					if (removeDigitCellsMap.isEmpty()) continue@forGroupCombination

					// setup region and addresses of houses
					val regionCells = arrayListOf<Cell>()
					regionCells.addAll(houseCells)
					var houseAdrList = houseAdr

					// check for LOCKED only for pairs and triples
					if (groupSize < 4) {
						var extraHouseFound = false
						var extraHouseCells = listOf<Cell>()
						var extraHousesAdr = ""
						var extraCellsFound = listOf<Cell>()

						when (houseType) {

							HouseTypes.ROW, HouseTypes.COL -> {
								val cellsFoundInBoxList = cellsFound.map(Cell::sectorIndex).toSet()
								if (cellsFoundInBoxList.size == 1) {
									extraHouseFound = true
									extraHouseCells = board.getHousesSectors()[cellsFoundInBoxList.first()].cells.toList()
									extraHousesAdr = HouseTypes.BOX.houseAdr(extraHouseCells[0])
									extraCellsFound = if (houseType == HouseTypes.ROW) {
										extraHouseCells.filter { it.rowIndex != cellsFound.first().rowIndex }
									} else {
										extraHouseCells.filter { it.columnIndex != cellsFound.first().columnIndex }
									}
								}
							}

							HouseTypes.BOX -> {
								val cellsFoundInRowList = cellsFound.map(Cell::rowIndex).toSet()
								if (cellsFoundInRowList.size == 1) {
									extraHouseFound = true
									extraHouseCells = board.getHousesRows()[cellsFoundInRowList.first()].cells.toList()
									extraHousesAdr = HouseTypes.ROW.houseAdr(extraHouseCells[0])
									extraCellsFound = extraHouseCells
										.filter{ it.sectorIndex != cellsFound.first().sectorIndex }
								}

								val cellsFoundInColList = cellsFound.map(Cell::columnIndex).toSet()
								if (cellsFoundInColList.size == 1) {
									extraHouseFound = true
									extraHouseCells = board.getHousesColumns()[cellsFoundInColList.first()].cells.toList()
									extraHousesAdr = HouseTypes.COL.houseAdr(extraHouseCells[0])
									extraCellsFound = extraHouseCells
										.filter { it.sectorIndex != cellsFound.first().sectorIndex }
								}
							}
						}
						if (extraHouseFound) {
							val extraCellsToWorkOn = extraCellsFound
								.filter { it.value == 0 }
								.filter { it.primaryMarks.marksValues.intersect(
									groupCombination.toSet()).isNotEmpty() }
							if (extraCellsToWorkOn.isNotEmpty()) {
								regionCells.addAll(extraHouseCells.filter { it !in regionCells })
								houseAdrList = "$houseAdr,$extraHousesAdr"
								groupTypeName = "Locked"
								// add extraCells to cellsToCheck and recreate the map
								extraHouseCells
									.filter { it.value == 0 }
									.filter { it !in cellsFound }
									.filter { it !in cellsToCheck }
									.forEach(cellsToCheck::add)
								removeDigitCellsMap = mutableMapOf()
								for (cell in cellsToCheck) {
									val candidates = cell.primaryMarks.marksValues.filter { it in groupCombination }
									for (candidate in candidates) {
										val removeCells = removeDigitCellsMap.getOrDefault(candidate, mutableListOf())
										removeCells.add(cell)
										removeDigitCellsMap[candidate] = removeCells
									}
								}
							}
						}
					}

					// user action needed

					for ((candidate, cells) in removeDigitCellsMap.toSortedMap()) {
						nextStepActionRemoveCandidates[candidate] = cells
					}
					nextStepState = NextStepStates.STEP_FOUND
					nextStepStrategyId = if (groupTypeName=="Naked") {
						when(groupSize) {
							2 -> StrategyIds.NAKED_PAIR
							3 -> StrategyIds.NAKED_TRIPLE
							4 -> StrategyIds.NAKED_QUAD
							else -> StrategyIds.NAKED_GROUP
						}
					} else {
						when(groupSize) {
							2 -> StrategyIds.LOCKED_PAIR
							3 -> StrategyIds.LOCKED_TRIPLE
							else -> StrategyIds.LOCKED_GROUP
						}
					}
					nextStepStrategyName = if (groupTypeName=="Naked") {
						when(groupSize) {
							2,3,4 -> nextStepStrategyId.getStrategyName(context)
							else -> context.getString(R.string.hint_strategy_naked_group,"$groupSize")
						}
					} else {
						when(groupSize) {
							2,3 -> nextStepStrategyId.getStrategyName(context)
							else -> context.getString(R.string.hint_strategy_locked_group,"$groupSize")
						}
					}

					val candidates = groupCombination.joinToString(",")

					when(hintLevel) {
						HintLevels.LEVEL1 -> {
							nextStepText = nextStepStrategyName
						}
						HintLevels.LEVEL2 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_naked_group_candidates,
								"{${candidates}}")
						}
						HintLevels.LEVEL3 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_naked_group_candidates,
								"{${candidates}}")
							nextStepText += "\n" + context.resources.getQuantityString(
								R.plurals.hint_strategy_naked_group_house,((houseAdrList.length-1)),
								"(${houseAdrList})")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_naked_group_cells,
								getCellsGridAddress(cellsFound))
							cellsToHighlight[HintHighlight.REGION] = regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] = cellsFound.map { it.rowIndex to it.columnIndex }
						}
						HintLevels.LEVEL4 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_naked_group_candidates,
								"{${candidates}}")
							nextStepText += "\n" + context.resources.getQuantityString(
								R.plurals.hint_strategy_naked_group_house,(houseAdrList.length-1),
								"(${houseAdrList})")
							nextStepText += "\n" + context.getString(
								R.string.hint_strategy_naked_group_cells,
								getCellsGridAddress(cellsFound))
							nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
							cellsToHighlight[HintHighlight.REGION] = regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] = cellsFound.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.TARGET] = removeDigitCellsMap.values.flatten().map { it.rowIndex to it.columnIndex }
						}
					} // when(hintLevel)
					return true
				} // forGroupCombination@
			} // forHouse@
		} // forHouseType@
		return false
	}

}
