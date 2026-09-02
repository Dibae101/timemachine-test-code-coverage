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

@file:Suppress("KDocUnresolvedReference")

/** package: NextStep
 *
 * This package contains the code to find a possible next step based on different strategies.
 * To check that the current puzzle is valid, same pre-check functions are available.
 *
 * Object of the NextStep is to find a value for a cell or to reduce the candidates in the primary
 * marks for each cell.
 *
 * These data are used to find a next step:
 *  - value of the cells
 *    - given by the puzzle and entered by the user
 *  - the primary marks, the secondary marks are not used
 *  - solutions value for the cells computed by the application
 *
 * The output to user is on one hand a message with the main details to understand the
 * used strategy and on the other highlighted cells to see the region, the cause and the target
 * of the used strategy.
 *
 * Following syntax will be used for the inline documentation of the output towards the user:
 *   {n} ..... value or candidate
 *   {n,n} ... list of values or candidates
 *   (h) ..... house
 *   (h,h) ... list of houses
 *   [c] ..... cell                   !! KDocUnresolvedReference -> we use "[ c ]" to avoid this problem
 *   [c,c] ... list of cells
 *
 * Detailed documentation will be found in each class.
 *
 * These are the pre-checks which are implemented and should be called before
 * calling a strategy-function to get the next step.
 *
 * 	- Check cells for wrong values
 * 	- Check primary marks for missing of the solution value for the cell
 * 	- Check primary marks for obsolete values
 *
 * And these are the strategy-function to get a next step. They work independently, they should
 * be call in the following order to start with simple and end with hard strategies.
 *
 * Check for:
 *
 * 	- Full House / Last Digit
 * 	- Naked Single
 * 	- Hidden Single
 * 	- Naked Pair / Locked Pair -> Naked Group with size 2
 * 	- Hidden Pair -> Hidden Group with size 2
 * 	- Pointing Pair / Pointing Triple
 * 	- Claiming Pair / Claiming Triple
 * 	- Naked Triple / Locked Triple -> Naked Group with size 3
 * 	- Hidden Triple -> Hidden Group with size 3
 * 	- Naked Quad -> Naked Group with size 4
 * 	- Hidden Quad -> Hidden Group with size 4
 * 	- X-Wing -> Fish Basic Group with size 2
 * 	- Remote Pair
 * 	- Chute Remote Pair (Single/Double/Bonus)
 *  - Simple Coloring Type 1 and Type 2
 *  - Turbot Skyscraper
 *  - Turbot 2-String-Kite
 *  - Turbot Crane
 * 	- Empty Rectangle
 * 	- Swordfish -> Fish Basic Group with size 3
 * 	- XY-Wing
 * 	- XYZ-Wing
 * 	- X-Chain
 * 	- X-Chain-Loop
 * 	- X-Chain-One-Endpoint (or Loop-Discontinuous)
 * 	- XY-Chain
 * 	- XY-Chain-Loop
 * 	- BUG+1
 * 	- Unique Rectangle Type 1,1- | 2 | 3 | 4,4- | 5,5+ | 6 | 7
 * 	- Jellyfish -> Fish Basic Group with size 4
 * 	- WXYZ-Wing
 * 	- Starfish -> Fish Basic Group with size 5
 * 	- Whale -> Fish Basic Group with size 6
 * 	- Leviathan -> Fish Basic Group with size 7
 *
 *
 *
 *
 * And this is currently the list with strategies which are waiting for implementation:
 *
 *  - Sue-De-Coq
 *  - fishes extended
 * 	- X-Wing
 * 		- finned
 * 		- sashimi finned
 *  - Grouped X-Chains / Grouped Turbot Fishes
 *  	- Grouped Skyscraper
 *  	- Grouped 2-String Kite
 * 	- X-Chain-One-Endpoint (Loop-Discontinuous), examples are needed for testing
 * 		- weak links
 * 		- different links
 *  - rectangle elimination (includes empty rectangles)
 *  - wxyz-wings extended, pivot cell with only 3 candidates
 *  - grouped 2-String Kite
 *  - BUG (Bi-value Universal Grave)
 *  	- BUG+2
 *  	- BUG+3
 *  - BUGLite 1-4
 * 	- W-Wing (type 1-4)
 * 	- Rectangle Elimination
 * 	- Locked Cell Cycle
 * 		- type 1 + 2
 * 	- Avoidable Rectangle
 * 		- type 1 - 3
 * 	- Chains
 * 		- XY-Wing
 *  	- singles
 * 		- weak singles
 * 		- pairs
 *
 */

package org.moire.opensudoku.game.nextstep

import android.content.Context
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.SudokuBoard

/** NextStepStates
 *
 * This enum defines the possible states of the search for a next step.
 */
enum class NextStepStates() {
	NONE,
	STEP_NOT_FOUND,
	STEP_FOUND,
	PROBLEM_FOUND,
}

