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

package org.moire.opensudoku.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import org.moire.opensudoku.R

class CopyToClpDialogFragment : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setTitle(R.string.copy_to_clp)
			.setSingleChoiceItems(R.array.puzzle_value_types, checkedItemIndex) { _: DialogInterface?, whichButton: Int ->
				checkedItemIndex = whichButton
			}
			.setPositiveButton(android.R.string.copy) { _: DialogInterface?, _: Int ->
				setFragmentResult(requestKey, bundleOf())
			}
			.setNeutralButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }

		return builder.create()
	}

	companion object {
		private val requestKey: String = CopyToClpDialogFragment::class.java.simpleName
		var puzzleID: Long = 0
		var checkedItemIndex: Int = 0

		fun setListener(parent: FragmentActivity, callback: (whichButton: Int) -> Unit) {
			parent.supportFragmentManager.setFragmentResultListener(requestKey, parent) { _, _ ->
				callback(checkedItemIndex)
			}
		}
	}
}
