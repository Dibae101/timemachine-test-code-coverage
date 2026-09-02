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

package org.moire.opensudoku.gui.rating

import android.os.Bundle
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.moire.opensudoku.R
import org.moire.opensudoku.db.ALL_IDS
import org.moire.opensudoku.gui.screen.folder_list.FolderTaskViewModel
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.DialogParams
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListRecyclerAdapter

class PuzzleRatingActivity : ThemedActivity() {
	private lateinit var puzzleRatingParams: PuzzleRatingTaskParams
	private lateinit var finishDialog: SimpleDialog
	val viewModel: FolderTaskViewModel by viewModels()

	// use of FolderTaskViewModel.ProgressUiState
	//	  folderName ... SUID of the puzzle
	//    maxValue ... count of the cells of the board
	//    currentValue ... count of the solved cells of the board
	//    corruptedCount ... not in use

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.progress)

		val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_layout)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			appBarLayout.updatePadding(
				left = systemBars.left,
				top = systemBars.top,
				right = systemBars.right
			)
			insets
		}

		registerDialogsListeners()

		singleRunPuzzleRating()
	}

	//@SuppressLint("SetTextI18n")
	private fun singleRunPuzzleRating() {
		val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
		val titleTextView = findViewById<TextView?>(R.id.progress_introduction)
		val progressTextView = findViewById<TextView?>(R.id.progress_status_text)
		//val corruptedCountTextView = findViewById<TextView?>(R.id.corrupted_games_found)

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect {
					titleTextView?.text = if (it.folderName != null) {
						getString(R.string.rating_puzzle_name, it.folderName)
					} else ""
					if (it.currentValue >= 0) {
						progressBar.isIndeterminate = false
						progressBar.max = it.maxValue
						progressBar.progress = it.currentValue
						progressTextView?.text = getString(R.string.cells_solved_of,
							it.currentValue.toString(), it.maxValue.toString())
					} else {
						progressBar.isIndeterminate = true
						if (it.maxValue > 0) {
							progressTextView?.text = getString(R.string.cells_solved_of,
								"?", it.maxValue.toString())
						} else {
							progressTextView?.text = getString(R.string.cells_solved_of,
								"?", "?")
						}
					}
					//if (it.corruptedCount > 0) {
					//	corruptedCountTextView?.apply {
					//		text = "not in use"
					//		visibility = View.VISIBLE
					//	}
					//}
				}
			}
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.taskFinished.collect {
					if (it.isFinished) {
						onRatingFinished(it.isSuccess, it.message)
					}
				}
			}
		}

		// get parameter puzzleId
		if (!intent.hasExtra(Tag.PUZZLE_ID)) return
		puzzleRatingParams = PuzzleRatingTaskParams()
		val puzzleId = intent.getLongExtra(Tag.PUZZLE_ID, ALL_IDS)
		puzzleRatingParams.puzzleId = puzzleId
		intent.removeExtra(Tag.PUZZLE_ID) // make sure rating will not execute again...

		val ratingTask = PuzzleRatingTask(puzzleRatingParams)
		viewModel.init(applicationContext, ratingTask)

	}

	private fun onRatingFinished(isSuccess: Boolean, resultMessage: String) {
		if (isSuccess) {
			finishDialog.show(supportFragmentManager, resultMessage)
		} else {
			finishDialog.show(supportFragmentManager, resultMessage)
		}
	}

	private fun registerDialogsListeners() {
		finishDialog = SimpleDialog(DialogParams().apply { title = getString(R.string.rating); resultKey = "?" })
		finishDialog.registerOnDismissCallback(this) { finish() }
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		return super.onOptionsItemSelected(item)
	}
}
