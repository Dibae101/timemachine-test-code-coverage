/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2026 by Open Sudoku authors.
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
import java.util.regex.Pattern

/**
 * Collection of Sudoku cells. This class in fact represents one Sudoku board (9x9).
 */
class SudokuBoard private constructor(val cells: Array<Array<Cell>>, val isEditMode: Boolean) {
	private val changeListeners: MutableList<() -> Unit> = ArrayList()

	// Helper arrays, contains references to the groups of cells, which should contain unique numbers.
	private lateinit var sectors: Array<CellGroup>
	private lateinit var rows: Array<CellGroup>
	private lateinit var columns: Array<CellGroup>
	private var isAllValidated = false
	private var lastAllValid = false
	private var batchUpdateCount = 0

	private var valuesUseCountCache: Array<Int>? = null
	private var valuesCountCache: Int? = null

	var originalValues: String = ""
		get() {
			if (isEditMode) field = serialize(DATA_VERSION_PLAIN) // in edit mode update always
			else if (field == "") field = serialize(DATA_VERSION_ORIGINAL) // in normal play mode generate only once
			return field
		}

	var solutionCount: Int = -1
		get() {
			if (field == -1) {
				val cachedSolution = game?.solution
				if (cachedSolution != null) {
					if (cachedSolution == "none") {
						field = 0
					} else {
						deserializeSolution(cachedSolution)
						field = 1
					}
				}
				if (field == -1) {
					val (solutionCount, validSolution) = SudokuSolver.solve(this@SudokuBoard)
					field = solutionCount
					if (validSolution != null && !isEditMode) {
						setSolution(validSolution)
					}
				}
			}
			return field
		}
		private set

	internal var game: SudokuGame? = null

	private fun deserializeSolution(solutionData: String) {
		var pos = 0
		for (row in 0..<SUDOKU_SIZE) {
			for (col in 0..<SUDOKU_SIZE) {
				getCell(row, col).solution = solutionData[pos++].digitToInt()
			}
		}
	}

	fun serializeSolution(): String {
		val sb = StringBuilder()
		cells.forEach { row ->
			row.forEach { cell ->
				sb.append(cell.solution)
			}
		}
		return sb.toString()
	}

	private fun setSolution(validSolution: java.util.ArrayList<Array<Int>>) {
		for (rowColumnValue in validSolution) {
			val row = rowColumnValue[0]
			val column = rowColumnValue[1]
			val value = rowColumnValue[2]
			val cell = getCell(row, column)
			cell.solution = value
		}
	}

	/**
	 * Wraps given array in this object.
	 */
	init {
		initCollection()
	}

	/**
	 * True if no value is entered to any of cells.
	 */
	val isEmpty: Boolean
		get() {
			for (row in 0..<SUDOKU_SIZE) {
				for (col in 0..<SUDOKU_SIZE) {
					val cell = cells[row][col]
					if (cell.value != 0) return false
				}
			}
			return true
		}

	val hasMistakes: Boolean
		get() {
			for (row in 0..<SUDOKU_SIZE) {
				for (col in 0..<SUDOKU_SIZE) {
					val cell = cells[row][col]
					if (cell.value != 0 && !cell.isCorrect) return true
				}
			}
			return false
		}

	/**
	 * Gets cell at given position.
	 */
	fun getCell(rowIndex: Int, colIndex: Int): Cell = cells[rowIndex][colIndex]

	/**
	 * Get houses for rows, columns and sectors
	 */
	fun getHousesRows(): Array<CellGroup> = rows

	fun getHousesColumns(): Array<CellGroup> = columns

	fun getHousesSectors(): Array<CellGroup> = sectors

	private fun markAllCellsAsValid() {
		cells.forEach { row ->
			row.forEach { cell ->
				cell.isValid = true
			}
		}
	}

	/**
	 * Validates numbers in collection according to the Sudoku rules. Cells with invalid
	 * values are marked - you can use getInvalid method of cell to find out whether cell
	 * contains valid value.
	 *
	 * @return True if validation is successful.
	 */
	fun validateAllCells(): Boolean {
		if (batchUpdateCount > 0) return true
		if (isAllValidated) return lastAllValid
		// first set all cells as valid
		markAllCellsAsValid()
		// run validation in groups
		var allValid = true
		rows.forEach { row ->
			allValid = row.validate() && allValid
		}
		columns.forEach { column ->
			allValid = column.validate() && allValid
		}
		sectors.forEach { sector ->
			allValid = sector.validate() && allValid
		}

		isAllValidated = true
		lastAllValid = allValid
		return allValid
	}

