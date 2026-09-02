package org.moire.opensudoku.pageobject

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.moire.opensudoku.R

fun onFolderListScreen() = FolderListScreenPageObject()

class FolderListScreenPageObject : BasePageObject() {

	fun assertFolderListVisible() = apply {
		onView(withText(R.string.folders))
			.check(matches(isDisplayed()))
	}
}