/**
 * Abstract class for the computing of a next step for:
 * - a classic sudoku puzzle
 * - with a size of 9x9
 * - with candidates for each unsolved cell
 * - using different strategies implemented in subclasses
 */
abstract class NextStep( private val context: Context ) {

	/** allowed digits for values and candidates
	 */
	protected val allowedDigits = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

	/** nextStepState
	 *  This variable contain a state information about the search for a next step
	 */
	var nextStepState = NextStepStates.NONE
		protected set

	/** nextStepUsedStrategy...
	 * These two variables contain the Id and the text of the strategy found for the NextStep.
	 */
	var nextStepStrategyId: StrategyIds = StrategyIds.NONE
		protected set
	var nextStepStrategyName = ""
		protected set

	/** nextStepText
	 * This variable contains the NextStep. It contains information about the strategy found.
	 * Depending on the hint level more information will be given.
	 */
	var nextStepText = ""
		protected set

	/** nextStepActionSetValues
	 * This variable contains the actions for the NextStep to set values.
	 * It contains for each value the cells, where it should be set.
	 * Is the variable empty, then there is nothing to SET.
	 */
	var nextStepActionSetValues = mutableMapOf<Int, MutableList<Cell>>()
		protected set

	/** nextStepActionRemoveCandidates
	 * This variable  the actions for the NextStep to remove candidates.
	 * It contains for each candidate (-number) the cells, where it should be removed.
	 * Is the variable empty, then there is nothing to REMOVE.
	 */
	var nextStepActionRemoveCandidates = mutableMapOf<Int, MutableList<Cell>>()
		protected set

	/** cellsToHighlight
	 *  cellsToHighlight contains for each map all cells even if they are also in another map
	 *  Example for "hidden pairs":
	 *   - the REGION-map contains all cells including the CAUSE-cells and the TARGET-cells
	 *   - the CAUSE-map contains the TARGET-cells too
	 *   - the TARGET-map contains only the TARGET-cells
	 *  The program logic to highlight the cells must take care of this.
	 */
	var cellsToHighlight = mutableMapOf<HintHighlight, List<Pair<Int, Int>>>()
		protected set

	/** search
	 * This function tries to find a next step using a strategy. There is one special case: this
	 * function should also be used to do the pre-checks.
	 *
	 * The function uses the parameter board and hint level from the class constructor. It return
	 * the following data:
	 *  - boolean : true = next step found / false = no next step found
	 *  			true = pre-check failed / false = pre-check done successfully
	 *  - string: message for the user with information of the next step or failed pre-check
	 *  - MAP(...): lists with cells to highlight
	 */
	abstract fun search(): Boolean


	/** getNextStepSignature
	 * This function return a signature for the next step.
	 * The signature can be used to checked if a new next step was found.
	 */
	fun getNextStepSignature(): String {
		var signature = ""
		// strategy
		signature += "%03d".format(nextStepState.ordinal)
		signature += " "
		// set value
		val setValues = nextStepActionSetValues.keys.sorted()
		for (setValue in setValues)
		{
			signature += getCellsGridAddress( nextStepActionSetValues[setValue]!!.toList() )
			signature += "={$setValue}"
			if (setValue != setValues.last()) signature += " "
		}
		// remove candidate
		val actionCandidates = nextStepActionRemoveCandidates.keys.sorted()
		for (actionCandidate in actionCandidates)
		{
			signature += getCellsGridAddress( nextStepActionRemoveCandidates[actionCandidate]!!.toList() )
			signature += "-{$actionCandidate}"
			if (actionCandidate != actionCandidates.last()) signature += " "
		}
		return signature
	}


	/** getNextStepActionSetValuesAsText
	 * This function convert the action list to set values to text.
	*/
	fun getNextStepActionSetValuesAsText(): String {
		var text = ""
		val setValues = nextStepActionSetValues.keys.sorted()
		for (setValue in setValues)
		{
			text += context.getString(R.string.hint_enter_value_into,
				"{$setValue}",
				getCellsGridAddress( nextStepActionSetValues[setValue]!!.toList() )
			)
			if (setValue != setValues.last()) {
				text += "\n"
			}
		}
		return text
	}

	/** getNextStepActionRemoveCandidatesAsText
	 * This function convert the action list to remove candidates to text.
	 */
	fun getNextStepActionRemoveCandidatesAsText(): String {
		var text = ""
		val actionCandidates = nextStepActionRemoveCandidates.keys.sorted()
		for (actionCandidate in actionCandidates)
		{
			var actionCells = nextStepActionRemoveCandidates[actionCandidate]!!.toList()
			actionCells = actionCells.sortedWith(compareBy(Cell::rowIndex, Cell::columnIndex))
			text += context.getString(
				R.string.hint_remove_candidate_from,
				"{$actionCandidate}",	getCellsGridAddress(actionCells)
			)
			if (actionCandidate != actionCandidates.last()) {
				text += "\n"
			}
		}
		return text
	}

