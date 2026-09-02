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

package org.moire.opensudoku.gui.rating

import android.content.Context
import org.moire.opensudoku.R
import org.moire.opensudoku.game.NextStepHint
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.nextstep.HintLevels
import org.moire.opensudoku.game.nextstep.NextStepStates
import org.moire.opensudoku.game.nextstep.StrategyIds
import org.moire.opensudoku.game.nextstep.StrategyLevelIds
import kotlin.collections.iterator
import kotlin.collections.set


/** TODO
 *
 * - write a callback function or similar to update the values for lifecycleScope
 * - add copy to clipboard for the rating result
 *
 */

enum class RatingResultState {
	NONE,
	PROBLEM_FOUND,
	RATING_INCOMPLETE,
	RATING_FINISHED,
}


class PuzzleRatingStepData {
	var ratingStepNBr = 0
	var nextStepText = ""
	var nextStepStrategyId = StrategyIds.NONE
	var nextStepStrategyName = ""
	var nextStepRatingValue = 0.0
}


class PuzzleRating(val game: SudokuGame, val context: Context) {

	val boardCellsCount = SudokuBoard.SUDOKU_SIZE * SudokuBoard.SUDOKU_SIZE
	private var ratingResultState = RatingResultState.NONE
	private var stepDataList = mutableListOf<PuzzleRatingStepData>()
	private val strategyTextMap = mutableMapOf<StrategyIds, String>()
	private val strategyCountMap = mutableMapOf<StrategyIds, Int>()
	private var ratingValueGame = 0.0
	private var ratingLevelGame = StrategyLevelIds.BASIC

	fun startRatingOriginal(): RatingResultState {
		ratingResultState = RatingResultState.NONE
		stepDataList.clear()
		// reset game, set board cells to given values
		game.reset()
		// fill board with primary pencil marks
		game.board.fillInPrimaryMarks()
		return startRatingCurrent()
	}

	fun startRatingCurrent(): RatingResultState {

		stepDataList.clear()
		ratingResultState = RatingResultState.NONE

		// compute for all unsolved cell the solution and check the count solutions
		val solutionCount = game.solutionCount
		if (solutionCount == 0) {
			stepDataList.add(PuzzleRatingStepData())
			stepDataList.last().ratingStepNBr = 0
			stepDataList.last().nextStepText = context.getString(R.string.puzzle_has_no_solution)
			ratingResultState = RatingResultState.PROBLEM_FOUND
			return ratingResultState
		} else if (solutionCount > 1) {
			stepDataList.add(PuzzleRatingStepData())
			stepDataList.last().ratingStepNBr = 0
			stepDataList.last().nextStepText = context.getString(R.string.puzzle_has_multiple_solutions)
			ratingResultState = RatingResultState.PROBLEM_FOUND
			return ratingResultState
		}

		// try to solve the puzzle step by step
		for (stepNbr in 1..1000) {
			stepDataList.add(PuzzleRatingStepData())
			stepDataList.last().ratingStepNBr = stepNbr
			val nextStepHint = NextStepHint(context, game.board, HintLevels.LEVEL4)
			val nextStep = nextStepHint.getNextStepHint()
			stepDataList.last().nextStepText = nextStep.nextStepText
			stepDataList.last().nextStepStrategyId = nextStep.nextStepStrategyId
			stepDataList.last().nextStepStrategyName = nextStep.nextStepStrategyName

			stepDataList.last().nextStepRatingValue = nextStep.nextStepStrategyId.getRatingValue(
				game.board.valuesCount)
			ratingValueGame += stepDataList.last().nextStepRatingValue

			if (nextStep.nextStepStrategyId.getStrategyLevel() > ratingLevelGame)
				ratingLevelGame = nextStep.nextStepStrategyId.getStrategyLevel()

			strategyTextMap[nextStep.nextStepStrategyId] = nextStep.nextStepStrategyName
			val cnt = strategyCountMap.getOrDefault(nextStep.nextStepStrategyId,0) + 1
			strategyCountMap[nextStep.nextStepStrategyId] = cnt

			when(nextStep.nextStepState) {
				NextStepStates.PROBLEM_FOUND -> {
					ratingResultState = RatingResultState.PROBLEM_FOUND
					// no rating will bewritten into the database
					return ratingResultState
				}
				NextStepStates.STEP_NOT_FOUND -> {
					ratingResultState = RatingResultState.RATING_INCOMPLETE
					// the highest rating will be written into the database (master,999)
					return ratingResultState
				}
				NextStepStates.STEP_FOUND -> {
					if (nextStep.nextStepActionSetValues.isNotEmpty()) {
						for (valueCells in nextStep.nextStepActionSetValues) {
							for (cell in valueCells.value) {
								game.setCellValue(cell,valueCells.key, false)
							}
						}
					}
					if (nextStep.nextStepActionRemoveCandidates.isNotEmpty()) {
						for (candidateCells in nextStep.nextStepActionRemoveCandidates) {
							for (cell in candidateCells.value) {
								game.removeNumberFromCellPrimaryMarks(cell,candidateCells.key, false)
							}
						}
					}
					// solved?
					val valueCount = game.board.valuesCount
					if (valueCount >= boardCellsCount) {
						// solved
						ratingResultState = RatingResultState.RATING_FINISHED
						// rating will be written into the database
						return ratingResultState
					}
					//
					// TODO: Update progressbar
					//			PuzzleRatingTask -> currentValue = valueCount
					//
				}
				else -> {
					throw IllegalStateException("Illegal NextStepState -> check coding!")
				}
			}
		}// for

		stepDataList.add(PuzzleRatingStepData())
		stepDataList.last().ratingStepNBr = 1000
		stepDataList.last().nextStepText = context.getString(R.string.rating_stop_after_n_steps
			,stepDataList.last().ratingStepNBr.toString())
		ratingResultState = RatingResultState.PROBLEM_FOUND
		return ratingResultState
	}

