package org.moire.opensudoku.pageobject

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.moire.opensudoku.R

fun onSudokuPlayScreen() = SudokuPlayScreenPageObject()

class SudokuPlayScreenPageObject : BasePageObject() {

	fun dismissWelcomeDialog() = apply {
		onView(withText(R.string.welcome))
			.check(matches(isDisplayed()))
		onView(withText(R.string.close))
			.perform(click())
	}

	fun dismissPopUpDialog() = apply {
		onView(withText(R.string.popup))
			.check(matches(isDisplayed()))
		onView(withText(R.string.close))
			.perform(click())
	}

	fun assertSudokuBoardVisible() = apply {
		onView(withId(R.id.play_board_view))
			.check(matches(isDisplayed()))
	}
}
