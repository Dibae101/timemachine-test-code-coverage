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

/** Strategy: Remote Pair
 */
class NextStepRemotePair(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForRemotePair()
	}

	/*** find next chain cell
	 *
	 * This function can be call recursively!
	 *
	 * Input:
	 * - chainCells = cells already in the chain
	 * - unusedCells = cells which can be used for the chain
	 *
	 * Return:
	 * - cells
	 * 		- if no remote pair found: empty list
	 * 		- if a remote pair found:
	 * 			- ordered list with cells in the chain
	 * 			- first and last cell are the remote pair
	 */
	private fun findNextChainCell(
		chainCells: List<Cell>,
		unusedCells: List<Cell> ): MutableList<Cell> {

		val emptyCellList = mutableListOf<Cell>()
		val cellA = chainCells.last()

		// find all cells for cellA in the same houses (row.col,box)
		val possibleCellsB = mutableListOf<Cell>()
		var cells = unusedCells.filter { it.rowIndex == cellA.rowIndex }
		if ( cells.size == 1) possibleCellsB.addAll(cells)
		cells = unusedCells.filter { it.columnIndex == cellA.columnIndex }
		if ( cells.size == 1) possibleCellsB.addAll(cells)
		cells = unusedCells.filter { it.sectorIndex == cellA.sectorIndex }
		if ( cells.size == 1) possibleCellsB.addAll(cells)
		if ( possibleCellsB.isEmpty() ) return emptyCellList

		// loop thru possible cells
		forCellB@ for ( cellB in possibleCellsB ) {
			val newChainCells = mutableListOf<Cell>()
			newChainCells.addAll(chainCells)
			newChainCells.add(cellB)
			// check remote pair for needed action
			if ( newChainCells.size in listOf(4,6,8,10,12) ) {
				val remotePairCellA = newChainCells.first()
				val remotePairCellB = newChainCells.last()
				val x = remotePairCellA.primaryMarks.marksValues[0]
				val y = remotePairCellA.primaryMarks.marksValues[1]
				val seenCells = getUnsolvedCellsSeenByAll(listOf(remotePairCellA,remotePairCellB))
				val actionCells = seenCells
					.filter { it !in newChainCells}
					.filter { it.value == 0 }
					.filter { (it.primaryMarks.hasNumber(x) || it.primaryMarks.hasNumber(y)) }
				// remote pair with action needed found
				if ( actionCells.isNotEmpty() ) return newChainCells
			}
			val newUnusedCells = unusedCells.filter { it != cellB }
			// find next cells in chain
			val foundChainCells = findNextChainCell(newChainCells,newUnusedCells)
			if ( foundChainCells.isNotEmpty() ) return foundChainCells
		}//forCellB@
		return emptyCellList
	}

	/*** Remote Pair
	 *
	 *  - two candidates => Bi-Value => BV => with candidates X and y
	 *  - cell with two remaining candidates => Bi-Value-Cell => BVC
	 *  - two BVCs in one house => Naked Pair
	 *  - chain of Naked Pairs in different houses linked via one BVC from house to house => AIC = Alternating Inference Chain
	 *  - minimum of 4 cells in three houses
	 *  - even count of cells (4,6,8)
	 *  - the start BVC and the end BVC named Remote Pair
	 *  - in cells seen by the remote pair the candidates of the Bi-Value can be removed
	 *
	 *	Remote Pair for 4 BVCs
	 *
	 *		BVC1 ==== BVC2 ==== BVC3 ==== BVC4
	 *		|         |         |         |
	 *		+ house_1 + house_2 + house_3 +
	 *		|                             |
	 *		+-------- remote pair --------+
	 *		|                             |
	 *		+--<cells seen, remove BVs >--+
	 *
	 * Message:
	 *
	 *  "Remote Pair"
	 *  " ⊞ Candidates ➠ {4,7}"
	 *  " ⊞ Chain houses ➠ (b1,r2,c4,r4,c6,r9,c8)"
	 *  " ⊞ Chain cells ➠ [ r1c1,r2c3,r2c4,r4c4,r4c6,r9c6,r9c8,r8c8 ]"
	 *  " ⊞ Remote Pair ➠ [ r1c1,r5c8 ]"
	 * 	" ✎ remove candidate {7} from [ r5c1 ]"
	 *
	 */
	private fun checkForRemotePair(): Boolean {

		// build a list of all BVCs
		val xyCellsAll = mutableListOf<Cell>()
		board.cells.forEach { row ->
			row.forEach forCells@{ cell ->
				if (cell.value > 0) return@forCells // cell has already a value
				if (cell.primaryMarks.marksValues.size != 2) return@forCells
				xyCellsAll.add(cell)
			}
		}

		// build a map of BV and cells
		val xyCellsAllMap = xyCellsAll
			.groupBy { it.primaryMarks.marksValues.toSet() }
			.toMap()
			.filter { it.value.size >= 4 }

		// loop over BV (xy)
		forXY@ for ( (xy, xyCells) in xyCellsAllMap.iterator() ) {
			forStartCell@ for ( startCell in xyCells ) {
				val unusedCells = xyCells.filter { it != startCell }
				var chainCells = mutableListOf(startCell)

				chainCells = findNextChainCell(chainCells,unusedCells)
				if ( chainCells.isEmpty() ) continue@forStartCell

				if ( chainCells.size in listOf(4,6,8,10,12) ) {
					val remotePairCellA = chainCells.first()
					val remotePairCellB = chainCells.last()
					val x = remotePairCellA.primaryMarks.marksValues[0]
					val y = remotePairCellA.primaryMarks.marksValues[1]
					val seenCellsByRemotePair = getUnsolvedCellsSeenByAll(listOf(remotePairCellA, remotePairCellB))
					val cellsToWorkOn = seenCellsByRemotePair
						.filter { it !in chainCells}
						.filter { it.value == 0 }
						.filter { (it.primaryMarks.hasNumber(x) || it.primaryMarks.hasNumber(y)) }
					if ( cellsToWorkOn.isEmpty() ) continue@forStartCell
					// remote pair with action needed found
					// get actions by candidate
					var cellsToWorkOnMap = mutableMapOf<Int, MutableList<Cell>>()
					for ( cell in cellsToWorkOn ) {
						val candidates = cell.primaryMarks.marksValues.filter { it in xy }
						for (c in candidates) {
							val cells = cellsToWorkOnMap.getOrDefault(c, mutableListOf())
							cells.add(cell)
							cellsToWorkOnMap[c] = cells
						}
					}
					cellsToWorkOnMap = cellsToWorkOnMap.toSortedMap()
					cellsToWorkOnMap.forEach { (number, cells) ->
						nextStepActionRemoveCandidates[number] = cells as MutableList
					}

					val remotePairCells = listOf(remotePairCellA,remotePairCellB)
					val remotePairCandidates = formatCellMarks(remotePairCellA)
					var regionCells = mutableListOf<Cell>()

					var chainHouses = "("
					for ( n in 0..(chainCells.size-2) ) {
						if ( chainHouses.length > 1 ) chainHouses += ","
						val cellA = chainCells[n]
						val cellB = chainCells[n+1]
						if (cellA.rowIndex == cellB.rowIndex) {
							chainHouses += cellA.rowAddress
							regionCells.addAll(cellA.row!!.cells)
						} else if (cellA.columnIndex == cellB.columnIndex) {
							chainHouses += cellA.columnAddress
							regionCells.addAll(cellA.column!!.cells)
						} else if (cellA.sectorIndex == cellB.sectorIndex) {
							chainHouses += cellA.sectorAddress
							regionCells.addAll(cellA.sector!!.cells)
						}
					}
					chainHouses += ")"

					regionCells = regionCells.toSet().toMutableList()
					nextStepState = NextStepStates.STEP_FOUND
					nextStepStrategyId = StrategyIds.REMOTE_PAIR
					nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

					when(hintLevel) {
						HintLevels.LEVEL1 -> {
							nextStepText = nextStepStrategyName
						}
						HintLevels.LEVEL2 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_candidates
								,remotePairCandidates)
						}
						HintLevels.LEVEL3 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_candidates
								,remotePairCandidates)
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_houses
								,chainHouses)
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_chain
								,getCellsGridAddress(chainCells))
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_cells
								,getCellsGridAddress(remotePairCells))
							cellsToHighlight[HintHighlight.REGION] =
								regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] =
								chainCells.map { it.rowIndex to it.columnIndex }
						}
						HintLevels.LEVEL4 -> {
							nextStepText = nextStepStrategyName
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_candidates
								,remotePairCandidates)
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_houses
								,chainHouses)
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_chain
								,getCellsGridAddress(chainCells))
							nextStepText += "\n" + context.getString(R.string.hint_strategy_remote_pair_cells
								,getCellsGridAddress(remotePairCells))
							nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
							cellsToHighlight[HintHighlight.REGION] =
								regionCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.CAUSE] =
								chainCells.map { it.rowIndex to it.columnIndex }
							cellsToHighlight[HintHighlight.TARGET] =
								cellsToWorkOn.map { it.rowIndex to it.columnIndex }
						}
					}
					return true
				}
			}//forStartCell@
		}//forXY@
		return false
	}

}

