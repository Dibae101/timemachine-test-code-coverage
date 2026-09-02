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

class SetCellValueAndRemoveMarksCommand : AbstractMultiMarksCommand {
	private var cellRow = 0
	private var cellColumn = 0
	private var value = 0
	private var oldValue = 0

	constructor(cell: Cell, value: Int) {
		cellRow = cell.rowIndex
		cellColumn = cell.columnIndex
		this.value = value
	}

	internal constructor()

	val cell: Cell
		get() = cells.getCell(cellRow, cellColumn)

	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		data.append(cellRow).append("|")
		data.append(cellColumn).append("|")
		data.append(value).append("|")
		data.append(oldValue).append("|")
	}

	override fun deserialize(data: StringTokenizer, dataVersion: Int) {
		super.deserialize(data, dataVersion)
		cellRow = data.nextToken().toInt()
		cellColumn = data.nextToken().toInt()
		value = data.nextToken().toInt()
		oldValue = data.nextToken().toInt()
	}

	override fun execute() {
		primaryMarksBeforeCommandEntries.clear()
		saveOldMarks()
		cells.removeMarksForChangedCell(cell, value)
		oldValue = cell.value
		cell.value = value
	}

	override fun undo(): Cell {
		super.undo()
		cell.value = oldValue
		return cell
	}
}
