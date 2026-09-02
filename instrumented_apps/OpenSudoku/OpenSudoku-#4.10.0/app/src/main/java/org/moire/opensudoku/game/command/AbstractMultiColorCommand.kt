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

package org.moire.opensudoku.game.command

import org.moire.opensudoku.game.Cell
import java.util.StringTokenizer

abstract class AbstractMultiColorCommand : AbstractCellCommand() {
	protected var colorsBeforeCommandEntries: MutableList<ColorEntry> = ArrayList()

	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		data.append(colorsBeforeCommandEntries.size).append("|")
		for (entry in colorsBeforeCommandEntries) {
			data.append(entry.rowIndex).append("|")
			data.append(entry.colIndex).append("|")
			data.append(entry.color).append("|")
		}
	}

	override fun deserialize(data: StringTokenizer, dataVersion: Int) {
		super.deserialize(data, dataVersion)
		val size = data.nextToken().toInt()
		repeat(size) {
			val row = data.nextToken().toInt()
			val col = data.nextToken().toInt()
			val color = data.nextToken().toInt()
			colorsBeforeCommandEntries.add(ColorEntry(row, col, color))
		}
	}

	override fun undo(): Cell? {
		for (entry in colorsBeforeCommandEntries) {
			cells.getCell(entry.rowIndex, entry.colIndex).color = entry.color
		}
		return null
	}

	protected class ColorEntry(var rowIndex: Int, var colIndex: Int, var color: Int)
}
