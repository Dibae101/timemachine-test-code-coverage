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

package org.moire.opensudoku.gui.screen.title

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.moire.opensudoku.R
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.utils.AndroidUtils

class Changelog : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val whatsNewView = layoutInflater.inflate(R.layout.whats_new, null)
		return AlertDialog.Builder(requireActivity())
			.setIcon(R.mipmap.ic_launcher)
			.setTitle(R.string.what_is_new)
			.setView(whatsNewView)
			.setPositiveButton(android.R.string.ok, null)
			.create()
	}

	companion object {
		private const val CHANGELOG_PREFERENCE_NAME = "changelog"
		fun showOnFirstRun(activity: ThemedActivity) {
			val prefs: SharedPreferences = activity.getSharedPreferences(CHANGELOG_PREFERENCE_NAME, Context.MODE_PRIVATE)
			val versionKey = AndroidUtils.getAppVersionCode(activity).toString()
			if (!prefs.getBoolean(versionKey, false)) {
				prefs.edit().apply { putBoolean(versionKey, true) }.apply()
				Changelog().show(activity.supportFragmentManager, Changelog::class.simpleName)
			}
		}
	}
}