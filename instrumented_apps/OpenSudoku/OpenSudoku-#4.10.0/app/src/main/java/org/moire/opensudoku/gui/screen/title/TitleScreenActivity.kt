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

package org.moire.opensudoku.gui.screen.title

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.nextstep.StrategyIds
import org.moire.opensudoku.gui.Tag.IS_RANDOM_PUZZLE
import org.moire.opensudoku.gui.Tag.PUZZLE_ID
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.AboutDialogFragment
import org.moire.opensudoku.gui.fragments.DialogParams
import org.moire.opensudoku.gui.fragments.RandomPuzzleDialogFragment
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.screen.GameSettingsActivity
import org.moire.opensudoku.gui.screen.folder_list.FolderListActivity
import org.moire.opensudoku.gui.screen.game_play.SudokuPlayActivity
import org.moire.opensudoku.utils.Colors
import org.moire.opensudoku.utils.OptionsMenuItem
import org.moire.opensudoku.utils.addAll
import java.io.IOException
import java.util.Date

class TitleScreenActivity : ThemedActivity() {

	private lateinit var restoreOldCustomColorsDialog: SimpleDialog
	private val viewModel: TitleScreenViewModel by viewModels()

	private lateinit var resumeButton: Button
	private lateinit var randomUnsolvedButton: Button
	private lateinit var challengeButton: Button
	private lateinit var aboutDialog: AboutDialogFragment
	private lateinit var logFileSelection: ActivityResultLauncher<Intent>
	private lateinit var hintStrategiesDialog: SimpleDialog
	private var isInitialized = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_title_screen)

		val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)

		val content = findViewById<View>(R.id.title_screen_content)
		val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_layout)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
			val margin = (if (isLandscape) 16 else 30) * resources.displayMetrics.density.toInt()
			appBarLayout.updatePadding(
				left = systemBars.left,
				top = systemBars.top,
				right = systemBars.right
			)
			content.updatePadding(
				left = systemBars.left + margin,
				top = margin,
				right = systemBars.right + margin,
				bottom = systemBars.bottom + margin
			)
			insets
		}

		resumeButton = findViewById(R.id.resume_button)
		randomUnsolvedButton = findViewById(R.id.random_unsolved_button)
		challengeButton = findViewById(R.id.challenge_button)
		val puzzleListButton = findViewById<Button>(R.id.puzzle_lists_button)
		val settingsButton = findViewById<Button>(R.id.settings_button)
		puzzleListButton.setOnClickListener { startActivity(Intent(this, FolderListActivity::class.java)) }
		settingsButton.setOnClickListener { startActivity(Intent(this, GameSettingsActivity::class.java)) }
		aboutDialog = AboutDialogFragment()
		RandomPuzzleDialogFragment.init(this, GameSettings(applicationContext)) {
			playRandomPuzzle()
		}
		hintStrategiesDialog = SimpleDialog(DialogParams().apply {
			resultKey = "HintStrategies"
			iconId = R.drawable.ic_auto_fix
			titleId = R.string.hint_strategies_dialog_title
		})

		logFileSelection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == RESULT_OK) {
				result.data?.data?.let(::saveLogsToFile)
			} else if (result.resultCode == RESULT_CANCELED) {
				Toast.makeText(applicationContext, R.string.operation_cancelled, Toast.LENGTH_LONG).show()
			}
		}

		// check the preference to skip the title screen and launch the folder list activity directly
		if (savedInstanceState == null) {
			if (viewModel.shouldShowPuzzleListOnStartup()) {
				startActivity(Intent(this, FolderListActivity::class.java))
			} else { // show changelog on first run
				Changelog.showOnFirstRun(this)
			}
		}

		restoreOldCustomColorsDialog = SimpleDialog(DialogParams().apply {
			resultKey = "RestoreOldCustomColors"
			messageId = R.string.do_you_want_to_restore_the_old_colors
			positiveButtonString = R.string.yes
			negativeButtonString = R.string.no
			neutralButtonString = R.string.ask_me_later
		}).apply {
			registerPositiveButtonCallback(this@TitleScreenActivity) { viewModel.gameSettings.restoreAllAvailableOldCustomColors() }
			registerNegativeButtonCallback(this@TitleScreenActivity) { viewModel.gameSettings.removeOldCustomColorsSettings() }
		}

		viewModel.initScreen {
			isInitialized = true
			updateView()
			preferencesUpdates()
		}
	}

	private fun preferencesUpdates() {
		restoreOldSudokuColorsIfExist()
		viewModel.gameSettings.updateDoublePencilMarksSetting()
	}

	// shows either resume button or random unsolved puzzle button. but never both at a time.
	private fun setupResumeAndRandomButton() {
		val resumePuzzleID = viewModel.getResumePuzzle()
		if (resumePuzzleID != null) {
			resumeButton.visibility = View.VISIBLE
			resumeButton.setOnClickListener {
				val intentToPlay = Intent(this@TitleScreenActivity, SudokuPlayActivity::class.java)
				intentToPlay.putExtra(PUZZLE_ID, resumePuzzleID)
				startActivity(intentToPlay)
			}
		} else {
			resumeButton.visibility = View.GONE
		}

		randomUnsolvedButton.setOnClickListener {
			RandomPuzzleDialogFragment().show(supportFragmentManager)
		}
	}

	// shows either resume button or random unsolved puzzle button. but never both at a time.
	private fun playRandomPuzzle() {
		val randomPuzzleID = viewModel.getRandomPuzzle()
		if (randomPuzzleID == null) {
			SimpleDialog().show(supportFragmentManager, R.string.could_not_find_any_puzzle_matching_the_criteria)
			return
		}

		val intentToPlay = Intent(this@TitleScreenActivity, SudokuPlayActivity::class.java)
		intentToPlay.putExtra(PUZZLE_ID, randomPuzzleID)
		intentToPlay.putExtra(IS_RANDOM_PUZZLE, true)
		startActivity(intentToPlay)
	}

	private fun setupChallengeButton() {
		val puzzleID = viewModel.getChallengePuzzle()
		if (puzzleID != null) {
			challengeButton.text = getString(R.string.xmas_challenge)
			challengeButton.visibility = View.VISIBLE
			challengeButton.setOnClickListener {
				val intentToPlay = Intent(this@TitleScreenActivity, SudokuPlayActivity::class.java)
				intentToPlay.putExtra(PUZZLE_ID, puzzleID)
				startActivity(intentToPlay)
			}
		} else {
			challengeButton.visibility = View.GONE
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		if (!isInitialized) return false

		menu.addAll(OptionsMenuItems.entries)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			OptionsMenuItems.SETTINGS.id -> {
				startActivity(Intent(this, GameSettingsActivity::class.java))
				return true
			}

			OptionsMenuItems.SAVE_LOGS.id -> {
				saveLogs()
				return true
			}

			OptionsMenuItems.HINT_STRATEGIES.id -> {
				var message = ""
				val strategyNameList = StrategyIds.getStrategyNameListGroupByLevel(this)
				message += getString(R.string.hint_strategies_dialog_header,"${strategyNameList.size}")
				message += "\n\n"
				for (n in strategyNameList.indices) {
					message += "${strategyNameList[n]}\n"
				}
				hintStrategiesDialog.show(supportFragmentManager, message)
				return true
			}

			OptionsMenuItems.WHATS_NEW.id -> {
				Changelog().show(supportFragmentManager, Changelog::class.simpleName)
				return true
			}

			OptionsMenuItems.ABOUT.id -> {
				aboutDialog.show(supportFragmentManager, "AboutDialog")
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onResume() {
		super.onResume()
		if (isInitialized) updateView()
	}

	private fun updateView() {
		setupResumeAndRandomButton()
		setupChallengeButton()
		invalidateOptionsMenu()
	}

	private fun saveLogs() {
		val timestamp = DateFormat.format("yyyy-MM-dd_HH-mm-ss", Date()).toString()
		val fileName = "opensudoku-log-$timestamp"

		logFileSelection.launch(
			Intent(Intent.ACTION_CREATE_DOCUMENT)
				.addCategory(Intent.CATEGORY_OPENABLE)
				.setType("text/plain")
				.putExtra(Intent.EXTRA_TITLE, "$fileName.txt")
		)
	}

	private fun saveLogsToFile(uri: Uri) {
		val log = viewModel.getLogs()

		contentResolver.openOutputStream(uri)?.use { outputStream ->
			outputStream.write(log.toByteArray(Charsets.UTF_8))
		} ?: throw IOException("Unable to open URI for writing")
		Toast.makeText(applicationContext, R.string.logs_saved, Toast.LENGTH_LONG).show()
	}

	private fun restoreOldSudokuColorsIfExist() {
		if (!viewModel.gameSettings.hasOldCustomColorsSettings()) {
			return
		}

		if (viewModel.gameSettings.hasPreference(Colors.LINE.key(true)) ||
			viewModel.gameSettings.hasPreference(Colors.LINE.key(false)))
		{
			restoreOldCustomColorsDialog.show(supportFragmentManager)
		} else {
			viewModel.gameSettings.restoreAllAvailableOldCustomColors()
		}
	}

	companion object {
		enum class OptionsMenuItems(
			@field:StringRes override val titleRes: Int,
			@field:DrawableRes override val iconRes: Int,
			override val shortcutKey: Char
		) : OptionsMenuItem {
			SETTINGS(R.string.settings, R.drawable.ic_settings, 's'),
			SAVE_LOGS(R.string.save_logs, R.drawable.ic_save, 'l'),
			HINT_STRATEGIES(R.string.hint_strategies_menue_item, R.drawable.ic_auto_fix, 'h'),
			WHATS_NEW(R.string.whats_new, R.drawable.ic_info, 'w'),
			ABOUT(R.string.about, R.drawable.ic_info, 'a');

			override val id = ordinal + Menu.FIRST
			override val isAction: Boolean = false
		}
	}
}