	/* faster than validate() but doesn't mark flags inside the cells if they're valid or not */
	fun isValid(): Boolean {
		rows.forEach { row ->
			if (!row.validate()) return@isValid false
		}
		columns.forEach { column ->
			if (!column.validate()) return@isValid false
		}
		sectors.forEach { sector ->
			if (!sector.validate()) return@isValid false
		}
		return true
	}

	val isSolved: Boolean
		get() {
			cells.forEach { row ->
				row.forEach { cell ->
					if (cell.value == 0 || !cell.isValid) {
						return false
					}
				}
			}
			return true
		}

	/**
	 * Fills in all valid primary marks for all cells based on the values in each row, column,
	 * and sector. This is a destructive operation in that the existing marks are overwritten.
	 */
	fun fillInPrimaryMarks() {
		beginBatchUpdate()
		cells.forEach { row ->
			row.forEach { cell ->
				cell.run {
					primaryMarks = CellMarks()
					for (i in 1..SUDOKU_SIZE) {
						if (!(this.row ?: continue).contains(i) && !(column ?: continue).contains(i) && !(sector ?: continue).contains(i)) {
							primaryMarks = primaryMarks.addNumber(i)
						}
					}
				}
			}
		}
		endBatchUpdate()
	}

	/**
	 * Fills in main marks with all values for all cells.
	 * This is a destructive operation in that the existing main marks are overwritten.
	 */
	fun fillInPrimaryMarksWithAllValues() {
		beginBatchUpdate()
		cells.forEach { row ->
			row.forEach { cell ->
				cell.primaryMarks = CellMarks()
				for (i in 1..SUDOKU_SIZE) {
					cell.primaryMarks = cell.primaryMarks.addNumber(i)
				}
			}
		}
		endBatchUpdate()
	}

	fun removeMarksForChangedCell(cell: Cell, number: Int) {
		if (number < 1 || number > 9) {
			return
		}
		beginBatchUpdate()
		val cells: MutableList<Cell> = ArrayList()
		cells.addAll(listOf(*(cell.row ?: return).cells))
		cells.addAll(listOf(*(cell.column ?: return).cells))
		cells.addAll(listOf(*(cell.sector ?: return).cells))
		for (c in cells) {
			c.primaryMarks = c.primaryMarks.removeNumber(number)
			c.secondaryMarks = c.secondaryMarks.removeNumber(number)
		}
		endBatchUpdate()
	}

	/**
	 * Returns how many times each value is used in `CellCollection`.
	 * Returns map with entry for each value.
	 */
	val valuesUseCount: Array<Int>
		get() {
			valuesUseCountCache?.let { return it }
			val valuesUseCount = Array(SUDOKU_SIZE + 1) { 0 }
			cells.forEach { row ->
				row.forEach { cell ->
					valuesUseCount[cell.value] += 1
				}
			}
			valuesUseCountCache = valuesUseCount
			return valuesUseCount
		}

	/**
	 * Returns how many times any non-empty value is used in `CellCollection`.
	 * In other words: This value is the number of solved cells.
	 */
	val valuesCount: Int
		get() {
			valuesCountCache?.let { return it }
			var valuesCount = 0
			cells.forEach { row ->
				row.forEach { cell ->
					if (cell.value != 0) {
						valuesCount += 1
					}
				}
			}
			valuesCountCache = valuesCount
			return valuesCount
		}

	/**
	 * Initializes collection, initialization has two steps:
	 * 1) Groups of cells which must contain unique numbers are created.
	 * 2) Row and column index for each cell is set.
	 */
	private fun initCollection() {
		rows = Array(SUDOKU_SIZE) { CellGroup() }
		columns = Array(SUDOKU_SIZE) { CellGroup() }
		sectors = Array(SUDOKU_SIZE) { CellGroup() }
		cells.forEachIndexed { rowIndex, row ->
			row.forEachIndexed { columnIndex, cell ->
				cell.initCollection(
					this, rowIndex, columnIndex,
					sectors[rowIndex / 3 * 3 + columnIndex / 3],
					rows[rowIndex],
					columns[columnIndex]
				)
			}
		}
	}

	/**
	 * Returns a string representation of this collection in a default
	 * ([.DATA_PATTERN_VERSION_4]) format version.
	 *
	 * @see .serialize
	 * @return A string representation of this collection.
	 */
	fun serialize(): String = serialize(CURRENT_DATA_VERSION)

