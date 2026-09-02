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

package org.moire.opensudoku.gui.importing

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
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
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.DialogParams
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.screen.folder_list.FolderTaskViewModel
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListActivity
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI

private const val bundleKeyIsSuccess = "isSuccess"
private const val bundleKeyFolderId = "folderId"

/**
 * This activity is responsible for importing puzzles from various sources
 * (web, file, .opensudoku, .sdm, extras).
 */


class PuzzleImportActivity : ThemedActivity() {
	val viewModel: FolderTaskViewModel by viewModels()
	private lateinit var importFinishedDialog: SimpleDialog

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
		singleRunImportFromFile()
	}

	private fun registerDialogsListeners() {
		importFinishedDialog = SimpleDialog(DialogParams().apply {
			resultKey = "ImportFinished"
			titleId = R.string.import_title
		})
		importFinishedDialog.registerOnDismissCallback(this) { arguments ->
			val isSuccess = arguments!!.getBoolean(bundleKeyIsSuccess)
			val folderId = arguments.getLong(bundleKeyFolderId)
			onImportFinishedListener(isSuccess, folderId)
		}
	}

	private fun singleRunImportFromFile() {
		val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
		val titleTextView = findViewById<TextView?>(R.id.progress_introduction)
		val progressTextView = findViewById<TextView?>(R.id.progress_status_text)
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect {
					titleTextView?.text = if (it.folderName != null) { resources.getQuantityString(R.plurals.target_folders, 1, it.folderName) } else ""
					if (it.currentValue >= 0) {
						progressBar.isIndeterminate = false
						progressBar.max = it.maxValue
						progressBar.progress = it.currentValue

						var counts = it.currentValue.toString()
						if (it.maxValue > 0) {
							counts += "/" + it.maxValue.toString()
						}
						progressTextView?.text = resources.getQuantityString(R.plurals.puzzle_processed_status, it.currentValue, counts)
					} else {
						progressBar.isIndeterminate = true
						if (it.maxValue > 0) {
							progressTextView?.text = getString(R.string.found_n_puzzles, it.maxValue)
						} else {
							progressTextView?.text = getString(R.string.checking_duplicates)
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.taskFinished.collect {
					if (it.isFinished) onImportFinished(it.folderId, it.isSuccess, it.message)
				}
			}
		}

		val dataUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getParcelableExtra(uriExtraName, Uri::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getParcelableExtra(uriExtraName)
		}
		if (dataUri == null) {
			return
		}
		intent.removeExtra(uriExtraName) // make sure the import is not executed again on activity events like screen rotation

		// Read the import strategy to use from the settings
		val importStrategy = GameSettings(applicationContext).importStrategy
		val streamReader: InputStreamReader

		if (dataUri.scheme == "content") {
			try {
				streamReader = InputStreamReader(contentResolver.openInputStream(dataUri))
			} catch (e: FileNotFoundException) {
				Log.e(javaClass.simpleName, "Cannot read input", e)
				Toast.makeText(this, R.string.invalid_format, Toast.LENGTH_LONG).show()
				finish()
				return
			}
		} else {
			val juri: URI
			Log.v(javaClass.simpleName, "$dataUri")
			try {
				juri = URI(
					dataUri.scheme, dataUri.schemeSpecificPart, dataUri.fragment
				)
				streamReader = InputStreamReader(juri.toURL().openStream())
			} catch (e: Exception) {
				Log.e(javaClass.simpleName, "Cannot read input", e)
				Toast.makeText(this, R.string.invalid_format, Toast.LENGTH_LONG).show()
				finish()
				return
			}
		}

		val cBuffer = CharArray(512)
		val numberOfCharactersRead: Int
		try {
			// read first 512 bytes to check the type of file
			numberOfCharactersRead = streamReader.read(cBuffer, 0, 512)
			streamReader.close()
		} catch (e: IOException) {
			@Suppress("HardCodedStringLiteral")
			Log.e(javaClass.simpleName, "Cannot read input", e)
			Toast.makeText(this, R.string.invalid_format, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		if (numberOfCharactersRead < 81) {
			// At least one full 9x9 game needed in case of SDM
			@Suppress("HardCodedStringLiteral")
			Log.e(javaClass.simpleName, "Input data too small")
			Toast.makeText(this, R.string.invalid_format, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		val cBufferStr = String(cBuffer, 0, numberOfCharactersRead)

		@Suppress("RegExpSimplifiable", "HardCodedStringLiteral")
		val importTask: ImportTask = if (cBufferStr.contains("<opensudoku")) { // Recognized OpenSudoku file
			OpenSudokuImportTask(dataUri, importStrategy)
		} else if (cBufferStr.matches("""[.0-9\n\r]{$numberOfCharactersRead}""".toRegex())) { // Recognized Sudoku SDM file
			SdmImportTask(dataUri, importStrategy)
		} else if (SepbRegex.containsMatchIn(cBufferStr)) { // Recognized Sudoku Exchange "Puzzle Bank" file
			SepbImportTask(dataUri, importStrategy)
		} else {
			Log.e(javaClass.simpleName, "Unknown type of data provided (mime-type=${intent.type}; uri=$dataUri), exiting.")
			Toast.makeText(this, R.string.invalid_format, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		viewModel.init(applicationContext, importTask)
	}

	private fun onImportFinished(folderId: Long, isSuccess: Boolean, resultMessage: String) {
		importFinishedDialog.run {
			arguments = Bundle(2).apply {
				putBoolean(bundleKeyIsSuccess, isSuccess)
				putLong(bundleKeyFolderId, folderId)
			}
			show(supportFragmentManager, resultMessage)
		}
	}

	/**
	 * Occurs when import is finished.
	 *
	 * @param importSuccessful Indicates whether import was successful.
	 * @param folderId         Contains id of imported folder, or -1 if multiple folders were imported.
	 */
	private fun onImportFinishedListener(importSuccessful: Boolean, folderId: Long) {
		if (importSuccessful) {
			if (folderId != -1L) {
				// only one folder was imported, open this folder automatically
				val i = Intent(this@PuzzleImportActivity, PuzzleListActivity::class.java)
				i.putExtra(Tag.FOLDER_ID, folderId)
				startActivity(i)
			}
		}
		finish()
	}

	companion object {
		@Suppress("HardCodedStringLiteral")
		val uriExtraName = PuzzleImportActivity::class.qualifiedName + "uri"
		fun register(activity: ComponentActivity): ActivityResultLauncher<Intent> {
			return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == RESULT_OK && result.data?.data != null) {
					val uri = result.data?.data ?: return@registerForActivityResult
					val importActivity = Intent(activity, PuzzleImportActivity::class.java)
					importActivity.putExtra(uriExtraName, uri)
					activity.startActivity(importActivity)
				} else {
					@Suppress("HardCodedStringLiteral") Log.d(PuzzleImportActivity::class.simpleName, "No data provided, exiting.")
				}
			}
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		return super.onOptionsItemSelected(item)
	}
}
