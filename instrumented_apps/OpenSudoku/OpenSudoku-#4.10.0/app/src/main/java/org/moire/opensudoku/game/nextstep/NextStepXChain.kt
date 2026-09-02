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
import kotlin.collections.mutableMapOf

/** Strategy: X-Chain
 * Search for a X-Chain and check if it is a Loop.
 * Start with four link in a chain and stop with 9 links.
 */
class NextStepXChain(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForXChain()
	}

	/** chainData
	 *  structure to keep the data of a chain
	 */
	private data class ChainData(val chainCandidate: Int, val chainStartCell: Cell) {
		var candidate = 0
			private set
		val cells = mutableListOf<Cell>()

		val linkCount: Int
			get() = cells.size-1

		val startCell: Cell
			get() = cells.first()

		val endCell: Cell
			get() = cells.last()

		init {
			candidate = chainCandidate
			addChainCell(chainStartCell)
		}

		fun addChainCell(cell:Cell){
			cells.add(cell)
		}

		fun getCopy(): ChainData {
			val newChain = ChainData(this.candidate,this.startCell)
			for (i in 1..this.linkCount) {
				newChain.addChainCell(this.cells[i])
			}
			return newChain
		}

		fun getStartString(onlyCandidates: Boolean=false, chainIsLoop: Boolean=false): String {
			var s = ""
			s +=  if (onlyCandidates) "" else "[${cells[0].gridAddress}]"
			s += "{${candidate}}"
			s += if (chainIsLoop) "   <-[]->" else "   []->"
			return s
		}

		fun getCellsString(chainIsLoop: Boolean=false): String {
			var s = ""
			s += if (chainIsLoop) ".." else ""
			for (n in 0..linkCount) {
				s += "[${cells[n].gridAddress}]"
				s += if (n%2==0) "=" else "-"
			}
			s = s.dropLast(1)
			s += if (chainIsLoop) ".." else ""
			return s
		}

		fun isLoop(): Boolean {
			return (startCell.rowIndex == endCell.rowIndex
				|| startCell.columnIndex == endCell.columnIndex
				|| startCell.sectorIndex == endCell.sectorIndex)
		}

		fun getActionCells(pincerCells: List<Cell>,isLoop: Boolean): List<Cell> {
			var actionCells = getUnsolvedCellsSeenByAll(pincerCells)
			actionCells = actionCells
				.filter { it.primaryMarks.hasNumber(candidate) }
			actionCells = if (isLoop) { // chain loop-> CANNIBALISM not allowed
				actionCells.filter { it !in cells }
			} else { // chain -> CANNIBALISM allowed
				actionCells.filter { it !in listOf(startCell,endCell) }
			}
			return actionCells
		}
	}


	/** checkForXChain
	 *
	 *  - X-Chain is an AIC (Alternating Inference Chain) Type I
	 *  - used for single candidate elimination
	 *  - all link cells contains the same candidate
	 *  - an X-Chain always has an ODD # of links (3,5,7,9,11,..)
	 *  - there are some special X-Chains with 3 links:
	 *    - Skyscraper
	 *    - 2-String-Kite
	 *    - Turbot-Fish
	 *  - the chain has Alternate Inferences (links) between strong and weak link
	 *  - the chain starts and ends with a strong link
	 *  - in any open-ended chain (not loop) the # of nodes will be equal to the # of links -1
	 *  - a strong link can be used as a weak link, but not the way around => surrogate link
	 *  - there is an EFFECTIVE strong link between the start cell and end cell of an X-Chain
	 *     • An X-Chain is Reversible! The Chain can „start“ at both ends
	 *     • Types:
	 *         ◦ X-Chain
	 *         	   ▪ chain with 5 links:
	 *         	   	* (N1) ⇔ (N2) ↔ (N3) ⇔ (N4) ↔ (N5) ⇔ (N6)
	 *         	   	* (+x) ⇒ (-x) → (+x) ⇒ (-x) → (+x) ⇒ (-x)
	 *         	   	* (-x) ⇐ (+x) ← (-x) ⇐ (+x) ← (-x) ⇐ (+x)
	 *         	   ▪ has 3,5,7,9,... links
	 *             ▪ has # of nodes is # of links plus 1
	 *             ▪ endpoints do not see each other
	 *             => in all cells seen by the endpoints the candidate can be removed
	 *         ◦ X-Chain Loop (Nice Loop | Continuous Loop)
	 *             ▪ chain with 5 links:
	 *              * |↔ (N1) ⇔ (N2) ↔ (N3) ⇔ (N4) ↔ (N5) ⇔ (N6) ↔|
	 *              * |→ (+x) ⇒ (-x) → (+x) ⇒ (-x) → (+x) ⇒ (-x) →|
	 *              * |← (-x) ⇐ (+x) ← (-x) ⇐ (+x) ← (-x) ⇐ (+x) ←|
	 *         	   ▪ has 3,5,7,9,... links
	 *             ▪ has # of nodes is # of links plus 1
	 *             ▪ endpoints see each other or there is a weak link between the endpoints
	 *             ▪ in a continuous loop all the weak links become strong links!
	 *             ▪ is known as Fish-Cycle or an X-Cycle
	 *             ▪ an X-Wing is the simplest form of a continuous loop
	 *             ▪ each strong link (and weak link) acts as pair of endpoints
	 *             => for each link:
	 *             		 from each cell seen by the link cells the candidate can be removed
	 *         ◦ X-Chain (Loop) with One Endpoint (or loop discontinuous)
	 *             ▪ chain with 5 links:
	 *               * (N1) ⇔ (N2) ↔ (N3) ⇔ (N4) ↔ (N5) ⇔ (N1)
	 *               * (-x) ⇒ (+x) → (-x) ⇒ (+x) → (-x) ⇒ (+x)
	 *               * (+x) ⇒ (-x) → (?x) ⇒ (?x) → (?x) ⇒ (?x)
	 *             ▪ like X-Chain Nice Loop
	 *             ▪ has 3,5,7,9,... links
	 *             ▪ has # of nodes is the same as the # of links
	 *             ▪ start and end point are the same cell
	 *             	 * joined by strong links
	 *             		=> set the candidate to the start-/end cell
	 *             * When the discontinuity has 2 links with weak inference for a single digit,
	 *               this digit can be eliminated from the discontinuity.
	 *               => not handled yet, samples are needed for testing
	 *             * When the discontinuity has 2 links with different inference for 2 different
	 *               digits, the digit providing the weak inference can be eliminated
	 *               from the discontinuity.
	 *               => not handled yet, samples are needed for testing
	 *     • CANNIBALISM!
	 *         ◦ Occurs when a Candidate that is part of the Chain can see Both Endpoints
	 *         ◦ It can be eliminated because at least ONE of the Endpoints MUST be True!
	 *
	 * Remarks:
	 * 	- special X-Chains are not sorted out!!!
	 *
	 * Message:
	 *
	 *  "X-Chain"
	 *  " ⊞ Candidate ➠ {1}"
	 *  " ⊞ Start ➠ [ r4c3 ]{1}  []->"
	 *  " ⊞ Cells ➠ [ r4c3 ]=[ r8c3 ]-[ r7c2 ]=[ r7c7 ]-[ r6c7 ]=[ r6c5 ]"
	 * 	" ✎ remove candidate {1} from [ r4c4,r4c5 ]"
	 *
	 *  "X-Chain-Loop"
	 *  " ⊞ Candidate ➠ {7}"
	 *  " ⊞ Start ➠ [ r2c1 ]{7}  <-[]->"
	 *  " ⊞ Cells ➠ ..[ r2c1 ]=[ r2c4 ]-[ r3c5 ]=[ r7c5 ]-[ r7c2 ]=[ r9c1 ].."
	 * 	" ✎ remove candidate {7} from [ r1c1,r1c4,r1c6,r3c4,r3c6,r7c6 ]"
	 *
	 *  "X-Chain-One-Endpoint"
	 *  " ⊞ Candidate ➠ {7}"
	 *  " ⊞ Start ➠ [ r2c1 ]{7}  <-[]->"
	 *  " ⊞ Cells ➠ ..[ r2c1 ]=[ r2c4 ]-[ r3c5 ]=[ r7c5 ]-[ r7c2 ]=[ r9c1 ].."
	 * 	" ✎ enter value {7} into [ r2c1 ]"
	 *
	 *
	 */
	private fun checkForXChain(): Boolean {

		// get all possible start cells
		val startCellMap = getBoardStartCellMap(board)

		// create inial list of chains
		val chainQueue: Queue<ChainData> = LinkedList()
		for (candidate in startCellMap.keys) {
			for (cell in startCellMap[candidate]!!) {
				val chain = ChainData(candidate,cell)
				chainQueue.add(chain)
			}
		}

		// loop thru queue until chain ends or 9 link reached
		whileQueue@ while (chainQueue.isNotEmpty()) {
			val currentChain = chainQueue.remove()

			// get all next possible links for last cell in chain
			val nextLinkMustBeStrong = ((currentChain.linkCount+1)%2)!=0
			val nextPossibleLinkCells = getPossibleCellsForNextLink(currentChain.candidate,
				currentChain.endCell,nextLinkMustBeStrong)

			if (nextPossibleLinkCells.isEmpty()) continue@whileQueue

			// build new chains with the found cells
			forPossibleCell@ for (cell in nextPossibleLinkCells) {
				val newLinkCount = currentChain.linkCount + 1
				// new cell should not be part of the current chain, with one exception ...
				if ((newLinkCount < 3) || ((newLinkCount % 2) == 0)) {
					if (cell in currentChain.cells) continue@forPossibleCell
				} else {
					// new link count will be 3 or higher and new link will be a strong link
					// exception for x-chain loop with start cell = end cell
					val tempCells = currentChain.cells.toMutableList()
					tempCells.removeAt(0)
					if (cell in tempCells) continue@forPossibleCell
				}
				val newChain = currentChain.getCopy()
				newChain.addChainCell(cell)
				// should we go on searching for new links
				if (cell != currentChain.startCell && newChain.linkCount < 9) {
					chainQueue.add(newChain)
				}
				// check only chains with 3 links or more
				if (newChain.linkCount < 3) continue@forPossibleCell
				// check only chains with odd links count (3,5,7,9,..)
				if ((newChain.linkCount%2)==0) continue@forPossibleCell

				// check for chain type
				// check for chain type: x-chain loop discontinuous (nice loop)
				// 	-> start and end point are the same cell
				if (newChain.startCell == newChain.endCell) {
					nextStepActionSetValues[newChain.candidate] = mutableListOf(newChain.startCell)
					buildMessageForChainOneEndpoint(newChain)
					return true
				} else {
					// it is a chain or chain loop
					val chainIsLoop = newChain.isLoop()
					// search action cells for normal chain
					var pincerCells = listOf(newChain.startCell, newChain.endCell)
					var actionCells = newChain.getActionCells(pincerCells,chainIsLoop)
					if (actionCells.isNotEmpty()) {
						nextStepActionRemoveCandidates[newChain.candidate] = actionCells as MutableList
					}
					if (chainIsLoop) {
						// search additional action cells for chain loop
						for (i in 0..<newChain.linkCount) {
							pincerCells = listOf(newChain.cells[i], newChain.cells[i+1])
							actionCells = newChain.getActionCells(pincerCells, true)
							if (actionCells.isNotEmpty()) {
								val cells = nextStepActionRemoveCandidates
									.getOrDefault(newChain.candidate,mutableListOf())
								cells.addAll(actionCells)
								nextStepActionRemoveCandidates[newChain.candidate] = cells
							}
						}
					}
					// if no action found continue
					if (nextStepActionRemoveCandidates.isEmpty()) continue@forPossibleCell
					// action found build message and return
					buildMessageForChain(newChain,chainIsLoop)
					return true
				}
				// no chain found || no chain with action found
			} // forPossibleCell@
		} // whileQueue@
		// no chain with action found
		return false
	}


	/** buildMessageForChain()
	 */
	private fun buildMessageForChain(chain: ChainData, chainIsLoop: Boolean) {

		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = if (chainIsLoop)
			StrategyIds.X_CHAIN_LOOP
		else
			StrategyIds.X_CHAIN
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

		val pincerCells = if (chainIsLoop) {
			chain.cells
		} else {
			listOf(chain.startCell,chain.endCell)
		}

		when (hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}

			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_candidate,
					"{${chain.candidate}}"
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
			}

			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_candidate,
					"{${chain.candidate}}"
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_cells,
					chain.getCellsString(chainIsLoop)
				)
				cellsToHighlight[HintHighlight.REGION] =
					chain.cells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					pincerCells.map { it.rowIndex to it.columnIndex }
			}

			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_candidate,
					"{${chain.candidate}}"
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_cells,
					chain.getCellsString(chainIsLoop)
				)
				nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
				cellsToHighlight[HintHighlight.REGION] =
					chain.cells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					pincerCells.map { it.rowIndex to it.columnIndex }
				val actionCells = nextStepActionRemoveCandidates.values.flatten().toSet().toList()
				cellsToHighlight[HintHighlight.TARGET] =
					actionCells.map { it.rowIndex to it.columnIndex }
			}
		}
	}


	/** buildMessageForChainOneEndpoint()
	 */
	private fun buildMessageForChainOneEndpoint(chain: ChainData) {

		val chainIsLoop = true
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.X_CHAIN_ONE_ENDPOINT
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

		val pincerCells = listOf(chain.startCell,chain.endCell)

		when (hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}

			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_candidate,
					"{${chain.candidate}}"
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_start,
					chain.getStartString(chainIsLoop = chainIsLoop)
				)
			}

			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_candidate,
					"{${chain.candidate}}"
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_cells,
					chain.getCellsString(chainIsLoop)
				)
				cellsToHighlight[HintHighlight.REGION] =
					chain.cells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					pincerCells.map { it.rowIndex to it.columnIndex }
			}

			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_candidate,
					"{${chain.candidate}}"
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_x_chain_cells,
					chain.getCellsString(chainIsLoop)
				)
				nextStepText += "\n" + getNextStepActionSetValuesAsText()
				cellsToHighlight[HintHighlight.REGION] =
					chain.cells.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					pincerCells.map { it.rowIndex to it.columnIndex }
				val actionCells = nextStepActionSetValues.values.flatten().toSet().toList()
				cellsToHighlight[HintHighlight.TARGET] =
					actionCells.map { it.rowIndex to it.columnIndex }
			}
		}
	}


	/** getPossibleCellsForNextLink
	*/
	private fun getPossibleCellsForNextLink(candidate: Int,
											startCell: Cell,
											strongLinkNeeded: Boolean): List<Cell> {
		var possibleCells = mutableListOf<Cell>()
		for (houseType in HouseTypes.entries) {
			var cells = when(houseType) {
				HouseTypes.ROW -> startCell.row!!.cells.toList()
				HouseTypes.COL -> startCell.column!!.cells.toList()
				HouseTypes.BOX -> startCell.sector!!.cells.toList()
			}
			cells = cells
				.filter { it.value == 0 }
				.filter { it.primaryMarks.hasNumber(candidate) }
				.filter { it != startCell }
			if (strongLinkNeeded) {
				if (cells.size == 1) possibleCells.add(cells.first())
			} else {
				possibleCells.addAll(cells)
			}
		}
		possibleCells = possibleCells
			.toSet()
			.toMutableList()
		return possibleCells.toList()
	}


	/** getBoardStartCellMap
	 *
	 * This function returns for each candidate (1..9) a list of cells with this candidate.
	 * - the cell must be unsolved
	 * - the cell must contain the candidate
	 * - the list of cells for one candidate must have more than 4 cells
	 * 		- 4 cells for the chain
	 * 		- 1 cell for the action
	 *
	 */
	fun getBoardStartCellMap(board: SudokuBoard): Map<Int, List<Cell>> {
		val startCellsMap = mutableMapOf<Int, List<Cell>>()
		val unsolvedCells = mutableListOf<Cell>()
		forRow@ for ( row in board.getHousesRows()) {
			forCellInRow@ for (cell in row.cells) {
				if (cell.value != 0) continue@forCellInRow
				unsolvedCells.add(cell)
			}
		}
		forCandidate@ for (candidate in allowedDigits) {
			val cells = unsolvedCells
				.filter { it.primaryMarks.hasNumber(candidate) }
				.toList()
			if (cells.size>4) startCellsMap[candidate] = cells
		}
		return startCellsMap
	}

}
