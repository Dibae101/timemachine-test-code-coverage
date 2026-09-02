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
package org.moire.opensudoku.gui.screen.puzzle_list

import android.content.Context
import org.moire.opensudoku.R

class PuzzleListFilter(context: Context) {
	var showStateNotStarted = true
	var showStatePlaying = true
	var showStateCompleted = true
	var showStateWithMistakes = false
	var showStateWithHints = false

	// Those strings used below in [toString()] are necessary for [PuzzleListActivity.updateFilterStatus] function status string updates.
	val notStartedStr = context.getString(R.string.not_started)
	val playingStr = context.getString(R.string.playing)
	val solvedStr = context.getString(R.string.solved)
	val withMistakesStr = context.getString(R.string.with_mistakes)
	val withHintsStr = context.getString(R.string.with_hints)

	override fun toString(): String {
		val visibleStates: MutableList<String?> = ArrayList()
		if (showStateNotStarted) visibleStates.add(notStartedStr)
		if (showStatePlaying) visibleStates.add(playingStr)
		if (showStateCompleted) visibleStates.add(solvedStr)

		val refinements: MutableList<String?> = ArrayList()
		if (showStateWithMistakes) refinements.add(withMistakesStr)
		if (showStateWithHints) refinements.add(withHintsStr)

		var result = visibleStates.joinToString(", ")
		if (refinements.isNotEmpty()) {
			result += " (${refinements.joinToString(", ")})"
		}
		return result
	}

	val anyActive get() = !showStateNotStarted || !showStatePlaying || !showStateCompleted || showStateWithMistakes || showStateWithHints
}
