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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.gui.screen.title.RandomPuzzleFilter

class RandomPuzzleDialogFragment : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_view)
			.setTitle(R.string.play_random_puzzle)
			.setMultiChoiceItems(
				R.array.random_puzzle_filter, booleanArrayOf(
					randomFilter.unsolved,
					randomFilter.withMistakes,
					randomFilter.withHints
				)
			) { _: DialogInterface?, whichButton: Int, isChecked: Boolean ->
				when (whichButton) {
					0 -> randomFilter.unsolved = isChecked
					1 -> randomFilter.withMistakes = isChecked
					2 -> randomFilter.withHints = isChecked
				}
			}
			.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
				setFragmentResult(Companion.tag, bundleOf())
			}
			.setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
		return builder.create()
	}

	fun show(manager: FragmentManager) {
		super.show(manager, this@RandomPuzzleDialogFragment.tag)
	}

	companion object {
		val tag: String = RandomPuzzleDialogFragment::class.java.simpleName
		val randomFilter = RandomPuzzleFilter()

		fun init(parent: FragmentActivity, settings: GameSettings, callback: () -> Unit) {
			randomFilter.unsolved = settings.randomOnlyUnsolved
			randomFilter.withMistakes = settings.randomOnlyWithMistakes
			randomFilter.withHints = settings.randomOnlyWithHints
			parent.supportFragmentManager.setFragmentResultListener(tag, parent) { _, _ ->
				settings.randomOnlyUnsolved = randomFilter.unsolved
				settings.randomOnlyWithMistakes = randomFilter.withMistakes
				settings.randomOnlyWithHints = randomFilter.withHints
				callback()
			}
		}
	}
}