	/*
	 * helper
	 */
	companion object {


		/** getBoardBiValues
		 *
		 * This function return a list of all BI-Values that the cells of board contains.
		 * - the cell is unsolved
		 * - the cell contains only to candidates
		 *
		 */
		fun getBoardBiValues(board: SudokuBoard): List<List<Int>> {
			val allBiValues = mutableListOf<List<Int>>()
			forRow@ for ( row in board.getHousesRows()) {
				forCellInRow@ for (cell in row.cells) {
					if (cell.value != 0) continue@forCellInRow
					if (cell.primaryMarks.marksValues.size != 2) continue@forCellInRow
					allBiValues.add(cell.primaryMarks.marksValues)
				}
			}
			return allBiValues.toSet().toList()
		}


		/** getBoardBiValueCells
		 *
		 * This function return a list of all BI-Value cells that the board contains.
		 * - the cell is unsolved
		 * - the cell contains only to candidates
		 *
		 */
		fun getBoardBiValueCells(board: SudokuBoard): List<Cell> {
			val allBiValueCells = mutableListOf<Cell>()
			forRow@ for ( row in board.getHousesRows()) {
				forCellInRow@ for (cell in row.cells) {
					if (cell.value != 0) continue@forCellInRow
					if (cell.primaryMarks.marksValues.size != 2) continue@forCellInRow
					allBiValueCells.add(cell)
				}
			}
			return allBiValueCells.toList()
		}


		fun getCellsGridAddress(cells: List<Cell>): String {
			val cellAdrList = ArrayList<String>()
			cells.forEach { cellAdrList.add(it.gridAddress) }
			return cellAdrList.joinToString(",", "[", "]")
		}

		fun formatCellMarks(cell: Cell): String = cell.primaryMarks.marksValues.joinToString(",", "{", "}")
		fun formatCellMarks(list: List<Int>): String = list.joinToString(",", "{", "}")

		fun getUnsolvedCellsSeenByAll(cellList: List<Cell>): List<Cell> {
			var seenCells = setOf<Cell>()
			for ((i,cell) in cellList.withIndex()) {
				val seenCellsByCell = setOf(*cell.row!!.cells, *cell.column!!.cells, *cell.sector!!.cells)
				seenCells = if (i==0) seenCellsByCell else seenCells.intersect(seenCellsByCell)
			}
			seenCells = seenCells
				.filter { cell -> cell.value == 0 } // only unsolved cells
				.filter { it !in cellList }         // only other cells
				.toSet()
			return seenCells.toList()
		}

		fun <T> combinations(pool: List<T>, length: Int): Sequence<List<T>> = sequence {

			/*** Combinations without repetitions
			 *
			 * binomial coefficient:
			 *
			 * 		  n!         ( n )
			 *    ---------   => (   )
			 *    (n-k)!*k!      ( k )
			 */

			if (pool.size < length) error("pool size (${pool.size}) must be >= length ($length)")
			if (length !in listOf(2, 3, 4, 5, 6, 7)) error("length must be 2, 3, 4, 5, 6 or 7")

			when (length) {
				2 -> {
					for (i in 0..<pool.size - (length-1))
						for (j in i+1..<pool.size)
							yield(listOf(pool[i], pool[j]))
				}

				3 -> {
					for (i in 0..<pool.size - (length-1))
						for (j in i+1..<pool.size - (length-2))
							for (k in j+1..<pool.size)
								yield(listOf(pool[i], pool[j], pool[k]))
				}

				4 -> {
					for (i in 0..<pool.size - (length-1))
						for (j in i+1..<pool.size - (length-2))
							for (k in j+1..<pool.size - (length-3))
								for (l in k+1..<pool.size)
									yield(listOf(pool[i], pool[j], pool[k], pool[l]))
				}

				5 -> {
					for (i in 0..<pool.size - (length-1))
						for (j in i+1..<pool.size - (length-2))
							for (k in j+1..<pool.size - (length-3))
								for (l in k+1..<pool.size - (length-4))
									for (m in l+1..<pool.size)
										yield(listOf(pool[i], pool[j], pool[k], pool[l], pool[m]))
				}

				6 -> {
					for (i in 0..<pool.size - (length-1))
						for (j in i+1..<pool.size - (length-2))
							for (k in j+1..<pool.size - (length-3))
								for (l in k+1..<pool.size - (length-4))
									for (m in l+1..<pool.size - (length-5))
										for (n in m+1..<pool.size)
											yield(listOf(pool[i], pool[j], pool[k], pool[l], pool[m], pool[n]))
				}

				7 -> {
					for (i in 0..<pool.size - (length-1))
						for (j in i+1..<pool.size - (length-2))
							for (k in j+1..<pool.size - (length-3))
								for (l in k+1..<pool.size - (length-4))
									for (m in l+1..<pool.size - (length-5))
										for (n in m+1..<pool.size - (length-6))
											for (o in n+1..<pool.size)
												yield(listOf(pool[i], pool[j], pool[k], pool[l], pool[m], pool[n], pool[o]))
				}
			}
		}

	}
}


