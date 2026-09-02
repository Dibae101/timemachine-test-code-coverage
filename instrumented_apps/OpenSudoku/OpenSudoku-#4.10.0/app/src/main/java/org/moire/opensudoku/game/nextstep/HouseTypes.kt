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

import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.SudokuBoard

/** enum class for the base types of a house
 *
 * a house could be:
 * 		- cells in a row
 * 		- cells in a column
 * 		- cells in a box (or sector)
 */
enum class HouseTypes() {
	ROW, COL, BOX;

	private fun houseShortcut(): String {
		return when( this ) {
			ROW -> SudokuBoard.ADDRESS_PREFIX_ROW
			COL -> SudokuBoard.ADDRESS_PREFIX_COLUMN
			BOX -> SudokuBoard.ADDRESS_PREFIX_SECTOR
		}
	}

	private fun houseNumber(cell: Cell): Int {
		val number = when( this ) {
			ROW -> cell.rowIndex + 1
			COL -> cell.columnIndex + 1
			BOX -> (cell.rowIndex / 3 * 3) + (cell.columnIndex / 3) + 1
		}
		return number
	}

	fun houseAdr(cell: Cell): String = "${houseShortcut()}${houseNumber(cell)}"
}

