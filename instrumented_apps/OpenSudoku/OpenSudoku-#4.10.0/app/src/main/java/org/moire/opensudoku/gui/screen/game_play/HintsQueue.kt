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
package org.moire.opensudoku.gui.screen.game_play

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.gui.ThemedActivity
import java.util.LinkedList
import java.util.Queue

private const val PREF_FILE_NAME = "hints"

class HintsQueue(private val activity: ThemedActivity) {
	private val messages: Queue<Message>
	private val hintDialog: AlertDialog
	private val prefs: SharedPreferences = activity.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
	private var oneTimeHintsEnabled: Boolean

	init {
		val settings = GameSettings(activity)
		settings.registerOnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String? ->
			if (key == "show_hints") {
				oneTimeHintsEnabled = sharedPreferences.getBoolean("show_hints", true)
			}
		}
		oneTimeHintsEnabled = settings.isShowHints

		//processQueue();
		val hintClosed = DialogInterface.OnClickListener { _: DialogInterface?, _: Int -> }
		hintDialog = AlertDialog.Builder(activity)
			.setIcon(R.drawable.ic_info)
			.setTitle(R.string.hint)
			.setMessage("")
			.setPositiveButton(R.string.close, hintClosed)
			.create()
		hintDialog.setOnDismissListener { processQueue() }
		messages = LinkedList()
	}

	private fun addHint(hint: Message) {
		synchronized(messages) { messages.add(hint) }
		synchronized(hintDialog) {
			if (!hintDialog.isShowing) {
				processQueue()
			}
		}
	}

	private fun processQueue() {
		showHintDialog(synchronized(messages) { messages.poll() ?: return@processQueue })
	}

	private fun showHintDialog(hint: Message) {
		synchronized(hintDialog) {
			hintDialog.setTitle(activity.getString(hint.titleResID))
			hintDialog.setMessage(activity.getText(hint.messageResID))
			hintDialog.show()
		}
	}

	fun showHint(titleResID: Int, messageResID: Int) {
		val hint = Message()
		hint.titleResID = titleResID
		hint.messageResID = messageResID
		addHint(hint)
	}

	fun showOneTimeHint(key: String, titleResID: Int, messageResID: Int) {
		if (oneTimeHintsEnabled) {

			val hintKey = "hint_$key"
			if (!prefs.getBoolean(hintKey, false)) {
				showHint(titleResID, messageResID)
				val editor = prefs.edit()
				editor.putBoolean(hintKey, true)
				editor.apply()
			}
		}
	}

	private class Message {
		var titleResID = 0
		var messageResID = 0 //Object[] args;
	}
}
