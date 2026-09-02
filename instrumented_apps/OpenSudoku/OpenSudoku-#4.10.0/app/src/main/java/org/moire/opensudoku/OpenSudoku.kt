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

package org.moire.opensudoku

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import org.moire.opensudoku.game.GameSettings

class OpenSudoku : Application() {
	init {
		if (BuildConfig.DEBUG) {
			StrictMode.setVmPolicy(
				VmPolicy.Builder()
					.detectLeakedClosableObjects()
					.penaltyLog()
					.build()
			)
		}
	}

	override fun onCreate() {
		super.onCreate()

		// Migrate shared preference keys from version to version.
		val settings = GameSettings(applicationContext)
		val oldVersion = settings.schemaVersion
		val newVersion = BuildConfig.VERSION_CODE
		if (oldVersion != newVersion) {
			if (BuildConfig.DEBUG) Log.d(javaClass.simpleName, "Upgrading shared preferences: $oldVersion -> $newVersion")
			settings.schemaVersion = newVersion
		}
	}
}
