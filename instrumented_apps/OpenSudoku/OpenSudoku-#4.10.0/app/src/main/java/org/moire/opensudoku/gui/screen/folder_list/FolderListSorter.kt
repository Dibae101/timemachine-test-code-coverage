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

package org.moire.opensudoku.gui.screen.folder_list

import org.moire.opensudoku.db.FoldersColumn

class FolderListSorter(sortType: Int = SORT_BY_NAME, var isAscending: Boolean = false) {
	internal var sortType: Int = sortType
		set(value) {
			field = if (value in 0..<SORT_TYPE_OPTIONS_LENGTH) value else SORT_BY_NAME
		}

	val sortOrder: String
		get() {
			val sortBy = when (sortType) {
				SORT_BY_NAME -> FoldersColumn.NAME.nme + " COLLATE NOCASE"
				SORT_BY_CREATED -> FoldersColumn.CREATED.nme
				SORT_BY_ID -> FoldersColumn.ID.nme
				else -> FoldersColumn.NAME.nme
			}
			val order = if (isAscending) " ASC" else " DESC"
			return sortBy + order
		}

	companion object {
		const val SORT_BY_NAME = 0
		const val SORT_BY_CREATED = 1
		const val SORT_BY_ID = 2 // no sort
		private const val SORT_TYPE_OPTIONS_LENGTH = 3
	}
}
