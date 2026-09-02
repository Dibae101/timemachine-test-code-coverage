package org.moire.opensudoku.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.moire.opensudoku.pageobject.onFolderListScreen
import org.moire.opensudoku.pageobject.onSettingsScreen
import org.moire.opensudoku.pageobject.onSudokuPlayScreen
import org.moire.opensudoku.pageobject.onTitleScreen
import org.moire.opensudoku.rule.FixedTime
import org.moire.opensudoku.rule.applicationLaunchRule

@RunWith(AndroidJUnit4::class)
class TitleScreenTest {

	@get:Rule
	val rule = applicationLaunchRule()

	@Test
	fun startNewGameAndCheckIfItCanBeResumed() {
		onTitleScreen()
			.dismissChangelogDialog()
			.assertResumeButtonVisibility(false)
			.startNewRandomGame()
		onSudokuPlayScreen()
			.dismissWelcomeDialog()
			.dismissPopUpDialog()
			.assertSudokuBoardVisible()
			.goBack()
		onTitleScreen()
			.assertResumeButtonVisibility(true)
	}

	@Test
	fun goToFolderListScreen() {
		onTitleScreen()
			.dismissChangelogDialog()
			.navigateToFolderList()
		onFolderListScreen()
			.assertFolderListVisible()
	}

	@Test
	@FixedTime("2025-12-01T00:00:00")
	fun goToChristmasChallenge() {
		onTitleScreen()
			.dismissChangelogDialog()
			.startChristmasChallengeGame()
		onSudokuPlayScreen()
			.dismissWelcomeDialog()
			.dismissPopUpDialog()
			.assertSudokuBoardVisible()
	}

	@Test
	@FixedTime("2026-01-01T00:00:00")
	fun assertNoChristmasChallenge() {
		onTitleScreen()
			.dismissChangelogDialog()
			.assertNoChristmasChallenge()
	}

	@Test
	fun goToSettingsScreen() {
		onTitleScreen()
			.dismissChangelogDialog()
			.navigateToSettingsScreen()
		onSettingsScreen()
			.assertSettingsScreenVisible()
	}

	@Test
	fun goToAboutPage() {
		onTitleScreen()
			.dismissChangelogDialog()
			.clickOnMenu()
			.navigateToAboutDialog()
			.assertAboutDialogVisible()
	}
}
