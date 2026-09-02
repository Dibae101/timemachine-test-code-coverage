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
import java.util.LinkedList
import java.util.Queue
import kotlin.collections.mutableListOf

/** Strategy: Simple Coloring
 *
 * Currently only Type 1 and Type 2 are handled.
 *
 */
class NextStepSimpleColoring(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForSimpleColoringType1Type2()
	}

	/*** Simple Coloring Type 1 and Type 2
	 *
	 * This strategy works with:
	 * 	- a single candidate
	 * 	- a network (tree) of strong links with this candidate
	 * 	- each cell in a level (branch) belongs to the group A or B
	 * 	- in each cell not part of the network, but seeing a cell of
	 * 	  group A and a cell of group B the candidate can be removed
	 *  - if one cell in group A has a value with the candidate, all other
	 *  	cells in this group will get also the candidate
	 *    and alle cells in group B will get not the candidate
	 *  - the same if group A and B changes
	 *
	 * * Type 2
	 *   if in a group two cells see each other, this group is FALSE and all candidates
	 *   can be removed. Instead of removing candidates, set all candidate of the other
	 *   group. This will have the same result.
	 *
	 * * Type 1
	 *   if a cell with the candidate, witch is not part of the network, sees a cell of group A
	 *   and a cell of group B. the candidate in this cell can be removed.
	 *
	 *
	 * * Example board with:
	 * 	 	- show only content for cells with the candidate
	 * 	 	- candidate = 2
	 * 	 	- start cell = r1c1	(first member in group A)
	 * 	 	- group A (=0)
	 * 	 	- group B (=1)
	 * 	 	- X .. candidate in this cells can be removed, because the cell sees one cell A
	 * 	 			and one cell B
	 *
	 * 	 		A  .  2  2  .  .  .  .  .
	 * 	 		.  .  .  .  .  .  .  .  .
	 * 	 		.  B  .  .  .  A  .  .  .
	 * 	 		.  .  .  .  .  .  .  .  .
	 * 	 		.  .  .  .  2  2  2  .  .
	 * 	 		.  .  .  .  2  X  2  .  B
	 * 	 		.  2  .  2  2  .  .  .  .
	 * 	 		B  .  .  .  .  .  .  .  A
	 * 	 		.  .  .  A  .  .  B  .  .
	 *
	 *
	 * Message:
	 *
	 *  "Simple Coloring Type 1"
	 *  " ⊞ Candidate ➠ {2}"
	 *  " ⊞ Start cell ➠ [ r1c1 ]"
	 *  " ⊞ Cell group A ➠ [ r1c1,r8c9,r7c2,r3c6,r9c4 ]"
	 *  " ⊞ Cell group B ➠ [ r8c1,r3c2,r6c9,r9c7 ]"
	 * 	" ✎ remove candidate {2} from [ r6c6 ]"
	 *
	 *  "Simple Coloring Type 2"
	 *  " ⊞ Candidate ➠ {2}"
	 *  " ⊞ Start cell ➠ [ r1c1 ]"
	 *  " ⊞ Cell group A ➠ [ r1c1,r8c9,r7c2,r3c6,r9c4 ]"
	 *  " ⊞ Cell group B ➠ [ r8c1,r3c2,r6c9,r9c7 ]"
	 *  " ⊞ Cell group A is FALSE"
	 * 	" ✎ set value {2} to [ r8c1,r3c2,r6c9,r9c7 ]"
	 *
	 */
	private fun checkForSimpleColoringType1Type2(): Boolean {

		// loop thru all allowed digits
		forCandidate@ for (candidate in allowedDigits) {

			// create list of all cells with this candidate
			val cellsWithCandidate = getCellListWithCandidate(candidate)
			if (cellsWithCandidate.size < 4) continue@forCandidate // minimum of cells needed

			// loop thru all start cells to find a solution
			forStartCell@ for ( startCell in cellsWithCandidate) {

				// setup network
				val network = buildNetwork(Network(candidate,startCell))
				if (network.networkCells.size < 3) continue@forStartCell

				// check for Type 2 set actions
				val falseGroups = checkType2ForSetActions(network)
				if (falseGroups.isNotEmpty()) {
					if (falseGroups.size != 1) continue@forStartCell
					// a puzzle with 2 false groups is invalid
					// Simple Coloring Type 2 with 1 false group found
					buildMessageForType2(network, falseGroups)
					return true
				}

				// check for Type 1 remove actions
				val removeActionCells = checkForType1RemoveActions(network,cellsWithCandidate)
				if (removeActionCells.isNotEmpty()) {
					// Simple Coloring Type 1 with action found
					buildMessageForType1(network, removeActionCells)
					return true
				}

			} // forStartCell@
		} // forCandidate@
		return false // no "Simple Coloring ..." found
	}


	/** checkType2ForSetActions()
	 *
	 * - check if a cell in a group see another cell in the group
	 * - check all cells of both groups, because we handle only the case
	 *   that only one group contains cells seeing other cells in the group
	 */
	private fun checkType2ForSetActions(network: Network): List<Int> {
		val falseGroups = mutableListOf<Int>()
		forGroup@ for (groupIndex in 0..1) {
			val group = network.groupCells[groupIndex]!!
			val groupX = group.toMutableList()
			forCell1@ for (cell1 in group) {
				groupX.remove(cell1)
				forCell2@ for (cell2 in groupX) {
					//if (cell1 == cell2) continue@forCell
					if (cell1.rowIndex == cell2.rowIndex
						|| cell1.columnIndex == cell2.columnIndex
						|| cell1.sectorIndex == cell2.sectorIndex) {
						// two cells of a group see each other
						falseGroups.add(groupIndex)
					}
				}
			}
		}
		return falseGroups
	}


	/** buildMessageForType1()
	 *
	 */
	private fun buildMessageForType1(network: Network,actionCells: List<Cell>) {
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.SIMPLE_COLORING_TYPE_1
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
		nextStepActionRemoveCandidates[network.candidate] = actionCells as MutableList
		when(hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}
			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_candidate
					,"{${network.candidate}}")
			}
			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_candidate
					,"{${network.candidate}}")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_startcell
					,"[${network.startCell.gridAddress}]")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_cellgroup_a
					,getCellsGridAddress(network.groupCells[0]!!))
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_cellgroup_b
					,getCellsGridAddress(network.groupCells[1]!!))
				cellsToHighlight[HintHighlight.REGION] =
					network.groupCells[0]!!.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					network.groupCells[1]!!.map { it.rowIndex to it.columnIndex }
			}
			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_candidate
					,"{${network.candidate}}")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_startcell
					,"[${network.startCell.gridAddress}]")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_cellgroup_a
					,getCellsGridAddress(network.groupCells[0]!!))
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_1_cellgroup_b
					,getCellsGridAddress(network.groupCells[1]!!))
				nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
				cellsToHighlight[HintHighlight.REGION] =
					network.groupCells[0]!!.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					network.groupCells[1]!!.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.TARGET] =
					actionCells.map { it.rowIndex to it.columnIndex }
			}
		}
	}


	/** buildMessageForType2()
	 *
	 */
	private fun buildMessageForType2(network: Network,falseGroups: List<Int>) {

		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.SIMPLE_COLORING_TYPE_2
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

		var setActionCells = mutableListOf<Cell>()
		if (falseGroups.size == 1) {
			val g = if (falseGroups.first()==0) 1 else 0
			setActionCells = network.groupCells[g]!!
			nextStepActionSetValues[network.candidate] = setActionCells
		}

		val falseText = if (falseGroups.first()==0)
			context.getString(R.string.hint_strategy_simple_coloring_type_2_cellgroup_a_false)
		else
			context.getString(R.string.hint_strategy_simple_coloring_type_2_cellgroup_b_false)

		when(hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}
			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_candidate
					,"{${network.candidate}}")
			}
			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_candidate
					,"{${network.candidate}}")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_startcell
					,"[${network.startCell.gridAddress}]")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_cellgroup_a
					,getCellsGridAddress(network.groupCells[0]!!))
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_cellgroup_b
					,getCellsGridAddress(network.groupCells[1]!!))
				nextStepText += "\n$falseText"
				cellsToHighlight[HintHighlight.REGION] =
					network.groupCells[0]!!.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					network.groupCells[1]!!.map { it.rowIndex to it.columnIndex }
			}
			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_candidate
					,"{${network.candidate}}")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_startcell
					,"[${network.startCell.gridAddress}]")
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_cellgroup_a
					,getCellsGridAddress(network.groupCells[0]!!))
				nextStepText += "\n" + context.getString(R.string.hint_strategy_simple_coloring_type_2_cellgroup_b
					,getCellsGridAddress(network.groupCells[1]!!))
				nextStepText += "\n$falseText"
				nextStepText += "\n" + getNextStepActionSetValuesAsText()
				cellsToHighlight[HintHighlight.REGION] =
					network.groupCells[0]!!.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					network.groupCells[1]!!.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.TARGET] =
					setActionCells.map { it.rowIndex to it.columnIndex }
			}
		}
	}


	/** Network
	 *
	 *  structure to keep the data of the network
	 *
	 */
	private data class Network(val candidate: Int, val startCell: Cell) {
		val networkCells = mutableListOf<Cell>()
		val groupCells = mutableMapOf<Int,MutableList<Cell>>()

		init {
			groupCells[0] = mutableListOf<Cell>()
			groupCells[1] = mutableListOf<Cell>()
		}
	}


	/** buildNetwork()
	 *
	 * - build the network first node is the start cell
	 * - the child of a node belongs to the other group
	 * - there must be a string link between the node and the child
	 *
	 */
	private fun buildNetwork(network: Network): Network {
		val nodeQueue: Queue<Cell> = LinkedList()
		network.networkCells.add(network.startCell)
		network.groupCells[0]!!.add(network.startCell)
		nodeQueue.add(network.startCell)
		while (nodeQueue.isNotEmpty()) {
			val cell = nodeQueue.remove()
			val group = if ( cell in network.groupCells[0]!!) network.groupCells[1] else network.groupCells[0]
			for (houseCells in listOf(cell.row!!.cells,cell.column!!.cells,cell.sector!!.cells)) {
				val cells = houseCells
					.filter { it != cell }
					.filter { it.value == 0 }
					.filter { it.primaryMarks.hasNumber(network.candidate) }
				if (cells.size == 1) {
					if (cells.first() !in network.networkCells) {
						network.networkCells.add(cells.first())
						group!!.add(cells.first())
						nodeQueue.add(cells.first())
					}
				}
			}
		}
		return network
	}


	/** checkForType1RemoveActions()
	 *
	 * check if a cell with this candidate, which is not part of the network, is seen
	 * by a cell of both groups in the network
	 *
	 */
	private fun checkForType1RemoveActions(network: Network, cellsWithCandidate: List<Cell>): MutableList<Cell> {
		val actionCells = mutableListOf<Cell>()
		val possibleActionCells = cellsWithCandidate.filter { it !in network.networkCells }
		if (possibleActionCells.isNotEmpty()) {
			for (actionCell in possibleActionCells) {
				val cellsA = network.groupCells[0]!!
					.filter {
						it.rowIndex == actionCell.rowIndex
							|| it.columnIndex == actionCell.columnIndex
							|| it.sectorIndex == actionCell.sectorIndex
					}
				if (cellsA.isNotEmpty()) {
					val cellsB = network.groupCells[1]!!
						.filter {
							it.rowIndex == actionCell.rowIndex
								|| it.columnIndex == actionCell.columnIndex
								|| it.sectorIndex == actionCell.sectorIndex
						}
					if (cellsB.isNotEmpty()) {
						actionCells.add(actionCell)
					}
				}
			}
		}
		return actionCells
	}


	/** getCellListWithCandidate()
	 *
	 * create list of all cells with this candidate
	 *
	 */
	private fun getCellListWithCandidate(candidate: Int): MutableList<Cell> {
		// create list of all cells with this candidate
		val cellsWithCandidate = mutableListOf<Cell>()
		board.cells.forEach { row ->
			row.forEach forCells@{ cell ->
				if (cell.value > 0) return@forCells // cell has already a value
				if (!cell.primaryMarks.hasNumber(candidate)) return@forCells
				cellsWithCandidate.add(cell)
			}
		}
		return cellsWithCandidate
	}

}

