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

import java.util.StringTokenizer

enum class HintHighlight {
	// possible level of highLighting of hint for a cell
	// the program logic takes care that the highest level is visible for the user
	NONE,
	REGION, // HighLight level 1
	CAUSE,  // HighLight level 2
	TARGET, // HighLight level 3
}

/**
 * Sudoku cell. Every cell has a value, set of marks attached to it and a basic state (whether it is editable and valid).
 */
class Cell private constructor(value: Int, primaryMark: CellMarks, secondaryMark: CellMarks) {
	// final solution of this cell
	var solution: Int = 0

	var hintHighlight: HintHighlight = HintHighlight.NONE

	/**
	 * Cell's row index within [SudokuBoard].
	 */
	var rowIndex = -1
		private set

	/**
	 * Cell's column index within [SudokuBoard].
	 */
	var columnIndex = -1
		private set

	/**
	 * Cell's sector (box) index within [SudokuBoard].
	 */
	var sectorIndex = -1
		private set

	/**
	 * Sector containing this cell. Sector is 3x3 group of cells.
	 */
	var sector: CellGroup? = null // sector containing this cell
		private set

	/**
	 * Returns row containing this cell.
	 */
	var row: CellGroup? = null // row containing this cell
		private set

	/**
	 * Returns column containing this cell.
	 */
	var column: CellGroup? = null // column containing this cell
		private set

	/**
	 * Returns the address of the cell in a readable format "rNcN"
	 */
	var gridAddress: String = ""
		private set

	/**
	 * Returns the address of the cells row in a readable format "rN"
	 */
	var rowAddress: String = ""
		private set

	/**
	 * Returns the address of the cells column in a readable format "cN"
	 */
	var columnAddress: String = ""
		private set

	/**
	 * Returns the address of the cells sector in a readable format "bN"
	 */
	var sectorAddress: String = ""
		private set
	/**
	 * Cell's value. Value can be 1-9 or 0 if cell is empty.
	 */
	var value: Int = value
		set(value) {
			if (field == value) return
			@Suppress("HardCodedStringLiteral")
			require(value in 0..9) { "Value must be between 0-9." }
			field = value
			onChange()
		}

	/**
	 * Custom background color index (0-9, 0 means no color).
	 */
	var color: Int = 0
		set(value) {
			if (field == value) return
			@Suppress("HardCodedStringLiteral")
			require(value in 0..9) { "Color index must be between 0-9." }
			field = value
			onChange()
		}

	/**
	 * Primary marks attached to the cell.
	 */
	var primaryMarks: CellMarks = primaryMark
		set(newValue) {
			if (field == newValue) return
			field = newValue
			onChange()
		}

	/**
	 * Secondary marks attached to the cell
	 */
	var secondaryMarks: CellMarks = secondaryMark
		set(newValue) {
			if (field == newValue) return
			field = newValue
			onChange()
		}

	/**
	 * True if cell can be edited.
	 */
	var isEditable: Boolean = true

	/**
	 * Returns true, if cell contains valid value according to Sudoku rules.
	 */
	var isValid: Boolean = true

	// true if current value matches final solution (if exists)
	val isCorrect: Boolean
		get() = solution == 0 || value == solution

	private val cellCollectionLock = Any()

	// if cell is included in collection, here are some additional information
	// about collection and cell's position in it
	private var cellCollection: SudokuBoard? = null

	/**
	 * Creates empty editable cell.
	 */
	constructor() : this(0, CellMarks(), CellMarks())

	init {
		@Suppress("HardCodedStringLiteral")
		require(value in 0..9) { "Value must be between 0-9." }
	}

	/**
	 * Called when `Cell` is added to [SudokuBoard].
	 *
	 * @param rowIndex Cell's row index within collection.
	 * @param columnIndex Cell's column index within collection.
	 * @param sector   Reference to sector group in which cell is included.
	 * @param row      Reference to row group in which cell is included.
	 * @param column   Reference to column group in which cell is included.
	 */
	fun initCollection(cellCollection: SudokuBoard, rowIndex: Int, columnIndex: Int, sector: CellGroup, row: CellGroup, column: CellGroup) {
		synchronized(cellCollectionLock) { this.cellCollection = cellCollection }
		this.rowIndex = rowIndex
		this.columnIndex = columnIndex
		sectorIndex = ((this.rowIndex / 3 * 3) + (this.columnIndex / 3))
		this.sector = sector
		this.row = row
		this.column = column
		sector.addCell(this)
		row.addCell(this)
		column.addCell(this)
		// fill cell addresses for row, column, sector, grid
		rowAddress = SudokuBoard.ADDRESS_PREFIX_ROW + "${this.rowIndex + 1}"
		columnAddress = SudokuBoard.ADDRESS_PREFIX_COLUMN + "${this.columnIndex + 1}"
		sectorAddress = SudokuBoard.ADDRESS_PREFIX_SECTOR + "${this.sectorIndex + 1}"
		gridAddress = rowAddress + columnAddress
	}

	/**
	 * Appends string representation of this object to the given `StringBuilder`
	 * in a given data format version.
	 * You can later recreate object from this string by calling [.deserialize].
	 *
	 * @param data A `StringBuilder` where to write data.
	 */
	fun serialize(data: StringBuilder, dataVersion: Int) {
		when (dataVersion) {
			SudokuBoard.DATA_VERSION_ORIGINAL -> {
				data.append(if (isEditable) "0" else value)
			}

			SudokuBoard.DATA_VERSION_PLAIN -> {
				data.append(value)
			}

			else -> {
				data.append(value).append("|")
				if (primaryMarks.isEmpty) {
					data.append("0").append("|")
				} else {
					primaryMarks.serialize(data)
				}
				if (dataVersion >= SudokuBoard.DATA_VERSION_4) {
					if (secondaryMarks.isEmpty) {
						data.append("0").append("|")
					} else {
						secondaryMarks.serialize(data)
					}
				}
				if (dataVersion >= SudokuBoard.DATA_VERSION_5) {
					data.append(color).append("|")
				}
				data.append(if (isEditable) "1" else "0").append("|")
			}
		}
	}

	/**
	 * Notify CellCollection that something has changed.
	 */
	private fun onChange() {
		synchronized(cellCollectionLock) {
			cellCollection?.onChange()
		}
	}

	companion object {
		/**
		 * Creates instance from given `StringTokenizer`.
		 */
		fun deserialize(data: StringTokenizer, version: Int): Cell {
			val cell = Cell()
			cell.value = data.nextToken().toInt()
			cell.primaryMarks = CellMarks.deserialize(data.nextToken(), version)
			if (version >= SudokuBoard.DATA_VERSION_4) {
				cell.secondaryMarks = CellMarks.deserialize(data.nextToken(), version)
			}
			if (version >= SudokuBoard.DATA_VERSION_5) {
				cell.color = data.nextToken().toInt()
			}
			cell.isEditable = data.nextToken() == "1"
			return cell
		}
	}
}
