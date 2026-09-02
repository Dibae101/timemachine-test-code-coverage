/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2025-2025 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.gui

import org.moire.opensudoku.game.SudokuBoard
import java.math.BigInteger
import kotlin.Array
import kotlin.plus
import kotlin.toBigInteger

class SuidGenerator3F(): SuidGenerator {
	class ValuePossibility() {
		val possibleValues = Array(9) { Array(9) { Array(9) {true} } }

		fun reserveRow(rowIndex: Int, valueIndex: Int) {
			for (columnIndex in 0..8) {
				possibleValues[rowIndex][columnIndex][valueIndex] = false
			}
		}

		fun reserveColumn(columnIndex: Int, valueIndex: Int) {
			for (rowIndex in 0..8) {
				possibleValues[rowIndex][columnIndex][valueIndex] = false
			}
		}

		fun reserveValue(rowIndex: Int, columnIndex: Int) {
			for (valueIndex in 0..8) {
				possibleValues[rowIndex][columnIndex][valueIndex] = false
			}
		}

		fun reserveBox(rowBy3: Int, columnBy3: Int, valueIndex: Int) {
			for (rowIndex in rowBy3*3..rowBy3*3+2) {
				for (columnIndex in columnBy3*3..columnBy3*3+2) {
					possibleValues[rowIndex][columnIndex][valueIndex] = false
				}
			}
		}

		fun placeValue(rowIndex: Int, columnIndex: Int, valueIndex: Int) {
			reserveRow(rowIndex, valueIndex)
			reserveColumn(columnIndex, valueIndex)
			reserveBox(rowIndex / 3, columnIndex / 3, valueIndex)
			reserveValue(rowIndex, columnIndex)
		}

		fun countPossibleValues(rowIndex: Int, columnIndex: Int): Int {
			return possibleValues[rowIndex][columnIndex].count { it }
		}

		fun rowColToPossibleValuePlacement(rowIndex: Int, columnIndex: Int, targetValueIndex: Int): Int {
			var possibleValuePlacement = 0
			for (valueIndex in 0..8) {
				if (!possibleValues[rowIndex][columnIndex][valueIndex]) continue
				if (valueIndex == targetValueIndex) return possibleValuePlacement
				possibleValuePlacement += 1
			}
			throw IllegalArgumentException("This is not a solvable Sudoku")
		}

		fun possibleValuePlacementToValueIndex(rowIndex: Int, columnIndex: Int, targetPossiblePlacement: Int): Int {
			var possibleValuePlacement = 0
			for (valueIndex in 0..8) {
				if (!possibleValues[rowIndex][columnIndex][valueIndex] ) continue
				if (targetPossiblePlacement == possibleValuePlacement) return valueIndex
				possibleValuePlacement += 1
			}
			throw IllegalArgumentException("It is not a solvable Sudoku")
		}

		fun findLowestPossibleValuesCell(): Pair<Int,Int> {
			var lowestCount = Int.MAX_VALUE
			var lowestCountAddress = Pair(0,0)
			for (rowIndex in 0..8) {
				for (columnIndex in 0..8) {
					val possibleValuesCount = countPossibleValues(rowIndex,columnIndex)
					if (possibleValuesCount > 0 && possibleValuesCount < lowestCount) {
						lowestCount = possibleValuesCount
						lowestCountAddress = Pair(rowIndex, columnIndex)
					}
				}
			}
			return lowestCountAddress
		}
	}

	val values = ValuePossibility()

	override fun encode(board: SudokuBoard): String {
		val workingBoard = board.copyOriginal(false)
		if (workingBoard.solutionCount != 1) return ""
		val visibleBits = generateInitialValuesBitmap(workingBoard)

		var encodedBI: BigInteger = 0.toBigInteger()
		var multiplier: BigInteger = 1.toBigInteger()
		repeat(81) {
			val (rowIndex, columnIndex) = values.findLowestPossibleValuesCell()
			val value = workingBoard.cells[rowIndex][columnIndex].let { if (it.value != 0) it.value else it.solution }
			encodedBI += values.rowColToPossibleValuePlacement(rowIndex, columnIndex, value - 1).toBigInteger() * multiplier
			multiplier *= values.countPossibleValues(rowIndex, columnIndex).toBigInteger()
			values.placeValue(rowIndex, columnIndex, value - 1)
		}

		return ((encodedBI shl 81) or visibleBits).base62
	}

	override fun decode(encodedBase62: String): String {
		var encodedBI = encodedBase62.base62ToBigInteger()
		var remainingEncodedBI = encodedBI shr 81

		val resultBoard = SudokuBoard.createEmpty(true)
		repeat(81) {
			val (rowIndex, columnIndex) = values.findLowestPossibleValuesCell()
			val divider = values.countPossibleValues(rowIndex, columnIndex).toBigInteger()
			val possibleValuePlacement = (remainingEncodedBI % divider).toInt()
			remainingEncodedBI /= divider
			val valueIndex = values.possibleValuePlacementToValueIndex(rowIndex, columnIndex, possibleValuePlacement)
			values.placeValue(rowIndex, columnIndex, valueIndex)
			resultBoard.cells[rowIndex][columnIndex].value = valueIndex + 1
		}

		applyInitialValuesBitmap(resultBoard, encodedBI)
		return resultBoard.originalValues
	}
}
