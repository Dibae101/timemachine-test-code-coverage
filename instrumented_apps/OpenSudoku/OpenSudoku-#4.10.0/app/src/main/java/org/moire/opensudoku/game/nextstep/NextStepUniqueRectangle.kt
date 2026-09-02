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


/** Strategy: Unique Rectangle Group
 *
 * The strategies in the group "Unique Rectangle" (UR) are based on four cells.
 * These cells are located in exactly two row, two columns and two boxes.
 * All four cells have the same two candidates A and B. (pure BVC/UR-Cells)
 *
 * 			AB - AB
 * 		    |    |
 * 		    AB - AB
 *
 * This situation will lead into a puzzle with more than on solutions. (deadly pattern)
 * Due too the fact that we are dealing with puzzles with only one solution, we can use this
 * condition as a base for the strategies in this group.
 *
 * For all strategies there must be at least ONE PURE BVC of the four UR-Cells with only two
 * candidates.
 *
 * There is one exception for UR1 with an incomplete corner cell. The corner cells contains only
 * one of the candidates and in minimum one additional extra candidate. (UR1-)
 *
 * The strategy package 'Unique Rectangle Group' contains the following strategies:
 *
 *  - Unique Rectangle Type 1 / 1-
 *  - Unique Rectangle Type 2
 *  - Unique Rectangle Type 3
 *  - Unique Rectangle Type 4 / 4-
 *  - Unique Rectangle Type 5 / 5+
 *  - Unique Rectangle Type 6
 *  - Unique Rectangle Type 7
 *
 */