	private fun buildStrategyUsedTable(): String{
		var txt = ""
		for (e in strategyTextMap.toSortedMap()) {
			val c = strategyCountMap[e.key]
			txt += "${"%3d".format(c)} * ${e.value} \n"
		}
		return txt
	}

	/* for later use
	private fun buildSolvingPath(): String{
		var txt = ""
		for (stepData in stepDataList) {
			txt += "\n(${"%03d".format(stepData.ratingStepNBr)}) " + stepData.nextStepText + "\n"
		}
		return txt
	}
	*/

	fun getProblemText(): String {
		var txt = ""
		if (ratingResultState != RatingResultState.PROBLEM_FOUND)
			throw IllegalStateException("Illegal RatingResultState - Last rating did not run into a problem! Check coding ...")
		txt += stepDataList.last().nextStepText
		if(stepDataList.last().ratingStepNBr > 1) {
			txt += "\n\n"
			txt += context.getString(R.string.rating_stop_after_n_steps
				,stepDataList.last().ratingStepNBr.toString())
		}
		return txt
	}


	fun getIncompleteRatingText(): String {
		var txt = ""
		if (ratingResultState != RatingResultState.RATING_INCOMPLETE)
			throw IllegalStateException( "Illegal RatingResultState - Last rating run was not incomplete! Check coding ...")
		txt += context.getString(R.string.rating_stop_after_n_steps
			,(stepDataList.last().ratingStepNBr -1).toString()) + "\n\n"
		txt += stepDataList.last().nextStepText + "\n\n"
		txt += context.getString(R.string.rating_puzzle_used_strategies) + "\n\n"
		txt += buildStrategyUsedTable()
		return txt
	}


	fun getRatingValue(): Int {
		// the rating value count is in seconds, but we will show the rating in minutes
		return (ratingValueGame / 60).toInt()
	}

	fun getRatingLevel(): Int { return ratingLevelGame.getStrategyLevelId() }


	fun getRatingText(): String {
		var txt = ""
		if (ratingResultState != RatingResultState.RATING_FINISHED)
			throw IllegalStateException("Illegal RatingResultState - Last rating run did not finished! Check coding ...")
		txt += context.getString(R.string.rating_puzzle_level,
		ratingLevelGame.getStrategyLevelName(context)) + "\n"
		txt += context.getString(R.string.rating_puzzle_rating_value,
			"${getRatingValue()} \n\n")
		txt += context.getString(R.string.rating_puzzle_used_strategies) + "\n\n"
		txt += buildStrategyUsedTable()
		return txt
	}

}
