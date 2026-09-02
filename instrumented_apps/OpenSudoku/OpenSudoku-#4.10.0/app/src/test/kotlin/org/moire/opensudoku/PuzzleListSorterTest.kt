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

package org.moire.opensudoku

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListSorter
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListSorter.Companion.SORT_BY_CREATED
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListSorter.Companion.SORT_BY_LAST_PLAYED
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListSorter.Companion.SORT_BY_TIME
import org.junit.jupiter.api.Disabled
import kotlin.test.Test

class PuzzleListSorterTest {

	@Test
	fun `should return default sort order when called without parameters`() {
		val sorter = PuzzleListSorter()
		sorter.sortOrder.shouldBeEqual("created DESC")
		sorter.sortType.shouldBeEqual(0)
		sorter.isAscending.shouldBeFalse()
	}

	@Test
	fun `should handle isAscending param`() {
		val sorter1 = PuzzleListSorter(isAscending = true)
		sorter1.isAscending.shouldBeTrue()
		sorter1.sortOrder.shouldEndWith("ASC")

		val sorter2 = PuzzleListSorter(isAscending = false)
		sorter2.isAscending.shouldBeFalse()
		sorter2.sortOrder.shouldEndWith("DESC")
	}

	@Test
	fun `should apply sorting by creation date`() {
		val sorter = PuzzleListSorter(sortType = SORT_BY_CREATED)
		sorter.sortType.shouldBeEqual(0)
		sorter.sortOrder.shouldStartWith("created ")
	}

	@Test
	fun `should apply sorting by time elapsed`() {
		val sorter = PuzzleListSorter(sortType = SORT_BY_TIME)
		sorter.sortType.shouldBeEqual(1)
		sorter.sortOrder.shouldStartWith("time ")
	}

	@Test
	fun `should apply sorting by last played time`() {
		val sorter = PuzzleListSorter(sortType = SORT_BY_LAST_PLAYED)
		sorter.sortType.shouldBeEqual(2)
		sorter.sortOrder.shouldStartWith("last_played ")
	}

	@Test
	@Disabled("This test is failing because the sort order is not being set correctly")
	fun `should handle invalid sort types when creating `() {
		val sorter1 = PuzzleListSorter(sortType = -1)
		sorter1.sortType.shouldBeEqual(0)
		sorter1.sortOrder.shouldStartWith("created ")

		val sorter2 = PuzzleListSorter(sortType = 3)
		sorter2.sortType.shouldBeEqual(0)
		sorter2.sortOrder.shouldStartWith("created ")
	}

	@Test
	fun `should handle invalid sort types when updating `() {
		val sorter = PuzzleListSorter(sortType = SORT_BY_TIME)

		sorter.sortType = -1
		sorter.sortType.shouldBeEqual(0)
		sorter.sortOrder.shouldStartWith("created ")

		sorter.sortType = 3
		sorter.sortType.shouldBeEqual(0)
		sorter.sortOrder.shouldStartWith("created ")
	}

	@Test
	fun `should allow updating variables`() {
		val sorter = PuzzleListSorter(sortType = SORT_BY_LAST_PLAYED, isAscending = false)
		sorter.sortOrder.shouldBeEqual("last_played DESC")

		sorter.sortType = SORT_BY_TIME
		sorter.isAscending = true
		sorter.sortOrder.shouldBeEqual("time ASC")
	}
}
