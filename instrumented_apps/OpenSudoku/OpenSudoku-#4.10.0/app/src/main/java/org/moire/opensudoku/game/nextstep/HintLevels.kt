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

/** enum class for the hint level to be used
 *
 *  Level 1: only strategy
 *    -> "Pointing Pair"
 *  Level 2: strategy and key values
 *    -> "Pointing Pair: {8}"
 *  Level 3: strategy, key values and cells
 *    -> "Pointing Pair: {8} -> (b2,c4) / [ r1c4,r3c4 ]"
 *  Level 4: strategy, key values, cells and actions
 *    -> "Pointing Pair: {8} -> (b2,c4) [ r1c4,r3c4 ]
 *        -> remove {8} from [ r4c4,r5c4,r6c4 ]"
 *
 */
enum class HintLevels() {
	LEVEL1, LEVEL2,	LEVEL3,	LEVEL4;

	fun getHintUsageValue(): Int {
		return when (this) {
			LEVEL1 -> 1
			LEVEL2 -> 2
			LEVEL3 -> 4
			LEVEL4 -> 8
		}
	}

}

