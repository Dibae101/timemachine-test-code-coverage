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

@file:Suppress("HardCodedStringLiteral")

package org.moire.opensudoku.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log
import androidx.core.database.getIntOrNull
import org.moire.opensudoku.game.FolderInfo
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListFilter
import org.moire.opensudoku.gui.screen.title.RandomPuzzleFilter
import java.io.Closeable
import java.time.Instant
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Wrapper around OpenSudoku's database.
 * Should not call the constructor from the application main thread as it may block it until DB is opened.
 * You have to close connection when you're done with the database.
 */
class SudokuDatabase(context: Context, readOnly: Boolean) : Closeable {
	private val openHelper: DatabaseHelper = DatabaseHelper.getInstance(context)
	private val db: SQLiteDatabase = if (readOnly) openHelper.readableDatabase else openHelper.writableDatabase
	private var executorService: ExecutorService? = Executors.newSingleThreadExecutor()

	// FOLDER METHODS

	/**
	 * Returns list of puzzle folders.
	 */
	fun getFolderList(withCounts: Boolean = false, sortOrder: String? = null): List<FolderInfo> {
		val qb = SQLiteQueryBuilder()
		val folderList: MutableList<FolderInfo> = LinkedList()
		qb.tables = FOLDERS_TABLE_NAME
		qb.query(db, null, null, null, null, null, sortOrder).forEach { cursor ->
			val folderInfo = cursor.folderInfo
			if (withCounts) {
				folderInfo.apply { supplyCounts(this) }
			}
			folderList.add(folderInfo)
		}
		return folderList
	}

	fun getFolderListWithCountsAsync(loadedCallback: (List<FolderInfo>) -> Unit, sortOrder: String? = null) {
		executorService?.execute {
			try {
				val folderInfo = getFolderList(true, sortOrder)
				loadedCallback(folderInfo)
			} catch (e: Exception) {    // this is unimportant, we can log an error and continue
				Log.e(javaClass.simpleName, "Error occurred while loading folder list with info.", e)
			}
		}
	}

	/**
	 * Returns the folder info.
	 *
	 * @param folderId Primary key of folder.
	 */
	fun getFolderInfo(folderId: Long): FolderInfo? {
		with(SQLiteQueryBuilder()) {
			tables = FOLDERS_TABLE_NAME
			query(db, null,  "${FoldersColumn.ID}=$folderId", null, null, null, null).use { cursor ->
				return@getFolderInfo if (cursor.moveToFirst()) cursor.folderInfo else null
			}
		}
	}

	/**
	 * Returns the folder info.
	 *
	 * @param folderName Name of the folder to get info.
	 */
	private fun getFolderInfo(folderName: String): FolderInfo? {
		with(SQLiteQueryBuilder()) {
			tables = FOLDERS_TABLE_NAME
			query(db, null, "${FoldersColumn.NAME}=?", arrayOf(folderName), null, null, null).use { cursor ->
				return@getFolderInfo if (cursor.moveToFirst()) cursor.folderInfo else null
			}
		}
	}

	/**
	 * Returns the full folder info - this includes count of games in particular states.
	 *
	 * @param folderId Primary key of folder.
	 * @return folder info
	 */
	fun getFolderInfoWithCounts(folderId: Long): FolderInfo {
		return getFolderInfo(folderId)!!.apply { supplyCounts(this) }
	}

	private fun supplyCounts(folder: FolderInfo) {
		val q = "SELECT ${PuzzlesColumn.STATE}, COUNT(*) FROM $PUZZLES_TABLE_NAME WHERE ${PuzzlesColumn.FOLDER_ID} = ${folder.id} GROUP BY 1"
		db.rawQuery(q, null).use { cursor ->
			while (cursor.moveToNext()) {
				val state = cursor.getInt(0)
				val count = cursor.getInt(1)
				folder.puzzleCount += count
				if (state == SudokuGame.GAME_STATE_COMPLETED) {
					folder.solvedCount = count
				} else if (state == SudokuGame.GAME_STATE_PLAYING) {
					folder.playingCount = count
				}
			}
		}
	}

	fun getFolderInfoWithCountsAsync(folderId: Long, loadedCallback: (FolderInfo) -> Unit) {
		executorService?.execute {
			try {
				val folderInfo = getFolderInfoWithCounts(folderId)
				loadedCallback(folderInfo)
			} catch (e: Exception) {    // this is unimportant, we can log an error and continue
				Log.e(javaClass.simpleName, "Error occurred while loading full folder info.", e)
			}
		}
	}

