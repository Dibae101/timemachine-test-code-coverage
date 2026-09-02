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
package org.moire.opensudoku.game

import java.util.StringTokenizer

/**
 * Marks attached to a cell. Immutable objects.
 */
class CellMarks(private val binaryValue: Int = 0) {
	/**
	 * Marks associated with the cell.
	 */
	val marksValues: MutableList<Int>
		get() {
			val result: MutableList<Int> = ArrayList()
			var c = 1
			for (i in 1..9) {
				if (binaryValue and c != 0) {
					result.add(i)
				}
				c = c shl 1
			}
			return result
		}

	/**
	 * Returns true, if marks list is empty.
	 */
	val isEmpty: Boolean
		get() = binaryValue == 0

	/**
	 * Toggles marks value bit: if the value is already marked, it will be removed otherwise it will be added.
	 *
	 * @param value Number to toggle.
	 * @return New [CellMarks] instance with changes.
	 */
	fun toggleNumber(value: Int): CellMarks {
		require(!(value < 1 || value > 9)) { "Number must be between 1-9." }
		return CellMarks((binaryValue xor (1 shl value - 1)))
	}

	/**
	 * Adds number to the cell's marks (if not present already).
	 */
	fun addNumber(number: Int): CellMarks {
		require(!(number < 1 || number > 9)) { "Number must be between 1-9." }
		return CellMarks((binaryValue or (1 shl number - 1)))
	}

	/**
	 * Removes number from the cell's marks.
	 */
	fun removeNumber(number: Int): CellMarks {
		require(!(number < 1 || number > 9)) { "Number must be between 1-9." }
		return CellMarks((binaryValue and (1 shl number - 1).inv()))
	}

	fun hasNumber(number: Int): Boolean = if (number < 1 || number > 9) false else binaryValue and (1 shl number - 1) != 0

	/**
	 * Appends string representation of this object to the given `StringBuilder`.
	 * You can later recreate object from this string by calling [.deserialize].
	 */
	fun serialize(target: StringBuilder) {
		target.append(binaryValue)
		target.append("|")
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is CellMarks) return false
		return binaryValue == other.binaryValue
	}

	override fun hashCode(): Int = binaryValue

	companion object {
		val EMPTY = CellMarks()

		/**
		 * Creates instance from given string (string which has been
		 * created by [.serialize] or [.serialize] method).
		 * earlier.
		 */
		fun deserialize(pencilMarks: String, version: Int = SudokuBoard.CURRENT_DATA_VERSION): CellMarks {
			var markValue = 0
			if (pencilMarks != "" && pencilMarks != "-") {
				if (version == SudokuBoard.DATA_VERSION_1) {
					val tokenizer = StringTokenizer(pencilMarks, ",")
					while (tokenizer.hasMoreTokens()) {
						val value = tokenizer.nextToken()
						if (value != "-") {
							val number = value.toInt()
							markValue = markValue or (1 shl number - 1)
						}
					}
				} else {
					markValue = pencilMarks.toInt()
				}
			}
			return CellMarks(markValue)
		}

		/**
		 * Creates marks instance from given `int` array.
		 *
		 * @param pencilMarksNumbersArray Array of integers, which should be part of marks.
		 * @return New [CellMarks] instance.
		 */
		fun fromIntArray(pencilMarksNumbersArray: Array<Int>): CellMarks {
			var marksBinaryValue = 0
			for (n in pencilMarksNumbersArray) {
				marksBinaryValue = (marksBinaryValue or (1 shl n - 1))
			}
			return CellMarks(marksBinaryValue)
		}
	}
}
