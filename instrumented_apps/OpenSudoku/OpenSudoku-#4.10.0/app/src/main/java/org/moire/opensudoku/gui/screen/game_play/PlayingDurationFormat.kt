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

import java.util.Formatter

/**
 * Game time formatter.
 */
class PlayingDurationFormat {
	private val timeText = StringBuilder()
	private val gameTimeFormatter = Formatter(timeText)

	/**
	 * Formats time to format of mm:ss, hours are
	 * never displayed, only total number of minutes.
	 *
	 * @param time Time in milliseconds.
	 */
	fun format(time: Long): String {
		timeText.setLength(0)
		val seconds = time / 1000
		val minutes = seconds / 60
		val hours = minutes / 60
		if (hours > 0) {
			gameTimeFormatter.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
		} else {
			gameTimeFormatter.format("%02d:%02d", minutes, seconds % 60)
		}
		return "$timeText"
	}
}
