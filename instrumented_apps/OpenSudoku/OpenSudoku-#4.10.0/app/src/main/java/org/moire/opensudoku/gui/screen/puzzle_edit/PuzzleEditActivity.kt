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
package org.moire.opensudoku.gui.screen.puzzle_edit

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.moire.opensudoku.R
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.DialogParams
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.inputmethod.IMControlPanel
import org.moire.opensudoku.utils.OptionsMenuItem
import org.moire.opensudoku.utils.ThemeUtils
import org.moire.opensudoku.utils.addAll
import kotlin.getValue

/**
 * Activity for editing content of puzzle.
 */
class PuzzleEditActivity : ThemedActivity() {
	val viewModel: PuzzleEditViewModel by viewModels()

	private var saveAndFinishDialog: SimpleDialog? = null
	private var saveDialog: SimpleDialog? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.sudoku_edit)

		val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_layout)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout)) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			appBarLayout.updatePadding(
				left = systemBars.left,
				top = systemBars.top,
				right = systemBars.right
			)
			findViewById<View>(R.id.board_view).updatePadding(
				left = systemBars.left,
				right = systemBars.right
			)
			findViewById<View>(R.id.input_methods).updatePadding(
				left = systemBars.left,
				right = systemBars.right,
				bottom = systemBars.bottom
			)
			insets
		}

		val boardView = findViewById<SudokuBoardView>(R.id.board_view)

		if (!viewModel.initialized) {
			var puzzleId: Long = -1
			var folderId: Long = -1
			var board: SudokuBoard? = null
			when (intent.action) {
				Intent.ACTION_EDIT -> { // requested to edit, set that state, and the data being edited.
					puzzleId = intent.getLongExtra(Tag.PUZZLE_ID, -1L)
					@Suppress("HardCodedStringLiteral") require(puzzleId >= 0L) { "Extra with key PUZZLE_ID is required." }
				}

				Intent.ACTION_INSERT -> { // requested to create a new puzzle
					folderId = intent.getLongExtra(Tag.FOLDER_ID, -1L)
					@Suppress("HardCodedStringLiteral") require(folderId >= 0L) { "Extra with key FOLDER_ID is required." }
					intent.getStringExtra(Tag.CELL_COLLECTION)?.let { collectionStr ->
						val (newBoard, _) = SudokuBoard.deserialize(collectionStr, true)
						board = newBoard
					}
				}

				Intent.ACTION_PASTE -> { // requested to create a new puzzle
					folderId = intent.getLongExtra(Tag.FOLDER_ID, -1L)
					@Suppress("HardCodedStringLiteral") require(folderId >= 0L) { "Extra with key FOLDER_ID is required." }
					intent.getStringExtra(Tag.CELL_COLLECTION)!!.let { collectionStr ->
						board = deserialize(collectionStr)
					}
					if (board == null) {
						finish()
					}
				}

				else -> {
					@Suppress("HardCodedStringLiteral") Log.e(javaClass.simpleName, "Unknown action ${intent.action}, exiting.")
					finish()
					return
				}
			}

			viewModel.init(puzzleId, folderId, board)
		}

		// only SelectOnTap input method will be enabled
		val inputMethodsView = findViewById<IMControlPanel>(R.id.input_methods)
		inputMethodsView.isEnabled = false

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { newState ->
					newState.newPuzzle?.let { game ->
						boardView.setGame(game)
						inputMethodsView.initialize(boardView, game, null)
						inputMethodsView.inputMethods.forEach { it.isEnabled = false }
						inputMethodsView.imSelectOnTap.isEnabled = true
						inputMethodsView.activateInputMethod(IMControlPanel.INPUT_METHOD_SELECT_ON_TAP)
						inputMethodsView.isEnabled = true
					}
				}
			}
		}

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				showSaveDialog(true)
			}
		})

		registerDialogsListeners()
		ThemeUtils.applyConfiguredThemeToSudokuBoardView(boardView, this@PuzzleEditActivity)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menu.addAll(OptionsMenuItems.entries)

		// Generate any additional actions that can be performed on the overall list.  In a normal install, there are no additional
		// actions found here, but this allows other applications to extend our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0, ComponentName(this, PuzzleEditActivity::class.java), null, intent, 0, null
		)
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		super.onPrepareOptionsMenu(menu)

		val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
		if (!clipboard.hasPrimaryClip()) {
			// If the clipboard doesn't contain data, disable the paste menu item.
			menu.findItem(OptionsMenuItems.PASTE.id)?.isEnabled = false
		} else if (!(clipboard.primaryClipDescription!!.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clipboard.primaryClipDescription!!.hasMimeType(
				ClipDescription.MIMETYPE_TEXT_HTML
			))
		) {
			// This disables the paste menu item, since the clipboard has data but it is not plain text
			menu.findItem(OptionsMenuItems.PASTE.id)?.isEnabled = false
		} else {
			// This enables the paste menu item, since the clipboard contains plain text.
			menu.findItem(OptionsMenuItems.PASTE.id)?.isEnabled = true
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		when (item.itemId) {
			OptionsMenuItems.COPY.id -> {
				copyToClipboard()
				return true
			}

			OptionsMenuItems.PASTE.id -> {
				deserialize(getClipboard())?.let { board -> viewModel.setCells(board.cells) }
				return true
			}

			OptionsMenuItems.CHECK_VALIDITY.id -> {
				when (viewModel.solutionCount) {
					1 -> {
						SimpleDialog().show(supportFragmentManager, R.string.puzzle_solvable)
					}

					0 -> {
						SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_no_solution)
					}

					else -> {
						SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_multiple_solutions)
					}
				}
				return true
			}

			OptionsMenuItems.SAVE.id -> {
				showSaveDialog(false)
				return true
			}

			OptionsMenuItems.DISCARD.id -> {
				finish()
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	private fun registerDialogsListeners() {
		saveDialog = SimpleDialog(DialogParams().apply {
			resultKey = "PuzzleEditSaveDialog"
			positiveButtonString = R.string.save
		}).apply {
			registerPositiveButtonCallback(this@PuzzleEditActivity) {
				savePuzzle()
			}
		}

		saveAndFinishDialog = SimpleDialog(DialogParams().apply {
			resultKey = "PuzzleEditSaveAndFinish"
			positiveButtonString = R.string.save
			negativeButtonString = R.string.discard
		}).apply {
			registerPositiveButtonCallback(this@PuzzleEditActivity) {
				savePuzzle()
				finish()
			}
			registerNegativeButtonCallback(this@PuzzleEditActivity, ::finish)
		}
	}

	private fun savePuzzle() {
		if (viewModel.savePuzzle()) {
			Toast.makeText(applicationContext, R.string.puzzle_created, Toast.LENGTH_SHORT).show()
		} else {
			Toast.makeText(applicationContext, R.string.puzzle_updated, Toast.LENGTH_SHORT).show()
		}
	}


	internal fun showSaveDialog(shouldFinish: Boolean) {
		if (!viewModel.isModified()) {
			Toast.makeText(applicationContext, getString(R.string.puzzle_already_saved), Toast.LENGTH_SHORT).show()
			if (shouldFinish) finish()
			return
		}

		val dialog = (if (shouldFinish) saveAndFinishDialog else saveDialog)!!

		// check number of solutions
		viewModel.solutionCount.let { numberOfSolutions ->
			if (numberOfSolutions == 0) {
				dialog.show(
					supportFragmentManager, getString(R.string.puzzle_has_no_solution) + "\n"
							+ getString(R.string.do_you_want_to_save_anyway)
				)
				return
			} else if (numberOfSolutions > 1) {
				dialog.show(
					supportFragmentManager, getString(R.string.puzzle_has_multiple_solutions) + "\n"
							+ getString(R.string.do_you_want_to_save_anyway)
				)
				return
			}
		}

		// ask for confirmation if this new or modified puzzle already exists elsewhere
		val existingPuzzleFolderName = viewModel.getExistingPuzzleFolderName()
		if (existingPuzzleFolderName != null) {
			dialog.show(supportFragmentManager, getString(R.string.puzzle_already_exists, existingPuzzleFolderName) + "\n"
					+ getString(R.string.do_you_want_to_save_anyway))
			return
		}

		if (shouldFinish) {
			dialog.show(supportFragmentManager, R.string.do_you_want_to_save)
			return
		}

		// no dialog necessary, toast message will confirm the action
		savePuzzle()
	}

	/**
	 * Copies puzzle to primary clipboard in a plain text format (81 character string).
	 *
	 * @see SudokuBoard.serialize
	 */
	private fun copyToClipboard() {
		val clipData = ClipData.newPlainText(getString(R.string.sudoku_puzzle), viewModel.serializedCells)
		val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
		clipboard.setPrimaryClip(clipData)
		Toast.makeText(applicationContext, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
	}

	private fun deserialize(serialized: String): SudokuBoard? {
		val board = viewModel.deserialize(serialized)
		if (board != null){
			Toast.makeText(applicationContext, R.string.pasted_from_clipboard, Toast.LENGTH_SHORT).show()
		} else {
			Toast.makeText(applicationContext, R.string.invalid_puzzle_format, Toast.LENGTH_LONG).show()
		}
		return board
	}

	companion object {
		enum class OptionsMenuItems(
			@param:StringRes override val titleRes: Int, @param:DrawableRes override val iconRes: Int = 0, override val shortcutKey: Char = Char(0)
		) : OptionsMenuItem {
			COPY(android.R.string.copy),
			PASTE(android.R.string.paste),
			CHECK_VALIDITY(R.string.check_validity),
			SAVE(R.string.save, R.drawable.ic_save, 's'),
			DISCARD(R.string.discard, R.drawable.ic_close, 'd');

			override val id = ordinal + Menu.FIRST
			override val isAction: Boolean = false
		}
	}
}
