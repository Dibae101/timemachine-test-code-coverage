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

val base62codes = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()
val base62values = base62codes.associateWith { code -> base62codes.indexOf(code) }
val bi62 = 62.toBigInteger()

interface SuidGenerator {
	fun encode(board: SudokuBoard): String
	fun decode(encodedBase62: String): String

	fun generateInitialValuesBitmap(board: SudokuBoard): BigInteger {
		var ret = BigInteger.ZERO
		for (rowIndex in 0..8) {
			for (columnIndex in 0..8) {
				ret = ret shl 1
				if (board.cells[rowIndex][columnIndex].value != 0) ret = ret or BigInteger.ONE
			}
		}
		return ret
	}

	fun applyInitialValuesBitmap(targetBoard: SudokuBoard, visibilityBI: BigInteger) {
		var remainingEncBI = visibilityBI
		for (rowIndex in 8 downTo 0) {
			for (columnIndex in 8 downTo 0) {
				if ((remainingEncBI and BigInteger.ONE) == BigInteger.ZERO) targetBoard.cells[rowIndex][columnIndex].value = 0
				remainingEncBI = remainingEncBI shr 1
			}
		}
	}
}

val BigInteger.base62: String get() {
	var tmpBI = this
	var ret = ""
	while (tmpBI > 0.toBigInteger()) {
		ret = base62codes[(tmpBI % bi62).toInt()] + ret
		tmpBI /= bi62
	}
	return ret
}

fun String.base62ToBigInteger(): BigInteger {
	var bi = 0.toBigInteger()
	this.forEach { code -> bi = bi * bi62 + base62values[code]!!.toBigInteger() }
	return bi
}
