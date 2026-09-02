package org.moire.opensudoku.pageobject

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.moire.opensudoku.R

fun onSettingsScreen() = SettingsScreenPageObject()

class SettingsScreenPageObject : BasePageObject() {

	fun assertSettingsScreenVisible() = apply {
		onView(withText(R.string.game_settings))
			.check(matches(isDisplayed()))
	}
}
