/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2023 by original authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.game.nextstep

import android.content.Context
import org.moire.opensudoku.game.SudokuBoard

/** Strategy: Leviathan
 *
 *  The strategy "Leviathan" is implemented in the package "Fish Basic Group".
 *  It is a "Fish Basic Group" with size 7.
 */
class NextStepFish7nLeviathan(
	context: Context,
	board: SudokuBoard,
	hintLevel: HintLevels ): NextStepFishBasicGroup( context, board, hintLevel )
{

	override fun search(): Boolean {
		return checkForFishBasicGroup(7)
	}

}
