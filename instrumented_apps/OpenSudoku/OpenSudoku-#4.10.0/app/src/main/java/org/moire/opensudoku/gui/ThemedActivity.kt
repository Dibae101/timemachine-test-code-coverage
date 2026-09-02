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

import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.moire.opensudoku.R
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.utils.ThemeUtils

abstract class ThemedActivity : AppCompatActivity() {
	private var timestampWhenApplyingTheme: Long = 0
	override fun onCreate(savedInstanceState: Bundle?) {
		ThemeUtils.setThemeFromPreferences(this)
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		timestampWhenApplyingTheme = System.currentTimeMillis()
	}

	protected fun recreateActivityIfThemeChanged() {
		if (ThemeUtils.sTimestampOfLastThemeUpdate > timestampWhenApplyingTheme) {
			Log.d(javaClass.simpleName, "Theme changed, recreating activity")
			if (!isFinishing && !isChangingConfigurations) {
				ActivityCompat.recreate(this)
			}
		}
	}

	override fun onResume() {
		super.onResume()
		recreateActivityIfThemeChanged()
	}

	/**
	 * Pastes puzzle from primary clipboard in any of the supported formats.
	 *
	 * @see SudokuBoard.serialize
	 */
	fun getClipboard(): String {
		val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
		if (!clipboard.hasPrimaryClip() || (
					clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) != true &&
							clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) != true
					)
		) {
			Toast.makeText(applicationContext, R.string.invalid_mime_type, Toast.LENGTH_LONG).show()
			return ""
		}
		return clipboard.primaryClip!!.getItemAt(0).text.toString()
	}
}
