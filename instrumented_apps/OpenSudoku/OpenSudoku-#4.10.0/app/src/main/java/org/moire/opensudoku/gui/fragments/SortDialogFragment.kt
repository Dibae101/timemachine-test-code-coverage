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
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListSorter

class SortDialogFragment : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_sort)
			.setTitle(R.string.sort_puzzles_by)
			.setSingleChoiceItems(R.array.game_sort, listSorter.sortType) { _: DialogInterface?, whichButton: Int ->
				listSorter.sortType = whichButton
			}
			.setPositiveButton(R.string.sort_order_ascending) { _: DialogInterface?, _: Int ->
				listSorter.isAscending = true
				setFragmentResult(requestKey, bundleOf())
			}
			.setNegativeButton(R.string.sort_order_descending) { _: DialogInterface?, _: Int ->
				listSorter.isAscending = false
				setFragmentResult(requestKey, bundleOf())
			}
			.setNeutralButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
		return builder.create()
	}

	companion object {
		var listSorter = PuzzleListSorter()
		private val requestKey: String = SortDialogFragment::class.java.simpleName

		fun setListener(parent: FragmentActivity, callback: (Int, Boolean) -> Unit) {
			parent.supportFragmentManager.setFragmentResultListener(requestKey, parent) { _, _ ->
				callback(listSorter.sortType, listSorter.isAscending)
			}
		}
	}
}
