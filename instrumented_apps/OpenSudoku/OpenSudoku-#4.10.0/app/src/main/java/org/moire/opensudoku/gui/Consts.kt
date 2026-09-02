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

package org.moire.opensudoku.gui

import org.moire.opensudoku.db.ColumnName

// Intent data names and XML tags for export and import
object Tag {
	const val FOLDER = "folder"
	const val GAME = "game"
	const val PUZZLE_ID = "puzzle_id"
	const val IS_RANDOM_PUZZLE = "is_random_puzzle"
	const val CELL_COLLECTION = "cell_collection"
	const val FOLDER_ID = ColumnName.FOLDER_ID
	const val CREATED = ColumnName.CREATED
	const val STATE = ColumnName.STATE
	const val MISTAKE_COUNTER = ColumnName.MISTAKE_COUNTER
	const val HINT_USAGE = ColumnName.HINT_USAGE
	const val TIME = ColumnName.TIME
	const val LAST_PLAYED = ColumnName.LAST_PLAYED
	const val CELLS_DATA = ColumnName.CELLS_DATA
	const val USER_NOTE = ColumnName.USER_NOTE
	const val COMMAND_STACK = ColumnName.COMMAND_STACK
	const val NAME = ColumnName.NAME
	const val RATING_LEVEL = ColumnName.RATING_LEVEL
	const val RATING_VALUE = ColumnName.RATING_VALUE
	const val IMPORT_STRATEGY = "import_strategy"
}

