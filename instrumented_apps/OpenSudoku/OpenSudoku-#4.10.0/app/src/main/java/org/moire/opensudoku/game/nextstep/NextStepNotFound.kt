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
import org.moire.opensudoku.game.SudokuBoard

/** NextStepNotFound
 *
 * This class is used to signal that not further step was found.
 *
 */
class NextStepNotFound(
	context: Context,
	@Suppress("unused") private val board: SudokuBoard,
	@Suppress("unused") private val hintLevel: HintLevels ): NextStep(context) {

	init {
		nextStepState = NextStepStates.STEP_NOT_FOUND
		nextStepStrategyId = StrategyIds.NO_NEXT_STEP_FOUND
		nextStepStrategyName = nextStepStrategyId.getStrategyName(context)
		nextStepText = context.getString(R.string.hint_no_step_found)

	}

	override fun search(): Boolean {
		return true
	}

}

