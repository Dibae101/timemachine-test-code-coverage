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

class SetCellColorCommand(cell: Cell, private var newColor: Int) : AbstractSingleCellCommand(cell) {

	private var oldColor: Int = 0

	constructor() : this(Cell(), 0)

	override fun execute() {
		oldColor = cell.color
		cell.color = newColor
	}

	override fun undo(): Cell {
		cell.color = oldColor
		return cell
	}

	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		data.append(newColor).append("|")
	}

	override fun deserialize(data: StringTokenizer, dataVersion: Int) {
		super.deserialize(data, dataVersion)
		newColor = data.nextToken().toInt()
	}
}
