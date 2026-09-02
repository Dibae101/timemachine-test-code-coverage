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
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.HintHighlight
import java.util.LinkedList
import java.util.Queue

/** Strategy: XY-Chain
 * Search for a XY-Chain and check if it is a Loop.
 * Start with four cells in a chain and stop with 12 cells.
 */
class NextStepXYChain(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	override fun search(): Boolean {
		return checkForXYChain()
	}

	/** chainData
	 *  structure to keep the data of a chain
	 */
	private data class ChainData(val candidateZ: Int, val startCell: Cell) {
		private val myChainCells = mutableListOf<Cell>()
		private val myIncomingLinkCandidates = mutableListOf<Int>() // incoming link candidate
		private val myOutgoingLinkCandidates = mutableListOf<Int>() // outgoing link candidate
		var chainCount = 0
			private set

		init {
			addChainCell(candidateZ,startCell)
		}

		fun chainCells(): List<Cell>{
			return myChainCells.toList()
		}

		fun incomingLinkCandidates(): List<Int> {
			return myIncomingLinkCandidates.toList()
		}

		fun outgoingLinkCandidates(): List<Int> {
			return myOutgoingLinkCandidates.toList()
		}

		fun addChainCell(incomingLinkCandidate:Int,cell:Cell){
			myChainCells.add(cell)
			chainCount = myChainCells.size-1
			myIncomingLinkCandidates.add(incomingLinkCandidate)
			val outgoingLinkCandidate = cell.primaryMarks.marksValues.first { it != incomingLinkCandidate }
			myOutgoingLinkCandidates.add(outgoingLinkCandidate)
		}

		fun getCopy(): ChainData {
			val newChain = ChainData(this.candidateZ,this.startCell)
			for (i in 1..this.chainCount) {
					newChain.addChainCell(this.myIncomingLinkCandidates[i],this.myChainCells[i])
			}
			return newChain
		}

		fun getStartString(onlyCandidates: Boolean=false, chainIsLoop: Boolean=false): String {
			var s = ""
			s +=  if (onlyCandidates) "" else "[${myChainCells[0].gridAddress}]"
			s += "{${myIncomingLinkCandidates[0]},${myOutgoingLinkCandidates[0]}}"
			s += if (chainIsLoop) "   <-[]->" else "   []->"
			return s
		}

		fun getCellsString(chainIsLoop: Boolean=false): String {
			var s = ""
			s += if (chainIsLoop) ".." else ""
			for (n in 0..chainCount) {
				s += "[${myChainCells[n].gridAddress}]"
				//if ( n < chainCount) s += ","
			}
			s += if (chainIsLoop) ".." else ""
			return s
		}

		fun getCandidateString(chainIsLoop: Boolean=false): String {
			var s = ""
			s += if (chainIsLoop) ".." else ""
			for (n in 0..chainCount) {
				s += "{${myIncomingLinkCandidates[n]},${myOutgoingLinkCandidates[n]}}"
				//if ( n < chainCount) s += ","
			}
			s += if (chainIsLoop) ".." else ""
			return s
		}

		fun getChainString(onlyCells: Boolean=true, chainIsLoop: Boolean=false): String {
			var s = ""
			s += if (chainIsLoop) "<-> " else "|-> "
			for (n in 0..chainCount) {
				s += "[${myChainCells[n].gridAddress}]"
				if (!onlyCells)
					s += "{${myIncomingLinkCandidates[n]} <=> ${myOutgoingLinkCandidates[n]}}"
				if ( n < chainCount) s += " <-> "
			}
			s += if (chainIsLoop) " <->" else " <-|"
			return s
		}

		fun isInOneHouse(): Boolean {
			val rowCount = myChainCells.map{ it.rowIndex }.toSet().size
			val colCount = myChainCells.map{ it.columnIndex }.toSet().size
			val boxCount = myChainCells.map{ it.sectorIndex }.toSet().size
			return !(rowCount > 1 && colCount > 1 && boxCount > 1)
		}

		fun isLoop(): Boolean {
			val cellA = chainCells().first()
			val cellB = chainCells().last()
			return (cellA.rowIndex == cellB.rowIndex
				|| cellA.columnIndex == cellB.columnIndex
				|| cellA.sectorIndex == cellB.sectorIndex)
		}

		fun getActionCells(candidate:Int, pincerCells: List<Cell>): List<Cell> {
			var actionCells = getUnsolvedCellsSeenByAll(pincerCells)
			actionCells = actionCells
				.filter { it !in myChainCells }
				.filter { it.primaryMarks.hasNumber(candidate) }
			return actionCells
		}

	}

	/** checkForXYChain
	 *
	 * - chain of bi-value cells
	 * - bi-value cell has only two candidates (x,y) => strong link
	 * - cells are linked by one candidate => weak link
	 * - start cell and end cell must have one equal candidate it is the start/end-candidate z
	 *
	 *		r6c3( 6 ⇔ 4 ) ↔ r6c6( 4 ⇔ 3 ) ↔ r4c5( 3 ⇔ 4 ) ↔ r3c6( 4 ⇔ 2 ) ↔ r3c1( 2 ⇔ 6 )
	 *		=> remove 6 from r6c1
	 *
	 *		 	6 -> {6,4} <-4-> {4,3} <-3-> {3,4} <-4-> {4,2} <-2-> {2,6} -> 6
	 *
	 *		    		    z -> {z,x} ... {x,y} ... {y,z} -> z
	 *
	 *					( Z ⇔ A ) ↔ ( A ⇔ B ) ↔ ( B ⇔ C ) ↔ ( C ⇔ Z )
	 *
	 * - special case in focus: XY-Chain Loop
	 * 		- start cell and end cell see each other (in the same house)
	 * 		- chain is closed
	 *				|→ ( Z ⇔ A ) ↔ ( A ⇔ B ) ↔ ( B ⇔ C ) ↔ ( C ⇔ Z ) ←|
	 *
	 * - special cases not in our focus:
	 * 		- chain with two cells => naked pair
	 * 		- chain with three cells in a house => naked triple
	 * 		- chain with four cells in a house => naked quad
	 * 		- chain with three cells => XY-Wing
	 * 		- chain with the same candidates in all cells in more than one house => remote pair
	 *
	 * 	- the chain must have 4 or more cells
	 * 	- the cells must use be in more than one house
	 *	- the chain must use more then two candidates
	 *
	 *  - the program will not detect chains with more than 12 cells
	 *
	 *  - Action: XY-Chain
	 *		- in the cells seen by the start and end cell the start/end-candidate (common candidate)
	 *	      can be removed
	 *
	 *  - Action: XY-Chain Loop
	 *      - in a loop every cell can be the start cell
	 *		- in the cells seen by two neighbor chain cells the common candidate can be removed
	 *			- loop thru all cells in the chain
	 *
	 * Message:
	 *
	 *  "XY-Chain"
	 *  " ⊞ Start ➠ [ r1c4 ]{2,4}  []->"
	 *  " ⊞ Cells ➠ [ r1c1 ][ r1c2 ][ r8c2 ][ r8c5 ]"
	 *  " ⊞ Candidates ➠ {2,4}{4,1{1,7}{7,2}"
	 * 	" ✎ remove candidate {2} from [ r1c5,r8c4 ]"
	 *
	 *  "XY-Chain-Loop"
	 *  " ⊞ Start ➠ [ r4c3 ]{1,5}  <-[]->"
	 *  " ⊞ Cells ➠ ..[ r4c3 ][ r9c3 ][ r9c8 ][ r9c4 ][ r4c4 ].."
	 *  " ⊞ Candidates ➠ ..{1,5}{5,7}{7,1}{1,8}{8,1}.."
	 * 	" ✎ remove candidate {1} from [ r4c2,r4c7 ]"
	 * 	" ✎ remove candidate {5} from [ r2c3 ]"
	 *
	 */
	private fun checkForXYChain(): Boolean {

		// search all bi-value cells
		val biValueCells = getBoardBiValueCells(board)

		// create inial list of chains
		val chainQueue: Queue<ChainData> = LinkedList()
		for (cell in biValueCells) {
			for (candidate in cell.primaryMarks.marksValues) {
				val chain = ChainData(candidate,cell)
				chainQueue.add(chain)
			}
		}

		// loop thru queue until chain ends or 12 cells reached
		whileQueue@ while (chainQueue.isNotEmpty()) {
			val baseChain = chainQueue.remove()
			// get all possible cells for the next cell in the chain
			val possibleChainCells = getNextPossibleChainCells(baseChain)
			if (possibleChainCells.isEmpty()) continue@whileQueue
			// build new chains with the found cells
			forPossibleCell@ for (cell in possibleChainCells) {
				var chainIsLoop = false
				val newChain = baseChain.getCopy()
				newChain.addChainCell(baseChain.outgoingLinkCandidates().last(),cell)
				if (newChain.chainCount < 11) chainQueue.add(newChain)
				// check only chains with 4 and more cells
				if (newChain.chainCount < 3) continue@forPossibleCell
				// check new chain for XY-Chain and XY-Chain Loop
				// the incoming link candidate (z) from the first cell of the chain (start cell)
				// must be the same as the outgoing link candidate from the last cell of the chain
				if (newChain.incomingLinkCandidates().first() != newChain.outgoingLinkCandidates().last())
					continue@forPossibleCell
				// chain cells uses more than one house
				if (newChain.isInOneHouse())
					continue@forPossibleCell
				// get actions for chain
				val pincerCells =
					listOf(newChain.chainCells().first(), newChain.chainCells().last())
				val actionCandidate = newChain.incomingLinkCandidates().first()
				val actionCells = newChain.getActionCells(actionCandidate,pincerCells)
				if (actionCells.isNotEmpty()) {
					nextStepActionRemoveCandidates[actionCandidate] = actionCells as MutableList
				}
				// get additional actions for chain loop
				if (newChain.isLoop()) {
					chainIsLoop = true
					for (i in 0..(newChain.chainCount-1)) {
						val pincerCells =
							listOf(newChain.chainCells()[i], newChain.chainCells()[i+1])
						val actionCandidate = newChain.outgoingLinkCandidates()[i]
						val actionCells = newChain.getActionCells(actionCandidate,pincerCells)
						if (actionCells.isNotEmpty()) {
							val cells = nextStepActionRemoveCandidates.getOrDefault(actionCandidate,mutableListOf())
							cells.addAll(actionCells)
							nextStepActionRemoveCandidates[actionCandidate] = cells
						}
					}
				}
				// if no action found continue
				if (nextStepActionRemoveCandidates.isEmpty()) continue@forPossibleCell
				// action found build message and return
				buildMessageForChain(newChain,chainIsLoop)
				return true
			}
		}
		return false
	}

	/** getNextPossibleChainCells
	 * this function collects the possible cells for the last cells in the chain
	 */
	private fun getNextPossibleChainCells(chain: ChainData): List<Cell> {
		val lastChainCell = chain.chainCells().last()
		var possibleCells = mutableListOf<Cell>()
		for (houseType in HouseTypes.entries) {
			val cells = when(houseType) {
				HouseTypes.ROW -> lastChainCell.row!!.cells
				HouseTypes.COL -> lastChainCell.column!!.cells
				HouseTypes.BOX -> lastChainCell.sector!!.cells
			}
			possibleCells.addAll(cells)
		}
		possibleCells = possibleCells
			.filter { it !in chain.chainCells() }
			.filter { it.value == 0 }
			.filter { it.primaryMarks.marksValues.size == 2 }
			.filter { it.primaryMarks.hasNumber(chain.outgoingLinkCandidates().last()) }
			.toSet()
			.toMutableList()
		return possibleCells.toList()
	}

	/** buildMessageForChain()
	 */
	private fun buildMessageForChain(chain: ChainData,chainIsLoop: Boolean) {

		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = if (chainIsLoop)
			StrategyIds.XY_CHAIN_LOOP
		else
			StrategyIds.XY_CHAIN
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)

		val pincerCells = if (chainIsLoop) {
			chain.chainCells()
		} else {
			listOf(chain.chainCells().first(),chain.chainCells().last())
		}

		when (hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}

			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
			}

			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_cells,
					chain.getCellsString(chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_candidates,
					chain.getCandidateString(chainIsLoop)
				)
				cellsToHighlight[HintHighlight.REGION] =
					chain.chainCells().map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					pincerCells.map { it.rowIndex to it.columnIndex }
			}

			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_start,
					chain.getStartString(chainIsLoop=chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_cells,
					chain.getCellsString(chainIsLoop)
				)
				nextStepText += "\n" + context.getString(
					R.string.hint_strategy_xy_chain_candidates,
					chain.getCandidateString(chainIsLoop)
				)
				nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
				cellsToHighlight[HintHighlight.REGION] =
					chain.chainCells().map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] =
					pincerCells.map { it.rowIndex to it.columnIndex }
				val actionCells = nextStepActionRemoveCandidates.values.flatten().toSet().toList()
				cellsToHighlight[HintHighlight.TARGET] =
					actionCells.map { it.rowIndex to it.columnIndex }
			}
		}
	}
}
