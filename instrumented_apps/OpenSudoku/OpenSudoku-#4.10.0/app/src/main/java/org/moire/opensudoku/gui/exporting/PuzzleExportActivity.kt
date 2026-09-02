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

package org.moire.opensudoku.gui.exporting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.DialogParams
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.screen.folder_list.FolderTaskViewModel
import org.moire.opensudoku.utils.getFileName
import java.io.FileNotFoundException
import java.util.Date

class PuzzleExportActivity : ThemedActivity() {
	private lateinit var exportParams: ExportTaskParams
	private lateinit var finishDialog: SimpleDialog
	val viewModel: FolderTaskViewModel by viewModels()

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

		singleRunExportToFile()
	}

	private fun singleRunExportToFile() {
		val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
		val titleTextView = findViewById<TextView?>(R.id.progress_introduction)
		val progressTextView = findViewById<TextView?>(R.id.progress_status_text)
		val corruptedCountTextView = findViewById<TextView?>(R.id.corrupted_games_found)
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect {
					titleTextView?.text = if (it.folderName != null) {
						getString(R.string.exporting, it.folderName)
					} else ""
					if (it.currentValue >= 0) {
						progressBar.isIndeterminate = false
						progressBar.max = it.maxValue
						progressBar.progress = it.currentValue
						progressTextView?.text = getString(R.string.exported_num, it.currentValue, it.maxValue)
					} else {
						progressBar.isIndeterminate = true
						if (it.maxValue > 0) {
							progressTextView?.text = getString(R.string.found_n_puzzles, it.maxValue)
						} else {
							progressTextView?.text = getString(R.string.checking_duplicates)
						}
					}
					if (it.corruptedCount > 0) {
						corruptedCountTextView?.apply {
							text = getString(R.string.corrupted_count, it.corruptedCount)
							visibility = View.VISIBLE
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.taskFinished.collect {
					if (it.isFinished) {
						onExportFinished(it.isSuccess, it.corruptedCount, it.message)
					}
				}
			}
		}

		if (!intent.hasExtra(Tag.FOLDER_ID)) {
			return
		}
		val folderId = intent.getLongExtra(Tag.FOLDER_ID, ALL_IDS)
		intent.removeExtra(Tag.FOLDER_ID) // make sure the export will not execute again on activity events like screen rotation

		exportParams = ExportTaskParams()
		exportParams.folderId = folderId
		val puzzleId = intent.getLongExtra(Tag.PUZZLE_ID, ALL_IDS)
		exportParams.puzzleId = puzzleId
		@Suppress("HardCodedStringLiteral")
		val timestamp = DateFormat.format("yyyy-MM-dd_HH-mm-ss", Date()).toString()

		val fileSelection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == RESULT_OK) {
				result.data?.data?.let(::startExportToFileTask)
			} else if (result.resultCode == RESULT_CANCELED) {
				finish()
			}
		}

		viewModel.initDb(true) { db ->
			val fileName = if (folderId == ALL_IDS) {
				@Suppress("HardCodedStringLiteral") "all-folders-$timestamp"
			} else {
				val folderName = db.getFolderInfo(folderId).let { folder ->
					@Suppress("HardCodedStringLiteral") require(folder != null) { "Folder with id $folderId not found, exiting." }
					folder.name
				}
				if (puzzleId != ALL_IDS) "$folderName-$puzzleId-$timestamp" else "$folderName-$timestamp"
			}

			fileSelection.launch(
				@Suppress("HardCodedStringLiteral")
				Intent(Intent.ACTION_CREATE_DOCUMENT)
					.addCategory(Intent.CATEGORY_OPENABLE)
					.setType("application/x-opensudoku")
					.putExtra(Intent.EXTRA_TITLE, "$fileName.opensudoku")
			)
		}
	}

	private fun onExportFinished(isSuccess: Boolean, corruptedCount: Int, resultMessage: String) {
		if (isSuccess == true) {
			if (corruptedCount > 0) {
				finishDialog.show(supportFragmentManager, getString(R.string.puzzles_have_been_exported_with_corrupted,corruptedCount))
			} else {
				finishDialog.show(supportFragmentManager, getString(R.string.puzzles_have_been_exported, resultMessage))
			}
		} else {
			finishDialog.show(supportFragmentManager, R.string.unknown_export_error)
		}
	}

	private fun registerDialogsListeners() {
		finishDialog = SimpleDialog(DialogParams().apply { resultKey = "PuzzleExportResult" })
		finishDialog.registerOnDismissCallback(this) { finish() }
	}

	private fun startExportToFileTask(uri: Uri) {
		try {
			exportParams.fileOutputStream = contentResolver.openOutputStream(uri)
			exportParams.filename = uri.getFileName(contentResolver)
		} catch (_: FileNotFoundException) {
			finishDialog.show(supportFragmentManager, R.string.unknown_export_error)
		}

		val exportTask = ExportTask(exportParams)
		viewModel.init(applicationContext, exportTask)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		return super.onOptionsItemSelected(item)
	}
}
