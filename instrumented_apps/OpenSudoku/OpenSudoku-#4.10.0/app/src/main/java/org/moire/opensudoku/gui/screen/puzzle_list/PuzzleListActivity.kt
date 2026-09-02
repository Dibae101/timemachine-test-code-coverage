/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2026 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.moire.opensudoku.gui.screen.puzzle_list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.gui.DbKeeperViewModel
import org.moire.opensudoku.gui.screen.folder_list.FolderListActivity
import org.moire.opensudoku.gui.screen.GameSettingsActivity
import org.moire.opensudoku.gui.screen.puzzle_edit.PuzzleEditActivity
import org.moire.opensudoku.gui.exporting.PuzzleExportActivity
import org.moire.opensudoku.gui.screen.game_play.SudokuPlayActivity
import org.moire.opensudoku.gui.screen.SudokuScanActivity
import org.moire.opensudoku.gui.SuidGenerator3F
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.CopyToClpDialogFragment
import org.moire.opensudoku.gui.fragments.DeletePuzzleDialogFragment
import org.moire.opensudoku.gui.fragments.EditUserNoteDialogFragment
import org.moire.opensudoku.gui.fragments.FilterDialogFragment
import org.moire.opensudoku.gui.fragments.ResetAllDialogFragment
import org.moire.opensudoku.gui.fragments.ResetPuzzleDialogFragment
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.fragments.SortDialogFragment
import org.moire.opensudoku.gui.rating.PuzzleRatingActivity
import org.moire.opensudoku.utils.ContextMenuItem
import org.moire.opensudoku.utils.OptionsMenuItem
import org.moire.opensudoku.utils.addAll

private const val DeletePuzzleID = "DeletePuzzleID"
private const val ResetPuzzleID = "ResetPuzzleID"
private const val EditUserNotePuzzleID = "EditUserNotePuzzleID"
private const val CopyToClpPuzzleID = "CopyToClpPuzzleID"

class PuzzleListActivity : ThemedActivity() {
	val viewModel: DbKeeperViewModel by viewModels()

	private lateinit var scanActivityResult: ActivityResultLauncher<Intent>
	private lateinit var filterStatus: TextView
	private lateinit var listFilter: PuzzleListFilter
	private lateinit var listSorter: PuzzleListSorter
	private lateinit var settings: GameSettings

