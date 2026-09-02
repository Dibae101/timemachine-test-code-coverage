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
import org.moire.opensudoku.game.CellMarks
import java.util.StringTokenizer

class EditCellSecondaryMarksCommand : AbstractSingleCellCommand {
	private lateinit var marks: CellMarks
	private lateinit var marksBeforeCommand: CellMarks

	constructor(cell: Cell, marks: CellMarks) : super(cell) {
		this.marks = marks
		marksBeforeCommand = CellMarks()
	}

	internal constructor()

	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		marks.serialize(data)
		marksBeforeCommand.serialize(data)
	}

	override fun deserialize(data: StringTokenizer, dataVersion: Int) {
		super.deserialize(data, dataVersion)
		marks = CellMarks.deserialize(data.nextToken())
		marksBeforeCommand = CellMarks.deserialize(data.nextToken())
	}

	override fun execute() {
		marksBeforeCommand = cell.secondaryMarks
		cell.secondaryMarks = marks
	}

	override fun undo(): Cell {
		cell.secondaryMarks = marksBeforeCommand
		return cell
	}
}
