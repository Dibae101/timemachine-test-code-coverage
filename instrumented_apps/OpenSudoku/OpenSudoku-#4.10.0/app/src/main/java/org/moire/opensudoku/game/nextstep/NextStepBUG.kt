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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.mutableListOf
import kotlin.collections.set


/** Strategy: BUG Group
 *
 * the strategy package 'BUG Group' contains the following strategies:
 *
 * 	- BUG
 * 		- BUG+1
 * 		- BUG+2 //TODO
 * 		- BUG+3 //TODO
 *
 */
class NextStepBUG(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForBUG()
	}

	/*** BUG
	 *
	 *  BUG has the following variants:
	 *
	 *  	BUG+1 ... one cells with three candidates
	 *  	BUG+2 ... two cells with three candidates
	 *  	BUG+3 ... three cells with three candidates
	 *
	 * Messages:
	 *
	 *  "BUG (+1): {4}"
	 *  " ⊞ Candidate ➠ {4}"
	 *  " ⊞ Cell ➠ [ r3c4 ]"
	 *  " ⊞ Cells involved ➠ [ r2c5,r3c4,r3c6,r3c8,r6c4,r7c4 ]"
	 * 	" ✎ enter value {4} into [ r3c4 ]"
	 *
	 * Logic:
	 *
	 * 	- check if there are unsolved cells with only 2 or 3 candidates
	 * 	- check that there are only 1 to 3 cells with 3 candidates, these are out target cells
	 * 	- currently only 1 cell with 3 candidates (BUG+1) were implemented!
	 * 		- check which of the tree candidates in the cells are the target candidate
	 * 		- check that without the target candidate the puzzle fits the BUG definition
	 * 			- in every house each candidate occurs only two times
	 *
  	 */
	private fun checkForBUG(): Boolean {

		// check if there are unsolved cells with only 2 or 3 candidates
		// and build a list with all cells with three candidates
		val targetCellList = mutableListOf<Cell>()
		board.cells.forEach { row ->
			row.forEach forCells@{ cell ->
				if (cell.value > 0) return@forCells // cell has already a value
				if (cell.primaryMarks.marksValues.size !in 2..3) return false
				if (cell.primaryMarks.marksValues.size == 3) targetCellList.add(cell)
			}
		}

		// number of target cell can only between 1 and 3
		// if ( targetCellList.size !in 1 .. 3 ) return false

		// currently is only BUG+1 supported
		if ( targetCellList.size != 1) return false

		// go on for BUG+1
		val targetCell = targetCellList.first()

		// search target candidate
		val (targetCandidate, causeCells) = findTargetCandidate4BUG1(targetCell)
		if ( targetCandidate == 0 ) return false

		// check for that all candidate occurs only twice, exclude the candidates in the target cell
		if ( !checkPuzzle4BUG(targetCandidate,targetCell) ) return false

		// all check done -> valid BUG+1 found
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.BUG1
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

		val regionCells = mutableListOf<Cell>() // hole board -> no display needed
		// causeCells
		val targetCells = mutableListOf(targetCell)

		nextStepActionSetValues[targetCandidate] = mutableListOf(targetCell)

		when(hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}
			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_candidate
					,"{$targetCandidate}")
			}
			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_candidate
					,"{$targetCandidate}")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_cell
					, "[${targetCell.gridAddress}]" )
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_cells_involved
					, getCellsGridAddress(causeCells.toSet().sortedBy { it.gridAddress }.toList()) )
				cellsToHighlight[HintHighlight.REGION] =
					regionCells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					causeCells.map { it.rowIndex to it.columnIndex }
			}
			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_candidate
					,"{$targetCandidate}")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_cell
					, "[${targetCell.gridAddress}]" )
				nextStepText += "\n" + context.getString(R.string.hint_strategy_bug1_cells_involved
					, getCellsGridAddress(causeCells.toSet().sortedBy { it.gridAddress }.toList()) )
				nextStepText += "\n" + getNextStepActionSetValuesAsText()
				cellsToHighlight[HintHighlight.REGION] =
					regionCells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					causeCells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.TARGET] =
					targetCells.map { it.rowIndex to it.columnIndex }
			}
		}
		return true
	}

	/** checkPuzzle4BUG
	 *  check for that all candidate occurs only twice, exclude the candidates in the target cells
	 */
	private fun checkPuzzle4BUG(targetCandidate: Int, targetCell: Cell): Boolean {

		forHouseType@ for ( houseType in HouseTypes.entries) {
			val houses = when (houseType) {
				HouseTypes.ROW -> board.getHousesRows()
				HouseTypes.COL -> board.getHousesColumns()
				HouseTypes.BOX -> board.getHousesSectors()
			}
			forHouse@ for ( house in houses ) {
				val candidateCellsMap = mutableMapOf<Int, MutableList<Cell>>()
				val cells = house.cells
					.filter { it.value == 0 }
				if ( cells.isEmpty() ) continue@forHouse
				for ( cell in cells ) {
					for (candidate in cell.primaryMarks.marksValues) {
						if ( !(candidate == targetCandidate && cell == targetCell) ) {
							val cellList =
								candidateCellsMap.getOrDefault(candidate, mutableListOf())
							cellList.add(cell)
							candidateCellsMap[candidate] = cellList
						}
					}
				}
				for ( (_,v) in candidateCellsMap) {
					if ( v.size != 2) return false
				}
			} // forHouse@
		} // forHouseType@
		return true
	}

	/** findTargetCandidate4BUG1
	 * check for each candidate of the target cell the count in each house of the target cell
	 */
	private fun findTargetCandidate4BUG1(targetCell: Cell): Pair<Int, List<Cell>> {
		val candidateHouseAdrMap = mutableMapOf<Int, MutableList<Cell>>()
		for (candidate in targetCell.primaryMarks.marksValues) {
			for (houseType in HouseTypes.entries) {
				val houseCells = when (houseType) {
					HouseTypes.ROW -> targetCell.row!!.cells
					HouseTypes.COL -> targetCell.column!!.cells
					HouseTypes.BOX -> targetCell.sector!!.cells
				}
				val cellsWithCandidate = houseCells
					.filter { it.value == 0 }
					.filter { it.primaryMarks.hasNumber( candidate ) }
				if ( cellsWithCandidate.size == 3) {
					val cells = candidateHouseAdrMap.getOrDefault(candidate, mutableListOf())
					cells.addAll(cellsWithCandidate)
					candidateHouseAdrMap[candidate] = cells
				}
			}
		}
		val result = if ( candidateHouseAdrMap.size != 1) {
			Pair(0,listOf<Cell>())
		} else {
			Pair(candidateHouseAdrMap.keys.first(), candidateHouseAdrMap.values.first())
		}
		return result
	}

}

