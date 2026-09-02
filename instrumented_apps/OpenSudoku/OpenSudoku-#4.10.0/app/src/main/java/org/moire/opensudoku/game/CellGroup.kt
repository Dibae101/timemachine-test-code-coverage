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
package org.moire.opensudoku.game

/**
 * Represents group of cells which must each contain unique number.
 * Typical examples of instances are Sudoku row, column or sector (3x3 group of cells).
 */
class CellGroup {
	val cells = Array(SudokuBoard.SUDOKU_SIZE) { Cell() }
	private var position = 0
	fun addCell(cell: Cell) {
		cells[position] = cell
		position++
	}

	/**
	 * Validates numbers in given Sudoku group - numbers must be unique. Cells with invalid numbers are marked (see [Cell.isValid]).
	 * Method expects that cell's invalid properties has been set to false ([SudokuBoard.validateAllCells] does this).
	 *
	 * @return True if validation is successful.
	 */
	fun validate(): Boolean {
		var isAllValid = true
		val alreadyFoundValuesToCellMap = HashMap<Int, Cell>()
		for (cell in cells) {
			val cellValue = cell.value
			if (cellValue == 0) continue
			if (alreadyFoundValuesToCellMap.containsKey(cellValue)) {
				cell.isValid = false
				alreadyFoundValuesToCellMap.getValue(cellValue).isValid = false
				isAllValid = false
			} else {
				alreadyFoundValuesToCellMap[cellValue] = cell
			}
		}
		return isAllValid
	}

	operator fun contains(value: Int): Boolean = cells.any { it.value == value }
}