	/**
	 * Returns list of sudoku game objects asynchronously
	 *
	 * @param folderId Primary key of folder.
	 */
	fun getPuzzleListCursorAsync(
		folderId: Long = 0L,
		filter: PuzzleListFilter? = null,
		sortOrder: String? = null,
		loadedCallback: (Cursor) -> Unit
	) {
		executorService?.execute {
			val cursor = getPuzzleListCursor(folderId, filter, sortOrder)
			// Access count to trigger I/O on background thread
			cursor.count
			loadedCallback(cursor)
		}
	}

	/**
	 * Inserts new puzzle folder into the database.
	 *
	 * @param name    Name of the folder.
	 * @param createdOr0 Time of folder creation.
	 */
	fun insertFolder(name: String, createdOr0: Long): FolderInfo {
		val existingFolder = getFolderInfo(name)
		if (existingFolder != null) {
			return existingFolder
		}
		val created = if (createdOr0 > 0) createdOr0 else Instant.now().toEpochMilli()
		val values = ContentValues()
		values.put(FoldersColumn.CREATED.nme, created)
		values.put(FoldersColumn.NAME.nme, name)
		val rowId = db.insert(FOLDERS_TABLE_NAME, FoldersColumn.ID.nme, values)
		if (rowId < 0) {
			throw SQLException("Failed to insert folder '$name'.")
		}
		return FolderInfo(rowId, name, created)
	}

	/**
	 * Renames existing folder.
	 *
	 * @param folderId Primary key of folder.
	 * @param name     New name for the folder.
	 */
	fun renameFolder(folderId: Long, name: String) {
		val values = ContentValues()
		values.put(FoldersColumn.NAME.nme, name)
		db.update(FOLDERS_TABLE_NAME, values, "${FoldersColumn.ID}=$folderId", null)
	}

	/**
	 * Deletes given folder.
	 *
	 * @param folderId Primary key of folder.
	 */
	fun deleteFolder(folderId: Long) {
		// delete all puzzles in folder we are going to delete
		db.delete(PUZZLES_TABLE_NAME, "${PuzzlesColumn.FOLDER_ID}=$folderId", null)
		// delete the folder
		db.delete(FOLDERS_TABLE_NAME, "${FoldersColumn.ID}=$folderId", null)
	}

	// PUZZLE METHODS

	/**
	 * Deletes given puzzle from the database.
	 */
	fun deletePuzzle(puzzleID: Long) {
		db.delete(PUZZLES_TABLE_NAME, "${PuzzlesColumn.ID}=$puzzleID", null)
	}

	fun findPuzzle(originalValues: String, isEditMode: Boolean): SudokuGame? {
		SQLiteQueryBuilder().run {
			tables = PUZZLES_TABLE_NAME
			query(db, null, "${PuzzlesColumn.ORIGINAL_VALUES}=?", arrayOf(originalValues), null, null, null)
				.use { cursor -> if (cursor.moveToFirst()) return@findPuzzle extractSudokuGameFromCursorRow(cursor, isEditMode) }
		}
		return null
	}

	/**
	 * Returns sudoku game object.
	 *
	 * @param gameID Primary key of folder.
	 */
	@Suppress("SameReturnValue")
	internal fun getPuzzle(gameID: Long, isEditMode: Boolean): SudokuGame? {
		SQLiteQueryBuilder().run {
			tables = PUZZLES_TABLE_NAME
			query(db, null, "${PuzzlesColumn.ID}=$gameID", null, null, null, null)
				.use { cursor -> if (cursor.moveToFirst()) return@getPuzzle extractSudokuGameFromCursorRow(cursor, isEditMode) }
		}
		return null
	}

	/**
	 * Returns a random sudoku puzzle ID.
	 *
	 * @param filter Return only puzzles matching the filter.
	 */
	@Suppress("SameReturnValue", "HardCodedStringLiteral")
	internal fun getRandomPuzzleID(filter: RandomPuzzleFilter): Long? {
		var where = "1=1"
		if (filter.unsolved) {
			where += " AND ${PuzzlesColumn.STATE} != ${SudokuGame.GAME_STATE_COMPLETED}"
		}
		if (filter.withMistakes) {
			where += " AND ${PuzzlesColumn.STATE} != ${SudokuGame.GAME_STATE_NOT_STARTED} AND (${PuzzlesColumn.MISTAKE_COUNTER} IS NULL OR ${PuzzlesColumn.MISTAKE_COUNTER} > 0)"
		}
		if (filter.withHints) {
			where += " AND ${PuzzlesColumn.STATE} != ${SudokuGame.GAME_STATE_NOT_STARTED} AND (${PuzzlesColumn.HINT_USAGE} IS NULL OR ${PuzzlesColumn.HINT_USAGE} > 0)"
		}

		val query = "SELECT ${PuzzlesColumn.ID} FROM $PUZZLES_TABLE_NAME WHERE $where ORDER BY RANDOM() LIMIT 1"
		db.rawQuery(query, null).use { cursor ->
			if (cursor.moveToFirst()) {
				return@getRandomPuzzleID cursor.getLong(0)
			}
		}
		return null
	}

