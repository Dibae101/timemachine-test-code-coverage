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

package org.moire.opensudoku.db

import android.provider.BaseColumns

const val DATABASE_NAME = "opensudoku"
const val DATABASE_VERSION: Int = 17
const val PUZZLES_TABLE_NAME = "puzzles"
const val FOLDERS_TABLE_NAME = "folders"
const val ALL_IDS: Long = -1 // all folders

internal object ColumnName : BaseColumns {
	const val ID = BaseColumns._ID
	const val FOLDER_ID = "folder_id"
	const val CREATED = "created"
	const val STATE = "state"
	const val TIME = "time"
	const val MISTAKE_COUNTER = "mistake_counter"
	const val HINT_USAGE = "hint_usage"
	const val LAST_PLAYED = "last_played"
	const val ORIGINAL_VALUES = "original_values"
	const val CELLS_DATA = "cells_data"
	const val COMMAND_STACK = "command_stack"
	const val USER_NOTE = "user_note"
	const val NAME = "name"
	const val RATING_LEVEL = "rating_level"
	const val RATING_VALUE = "rating_value"
	const val SUID = "suid"
	const val SOLUTION = "solution"
}

enum class FoldersColumn(val nme: String) {
	ID(ColumnName.ID),
	CREATED(ColumnName.CREATED),
	NAME(ColumnName.NAME);

	var cid = ordinal

	override fun toString(): String {
		return nme
	}

	companion object {
		var indexesUpdated = false
	}
}

enum class PuzzlesColumn(val nme: String) {
	ID(ColumnName.ID),
	FOLDER_ID(ColumnName.FOLDER_ID),
	CREATED(ColumnName.CREATED),
	STATE(ColumnName.STATE),
	TIME(ColumnName.TIME),
	MISTAKE_COUNTER(ColumnName.MISTAKE_COUNTER),
	HINT_USAGE(ColumnName.HINT_USAGE),
	LAST_PLAYED(ColumnName.LAST_PLAYED),
	ORIGINAL_VALUES(ColumnName.ORIGINAL_VALUES),
	CELLS_DATA(ColumnName.CELLS_DATA),
	COMMAND_STACK(ColumnName.COMMAND_STACK),
	USER_NOTE(ColumnName.USER_NOTE),
	RATING_LEVEL(ColumnName.RATING_LEVEL),
	RATING_VALUE(ColumnName.RATING_VALUE),
	SUID(ColumnName.SUID),
	SOLUTION(ColumnName.SOLUTION);

	var cid = ordinal

	override fun toString(): String {
		return nme
	}
}