	/**
	 * Returns a string representation of this collection in a given data format version.
	 *
	 * @return A string representation of this collection.
	 *
	 * @see .DATA_PATTERN_VERSION_PLAIN
	 * @see .DATA_PATTERN_VERSION_4
	 */
	/**
	 * Writes collection to given `StringBuilder` in a default
	 * ([.DATA_PATTERN_VERSION_4]) data format version.
	 *
	 * @see .serialize
	 */
	fun serialize(dataVersion: Int = CURRENT_DATA_VERSION): String {
		val resultBuilder = StringBuilder()
		if (dataVersion > DATA_VERSION_PLAIN) {
			resultBuilder.append("version: ")
			resultBuilder.append(dataVersion)
			resultBuilder.append("\n")
		}
		cells.forEach { row ->
			row.forEach { cell ->
				cell.serialize(resultBuilder, dataVersion)
			}
		}
		return "$resultBuilder"
	}

	fun ensureOnChangeListener(listener: (() -> Unit)?) {
		requireNotNull(listener) { "The listener is null." }
		synchronized(changeListeners) {
			if (!changeListeners.contains(listener)) {
				changeListeners.add(listener)
			}
		}
	}

	/**
	 * Notify all registered listeners that something has changed.
	 */
	fun onChange() {
		if (batchUpdateCount > 0) return
		isAllValidated = false
		valuesUseCountCache = null
		valuesCountCache = null
		synchronized(changeListeners) {
			for (listener in changeListeners) {
				listener()
			}
		}
		if (isEditMode) {
			originalValues = ""
			solutionCount = -1
		}
	}

	/**
	 * Starts a batch update. During a batch update, [onChange] calls are ignored
	 * and [validateAllCells] is skipped.
	 */
	fun beginBatchUpdate() {
		batchUpdateCount++
	}

	/**
	 * Ends a batch update. If this was the last nested batch update, [onChange] is called.
	 */
	fun endBatchUpdate() {
		batchUpdateCount--
		if (batchUpdateCount == 0) {
			onChange()
		}
	}

	fun setCells(from: Array<Array<Cell>>) {
		for (row in 0..<SUDOKU_SIZE) {
			for (col in 0..<SUDOKU_SIZE) {
				cells[row][col] = from[row][col]
			}
		}
		onChange()
	}

	fun copyOriginal(setEditMode: Boolean): SudokuBoard {
		val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
		cells.forEachIndexed { rowIndex, row ->
			row.forEachIndexed { colIndex, cell ->
				val cellToCopy = this.cells[rowIndex][colIndex]
				if (cellToCopy.value != 0 && (isEditMode || !cellToCopy.isEditable)) {
					cell.value = cellToCopy.value
					cell.isEditable = false
				}
			}
		}
		return SudokuBoard(cells, setEditMode)
	}

