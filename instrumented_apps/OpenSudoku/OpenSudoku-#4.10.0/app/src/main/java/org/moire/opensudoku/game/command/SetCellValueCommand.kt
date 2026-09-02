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
package org.moire.opensudoku.game.command

import org.moire.opensudoku.game.Cell
import java.util.StringTokenizer

class SetCellValueCommand : AbstractSingleCellCommand {
	private var value = 0
	private var oldValue = 0

	constructor(cell: Cell, value: Int) : super(cell) {
		this.value = value
	}

	internal constructor()

	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		data.append(value).append("|")
		data.append(oldValue).append("|")
	}

	override fun deserialize(data: StringTokenizer, dataVersion: Int) {
		super.deserialize(data, dataVersion)
		value = data.nextToken().toInt()
		oldValue = data.nextToken().toInt()
	}

	override fun execute() {
		oldValue = cell.value
		cell.value = value
	}

	override fun undo(): Cell {
		cell.value = oldValue
		return cell
	}
}
