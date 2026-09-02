/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2026 by Open Sudoku authors.
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
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import org.moire.opensudoku.R
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListFilter

class FilterDialogFragment : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_filter, null)

		val cbNotStarted = view.findViewById<CheckBox>(R.id.filter_not_started)
		val cbPlaying = view.findViewById<CheckBox>(R.id.filter_playing)
		val cbSolved = view.findViewById<CheckBox>(R.id.filter_solved)
		val cbMistakes = view.findViewById<CheckBox>(R.id.filter_with_mistakes)
		val cbHints = view.findViewById<CheckBox>(R.id.filter_with_hints)
		val andAlsoLabel = view.findViewById<TextView>(R.id.filter_and_also_label)

		val mandatoryCheckboxes = listOf(cbNotStarted, cbPlaying, cbSolved)
		val refinementCheckboxes = listOf(cbMistakes, cbHints)

		// Initial states
		cbNotStarted.isChecked = listFilter.showStateNotStarted
		cbPlaying.isChecked = listFilter.showStatePlaying
		cbSolved.isChecked = listFilter.showStateCompleted
		cbMistakes.isChecked = listFilter.showStateWithMistakes
		cbHints.isChecked = listFilter.showStateWithHints

		fun updateUiStates() {
			val checkedMandatoryCount = mandatoryCheckboxes.count { it.isChecked }
			val isOnlyNotStartedChecked = cbNotStarted.isChecked && checkedMandatoryCount == 1

			mandatoryCheckboxes.forEach { cb ->
				cb.isEnabled = !(cb.isChecked && checkedMandatoryCount == 1)
			}

			val refinementsEnabled = !isOnlyNotStartedChecked
			refinementCheckboxes.forEach {
				it.isEnabled = refinementsEnabled
				if (!refinementsEnabled) {
					it.isChecked = false
				}
			}
			andAlsoLabel.isEnabled = refinementsEnabled
		}

		mandatoryCheckboxes.forEach { cb ->
			cb.setOnCheckedChangeListener { _, _ -> updateUiStates() }
		}

		updateUiStates()

		return AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_view)
			.setTitle(R.string.filter_by_game_state)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val checkedMandatoryCount = mandatoryCheckboxes.count { it.isChecked }
				val isOnlyNotStartedChecked = cbNotStarted.isChecked && checkedMandatoryCount == 1
				val refinementsEnabled = !isOnlyNotStartedChecked

				listFilter.showStateNotStarted = cbNotStarted.isChecked
				listFilter.showStatePlaying = cbPlaying.isChecked
				listFilter.showStateCompleted = cbSolved.isChecked
				listFilter.showStateWithMistakes = refinementsEnabled && cbMistakes.isChecked
				listFilter.showStateWithHints = refinementsEnabled && cbHints.isChecked
				setFragmentResult(requestKey, bundleOf())
			}
			.setNegativeButton(android.R.string.cancel, null)
			.create()
	}

	companion object {
		lateinit var listFilter: PuzzleListFilter
		private val requestKey: String = FilterDialogFragment::class.java.simpleName

		fun setListener(parent: FragmentActivity, callback: () -> Unit) {
			parent.supportFragmentManager.setFragmentResultListener(requestKey, parent) { _, _ ->
				callback()
			}
		}
	}
}
