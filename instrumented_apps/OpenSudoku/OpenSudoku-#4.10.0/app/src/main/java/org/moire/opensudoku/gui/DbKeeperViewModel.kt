/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2025 by Open Sudoku authors.
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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moire.opensudoku.db.SudokuDatabase

open class DbKeeperViewModel(application: Application) : AndroidViewModel(application) {
	var db: SudokuDatabase? = null // TODO: make db protected once all it's usage is moved from View to ViewModel (applies also to the initDb function)

	fun initDb(readOnly: Boolean, onInitFinished: ((db: SudokuDatabase)->Unit)?) {
		if (db != null) {
			onInitFinished?.invoke(db!!)
		} else {
			CoroutineScope(Dispatchers.IO).launch {
				db = SudokuDatabase(getApplication(), readOnly)
				onInitFinished?.run {
					withContext(Dispatchers.Main) {
						invoke(db!!)
					}
				}
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		db?.close()
	}
}
