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

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.moire.opensudoku.gui.screen.folder_list.FolderTaskModel
import org.moire.opensudoku.R
import org.moire.opensudoku.db.SudokuDatabase
import org.moire.opensudoku.game.FolderInfo

/** IMPORT_STRATEGY
 *
 * The ImportStrategy controls following actions during the import process:
 * 	1. the creation of duplicate puzzles
 * 	2. the import of additional data like current playing status or historical data
 *
 * Following modes are available:
 *
 * 	- NoDup .. original behavior
 * 	          Create puzzle only if not exists in the database (in all folders) and update only
 * 	          if the puzzle to import is already started independent of the state of the existing puzzle.
 * 	 		- duplicate check
 * 			  - insert new puzzle only if it not exists in all folders
 * 			  - check is done by the original values of both puzzles
 * 	 	    - data
 * 	 		  - all data from the import will be used to insert / update the puzzle
 * 	 		- insert / update
 * 	 	      - insert if puzzle do not exists
 * 	 	      - update if the puzzle state of the puzzle to import is started
 * 	 	        - this will overwrite the existing puzzle independent of the state
 * 	 	    - uncleared
 *            - should the program check before update if the puzzle to import is relay "newer"
 *              than the existing puzzle using lastPlayed?
 *
 * 	- Clean .. import will takeover only the original values and set the state GAME_STATE_NOT_STARTED
 * 	           This import strategy should be used to have fresh puzzles from an export of puzzles
 * 	           with different state and history.
 * 	 		- duplicate check
 * 	 	      - create no duplicate puzzles inside a folder
 * 	 	      - a puzzle is duplicate when the original (given) values plus creation timestamp are
 * 	 	        equal to a puzzle in the folder
 * 	 	      - it is possible to more than one puzzle with the same original values but different
 * 	 	        creation timestamp
 * 	 	    - data
 * 	 		  - use method SudokuGame.reset() to clean data
 * 	 		  - only the creation timestamp and the original values will be imported
 * 	 		- insert / update
 * 	 	      - insert if puzzle do not exists
 * 	 	      - update if the puzzle state in the database is equal to GAME_STATE_NOT_STARTED
 * 	 	        - we will not overwrite (reset) playing activities of the user
 * 	 	    - limitations
 * 	 	      ...
 * 	 	    - uncleared
 * 	 	      ...
 *
 * 	- Sync .. import will takeover all data
 * 	          This import strategy should be used to sync / mirror puzzles from one source to another.
 * 	          All data in the destination app will be overwritten by the import data.
 * 	 		- duplicate check
 * 	 	      - create no duplicate puzzles inside a folder
 * 	 	      - a puzzle is duplicate when the original (given) values plus creation timestamp are
 * 	 	        equal to a puzzle in the folder
 * 	 	      - it is possible to more than one puzzle with the same original values but different
 * 	 	        creation timestamp
 * 	 	    - data
 * 	 		  - all data from the import will be used to insert / update the puzzle
 * 	 	    - limitations
 * 	 	      ...
 * 	 	    - uncleared
 * 	 	      ...
 *
 ** OpenSudoku_V1 / SDM / SEP
 * Due to the fact that the import do not have any information regarding notes, progress state
 * or history, the import will only insert (append) non existing puzzles.
 * The user note will be filled with the index of the puzzle in the import file.
 *
 */

enum class ImportStrategyTypes(val value: Int) {

	NoDup(0),
	Clean(1),
	Sync(2);

	/**
	 * This function gets the name from resources for the current type.
	 */
	fun getName(context: Context): String {
		val name = when (this) {
			NoDup -> context.resources.getString(R.string.import_strategy_no_duplicates_name)
			Clean -> context.resources.getString(R.string.import_strategy_clean_name)
			Sync -> context.resources.getString(R.string.import_strategy_sync_name)
		}
		return name
	}

	companion object {

		/**
		 * This function convert the key from settings to a type.
		 */
		fun convertKey2Type(key: String?): ImportStrategyTypes {
			return when (key) {
				"nodup" -> NoDup
				"clean" -> Clean
				"sync" -> Sync
				else -> NoDup
			}
		}
	}
}