	internal fun insertPuzzle(newGame: SudokuGame): Long {
		val rowId = db.insert(PUZZLES_TABLE_NAME, null, newGame.contentValues)
		if (rowId < 0) {
			throw SQLException("Failed to insert puzzle.")
		}
		return rowId
	}

	internal fun updatePuzzle(game: SudokuGame): Int = db.update(PUZZLES_TABLE_NAME, game.contentValues, "${PuzzlesColumn.ID}=${game.id}", null)

	/** Update only the ratings for one puzzle
	 *
	 * @param puzzleId    ... key for the puzzle in the table
	 * @param ratingLevel ... new value for the rating level
	 * @param ratingValue ... new value for the rating value
     *
	 * no returns
	 *
	 */
	internal fun updatePuzzleRating(puzzleId: Long, ratingLevel: Int, ratingValue: Int) {
		val rowCnt = db.update(PUZZLES_TABLE_NAME, ContentValues().apply {
			put(PuzzlesColumn.RATING_LEVEL.nme, ratingLevel)
			put(PuzzlesColumn.RATING_VALUE.nme, ratingValue)
		}, "${PuzzlesColumn.ID}=${puzzleId}", null)
		if (rowCnt != 1) {
			throw SQLException("Failed to update the rating a puzzle.")
		}
	}

	internal fun updatePuzzleSuid(puzzleId: Long, suid: String) {
		db.update(PUZZLES_TABLE_NAME, ContentValues().apply {
			put(PuzzlesColumn.SUID.nme, suid)
		}, "${PuzzlesColumn.ID}=${puzzleId}", null)
	}

	internal fun updatePuzzleSolution(puzzleId: Long, solution: String?) {
		db.update(PUZZLES_TABLE_NAME, ContentValues().apply {
			if (solution == null) putNull(PuzzlesColumn.SOLUTION.nme) else put(PuzzlesColumn.SOLUTION.nme, solution)
		}, "${PuzzlesColumn.ID}=${puzzleId}", null)
	}

	internal fun resetAllPuzzles(folderID: Long) {
		db.update(PUZZLES_TABLE_NAME, ContentValues().apply {
			putNull(PuzzlesColumn.CELLS_DATA.nme)
			put(PuzzlesColumn.LAST_PLAYED.nme, 0)
			put(PuzzlesColumn.STATE.nme, SudokuGame.GAME_STATE_NOT_STARTED)
			put(PuzzlesColumn.MISTAKE_COUNTER.nme, 0)
			put(PuzzlesColumn.HINT_USAGE.nme, 0)
			put(PuzzlesColumn.TIME.nme, 0)
			put(PuzzlesColumn.USER_NOTE.nme, "")
			put(PuzzlesColumn.COMMAND_STACK.nme, "")
			put(PuzzlesColumn.RATING_LEVEL.nme, 0)
			put(PuzzlesColumn.RATING_VALUE.nme, 0)
		}, "${PuzzlesColumn.FOLDER_ID}=$folderID", null)
	}

	/**
	 * Returns list of sudoku game objects
	 *
	 * @param folderId Primary key of folder.
	 */
	internal fun getPuzzleListCursor(folderId: Long = 0L, filter: PuzzleListFilter? = null, sortOrder: String? = null): Cursor {
		val qb = SQLiteQueryBuilder()
		qb.tables = PUZZLES_TABLE_NAME
		if (folderId != 0L) {
			qb.appendWhere("${PuzzlesColumn.FOLDER_ID}=$folderId")
		}
		if (filter != null) {
			val states = mutableListOf<Int>()
			if (filter.showStateNotStarted) states.add(SudokuGame.GAME_STATE_NOT_STARTED)
			if (filter.showStatePlaying) states.add(SudokuGame.GAME_STATE_PLAYING)
			if (filter.showStateCompleted) states.add(SudokuGame.GAME_STATE_COMPLETED)

			if (states.isNotEmpty()) {
				qb.appendWhere(" AND ${PuzzlesColumn.STATE} IN (${states.joinToString(",")})")
			}

			val refinements = mutableListOf<String>()
			if (filter.showStateWithMistakes) {
				refinements.add("(${PuzzlesColumn.MISTAKE_COUNTER} > 0)")
			}
			if (filter.showStateWithHints) {
				refinements.add("(${PuzzlesColumn.HINT_USAGE} > 0)")
			}

			if (refinements.isNotEmpty()) {
				qb.appendWhere(" AND (${refinements.joinToString(" OR ")})")
			}
		}
		return qb.query(db, null, null, null, null, null, sortOrder)
	}

