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

package org.moire.opensudoku.gui.exporting

import android.content.Context
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.moire.opensudoku.db.ALL_IDS
import org.moire.opensudoku.db.SudokuDatabase
import org.moire.opensudoku.db.extractSudokuGameFromCursorRow
import org.moire.opensudoku.db.forEach
import org.moire.opensudoku.game.FolderInfo
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.gui.screen.folder_list.FolderTaskModel
import org.moire.opensudoku.gui.Tag
import org.xmlpull.v1.XmlSerializer
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer

const val FILE_EXPORT_VERSION = "3"

class ExportTask(val exportParams: ExportTaskParams) : FolderTaskModel() {
	private fun saveToFile(exportParams: ExportTaskParams, context: Context): Boolean {
		requireNotNull(exportParams.folderId) { "'folderId' param must be set" }
		requireNotNull(exportParams.fileOutputStream) { "Output stream cannot be null" }
		var isSuccess = true
		var writer: Writer? = null
		try {
			val serializer = Xml.newSerializer()
			writer = BufferedWriter(OutputStreamWriter(exportParams.fileOutputStream))
			serializer.setOutput(writer)
			serializer.startDocument("UTF-8", true)
			serializer.startTag("", "opensudoku")
			serializer.attribute("", "version", FILE_EXPORT_VERSION)

			SudokuDatabase(context, true).use { db ->
				serializePuzzles(db, serializer, exportParams.folderId!!, exportParams.puzzleId ?: ALL_IDS)
			}

			serializer.endTag("", "opensudoku")
			serializer.endDocument()
		} catch (e: IOException) {
			Log.e(javaClass.simpleName, "Error while exporting file.", e)
			isSuccess = false
		} finally {
			try {
				writer?.close()
			} catch (e: IOException) {
				Log.e(javaClass.simpleName, "Error while exporting file.", e)
				isSuccess = false
			}
		}
		return isSuccess
	}

	private fun serializePuzzles(db: SudokuDatabase, serializer: XmlSerializer, folderId: Long, gameId: Long = ALL_IDS) {
		val folderList: List<FolderInfo> = if (folderId == -1L) db.getFolderList() else listOf(db.getFolderInfo(folderId)!!)
		for (folder in folderList) {
			if (isCancelled) {
				break
			}
			serializer.startTag("", Tag.FOLDER)
			serializer.attribute("", Tag.NAME, folder.name)
			serializer.attribute("", Tag.CREATED, folder.created.toString())
			titleParam = folder.name

			if (gameId == ALL_IDS) {
				with(db.getPuzzleListCursor(folder.id)) {
					maxValue = count
					forEach { cursor ->
						if (isCancelled) return@forEach
						val game = extractSudokuGameFromCursorRow(cursor, false)
						if (game == null) {
							corruptedCount += 1
						} else {
							serializeGame(serializer, game)
						}
						currentValue += 1
					}
				}
			} else {
				maxValue = 1
				serializeGame(serializer, db.getPuzzle(gameId, false)!!)
				currentValue = 1
			}

			serializer.endTag("", Tag.FOLDER)
		}
	}

	private fun serializeGame(serializer: XmlSerializer, game: SudokuGame) {
		serializer.startTag("", Tag.GAME)
		serializer.attribute("", Tag.CREATED, game.created.toString())
		serializer.attribute("", Tag.STATE, game.state.toString())
		serializer.attribute("", Tag.MISTAKE_COUNTER, game.mistakeCounter.toString())
		serializer.attribute("", Tag.HINT_USAGE, game.hintUsage.toString())
		serializer.attribute("", Tag.TIME, game.playingDuration.toString())
		serializer.attribute("", Tag.LAST_PLAYED, game.lastPlayed.toString())
		serializer.attribute("", Tag.CELLS_DATA, game.board.serialize())
		serializer.attribute("", Tag.USER_NOTE, game.userNote)
		serializer.attribute("", Tag.COMMAND_STACK, game.commandStack.serialize())
		serializer.attribute("", Tag.RATING_LEVEL, game.ratingLevel.toString())
		serializer.attribute("", Tag.RATING_VALUE, game.ratingValue.toString())
		serializer.endTag("", Tag.GAME)
	}

	override suspend fun run(context: Context, onTaskFinished: (Long, Boolean, String) -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
			val isSuccess = saveToFile(exportParams, context)
			onTaskFinished(0, isSuccess, exportParams.filename!!)
		}
	}
}
