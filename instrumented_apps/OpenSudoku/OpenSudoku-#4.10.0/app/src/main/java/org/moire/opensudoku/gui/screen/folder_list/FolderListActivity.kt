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
package org.moire.opensudoku.gui.screen.folder_list

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.moire.opensudoku.R
import org.moire.opensudoku.db.ALL_IDS
import org.moire.opensudoku.gui.fragments.AboutDialogFragment
import org.moire.opensudoku.gui.fragments.AddFolderDialogFragment
import org.moire.opensudoku.gui.fragments.DeleteFolderDialogFragment
import org.moire.opensudoku.gui.fragments.RenameFolderDialogFragment
import org.moire.opensudoku.gui.fragments.RenameFolderDialogFragment.Companion.renameFolderID
import org.moire.opensudoku.gui.fragments.RenameFolderDialogFragment.Companion.renameFolderName
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.utils.ContextMenuItem
import org.moire.opensudoku.utils.OptionsMenuItem
import org.moire.opensudoku.utils.addAll
import kotlin.getValue
import androidx.core.net.toUri
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.gui.screen.title.Changelog
import org.moire.opensudoku.gui.DbKeeperViewModel
import org.moire.opensudoku.gui.screen.GameSettingsActivity
import org.moire.opensudoku.gui.exporting.PuzzleExportActivity
import org.moire.opensudoku.gui.importing.PuzzleImportActivity
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.FolderSortDialogFragment
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListActivity

const val storagePermissionCode = 1

/**
 * List of puzzle's folder. This activity also serves as root activity of application.
 */