	fun folderExists(folderName: String): Boolean = getFolderInfo(folderName) != null

	override fun close() {
		executorService?.shutdownNow()
		executorService = null
		// Do not close openHelper here as it is a singleton shared by multiple SudokuDatabase instances.
		// The SQLite database will be closed when the application process is terminated.
	}
}

internal fun extractSudokuGameFromCursorRow(cursor: Cursor, isEditMode: Boolean): SudokuGame? {
	return extractSudokuGameFromRawData(getRawGameData(cursor), isEditMode)
}

internal fun extractSudokuGameFromRawData(rawGameData: RawGameData?, isEditMode: Boolean): SudokuGame? {
	if (rawGameData == null) return null
	val (board, dataVersion) = SudokuBoard.deserialize(rawGameData.cellsData, isEditMode)
	return SudokuGame(board).apply {
		id = rawGameData.id
		created = rawGameData.created
		lastPlayed = rawGameData.lastPlayed
		state = rawGameData.state
		mistakeCounter = rawGameData.mistakeCounter
		hintUsage = rawGameData.hintUsage
		playingDuration = rawGameData.playingDuration
		userNote = rawGameData.userNote
		folderId = rawGameData.folderId
		commandStack.deserialize(rawGameData.commandStack, dataVersion)
		ratingLevel = rawGameData.ratingLevel
		ratingValue = rawGameData.ratingValue
		suid = rawGameData.suid
		solution = rawGameData.solution
	}
}

internal fun getRawGameData(cursor: Cursor): RawGameData? {
	try {
		val cellsDataColumnIndex = if (cursor.isNull(PuzzlesColumn.CELLS_DATA.cid)) PuzzlesColumn.ORIGINAL_VALUES.cid else PuzzlesColumn.CELLS_DATA.cid
		val gameData = RawGameData(
			id = cursor.getLong(PuzzlesColumn.ID.cid),
			folderId = cursor.getLong(PuzzlesColumn.FOLDER_ID.cid),
			created = cursor.getLong(PuzzlesColumn.CREATED.cid),
			state = cursor.getInt(PuzzlesColumn.STATE.cid),
			playingDuration = cursor.getLong(PuzzlesColumn.TIME.cid),
			mistakeCounter = cursor.getIntOrNull(PuzzlesColumn.MISTAKE_COUNTER.cid),
			hintUsage = cursor.getIntOrNull(PuzzlesColumn.HINT_USAGE.cid),
			lastPlayed = cursor.getLong(PuzzlesColumn.LAST_PLAYED.cid),
			cellsData = cursor.getString(cellsDataColumnIndex),
			commandStack = cursor.getString(PuzzlesColumn.COMMAND_STACK.cid) ?: "",
			userNote = cursor.getString(PuzzlesColumn.USER_NOTE.cid) ?: "",
			ratingLevel = cursor.getInt(PuzzlesColumn.RATING_LEVEL.cid),
			ratingValue = cursor.getInt(PuzzlesColumn.RATING_VALUE.cid),
			suid = cursor.getString(PuzzlesColumn.SUID.cid),
			solution = cursor.getString(PuzzlesColumn.SOLUTION.cid)
		)
		return gameData
	} catch (e: Exception) {    // this shouldn't normally happen, db corrupted
		Log.e(DB_TAG, "Error extracting SudokuGame from cursor", e)
		return null
	}
}

internal fun Cursor.forEach(callback: ((Cursor) -> Unit)) {
	if (moveToFirst()) {
		while (!isAfterLast) {
			callback(this)
			moveToNext()
		}
	}
	close()
}

internal val Cursor.originalValues: String
	get() = getString(PuzzlesColumn.ORIGINAL_VALUES.cid)

internal val Cursor.gameId: Long
	get() = getLong(PuzzlesColumn.ID.cid)

internal val Cursor.folderId: Long
	get() = getLong(FoldersColumn.ID.cid)

internal val Cursor.name: String
	get() = getString(FoldersColumn.NAME.cid) ?: ""

internal val Cursor.gameCreated: Long
	get() = getLong(PuzzlesColumn.CREATED.cid)

internal val Cursor.folderCreated: Long
	get() = getLong(FoldersColumn.CREATED.cid)

internal val Cursor.folderInfo: FolderInfo
	get() = FolderInfo(folderId, name, folderCreated)

const val DB_TAG = "SudokuDatabase"
