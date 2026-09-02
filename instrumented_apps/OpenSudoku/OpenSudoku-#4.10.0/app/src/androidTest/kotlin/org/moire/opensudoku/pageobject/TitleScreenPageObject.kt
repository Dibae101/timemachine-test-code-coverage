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

package org.moire.opensudoku.pageobject

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.startsWith
import org.moire.opensudoku.R

fun onTitleScreen() = TitleScreenPageObject()

class TitleScreenPageObject : BasePageObject() {

	fun dismissChangelogDialog() = apply {
		onView(withText(R.string.what_is_new))
			.check(matches(isDisplayed()))
		onView(withText(android.R.string.ok))
			.perform(click())
	}

	fun startNewRandomGame() = apply {
		onView(withText(R.string.play_random_puzzle))
			.perform(click())
	}

	fun assertResumeButtonVisibility(visible: Boolean) = apply {
		val view = onView(withText(R.string.resume))
		if (visible) {
			view.check(matches(isDisplayed()))
		} else {
			view.check(matches(not(isDisplayed())))
		}
	}

	fun navigateToFolderList() = apply {
		onView(withText(R.string.puzzle_lists))
			.perform(click())
	}

	fun startChristmasChallengeGame() = apply {
		onView(withText(R.string.xmas_challenge))
			.perform(click())
	}

	fun assertNoChristmasChallenge() = apply {
		onView(withText(R.string.xmas_challenge))
			.check(doesNotExist())
	}

	fun navigateToSettingsScreen() = apply {
		onView(withText(R.string.settings))
			.perform(click())
	}

	fun clickOnMenu() = apply {
		openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
	}

	fun navigateToAboutDialog() = apply {
		onView(withText(R.string.about))
			.perform(click())
	}

	fun assertAboutDialogVisible() = apply {
		onView(withText(startsWith("Open Sudoku v.")))
			.check(matches(isDisplayed()))
	}
}