class FolderListActivity : ThemedActivity() {
	val viewModel: DbKeeperViewModel by viewModels()
	var isInitialized = false
	private lateinit var importLauncher: ActivityResultLauncher<Intent>
	private lateinit var aboutDialog: AboutDialogFragment
	private lateinit var adapter: FolderListRecyclerAdapter
	private lateinit var recyclerView: RecyclerView
	private lateinit var menu: Menu
	private lateinit var listSorter: FolderListSorter


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.folder_list)

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
			findViewById<View>(R.id.folder_list_recycler).updatePadding(
				left = systemBars.left,
				right = systemBars.right,
				bottom = systemBars.bottom
			)
			insets
		}

		setTitle(R.string.folders)
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)
		findViewById<View>(R.id.get_more_puzzles_from_os1).setOnClickListener { openWebpage("https://opensudoku.moire.org/") }
		findViewById<View>(R.id.get_more_puzzles_from_sepb).setOnClickListener { openWebpage("https://github.com/grantm/sudoku-exchange-puzzle-bank") }
		findViewById<View>(R.id.get_more_puzzles_from_sudocue).setOnClickListener { openWebpage("https://www.sudocue.net/download.php") }

		val progressBar = findViewById<ProgressBar>(R.id.progress_spinner)
		progressBar.visibility = View.VISIBLE

		val settings = GameSettings(applicationContext)
		listSorter = FolderListSorter().apply {
			sortType = settings.sortType
			isAscending = settings.isSortAscending
		}

		adapter = FolderListRecyclerAdapter(this) { id: Long ->
			val i = Intent(applicationContext, PuzzleListActivity::class.java)
			i.putExtra(Tag.FOLDER_ID, id)
			startActivity(i)
		}

		recyclerView = findViewById(R.id.folder_list_recycler)
		recyclerView.adapter = adapter
		recyclerView.layoutManager = LinearLayoutManager(this)
		registerForContextMenu(recyclerView)

		aboutDialog = AboutDialogFragment()

		importLauncher = PuzzleImportActivity.Companion.register(this)

		viewModel.initDb(true) { db ->
			AddFolderDialogFragment.setListener(this@FolderListActivity) { newFolderName ->
				db.insertFolder(newFolderName, 0)
				updateList()
			}
			DeleteFolderDialogFragment.setListener(this@FolderListActivity) {
				db.deleteFolder(DeleteFolderDialogFragment.deleteFolderID)
				updateList()
			}
			RenameFolderDialogFragment.setListener(this@FolderListActivity) { newName ->
				if (db.folderExists(newName)) {
					SimpleDialog().show(supportFragmentManager, R.string.folder_already_exists)
				} else {
					db.renameFolder(renameFolderID, newName)
					updateList()
				}
			}

			registerDialogs(settings)

			isInitialized = true
			updateView()
			progressBar.visibility = View.GONE
		}

		// show changelog on first run (case when auto folder opening is enabled and a new app version was released)
		Changelog.Companion.showOnFirstRun(this)
	}

	private fun registerDialogs(settings: GameSettings) {
		FolderSortDialogFragment.setListener(this) { sortType, isAscending ->
			settings.sortType = sortType
			settings.isSortAscending = isAscending
			listSorter.sortType = sortType
			listSorter.isAscending = isAscending
			updateList()
		}
	}

	@SuppressLint("UnsafeImplicitIntentLaunch")
	private fun openWebpage(url: String) {
		val webpage = url.toUri()
		try {
			startActivity(Intent(Intent.ACTION_VIEW, webpage))
		} catch (_: ActivityNotFoundException) {
			val hintClosed = DialogInterface.OnClickListener { _: DialogInterface?, _: Int -> }
			AlertDialog.Builder(this)
				.setIcon(R.drawable.ic_error)
				.setTitle(R.string.error)
				.setMessage(getString(R.string.cannot_start_web_activity_please_open_the_webpage_manually, webpage))
				.setPositiveButton(R.string.close, hintClosed)
				.create()
				.show()
		}
	}

	override fun onResume() {
		super.onResume()
		if (isInitialized) updateView()
	}

	fun updateView() {
		updateList()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)

		menu.addAll(OptionsMenuItems.entries)

		// Generate any additional actions that can be performed on the
		// overall list.  In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0, ComponentName(this, FolderListActivity::class.java), null, intent, 0, null
		)
		this.menu = menu
		return true
	}

	override fun onContextItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			ContextMenuItems.EXPORT.id -> {
				startActivity(
					Intent(this, PuzzleExportActivity::class.java)
						.putExtra(Tag.FOLDER_ID, adapter.selectedFolderId)
				)
				return true
			}

			ContextMenuItems.RENAME.id -> {
				renameFolderID = adapter.selectedFolderId
				renameFolderName = viewModel.db!!.getFolderInfo(renameFolderID)?.name ?: return false
				RenameFolderDialogFragment().show(supportFragmentManager, "")
				return true
			}

			ContextMenuItems.DELETE.id -> {
				DeleteFolderDialogFragment.deleteFolderID = adapter.selectedFolderId
				DeleteFolderDialogFragment.folderName = viewModel.db!!.getFolderInfo(adapter.selectedFolderId)?.name ?: return false
				DeleteFolderDialogFragment().show(supportFragmentManager, "")
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
			OptionsMenuItems.ADD.id -> {
				AddFolderDialogFragment().show(supportFragmentManager, "")
				return true
			}

			OptionsMenuItems.IMPORT.id -> {
				importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"))
				return true
			}

			OptionsMenuItems.INSERT.id -> { // Launch activity to insert a new item
				SimpleDialog().show(supportFragmentManager, getString(R.string.in_order_to_create_your_own_puzzle))
				return true
			}

			OptionsMenuItems.EXPORT_ALL.id -> {
				startActivity(
					Intent(this, PuzzleExportActivity::class.java).putExtra(Tag.FOLDER_ID, ALL_IDS)
				)
				return true
			}

			OptionsMenuItems.SETTINGS.id -> {
				startActivity(Intent(this, GameSettingsActivity::class.java))
				return true
			}

			OptionsMenuItems.ABOUT.id -> {
				aboutDialog.show(supportFragmentManager, "AboutDialog")
				return true
			}

			OptionsMenuItems.SORT.id -> {
				FolderSortDialogFragment.listSorter = listSorter
				FolderSortDialogFragment().show(supportFragmentManager, "SortDialog")
				return true
			}

		}
		return super.onOptionsItemSelected(item)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (requestCode == storagePermissionCode) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				onOptionsItemSelected(menu.findItem(OptionsMenuItems.IMPORT.id))
			} else {
				Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}

	private fun updateList() {
		viewModel.db!!.getFolderListWithCountsAsync({ detailedFolderList ->
			runOnUiThread { adapter.updateFoldersList(detailedFolderList) }
		}, listSorter.sortOrder)
	}

	companion object {
		enum class ContextMenuItems(@StringRes override val titleRes: Int) : ContextMenuItem {
			EXPORT(R.string.export_folder),
			RENAME(R.string.rename_folder),
			DELETE(R.string.delete_folder);

			override val id = ordinal + Menu.FIRST
		}

		enum class OptionsMenuItems(
			@StringRes override val titleRes: Int, @DrawableRes override val iconRes: Int, override val shortcutKey: Char
		) : OptionsMenuItem {
			SORT(R.string.sort, R.drawable.ic_sort, 'o'),
			ADD(R.string.add_folder, R.drawable.ic_add, 'a'),
			EXPORT_ALL(R.string.export_all_folders, R.drawable.ic_share, 'e'),
			IMPORT(R.string.import_title, R.drawable.ic_baseline_download, 'i'),
			INSERT(R.string.add_puzzle, R.drawable.ic_add, 'n'),
			SETTINGS(R.string.settings, R.drawable.ic_settings, 's'),
			ABOUT(R.string.about, R.drawable.ic_info, 'b');

			override val id = ordinal + Menu.FIRST
			override val isAction: Boolean = false
		}
	}
}