/**
 * To add support for new import source, do following:
 *
 * 1) Subclass this class. Any input parameters specific for your import should be put in constructor of your class.
 * 2) In [.processImport] method process your data source (parse file or maybe download data from some other source) and save puzzles by calling [.importFolder]
 * and [.importGame] methods. Note that `importFolder` must be called first, otherwise `importGame` doesn't know where to put puzzles.
 * 3) Add code to [PuzzleImportActivity] which creates instance of your new class and passes it input parameters.
 */
abstract class ImportTask(var importStrategy: ImportStrategyTypes) : FolderTaskModel() {
	protected lateinit var database: SudokuDatabase
	protected lateinit var importError: String
	protected var importedCount = 0
	protected var duplicatesCount = 0
	protected var updatedCount = 0

	private lateinit var context: Context
	private lateinit var folder: FolderInfo // currently processed folder
	private var foldersUsed = HashSet<String>() // count of processed folders

	override suspend fun run(context: Context, onTaskFinished: (Long, Boolean, String) -> Unit) {
		this.context = context
		var isSuccess = false
		withContext(Dispatchers.IO) {
			try {
				isSuccess = processImportInternal()
			} catch (_: Exception) {
				importError = context.getString(R.string.unknown_import_error)
			}
			withContext(Dispatchers.Main) {
				onPostExecute(isSuccess, onTaskFinished)
			}
		}
	}

	private fun onPostExecute(isSuccess: Boolean, onImportFinished: (Long, Boolean, String) -> Unit) {
		var resultMessage = ""
		if (isSuccess) {
			if (foldersUsed.isNotEmpty()) resultMessage += context.resources.getQuantityString(
				R.plurals.target_folders,
				foldersUsed.size,
				foldersUsed.joinToString(", ")
			)
			if (importedCount > 0) resultMessage += "\n" + context.resources.getQuantityString(
				R.plurals.imported_new_puzzles_count,
				importedCount,
				importedCount
			)
			if (duplicatesCount > 0) resultMessage += "\n" + context.resources.getQuantityString(
				R.plurals.skipped_already_existing_puzzles_count,
				duplicatesCount,
				duplicatesCount
			)
			if (updatedCount > 0) resultMessage += "\n" + context.resources.getQuantityString(
				R.plurals.updated_existing_puzzles_with_saved_in_progress_games_count,
				updatedCount,
				updatedCount
			)
			resultMessage += "\n" + context.resources.getString(R.string.import_strategy_used, importStrategy.getName(context))
		} else {
			resultMessage = importError
		}

		val folderId = if (foldersUsed.size == 1) folder.id else -1
		onImportFinished(folderId, isSuccess, resultMessage)
	}

	private suspend fun processImportInternal(): Boolean {
		SudokuDatabase(context, false).use { database ->
			try {
				this.database = database
				processImport(context)  // let subclass handle the import
			} catch (_: Exception) {
				importError = context.getString(R.string.invalid_format)
			}
		}

		if (importedCount + duplicatesCount + updatedCount == 0) {
			importError = context.getString(R.string.no_puzzles_found)
			return false
		}

		return true
	}

	/**
	 * Subclasses should do all import work in this method.
	 */
	protected abstract suspend fun processImport(context: Context)

	class ImportFolderInfo(val name: String, val createdEpoch: Long = 0) {
		var id: Long = 0
		var isCreated = false
	}

	/**
	 * Gets or creates folder to append new puzzles to it.
	 *
	 * @return folder ID
	 */
	protected fun ensureFolder(folderInfo: ImportFolderInfo): Long {
		if (folderInfo.isCreated) {
			return folderInfo.id
		}
		folder = database.insertFolder(folderInfo.name, folderInfo.createdEpoch)
		foldersUsed.add(folder.name)
		folderInfo.id = folder.id
		folderInfo.isCreated = true

		return folderInfo.id
	}
}
