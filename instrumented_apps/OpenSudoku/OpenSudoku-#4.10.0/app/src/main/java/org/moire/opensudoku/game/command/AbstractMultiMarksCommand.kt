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
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.CellMarks
import java.util.StringTokenizer

abstract class AbstractMultiMarksCommand : AbstractCellCommand() {
	protected var primaryMarksBeforeCommandEntries: MutableList<MarksEntry> = ArrayList()
	protected var secondaryMarksBeforeCommandEntries: MutableList<MarksEntry> = ArrayList()
	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		data.append(primaryMarksBeforeCommandEntries.size).append("|")
		for (marksEntry in primaryMarksBeforeCommandEntries) {
			data.append(marksEntry.rowIndex).append("|")
			data.append(marksEntry.colIndex).append("|")
			marksEntry.marks.serialize(data)
		}
		data.append(secondaryMarksBeforeCommandEntries.size).append("|")
		for (marksEntry in secondaryMarksBeforeCommandEntries) {
			data.append(marksEntry.rowIndex).append("|")
			data.append(marksEntry.colIndex).append("|")
			marksEntry.marks.serialize(data)
		}
	}

	override fun deserialize(data: StringTokenizer, dataVersion: Int) {
		super.deserialize(data, dataVersion)
		var marksSize = data.nextToken().toInt()
		repeat(marksSize) {
			val row = data.nextToken().toInt()
			val col = data.nextToken().toInt()
			primaryMarksBeforeCommandEntries.add(MarksEntry(row, col, CellMarks.deserialize(data.nextToken())))
		}

		// There are no tokens for secondary marks if deserializing data from before double marks existed.
		if (dataVersion < SudokuBoard.DATA_VERSION_4 || !data.hasMoreTokens()) {
			return
		}
		marksSize = data.nextToken().toInt()
		repeat(marksSize) {
			val row = data.nextToken().toInt()
			val col = data.nextToken().toInt()
			secondaryMarksBeforeCommandEntries.add(MarksEntry(row, col, CellMarks.deserialize(data.nextToken())))
		}
	}

	override fun undo(): Cell? {
		for (marksEntry in primaryMarksBeforeCommandEntries) {
			cells.getCell(marksEntry.rowIndex, marksEntry.colIndex).primaryMarks = marksEntry.marks
		}
		for (marksEntry in secondaryMarksBeforeCommandEntries) {
			cells.getCell(marksEntry.rowIndex, marksEntry.colIndex).secondaryMarks = marksEntry.marks
		}
		return null
	}

	protected fun saveOldMarks() {
		for (row in 0..<SudokuBoard.SUDOKU_SIZE) {
			for (col in 0..<SudokuBoard.SUDOKU_SIZE) {
				primaryMarksBeforeCommandEntries.add(MarksEntry(row, col, cells.getCell(row, col).primaryMarks))
				secondaryMarksBeforeCommandEntries.add(MarksEntry(row, col, cells.getCell(row, col).secondaryMarks))
			}
		}
	}

	protected class MarksEntry(var rowIndex: Int, var colIndex: Int, var marks: CellMarks)
}