	companion object {
		const val SUDOKU_SIZE = 9

		/**
		 * String is expected to be in format "00002343243202...", where each number represents
		 * cell value and only the original values (not editable) are stored.
		 */
		var DATA_VERSION_ORIGINAL = -1

		/**
		 * String is expected to be in format "00002343243202...", where each number represents
		 * cell value, no other information can be set using this method.
		 */
		var DATA_VERSION_PLAIN = 0

		/**
		 * See [.DATA_PATTERN_VERSION_1] and [.serialize].
		 * Marks are stored as an array of numbers
		 */
		var DATA_VERSION_1 = 1

		private var DATA_VERSION_2 = 2
		private var DATA_VERSION_3 = 3

		/**
		 * Primary marks added
		 */
		var DATA_VERSION_4 = 4

		/**
		 * Custom background colors added
		 */
		var DATA_VERSION_5 = 5
		var CURRENT_DATA_VERSION = DATA_VERSION_5
		private val DATA_PATTERN_VERSION_PLAIN = Pattern.compile("^\\d{81}$")

		// version: <version:1..4>
		// <value:1..9>|<marks>|<editable:0,1> x81
		private val DATA_PATTERN_VERSION_1 = Pattern.compile(   // legacy OpenSudoku format
			"""^version: 1\n(?<nodeInfo>(?<value>\d)[|](?<primaryMarks>(?<mark>\d,)+|-)[|](?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_2 = Pattern.compile(   // legacy OpenSudoku format
			"""^version: 2\n(?<nodeInfo>(?<value>\d)[|](?<primaryMarks>\d{1,3})[|]{1,2}(?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_3 = Pattern.compile(   // legacy OpenSudoku format
			"""^version: 3\n(?<nodeInfo>(?<value>\d)[|](?<primaryMarks>\d{1,3})[|](?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_4 = Pattern.compile(
			"""^version: 4\n(?<nodeInfo>(?<value>\d)[|](?<primaryMarks>\d{1,3})[|](?<secondaryMarks>\d{1,3})[|](?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_5 = Pattern.compile(
			"""^version: 5\n(?<nodeInfo>(?<value>\d)[|](?<primaryMarks>\d{1,3})[|](?<secondaryMarks>\d{1,3})[|](?<color>\d)[|](?<isEditable>[01])[|]){81}$"""
		)

		/**
		 * Creates empty Sudoku board cell collection.
		 */
		fun createEmpty(isEditMode: Boolean): SudokuBoard {
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			return SudokuBoard(cells, isEditMode)
		}

		/**
		 * Creates instance from given string created by [serialize] method or simple 81 digit string.
		 * earlier.
		 */
		fun deserialize(data: String, isEditMode: Boolean): Pair<SudokuBoard, Int> {
			val lines = data.split("\n".toRegex())
				.dropLastWhile(String::isEmpty)
				.toTypedArray()
			require(lines.isNotEmpty()) { "Cannot deserialize Sudoku, data corrupted." }
			val line = lines[0]
			return if (line.startsWith("version:")) {
				val kv = line.split(":".toRegex())
					.dropLastWhile(String::isEmpty)
					.toTypedArray()
				val version = kv[1].trim { it <= ' ' }.toInt()
				val tokenizer = StringTokenizer(lines[1], "|")
				deserialize(tokenizer, version, isEditMode) to version
			} else {
				fromString(data, isEditMode) to DATA_VERSION_PLAIN
			}
		}

		/**
		 * Creates collection instance from given string. String is expected to contain (81 digits or dots), where each digit represents a cell value.
		 * Dots and 0s are converted to an empty cell.
		 */
		fun fromString(inputData: String, isEditMode: Boolean): SudokuBoard {
			val normalized = inputData.replace(".", "0")
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			var pos = 0
			cells.forEach { row ->
				row.forEach { cell ->
					while (pos < normalized.length && !normalized[pos].isDigit()) {
						pos++
					}
					val newValue = if (pos < normalized.length) normalized[pos].digitToInt() else 0
					cell.value = newValue
					cell.isEditable = newValue == 0
					pos++
				}
			}
			return SudokuBoard(cells, isEditMode)
		}

		fun fromArray(numbers: Array<IntArray>, isEditMode: Boolean): SudokuBoard {
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			cells.forEachIndexed { rowIndex, row ->
				row.forEachIndexed { columnIndex, cell ->
					cell.value = numbers[rowIndex][columnIndex]
					cell.isEditable = cell.value == 0
				}
			}
			return SudokuBoard(cells, isEditMode)
		}

		/**
		 * Creates instance from given `StringTokenizer`.
		 */
		private fun deserialize(tokenizer: StringTokenizer, version: Int, isEditMode: Boolean): SudokuBoard {
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			var r = 0
			var c = 0
			while (tokenizer.hasMoreTokens() && r < 9) {
				cells[r][c] = Cell.deserialize(tokenizer, version)
				c++
				if (c == 9) {
					r++
					c = 0
				}
			}
			return SudokuBoard(cells, isEditMode)
		}

		/**
		 * Returns true, if given `data` conform to format of any version.
		 */
		fun getDataVersion(data: String?): Int {
			if (data == null) {
				return -2
			}
			if (DATA_PATTERN_VERSION_PLAIN.matcher(data).matches()) {
				return DATA_VERSION_PLAIN
			}
			if (DATA_PATTERN_VERSION_1.matcher(data).matches()) {
				return DATA_VERSION_1
			}
			if (DATA_PATTERN_VERSION_2.matcher(data).matches()) {
				return DATA_VERSION_2
			}
			if (DATA_PATTERN_VERSION_3.matcher(data).matches()) {
				return DATA_VERSION_3
			}
			if (DATA_PATTERN_VERSION_4.matcher(data).matches()) {
				return DATA_VERSION_4
			}
			if (DATA_PATTERN_VERSION_5.matcher(data).matches()) {
				return DATA_VERSION_5
			}
			return -2
		}

		/**
		 * Prefix for the address of row, column and sector for a cell or house
		 */
		var ADDRESS_PREFIX_ROW = "r"
		var ADDRESS_PREFIX_COLUMN = "c"
		var ADDRESS_PREFIX_SECTOR = "b"

	}
}