open class NextStepUniqueRectangle(
	private val context: Context,
	private val board: SudokuBoard,
	private val hintLevel: HintLevels ): NextStep(context) {

	private data class UR(val urCandidates: List<Int>, val urCells: List<Cell>) {

		val pureURCells: List<Cell>
		val nonPureURCells: List<Cell>

		var urTypeFound: String = ""
		var extraNextStepText: String = ""

		init {
			urTypeFound = ""
			pureURCells = urCells
				.filter { it.primaryMarks.marksValues.size == urCandidates.size }
				.filter { it.primaryMarks.marksValues.toSet() == urCandidates.toSet() }
				.toList()
				nonPureURCells = urCells.filter { it !in pureURCells }.toList()
		}
	}

	override fun search(): Boolean {
		return checkForUniqueRectangle()
	}


	/** Strategy: Unique Rectangle Type 1 | 2 | 5 | 3 | 4 | 6 | 7
	 *
	 *  - UR-Cells = list of the four corner cells of the Unique Rectangle
	 *  			  => they always arranged in
	 *  					- 2 rows
	 *  					- 2 columns
	 *  					- 2 blocks
	 *  			  => they must contain the 2 UR candidates
	 *  			  => at least one of the four UR cells must contain only the two UR candidates
	 *  - pureCells = list of cells with only the two UR candidates
	 *  - urCandidates = list with the two UR candidates
	 *  - nonPureCells = list of cells with one or more additional candidates
	 *  - additionalCandidates = list of the additional candidates in the non-pure UR cells
	 *  - AB = urCandidates
	 *  - X  = one or more additional candidates
	 *  - x  = only one additional candidate
	 *
	 */
	private fun checkForUniqueRectangle(): Boolean {

		// create a list with all Bi-Values of the board
		val allBiValues = getBoardBiValues(board)

		// check for each Bi-Value (UR-Candidates)
		forURCandidates@ for ( urCandidates in allBiValues ) {
			val urCellCombinations = generateAllURCellsCombination(urCandidates)

			// check each combination
			forCombination@ for ( urCells in urCellCombinations ) {
				if (!urCellsCombinationAreValid(urCandidates, urCells)) continue@forCombination

				val ur = UR(urCandidates, urCells)

				// check the count of additional candidates
				val urCellsExtraCandidatesMap = mutableMapOf<Cell, List<Int>>()
				val urExtraCandidateCellsMap = mutableMapOf<Int, MutableList<Cell>>()
				for (urCell in urCells) {
					val cellExtraCandidates = urCell.primaryMarks.marksValues
						.filter { it !in urCandidates }
					if (cellExtraCandidates.isNotEmpty()) {
						urCellsExtraCandidatesMap[urCell] = cellExtraCandidates
						for (extraCandidate in cellExtraCandidates) {
							val mapEntryCells = urExtraCandidateCellsMap.getOrDefault(
								extraCandidate,
								mutableListOf()
							)
							mapEntryCells.add(urCell)
							urExtraCandidateCellsMap[extraCandidate] = mapEntryCells
						}
					}
				}

				// check for unique rectangle
				// some common data are store in the variable ur

				// check for Type 1
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType1(ur)

				// check for Type 2
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType2(ur)

				// check for Type 5
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType5(ur)

				// check for Type 5+
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType5p(ur)

				// check for Type 3
				// this check should be done before check for Type 4
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType3(ur)

				// check for Type 4
				// this check should be done before check for Type 6 and 7
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType4(ur)

				// check for Type 6
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType6(ur)

				// check for Type 7
				if (ur.urTypeFound.isEmpty()) checkForUniqueRectangleType7(ur)

				// no UR with action found, go to next UR cell combination
				if (ur.urTypeFound.isEmpty()) continue@forCombination

				// UR with action found
				buildNextStepMessage(ur)
				return true
			} // forCombination@

			// check for UR1 with incomplete corner cell with only one UR-candidate
			// instead of two (UR1m)
			if (checkForUniqueRectangleType1m(urCandidates)) return true

			// check for UR4 with incomplete line cells with only one/two UR-candidates
			// instead of two (UR4m)
			if (checkForUniqueRectangleType4m(urCandidates)) return true

		} // forURCandidates@
		return false // nothing found
	}


	/** buildNextStepMessage()
	 *
	 */
	private fun buildNextStepMessage(ur: UR)
	{
		var regionCells = mutableListOf<Cell>()
		val rowIndexList = ur.urCells.map { it.rowIndex }.toSet().toList()
		for ( rowIndex in rowIndexList ) {
			val cells = ur.urCells.filter { it.rowIndex == rowIndex }
			val mm = cells.map{ it.columnIndex }.toSortedSet()
			regionCells.addAll(cells.first().row!!.cells
				.filter { it.columnIndex >= mm.min() }
				.filter { it.columnIndex <= mm.max() }
			)
		}
		val columnIndexList = ur.urCells.map { it.columnIndex }.toSet().toList()
		for ( columnIndex in columnIndexList ) {
			val cells = ur.urCells.filter { it.columnIndex == columnIndex }
			val mm = cells.map{ it.rowIndex }.toSortedSet()
			regionCells.addAll(cells.first().column!!.cells
				.filter { it.rowIndex >= mm.min() }
				.filter { it.rowIndex <= mm.max() }
			)
		}
		regionCells = regionCells.toSet().toList() as MutableList<Cell>

		val causeCells = mutableListOf<Cell>()
		causeCells.addAll(ur.urCells)

		var targetCells = mutableListOf<Cell>()
		targetCells.addAll(nextStepActionRemoveCandidates.values.flatten())
		targetCells = targetCells.toSet().toList() as MutableList<Cell>

		when (hintLevel) {
			HintLevels.LEVEL1 -> {
				nextStepText = nextStepStrategyName
			}
			HintLevels.LEVEL2 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_unique_rectangle_candidates
					,formatCellMarks(ur.urCandidates))
			}
			HintLevels.LEVEL3 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_unique_rectangle_candidates
					,formatCellMarks(ur.urCandidates))
				nextStepText += "\n" + context.getString(R.string.hint_strategy_unique_rectangle_cells
					,getCellsGridAddress(ur.urCells))
				if (ur.extraNextStepText.isNotEmpty()) nextStepText += "\n" + ur.extraNextStepText
				cellsToHighlight[HintHighlight.REGION] = regionCells
					.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] = causeCells
					.map { it.rowIndex to it.columnIndex }
			}
			HintLevels.LEVEL4 -> {
				nextStepText = nextStepStrategyName
				nextStepText += "\n" + context.getString(R.string.hint_strategy_unique_rectangle_candidates
					,formatCellMarks(ur.urCandidates))
				nextStepText += "\n" + context.getString(R.string.hint_strategy_unique_rectangle_cells
					,getCellsGridAddress(ur.urCells))
				if (ur.extraNextStepText.isNotEmpty()) nextStepText += "\n" + ur.extraNextStepText
				nextStepText += "\n" + getNextStepActionRemoveCandidatesAsText()
				cellsToHighlight[HintHighlight.REGION] = regionCells
					.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.CAUSE] = causeCells
					.map { it.rowIndex to it.columnIndex }
				cellsToHighlight[HintHighlight.TARGET] = targetCells
					.map { it.rowIndex to it.columnIndex }
			}
		}
	}


	/** checkForUniqueRectangleType1()
	 *
	 * ** Unique Rectangle Type 1 (unique corner)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 3 pure UR cells (AB)
	 * 		- 1 non-pure UR (AB+X) with additional candidates
	 *
	 * 		AB --- AB+X
	 * 		|      |
	 * 		AB --- AB
	 *
	 * 	 Action:
	 *
	 * 	 	- the UR candidates can be removed from the non-pure UR cells
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 1 (unique corner"
	 *  	" ⊞ UR-Candidates ➠ {6,9}"
	 *  	" ⊞ UR-Cells ➠ [ r4c2,r4c5,r6c2,r6c5 ]"
	 *  	" ⊞ Corner-Cells ➠ [ r4c2 ]"
	 * 		" ✎ remove candidates {6,9} from [ r4c2 ]"
	 *
	 */
	private fun checkForUniqueRectangleType1(ur: UR) {
		if (ur.nonPureURCells.size != 1) return
		// only one cell with additional candidates
		ur.urTypeFound = "1"
		ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_1_extra_text,
			"[${ur.nonPureURCells.first().gridAddress}]")
		nextStepActionRemoveCandidates[ur.urCandidates[0]] = ur.nonPureURCells as MutableList
		nextStepActionRemoveCandidates[ur.urCandidates[1]] = ur.nonPureURCells
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_1
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
	}


	/** checkForUniqueRectangleType1m()
	 *
	 * ** Unique Rectangle Type 1m (incomplete unique corner)
	 *
	 * 	Logic:
	 *
	 * 		- 3 UR cells
	 * 		- 1 Cells with one UR candidate and additional candidates
	 * 		- 3 pure UR cells (AB)
	 * 		- 1 non-pure Cell ((A|B)+X) with additional candidates
	 *
	 * 		AB --- (A|B)+X
	 * 		|      |
	 * 		AB --- AB
	 *
	 * 	 Action:
	 *
	 * 	 	- the UR candidate can be removed from the non-pure UR cell
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 1m (incomplete unique corner)"
	 *  	" ⊞ UR-Candidates ➠ {6,9}"
	 *  	" ⊞ UR-Cells ➠ [ r4c2,r4c5,r6c2,r6c5 ]"
	 *  	" ⊞ Corner-Cells ➠ [ r4c2 ]"
	 * 		" ✎ remove candidate {9} from [ r4c2 ]"
	 *
	 */
	private fun checkForUniqueRectangleType1m(urCandidates: List<Int>): Boolean {
		// get all combinations for the 3 pure UR-Cells
		val ur1mCellCombinations = generateAllUR1mCellsCombination(urCandidates)
		// loop over all combinations
		forUR1mCombination@ for ( ur1mCells in ur1mCellCombinations ) {
			// check UR conditions: 2 rows, 2 columns, 2 boxes
			val rowList = ur1mCells.map { it.rowIndex }.toSet().toList()
			if ( rowList.size != 2 ) continue@forUR1mCombination // not only in two row
			val colList = ur1mCells.map { it.columnIndex }.toSet().toList()
			if ( colList.size != 2 ) continue@forUR1mCombination // not only in two columns
			val boxList = ur1mCells.map { it.sectorIndex }.toSet().toList()
			if ( boxList.size != 2 ) continue@forUR1mCombination // not only in two boxes
			// find the missing corner cell -> non-pure UR cell
			val rowInd = if (ur1mCells.filter{ it.rowIndex == rowList[0] }.size == 1) rowList[0] else rowList[1]
			val colInd = if (ur1mCells.filter{ it.columnIndex == colList[0] }.size == 1) colList[0] else colList[1]
			val cornerCell = board.cells[rowInd][colInd]
			// check corner cell
			if (cornerCell.value != 0) continue@forUR1mCombination // should be an empty cell
			val actionCandidates = cornerCell.primaryMarks.marksValues.filter { it in urCandidates }
			if (actionCandidates.size != 1) continue@forUR1mCombination // not only one UR candidate found
			val extraCandidates = cornerCell.primaryMarks.marksValues.filter { it !in urCandidates }
			if (extraCandidates.isEmpty()) continue@forUR1mCombination // no extra candidate found
			// UR1- found
			val ur = UR(urCandidates,(ur1mCells + listOf(cornerCell)))
			ur.urTypeFound = "1-"
			ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_1m_extra_text,
				"[${ur.nonPureURCells.first().gridAddress}]")
			nextStepActionRemoveCandidates[actionCandidates.first()] = ur.nonPureURCells as MutableList
			nextStepState = NextStepStates.STEP_FOUND
			nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_1M
			nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
			buildNextStepMessage(ur)
			return true
		} // forUR1mCombination@
		// no UR1m found
		return false
	}


	/** checkForUniqueRectangleType2()
	 *
	 * ** Unique Rectangle Type 2 (unique side)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 2 non-pure UR cells, both in the same row or column
	 * 		  => 2 pure UR cells, both in the same row or column
	 *		- both non-pure UR cells should contain one and the same additional candidate
	 *
	 * 		AB+x - AB+x		AB --- AB+x
	 *      |      |		|      |
	 *  	AB --- AB		AB --- AB+x
	 *
	 * 	 Action:
	 *
	 * 	 	- from all cells seen by the two non-pure UR cells the additional candidate can be removed
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 2 (unique side)"
	 *  	" ⊞ UR-Pair ➠ {4,6}"
	 *  	" ⊞ UR-Cells ➠ [ r6c1,r6c3,r8c1,r8c3 ]"
	 *  	" ⊞ Side-Cells ➠ [ r8c1,r8c3 ]"
	 * 		" ✎ remove candidate {3} from [ r8c2,r8c4,r7c2,r7c3 ]"
	 *
	 *
	 */
	private fun checkForUniqueRectangleType2(ur: UR) {
		if (ur.nonPureURCells.size != 2) return
		// exact 2 non-pure UR cells
		if ((ur.nonPureURCells[0].rowIndex != ur.nonPureURCells[1].rowIndex)
			&& (ur.nonPureURCells[0].columnIndex != ur.nonPureURCells[1].columnIndex)) return
		// both non-pure UR cells are on the same side (row or column)
		if (ur.nonPureURCells[0].primaryMarks.marksValues.size != 3) return
		if (ur.nonPureURCells[1].primaryMarks.marksValues.size != 3) return
		// only 1 additional candidate in both non-pure UR cells
		val additionalCandidates = ur.nonPureURCells
			.flatMap { it.primaryMarks.marksValues }
			.filter { it !in ur.urCandidates }
			.toSet().sorted().toList()
		if (additionalCandidates.size != 1) return
		// in both non-pure UR cells the additional candidate is the same
		// UR Type 2 found
		// cells seen by non-pure UR cells, without non-pure UR cells
		val actionCells = getUnsolvedCellsSeenByAll(ur.nonPureURCells)
			.filter { cell -> cell.primaryMarks.hasNumber(additionalCandidates.first()) }
		if (actionCells.isEmpty()) return
		// UR Type 2 with action found
		ur.urTypeFound = "2"
		ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_2_extra_text,
			getCellsGridAddress(ur.nonPureURCells))
		nextStepActionRemoveCandidates[additionalCandidates.first()] = actionCells as MutableList
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_2
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
	}


	/** checkForUniqueRectangleType3()
	 *
	 * ** Unique Rectangle Type 3 (unique subset)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 2 pure UR cells, must be on one side (in the same row or column)
	 * 		  => 2 non-pure UR cells, must be on the other side
	 *      - the additional candidates in both non-pure UR cells must not be the same
	 *
	 * 			AB+X - AB+Y		AB+X - AB
	 *  	    |      |		|	   |
	 *  		AB --- AB		AB+Y - AB
	 *
	 *      - combine only the additional candidates of both non-pure UR cells to a virtual cell
	 *        => the virtual cell acts as one cell
	 *        - search in the row/column of the 2 non-pure UR cells for a naked subset (naked pair|
	 *          naked triple|naked quad), excluding the 2 non-pure UR cells
	 *        - depending on the count of the additional candidates start searching with pair,
	 *          triple, quad
	 *        - if the non-pure UR cells are in one box we have to search in the box too!
	 *        - special case: if all cells of the naked pair (both non-pure UR cells and the
	 *          second naked pair cell), we have a virtual locked pair. In this case we can
	 *          remove the additional candidates from the unsolved cells from the box too
	 *
	 *         - the check for type 3 should be done before check for type 4
	 *
	 * 	 Action:
	 *
	 * 	 	- the subset candidates (additional candidates) can be removed from all unsolved cells
	 * 	      seen by the two non-pure UR cells and the subset cells, except themself
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 3 (unique subset)"
	 *  	" ⊞ UR-Candidates ➠ {4,5}"
	 *  	" ⊞ UR-Cells ➠ [ r2c4,r2c6,r7c4,r7c6 ]"
	 *  	" ⊞ Subset ➠ {1,6} ➠ Cells [ r7c1 ] + [ r7c4,r7c6 ]"
	 * 		" ✎ remove candidate {1} from [ r7c2 ]"
	 * 		" ✎ remove candidate {6} from [ r7c2 ]"
	 *
	 */
	private fun checkForUniqueRectangleType3(ur: UR) {
		if (ur.pureURCells.size != 2) return
		if ((ur.pureURCells[0].rowIndex != ur.pureURCells[1].rowIndex)
			&& (ur.pureURCells[0].columnIndex != ur.pureURCells[1].columnIndex)) return
		// two pure / non-pure UR cells in a row or column
		val virtualCellCandidates = ur.nonPureURCells
			.flatMap { it.primaryMarks.marksValues }
			.filter { it !in ur.urCandidates }
			.toSet().sorted().toList()
		// additional candidates in non-pure UR cells -> virtualCellCandidates
		// loop over possible houses for subset: row or column and in addition box
		forHouseType@ for (houseType in HouseTypes.entries) {
			var houseCells = listOf<Cell>()
			when (houseType) {
				HouseTypes.ROW -> {
					if (ur.nonPureURCells[0].rowIndex == ur.nonPureURCells[1].rowIndex) {
						houseCells = ur.nonPureURCells[0].row!!.cells.toList()
					}
				}
				HouseTypes.COL -> {
					if (ur.nonPureURCells[0].columnIndex == ur.nonPureURCells[1].columnIndex) {
						houseCells = ur.nonPureURCells[0].column!!.cells.toList()
					}
				}
				HouseTypes.BOX -> {
					if (ur.nonPureURCells[0].sectorIndex == ur.nonPureURCells[1].sectorIndex) {
						houseCells = ur.nonPureURCells[0].sector!!.cells.toList()
					}
				}
			}
			if (houseCells.isEmpty()) continue@forHouseType
			val possibleSubsetCells = houseCells
				.filter { it.value == 0 }
				.filter { it !in ur.nonPureURCells }.toList()
			var possibleCandidates = possibleSubsetCells
				.flatMap { it.primaryMarks.marksValues }
				.toSet().sorted().toList()
			possibleCandidates = (possibleCandidates + virtualCellCandidates)
				.toSet().sorted().toList()
			// loop over possible subset sizes
			forSubsetSize@ for (subsetSize in 2..4) {
				if (subsetSize < virtualCellCandidates.size) continue@forSubsetSize
				if (subsetSize > possibleCandidates.size) break@forSubsetSize
				val subsetCombinations = combinations(possibleCandidates, subsetSize).toList()
				// loop over subset combinations
				forSubsetCandidates@ for (subsetCandidates in subsetCombinations) {
					if (!subsetCandidates.containsAll(virtualCellCandidates)) continue@forSubsetCandidates
					val subsetCells = mutableListOf<Cell>()
					forSubsetCells@ for (cell in possibleSubsetCells) {
						if (subsetCandidates.containsAll(cell.primaryMarks.marksValues)) {
							subsetCells.add(cell)
						}
					}
					if (subsetCells.size != (subsetSize - 1)) continue@forSubsetCandidates
					// check if a candidate can be removed from the remaining cells in the house
					// special case: virtual locked pair
					val cellsSeen = getUnsolvedCellsSeenByAll(subsetCells+ur.nonPureURCells)
						.filter { it.value == 0 }
						.filter { it !in subsetCells }
						.filter { it !in ur.nonPureURCells }.toList()
					if (cellsSeen.isEmpty()) continue@forSubsetCandidates
					val actionCells = cellsSeen
						.filter { cell -> subsetCandidates.any { it in cell.primaryMarks.marksValues } }
					if (actionCells.isEmpty()) continue@forSubsetCandidates
					// UR Type 3 with action found
					ur.urTypeFound = "3"
					for (candidate in subsetCandidates) {
						val cells = actionCells
							.filter { it.primaryMarks.hasNumber(candidate) }
						if (cells.isNotEmpty())
							nextStepActionRemoveCandidates[candidate] = cells as MutableList
					}
					ur.extraNextStepText = context.getString(
						R.string.hint_strategy_unique_rectangle_type_3_extra_text,
						formatCellMarks(subsetCandidates),
						"${getCellsGridAddress(subsetCells)} + ${getCellsGridAddress(ur.nonPureURCells)}")
					nextStepState = NextStepStates.STEP_FOUND
					nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_3
					nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
					return
				} // forSubsetCandidates
			} // forSubsetSize
		} // forHouseType
	}


	/** checkForUniqueRectangleType4()
	 *
	 * ** Unique Rectangle Type 4 (unique subset)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 2 non-pure UR cells, both in the same row or column
	 * 		  => 2 pure UR cells, both in the same row or column
	 * 		- the non-pure UR cells can have different additional candidates
	 * 	    - one of the UR candidates must be in the cells seen by the non-pure UR cells
	 * 	    - the other UR candidate must not be in the cells seen by the non-pre UR Cells
	 *
	 *  	AB --- AB		AB --- AB+X
	 *      |      |		|	   |
	 * 		AB+X - AB+Y		AB --- AB+Y
	 *
	 * 	 Action:
	 *
	 * 	 	- the UR candidate who is present as a candidate in the cells seen by the
	 * 	 	  non-pure UR cells, can be removed from the non-pure UR cells
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 4 (unique subset)"
	 *  	" ⊞ UR-Candidates ➠ {1,6}"
	 *  	" ⊞ UR-Cells ➠ [ r4c3,r4c5,r5c3,r5c5 ]"
	 *  	" ⊞ Subset-Cells ➠ [ r4c3,r5c3 ]"
	 * 		" ✎ remove candidate {6} from [ r4c3,r5c3 ]"
	 *
	 */
	private fun checkForUniqueRectangleType4(ur: UR) {
		if (ur.nonPureURCells.size != 2) return
		// exact 2 non-pure UR cells
		if ((ur.nonPureURCells[0].rowIndex != ur.nonPureURCells[1].rowIndex)
			&& (ur.nonPureURCells[0].columnIndex != ur.nonPureURCells[1].columnIndex)) return
		// both non-pure UR cells are on the same side (row or column)
		val cellsSeen = getUnsolvedCellsSeenByAll(ur.nonPureURCells)
		val urCandidatesSeen = cellsSeen
			.map { it.primaryMarks.marksValues }
			.flatten().toSet().toList()
			.filter { it in ur.urCandidates }
			.toList()
		if (urCandidatesSeen.size != 1) return
		// only one UR candidate was present in the cells seen by the non-pure UR cells
		// UR Type 4 with action found
		ur.urTypeFound = "4"
		ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_4_extra_text,
			getCellsGridAddress(ur.nonPureURCells))
		nextStepActionRemoveCandidates[urCandidatesSeen.first()] = ur.nonPureURCells as MutableList
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_4
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
	}


	/** checkForUniqueRectangleType4m()
	 *
	 * ** Unique Rectangle Type 4m (incomplete unique subset)
	 *
	 * 	Logic:
	 *
	 * 		- 3 UR cells and 1 UR cell with a missing UR candidate
	 * 		- 2 pure UR cells in a row (or column)
	 * 		- 2 non-pure UR cells in the opposite row (or column)
	 * 			- 1 non-pure UR cell
	 * 			- 1 non-pure UR cell with only one UR candidate
	 * 			- both cells must have at least one additional candidate
	 * 			- both cells can have different additional candidates
	 * 	    	- the missing UR candidate must be in the cells seen by the non-pure UR cells
	 * 	    	- the other UR candidate must not be in the cells seen by the non-pre UR Cells
	 *
	 *  	AB ------ AB		AB --- (A|B)+X
	 *      |         |		    |	   |
	 * 		(A|B)+X - AB+Y		AB --- AB+Y
	 *
	 * 	 Action:
	 *
	 * 	 	- the missing UR candidate who is present as a candidate in the cells seen by the
	 * 	 	  non-pure UR cells, can be removed from the non-pure UR cells
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 4m (incomplete unique subset)"
	 *  	" ⊞ UR-Candidates ➠ {1,6}"
	 *  	" ⊞ UR-Cells ➠ [ r4c3,r4c5,r5c3,r5c5 ]"
	 *  	" ⊞ Subset-Cells ➠ [ r4c3,r5c3 ]"
	 * 		" ✎ remove candidate {6} from [ r5c3 ]"
	 *
	 */
	private fun checkForUniqueRectangleType4m(urCandidates: List<Int>): Boolean {
		// get all combinations for the 3 UR-Cells
		val ur4mCellCombinations = generateAllUR4mCellsCombination(urCandidates)
		// loop over all combinations
		forUR4mCombination@ for ( ur4mCells in ur4mCellCombinations ) {
			// check UR conditions: 2 rows, 2 columns, 2 boxes
			val rowList = ur4mCells.map { it.rowIndex }.toSet().toList()
			if ( rowList.size != 2 ) continue@forUR4mCombination // not only in two row
			val colList = ur4mCells.map { it.columnIndex }.toSet().toList()
			if ( colList.size != 2 ) continue@forUR4mCombination // not only in two columns
			val boxList = ur4mCells.map { it.sectorIndex }.toSet().toList()
			if ( boxList.size != 2 ) continue@forUR4mCombination // not only in two boxes
			// check for two pure UR cells in a row or column
			val pureURCells = ur4mCells
				.filter { it.primaryMarks.marksValues.size == 2 }
			if ( pureURCells.size != 2 ) continue@forUR4mCombination
			if ( (pureURCells[0].rowIndex != pureURCells[1].rowIndex)
				&& (pureURCells[0].columnIndex != pureURCells[1].columnIndex) )
				continue@forUR4mCombination
			// find the missing corner cell -> incomplete non-pure UR cell
			val rowInd = if (ur4mCells.filter{ it.rowIndex == rowList[0] }.size == 1) rowList[0] else rowList[1]
			val colInd = if (ur4mCells.filter{ it.columnIndex == colList[0] }.size == 1) colList[0] else colList[1]
			val cornerCell = board.cells[rowInd][colInd]
			if (cornerCell.value != 0) continue@forUR4mCombination // should be an empty cell
			val actionCandidates = cornerCell.primaryMarks.marksValues.filter { it in urCandidates }
			if (actionCandidates.size != 1) continue@forUR4mCombination // not only one UR candidate found
			val extraCandidates = cornerCell.primaryMarks.marksValues.filter { it !in urCandidates }
			if (extraCandidates.isEmpty()) continue@forUR4mCombination // no extra candidate found
			val urCells = ur4mCells + cornerCell
			val nonPureURCells = urCells.filter { it !in pureURCells }
			// check correct use of UR candidates
			val urCandidateToKeep = actionCandidates.first()
			val urCandidateToRemove = urCandidates.first { it != urCandidateToKeep }
			val cellsSeen = getUnsolvedCellsSeenByAll(nonPureURCells)
			if (cellsSeen.any { it.primaryMarks.hasNumber(urCandidateToKeep) })
				continue@forUR4mCombination
			if (cellsSeen.none { it.primaryMarks.hasNumber(urCandidateToRemove) })
				continue@forUR4mCombination
			// UR4- found
			val ur = UR(urCandidates,urCells)
			ur.urTypeFound = "4-"
			ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_4m_extra_text,
				getCellsGridAddress(ur.nonPureURCells))
			nextStepActionRemoveCandidates[urCandidateToRemove] =
				ur.nonPureURCells.filter { it.primaryMarks.hasNumber(urCandidateToRemove) } as MutableList
			nextStepState = NextStepStates.STEP_FOUND
			nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_4M
			nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
			buildNextStepMessage(ur)
			return true
		} // forUR4mCombination@
		// no UR1m found
		return false
	}


	/** checkForUniqueRectangleType5()
	 *
	 * ** Unique Rectangle Type 5 (unique diagonal)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 2 pure UR cells
	 * 		- 2 non-pure UR cells
	 * 		- the non-pure UR cells are on the same side (row/column) they are diagonal
	 * 		- the non-pure UR cells have only 1 and same additional candidate
	 *
	 * 		AB+x - AB		AB --- AB+x
	 *      |      |		|      |
	 *      AB --- AB+x		AB+x - AB
	 *
	 * 	 Action:
	 *
	 * 	 	- from all cells seen by the two non-pure UR cells the additional
	 * 	      candidate can be removed
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 5 (unique diagonal)"
	 *  	" ⊞ UR-Candidates ➠ {1,4}"
	 *  	" ⊞ UR-Cells ➠ [ r1c4,r1c6,r9c4,r9c6 ]"
	 *  	" ⊞ Diagonal-Cells ➠ [ r1c4,r9c6 ]"
	 * 		" ✎ remove candidate {9} from [ r7c4 ]"
	 *
	 */
	private fun checkForUniqueRectangleType5(ur: UR) {
		if (ur.nonPureURCells.size != 2) return
		// exact 2 non-pure UR cells
		if ((ur.nonPureURCells[0].rowIndex == ur.nonPureURCells[1].rowIndex)
			&& (ur.nonPureURCells[0].columnIndex == ur.nonPureURCells[1].columnIndex)) return
		// both non-pure UR cells are on diagonal
		if (ur.nonPureURCells[0].primaryMarks.marksValues.size != 3) return
		if (ur.nonPureURCells[1].primaryMarks.marksValues.size != 3) return
		// only 1 additional candidate in both non-pure UR cells
		val additionalCandidates = ur.nonPureURCells
			.flatMap { it.primaryMarks.marksValues }
			.filter { it !in ur.urCandidates }
			.toSet().sorted().toList()
		if (additionalCandidates.size != 1) return
		// in both non-pure UR cells the additional candidate is the same
		// UR Type 5 found
		// cells seen by non-pure UR cells, without non-pure UR cells
		val actionCells = getUnsolvedCellsSeenByAll(ur.nonPureURCells)
			.filter { cell -> cell.primaryMarks.hasNumber(additionalCandidates.first()) }
		if (actionCells.isEmpty()) return
		// UR Type 5 with action found
		ur.urTypeFound = "5"
		ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_5_extra_text,
			getCellsGridAddress(ur.nonPureURCells))
		nextStepActionRemoveCandidates[additionalCandidates.first()] = actionCells as MutableList
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_5
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
	}


	/** checkForUniqueRectangleType5p()
	 *
	 * ** Unique Rectangle Type 5+ (unique triple)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 1 pure UR cells
	 * 		- 3 non-pure UR cells
	 * 		- the non-pure UR cells have only 1 and same additional candidate
	 *
	 * 		AB --- AB+x
	 *      |      |
	 *  	AB+x - AB+x
	 *
	 * 	 Action:
	 *
	 * 	 	- from all cells seen by the three non-pure UR cells
	 * 	      the additional candidate can be removed
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 5+ (unique triple)"
	 *  	" ⊞ UR-Candidates ➠ {1,6}"
	 *  	" ⊞ UR-Cells ➠ [ r3c7,r2c9,r5c7,r5c9 ]"
	 *  	" ⊞ Triple-Cells ➠ [ r2c7,r2c9,r5c9 ]"
	 * 		" ✎ remove candidate {3} from [ r1c9 ]"
	 *
	 */
	private fun checkForUniqueRectangleType5p(ur: UR) {
		if (ur.nonPureURCells.size != 3) return
		// exact 3 non-pure UR cells
		if (ur.nonPureURCells[0].primaryMarks.marksValues.size != 3) return
		if (ur.nonPureURCells[1].primaryMarks.marksValues.size != 3) return
		if (ur.nonPureURCells[2].primaryMarks.marksValues.size != 3) return
		// only 1 additional candidate in all non-pure UR cells
		val additionalCandidates = ur.nonPureURCells
			.flatMap { it.primaryMarks.marksValues }
			.filter { it !in ur.urCandidates }
			.toSet().sorted().toList()
		if (additionalCandidates.size != 1) return
		// in all non-pure UR cells the additional candidate is the same
		// UR Type 5+ found
		// cells seen by non-pure UR cells, without non-pure UR cells
		val actionCells = getUnsolvedCellsSeenByAll(ur.nonPureURCells)
			.filter { cell -> cell.primaryMarks.hasNumber(additionalCandidates.first()) }
		if (actionCells.isEmpty()) return
		// UR Type 5 with action found
		ur.urTypeFound = "5+"
		ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_5p_extra_text,
			getCellsGridAddress(ur.nonPureURCells))
		nextStepActionRemoveCandidates[additionalCandidates.first()] = actionCells as MutableList
		nextStepState = NextStepStates.STEP_FOUND
		nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_5P
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
	}


	/** checkForUniqueRectangleType6()
	 *
	 * ** Unique Rectangle Type 6 (unique corner)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- 2 pure UR cells
	 * 		- 2 non-pure UR cells
	 * 	    - the non-pure UR cells are arranged diagonally
	 * 	    - the non-pure UR cells can have different additional candidates
	 *
	 * 		AB --- AB+X
	 *      |      |
	 *  	AB+Y - AB
	 *
	 * 		- check that one of the UR candidates do not exists in any row or column of the UR cells
	 * 	      (pure UR cells,excepting the UR cells), this candidate is the restricted UR candidate
	 * 	      -> restrictedURCandidate -> this candidate is restricted to the UR cells
	 * 	    - check that the other UR candidate appears in a row or column of every UR cells
	 * 	      (pure UR cells,excepting the UR cells), this will be the non-restricted UR candidate
	 *        -> nonRestrictedURCandidate -> every UR cell sees in minimum one time this candidate
	 *      - run this check with both UR candidates as restricted UR candidate
	 *
	 * 	 Action:
	 * 	 	- the restrictedURCandidate can be removed from the non-pure UR Cells and
	 * 	 	  the nonRestrictedURCandidate can be removed from the pure UR Cells
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 6 (unique corner)"
	 *  	" ⊞ UR-Candidates ➠ {2,3}"
	 *  	" ⊞ UR-Cells ➠ [ r4c7,r4c9,r9c7,r9c9 ]"
	 *  	" ⊞ Restricted candidate ➠ {2}"
	 * 		" ✎ remove candidate {2} from [ r4c7,r9c9 ]"
	 * 		" ✎ remove candidate {3} from [ r4c9,r9c7 ]"
	 *
	 */
	private fun checkForUniqueRectangleType6(ur: UR) {
		if (ur.nonPureURCells.size != 2) return
		// exact 2 non-pure UR cells
		if ((ur.nonPureURCells[0].rowIndex == ur.nonPureURCells[1].rowIndex)
			&& (ur.nonPureURCells[0].columnIndex == ur.nonPureURCells[1].columnIndex)) return
		// both non-pure UR cells are on diagonal
		val seenRowColCells = (ur.pureURCells[0].row!!.cells.toList() +
			ur.pureURCells[1].row!!.cells.toList() +
			ur.pureURCells[0].column!!.cells.toList() +
			ur.pureURCells[1].column!!.cells.toList())
			.filter { it !in ur.urCells }
			.filter { it.value == 0 }
			.toSet().toList()
		val seenCellsWithA = seenRowColCells
			.filter { it.primaryMarks.hasNumber(ur.urCandidates[0]) }
		val seenCellsWithB = seenRowColCells
			.filter { it.primaryMarks.hasNumber(ur.urCandidates[1]) }
		if (seenCellsWithA.isEmpty() && seenCellsWithB.isEmpty()) return
		if (seenCellsWithA.isNotEmpty() && seenCellsWithB.isNotEmpty()) return
		val restrictedURCandidate = if (seenCellsWithA.isEmpty())
			ur.urCandidates[0] else ur.urCandidates[1]
		val nonRestrictedURCandidate = if (seenCellsWithA.isNotEmpty())
			ur.urCandidates[0] else ur.urCandidates[1]
		// found the restricted UR candidate
		for ( cell in ur.nonPureURCells ) {
			val cellsFound = (cell.row!!.cells.toList() + cell.column!!.cells.toList())
				.filter { it !in ur.urCells }
				.filter { it.value == 0 }
				.filter { it.primaryMarks.hasNumber(nonRestrictedURCandidate) }
				.toSet().toList()
			if (cellsFound.isEmpty()) return
		}
		// found cells with non-restricted candidate outside UR cells
		// UR Type 6 with action found
		ur.urTypeFound = "6"
		ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_6_extra_text,
			"{$restrictedURCandidate}")
		nextStepState = NextStepStates.STEP_FOUND
		nextStepActionRemoveCandidates[restrictedURCandidate] = ur.nonPureURCells as MutableList
		nextStepActionRemoveCandidates[nonRestrictedURCandidate] = ur.pureURCells as MutableList
		nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_6
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
	}


	/** checkForUniqueRectangleType7()
	 *
	 * ** Unique Rectangle Type 7 (hidden rectangle)
	 *
	 * 	Logic:
	 *
	 * 		- 4 UR cells
	 * 		- case A
	 * 			- 1 pure UR cells
	 * 			- 3 non-pure UR cells
	 * 			- cells in focus = pure UR cell and the non-pure UR cell in the opposite corner
	 * 		- case B
	 * 			- 2 pure UR cells
	 * 			- 2 non-pure UR cells
	 * 			- cells in focus = the 2 pure UR cells should be arranged diagonally,
	 * 				same for the 2 non-pure UR cells
	 * 			- every pair of pure and diagonal non-pure UR cells are a separate case (B1+B2)
	 *		- in both cases the additional candidates in the non-pure UR cells must not be the same
	 *
	 * 		AB --- AB+X		AB --- AB+X
	 *      |      |		|	   |
	 *  	AB+Y - AB+Z		AB+Y - AB
	 *
	 * 		- check for every combination of pure UR cell and diagonal non-pure UR cell or as a
	 * 		  special case the pure UR cell
	 * 			- the pure UR cell is the start cell (startURCell)
	 * 			- the UR cell in the opposite corner is the cell to check (checkURCell)
	 * 			- check in the unsolved cells seen by the checkURCell in their row and column
	 * 				- one UR candidate should not exists in these cells (restrictedURCandidate)
	 * 				- the other should exists in these cells (nonRestrictedURCandidate)
	 *					- if we have found one restrictedURCandidate and one nonRestrictedURCandidate
	 *					  then we have found an UR Type 7
	 *
	 * 	 Action:
	 * 	 	- the nonRestrictedURCandidate can be removed from the checkURCell
	 *
	 * 	 Messages:
	 *
	 *  	"Unique Rectangle Type 7 (hidden rectangle)"
	 *  	" ⊞ UR-Candidates ➠ {2,3}"
	 *  	" ⊞ UR-Cells ➠ [ r4c7,r4c9,r9c7,r9c9 ]"
	 * 		" ✎ remove candidate {2} from [ r4c9 ]"
	 *
	 */
	private fun checkForUniqueRectangleType7(ur: UR) {
		if (!( (ur.pureURCells.size == 1)
			   || (ur.pureURCells.size == 2
				   && ur.pureURCells[0].rowIndex != ur.pureURCells[1].rowIndex
				   && ur.pureURCells[0].columnIndex != ur.pureURCells[1].columnIndex)
		)) return
		// Case A: 1 pure and 3 non-pure UR cells
		// Case B: 2 pure and 2 non-pure UR cells, each type arranged diagonally
		// Loop thru pure UR cell -> startURCell
		forPureURCell@ for (startURCell in ur.pureURCells) {
			// get diagonal cell -> checkURCell
			val checkURCell = ur.urCells
				.filter { it.rowIndex != startURCell.rowIndex }
				.filter { it.columnIndex != startURCell.columnIndex }
				.toList().first()
			// get cells seen by checkURCell in row and column
			val seenCells = checkURCell.row!!.cells
				.plus(checkURCell.column!!.cells)
				.filter { it.value == 0 }
				.filter { it !in ur.urCells }
				.toList()
			if (seenCells.isEmpty()) continue@forPureURCell
			val cnt0 = seenCells.count { it.primaryMarks.hasNumber(ur.urCandidates[0]) }
			val cnt1 = seenCells.count { it.primaryMarks.hasNumber(ur.urCandidates[1]) }
			if (cnt0 == 0 && cnt1 == 0) continue@forPureURCell
			if (cnt0 > 0 && cnt1 > 0) continue@forPureURCell
			//val restrictedURCandidate = if (cnt0 == 0) ur.urCandidates[0] else ur.urCandidates[1]
			val nonRestrictedURCandidate = if (cnt0 > 0) ur.urCandidates[0] else ur.urCandidates[1]
			// UR Type 7 with action found
			ur.urTypeFound = "7"
			ur.extraNextStepText = context.getString(R.string.hint_strategy_unique_rectangle_type_7_extra_text_1,
				"[${startURCell.gridAddress}]", "[${checkURCell.gridAddress}]")
			ur.extraNextStepText += "\n"
			ur.extraNextStepText += context.getString(R.string.hint_strategy_unique_rectangle_type_7_extra_text_2,
				"{$nonRestrictedURCandidate}")
			nextStepActionRemoveCandidates[nonRestrictedURCandidate] = listOf(checkURCell) as MutableList
			nextStepState = NextStepStates.STEP_FOUND
			nextStepStrategyId = StrategyIds.UNIQUE_RECTANGLE_TYPE_7
			nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
			break@forPureURCell
		}
	}


	/** generateAllURCellsCombination()
	 *
	 * This function generates a list with all possible combinations of the cells in the board
	 * containing the BiValue. If the combination of the cells is a valid UR-Cells combination
	 * must be checked in a further step.
	 *
	 */
	private fun generateAllURCellsCombination(biValue: List<Int>): List<List<Cell>> {
		// create a list with of cells containing both Bi-Values
		val allPossibleURCells = mutableListOf<Cell>()
		forRow@ for ( row in board.getHousesRows()) {
			forCellInRow@ for (cell in row.cells) {
				if (cell.value != 0) continue@forCellInRow // only empty cells
				if (cell.primaryMarks.marksValues.size < 2) continue@forCellInRow
				if ( !cell.primaryMarks.marksValues.containsAll(biValue) ) continue@forCellInRow
				allPossibleURCells.add(cell)
			}
		}
		if ( allPossibleURCells.size < 4 ) return listOf()
		return combinations(allPossibleURCells,4).toList()
	}


	/** urCellsCombinationAreValid()
	 *
	 * This function checks if the urCells are valid or not.
	 */
	private fun urCellsCombinationAreValid(biValue: List<Int>, urCells: List<Cell>): Boolean {
		var cellFound = false
		for ( cell in urCells ) {
			if ( cell.primaryMarks.marksValues.size == biValue.size
				&& cell.primaryMarks.marksValues.toSet() == biValue.toSet() ) {
				cellFound = true
				break
			}
		}
		if ( !cellFound ) return false // no pure cell found
		val rowList = urCells.map { it.rowIndex }.toSet().toList()
		if ( rowList.size != 2 ) return false // not only in two row
		val colList = urCells.map { it.columnIndex }.toSet().toList()
		if ( colList.size != 2 ) return false // not only in two columns
		val boxList = urCells.map { it.sectorIndex }.toSet().toList()
		if ( boxList.size != 2 ) return false // not only in two boxes
		return true // valid
	}


	/** generateAllUR1mCellsCombination()
	 *
	 * This function generates a list with all possible combinations of the cells in the board
	 * containing only the BiValue for UR1 with an incomplete corner cell.
	 *
	 */
	private fun generateAllUR1mCellsCombination(biValue: List<Int>): List<List<Cell>> {
		// create a list of cells containing only the Bi-Values
		val allPossibleURCells = mutableListOf<Cell>()
		forRow@ for ( row in board.getHousesRows()) {
			forCellInRow@ for (cell in row.cells) {
				if (cell.value != 0) continue@forCellInRow // only empty cells
				if (cell.primaryMarks.marksValues.size != 2) continue@forCellInRow
				if ( !cell.primaryMarks.marksValues.containsAll(biValue) ) continue@forCellInRow
				allPossibleURCells.add(cell)
			}
		}
		if ( allPossibleURCells.size < 3 ) return listOf()
		return combinations(allPossibleURCells,3).toList()
	}


	/** generateAllUR4mCellsCombination()
	 *
	 * This function generates a list with all possible combinations of the cells in the board
	 * containing the BiValue for UR4 with an incomplete corner cell.
	 *
	 */
	private fun generateAllUR4mCellsCombination(biValue: List<Int>): List<List<Cell>> {
		// create a list of cells containing the Bi-Values
		val allPossibleURCells = mutableListOf<Cell>()
		forRow@ for ( row in board.getHousesRows()) {
			forCellInRow@ for (cell in row.cells) {
				if (cell.value != 0) continue@forCellInRow // only empty cells
				if (cell.primaryMarks.marksValues.size < 2) continue@forCellInRow
				if ( !cell.primaryMarks.marksValues.containsAll(biValue) ) continue@forCellInRow
				allPossibleURCells.add(cell)
			}
		}
		if ( allPossibleURCells.size < 3 ) return listOf()
		return combinations(allPossibleURCells,3).toList()
	}

}