	private var folderID: Long = 0
	private var recyclerView: RecyclerView? = null
	private var adapter: PuzzleListRecyclerAdapter? = null
	private var isInitialized = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.sudoku_list)

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
			findViewById<View>(R.id.puzzle_list_recycler).updatePadding(
				left = systemBars.left,
				right = systemBars.right,
				bottom = systemBars.bottom
			)
			insets
		}

		filterStatus = findViewById(R.id.filter_status)
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)

		@Suppress("HardCodedStringLiteral")
		require(intent.hasExtra(Tag.FOLDER_ID)) { "No '${Tag.FOLDER_ID}' extra provided, exiting." }
		folderID = intent.getLongExtra(Tag.FOLDER_ID, 0)

		settings = GameSettings(applicationContext)
		listFilter = PuzzleListFilter(this).apply {
			showStateNotStarted = settings.isShowNotStarted
			showStatePlaying = settings.isShowPlaying
			showStateCompleted = settings.isShowCompleted
			showStateWithMistakes = settings.isShowWithMistakes
			showStateWithHints = settings.isShowWithHints
		}

		listSorter = PuzzleListSorter().apply {
			sortType = settings.sortType
			isAscending = settings.isSortAscending
		}

		scanActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == RESULT_OK && result.data?.getStringExtra(Tag.CELL_COLLECTION) != null) {
				startActivity(
					Intent(this, PuzzleEditActivity::class.java)
						.setAction(Intent.ACTION_INSERT)
						.putExtra(Tag.FOLDER_ID, folderID)
						.putExtra(Tag.CELL_COLLECTION, result.data?.getStringExtra(Tag.CELL_COLLECTION))
				)
			}
		}

		updateFilterStatus()

		viewModel.initDb(true) { db ->
			updateTitle()

			val isShowTime = settings.isShowTime
			val isShowMistakesCount = settings.isShowMistakeCounter
			db.getPuzzleListCursorAsync(folderID, listFilter, listSorter.sortOrder) { puzzlesCursor ->
				runOnUiThread {
					adapter = PuzzleListRecyclerAdapter(
						this,
						puzzlesCursor,
						::playSudoku,
						isShowTime,
						isShowMistakesCount,
						settings.highlightWrongValues,
						db
					)
					recyclerView = findViewById<RecyclerView>(R.id.puzzle_list_recycler)?.apply {
						adapter = this@PuzzleListActivity.adapter
						layoutManager = LinearLayoutManager(this@PuzzleListActivity)
					}
					recyclerView?.let { registerForContextMenu(it) }

					registerDialogs(settings)
					isInitialized = true
					updateView()
				}
			}
		}
	}

	private fun registerDialogs(settings: GameSettings) {
		ResetPuzzleDialogFragment.setListener(this) {
			val puzzle = viewModel.db!!.getPuzzle(ResetPuzzleDialogFragment.puzzleID, false)!!
			puzzle.reset()
			viewModel.db!!.updatePuzzle(puzzle)
			updateList()
		}
		DeletePuzzleDialogFragment.setListener(this) {
			val mostRecentId = settings.lastGamePuzzleId
			if (DeletePuzzleDialogFragment.puzzleId == mostRecentId) {
				settings.lastGamePuzzleId = 0
			}
			viewModel.db!!.deletePuzzle(DeletePuzzleDialogFragment.puzzleId)
			updateList()
		}
		FilterDialogFragment.setListener(this) {
			settings.isShowNotStarted = FilterDialogFragment.listFilter.showStateNotStarted
			settings.isShowPlaying = FilterDialogFragment.listFilter.showStatePlaying
			settings.isShowCompleted = FilterDialogFragment.listFilter.showStateCompleted
			settings.isShowWithMistakes = FilterDialogFragment.listFilter.showStateWithMistakes
			settings.isShowWithHints = FilterDialogFragment.listFilter.showStateWithHints
			listFilter = FilterDialogFragment.listFilter
			updateList()
		}
		SortDialogFragment.setListener(this) { sortType, isAscending ->
			settings.sortType = sortType
			settings.isSortAscending = isAscending
			listSorter.sortType = sortType
			listSorter.isAscending = isAscending
			updateList()
		}
		ResetAllDialogFragment.setListener(this) {
			viewModel.db!!.resetAllPuzzles(folderID)
			updateList()
		}
		CopyToClpDialogFragment.setListener(this) { checkedItemIndex ->
			val serializedValues: String
			val puzzle = viewModel.db!!.getPuzzle(CopyToClpDialogFragment.puzzleID, false)!!
			val selectedItem = resources.getStringArray(R.array.puzzle_value_types)[checkedItemIndex]
			serializedValues = when (selectedItem) {
				getString(R.string.puzzle_value_current) -> puzzle.board.serialize(SudokuBoard.DATA_VERSION_PLAIN)
				getString(R.string.puzzle_value_suid) -> {
					if (puzzle.board.solutionCount == 0) {
						SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_no_solution)
						return@setListener
					} else if (puzzle.board.solutionCount > 1) {
						SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_multiple_solutions)
						return@setListener
					} else {
						getString(R.string.suid_prefix) + SuidGenerator3F().encode(puzzle.board)
					}
				}
				else -> puzzle.board.originalValues
			}
			val clipData = ClipData.newPlainText(getString(R.string.sudoku_puzzle), serializedValues)
			with(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager) {
				setPrimaryClip(clipData)
			}
			Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
		}
		EditUserNoteDialogFragment.setListener(this) { newNote ->
			val game = viewModel.db!!.getPuzzle(EditUserNoteDialogFragment.puzzleId, false)!!
			game.userNote = newNote
			viewModel.db!!.updatePuzzle(game)
			updateList()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		adapter?.close()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLong(DeletePuzzleID, DeletePuzzleDialogFragment.puzzleId)
		outState.putLong(ResetPuzzleID, ResetPuzzleDialogFragment.puzzleID)
		outState.putLong(EditUserNotePuzzleID, EditUserNoteDialogFragment.puzzleId)
		outState.putLong(CopyToClpPuzzleID, CopyToClpDialogFragment.puzzleID)
	}

	override fun onRestoreInstanceState(state: Bundle) {
		super.onRestoreInstanceState(state)
		DeletePuzzleDialogFragment.puzzleId = state.getLong(DeletePuzzleID)
		ResetPuzzleDialogFragment.puzzleID = state.getLong(ResetPuzzleID)
		EditUserNoteDialogFragment.puzzleId = state.getLong(EditUserNotePuzzleID)
		CopyToClpDialogFragment.puzzleID = state.getLong(CopyToClpPuzzleID)
	}

	override fun onResume() {
		super.onResume()
		if (isInitialized) updateView()
	}

	fun updateView() {
		updateList()
		invalidateOptionsMenu()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		// if there is no activity in history and back button was pressed, go
		// to FolderListActivity, which is the root activity.
		if (isTaskRoot && keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(Intent(this, FolderListActivity::class.java))
			finish()
			return true
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)

		val menuItems = OptionsMenuItems.entries.filter {
			it != OptionsMenuItems.SCAN || settings.isScanPuzzleEnabled
		}
		menu.addAll(menuItems)

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0,
			ComponentName(this, PuzzleListActivity::class.java), null,
			intent, 0, null
		)
		return true
	}

	override fun onContextItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			ContextMenuItems.EDIT.id -> {
				val i = Intent(this, PuzzleEditActivity::class.java)
                i.action = Intent.ACTION_EDIT
				i.putExtra(Tag.PUZZLE_ID, PuzzleListRecyclerAdapter.selectedGameId)
				startActivity(i)
				return true
			}

			ContextMenuItems.DELETE.id -> {
				DeletePuzzleDialogFragment.puzzleId = PuzzleListRecyclerAdapter.selectedGameId
				DeletePuzzleDialogFragment().show(supportFragmentManager, ContextMenuItems.DELETE.name)
				return true
			}

			ContextMenuItems.EDIT_USER_NOTE.id -> {
				EditUserNoteDialogFragment.puzzleId = PuzzleListRecyclerAdapter.selectedGameId
				EditUserNoteDialogFragment.currentValue = viewModel.db!!.getPuzzle(EditUserNoteDialogFragment.puzzleId, false)!!.userNote
				EditUserNoteDialogFragment().show(supportFragmentManager, ContextMenuItems.EDIT_USER_NOTE.name)
				return true
			}

			ContextMenuItems.RESET.id -> {
				ResetPuzzleDialogFragment.puzzleID = PuzzleListRecyclerAdapter.selectedGameId
				ResetPuzzleDialogFragment().show(supportFragmentManager, null)
				return true
			}

			ContextMenuItems.EXPORT_GAME.id -> {
				startActivity(
					Intent(this, PuzzleExportActivity::class.java)
						.putExtra(Tag.FOLDER_ID, folderID)
						.putExtra(Tag.PUZZLE_ID, PuzzleListRecyclerAdapter.selectedGameId),
				)
				return true
			}

			ContextMenuItems.COPY_TO_CLP.id -> {
				CopyToClpDialogFragment.puzzleID = PuzzleListRecyclerAdapter.selectedGameId
				CopyToClpDialogFragment().show(supportFragmentManager, ContextMenuItems.COPY_TO_CLP.name)
				return true
			}

			ContextMenuItems.RATING_PUZZLE.id -> {
				startActivity(
					Intent(this, PuzzleRatingActivity::class.java)
						.putExtra(Tag.PUZZLE_ID, PuzzleListRecyclerAdapter.selectedGameId),
				)
				return true
			}
		}
		return false
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		when (item.itemId) {
			OptionsMenuItems.INSERT.id -> { // Launch activity to insert a new item
				startActivity(
					Intent(this, PuzzleEditActivity::class.java)
						.setAction(Intent.ACTION_INSERT)
						.putExtra(Tag.FOLDER_ID, folderID)    // we need to know folder in which the new puzzle will be stored
				)
				return true
			}

			OptionsMenuItems.PASTE.id -> { // Launch activity to insert a new item
				val clipData = getClipboard()
				if (clipData == "") return true
				startActivity(
					Intent(this, PuzzleEditActivity::class.java)
						.setAction(Intent.ACTION_PASTE)
						.putExtra(Tag.FOLDER_ID, folderID)    // we need to know folder in which the new puzzle will be stored
						.putExtra(Tag.CELL_COLLECTION, clipData)
				)
				return true
			}

			OptionsMenuItems.SCAN.id -> { // Launch activity to insert a new item
				scanActivityResult.launch(
					Intent(this, SudokuScanActivity::class.java)
						.setAction(Intent.ACTION_INSERT)
						.putExtra(Tag.FOLDER_ID, folderID)    // we need to know folder in which the new puzzle will be stored
				)
				return true
			}

			OptionsMenuItems.SETTINGS.id -> {
				startActivity(Intent(this, GameSettingsActivity::class.java))
				return true
			}

			OptionsMenuItems.FILTER.id -> {
				FilterDialogFragment.listFilter = listFilter
				FilterDialogFragment().show(supportFragmentManager, "FilterDialog")
				return true
			}

			OptionsMenuItems.SORT.id -> {
				SortDialogFragment.listSorter = listSorter
				SortDialogFragment().show(supportFragmentManager, "SortDialog")
				return true
			}

			OptionsMenuItems.RESET_ALL.id -> {
				ResetAllDialogFragment().show(supportFragmentManager, null)
				return true
			}

			OptionsMenuItems.EXPORT_FOLDER.id -> {
				startActivity(
					Intent(this, PuzzleExportActivity::class.java)
						.putExtra(Tag.FOLDER_ID, folderID)
				)
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	/**
	 * Updates whole list.
	 */
	private fun updateList() {
		if (!isInitialized) return
		updateTitle()
		updateFilterStatus()
		val isShowTime = settings.isShowTime
		val isShowMistakesCount = settings.isShowMistakeCounter
		val wrongValuesHighlightMode = settings.highlightWrongValues
		viewModel.db!!.getPuzzleListCursorAsync(folderID, listFilter, listSorter.sortOrder) { cursor ->
			runOnUiThread {
				adapter!!.updateGameList(cursor, isShowTime, isShowMistakesCount, wrongValuesHighlightMode)
			}
		}
	}

	private fun updateFilterStatus() {
		if (listFilter.anyActive) {
			filterStatus.text = getString(R.string.filter_active, listFilter)
			filterStatus.visibility = View.VISIBLE
		} else {
			filterStatus.visibility = View.GONE
		}
	}

	private fun updateTitle() {
		val folder = viewModel.db!!.getFolderInfo(folderID)
		title = folder?.name ?: ""
		viewModel.db!!.getFolderInfoWithCountsAsync(folderID) { folderInfo ->
			runOnUiThread { title = folderInfo.name + ": " + folderInfo.getDetail(applicationContext) }
		}
	}

	private fun playSudoku(puzzleID: Long) {
		val i = Intent(this@PuzzleListActivity, SudokuPlayActivity::class.java)
		i.putExtra(Tag.PUZZLE_ID, puzzleID)
		startActivity(i)
	}

	companion object {
		enum class OptionsMenuItems(
			@StringRes override val titleRes: Int, @DrawableRes override val iconRes: Int, override val shortcutKey: Char
		) : OptionsMenuItem {
			SORT(R.string.sort, R.drawable.ic_sort, 's'),
			FILTER(R.string.filter, R.drawable.ic_view, 'f'),
			INSERT(R.string.add_puzzle, R.drawable.ic_add, 'i'),
			PASTE(android.R.string.paste, R.drawable.ic_add, 'p'),
			SCAN(R.string.scan_puzzle, R.drawable.ic_baseline_photo_camera, 'c'),
			RESET_ALL(R.string.reset_all_puzzles, R.drawable.ic_eraser, 'r'),
			EXPORT_FOLDER(R.string.export_folder, R.drawable.ic_share, 'e'),
			SETTINGS(R.string.settings, R.drawable.ic_settings, 'o');

			override val id = ordinal + Menu.FIRST
			override val isAction: Boolean = false
		}
	}

	enum class ContextMenuItems(@StringRes override val titleRes: Int) : ContextMenuItem {
		EDIT(R.string.edit_puzzle),
		DELETE(R.string.delete_puzzle),
		RESET(R.string.reset_puzzle),
		EDIT_USER_NOTE(R.string.edit_note),
		EXPORT_GAME(R.string.export),
		COPY_TO_CLP(R.string.copy_to_clp),
		RATING_PUZZLE(R.string.rating_puzzle);

		override val id = ordinal + Menu.FIRST
	}
}
