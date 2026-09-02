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
import android.net.Uri
import android.text.format.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.moire.opensudoku.R
import org.moire.opensudoku.db.forEach
import org.moire.opensudoku.db.gameId
import org.moire.opensudoku.db.originalValues
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.SudokuGame.Companion.GAME_STATE_NOT_STARTED
import org.moire.opensudoku.utils.getFileName
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.Instant
import java.util.Date

/**
 * Handles import of Sudoku Exchange "Puzzle Bank" files (see https://github.com/grantm/sudoku-exchange-puzzle-bank)
 *
 * A file contains one or more puzzles.
 * The puzzle has the format 9x9.
 * One line in the file contains one puzzle with following information's:
 *   - SHA1 hash of the puzzle data (12 byte)
 *   - <SPACE>
 *   - puzzle data as string (9x9 = 81 bytes) , missing values are indicated by 0
 *   - <SPACE>
 *   - rating (format nn.n = 4 bytes)
 *   - <LF>
 *
 */
val SepbRegex = """\s*\w+\s+(?<cellValues>\d{81})\s+\d+.\d+\s*""".toRegex()

class SepbImportTask(private val uri: Uri, importStrategy: ImportStrategyTypes) : ImportTask(importStrategy) {

	override suspend fun processImport(context: Context) {
		val folderName = uri.getFileName(context.contentResolver)
			?: "${context.getString(R.string.import_title)} ${DateFormat.format("yyyy-MM-dd HH:mm:ss", Date())}"
		val folderInfo = ImportFolderInfo(folderName)
		titleParam = folderName
		val isr: InputStreamReader = if (uri.scheme == "content") {
			val contentResolver = context.contentResolver
			InputStreamReader(contentResolver.openInputStream(uri))
		} else {
			val url = URL("$uri")
			InputStreamReader(withContext(Dispatchers.IO) {
				url.openStream()
			})
		}

		// default value for the file created timestamp (not part of the import)
		val fileCreated: Long = Instant.now().toEpochMilli()

		// read all puzzles from import file
		val newPuzzlesOriginalValues = LinkedHashSet<String>()
		BufferedReader(isr).useLines { lineSequence ->
			lineSequence.forEach { inputLine ->
				if (isCancelled) return@processImport
				SepbRegex.find(inputLine)?.also { newPuzzlesOriginalValues.add(it.groups["cellValues"]?.value!!) }
				maxValue = newPuzzlesOriginalValues.size
			}
		}

		if (newPuzzlesOriginalValues.isEmpty()) return // nothing to do

		// read the existing puzzles
		val existingPuzzles = HashMap<String, Long>()
		if (importStrategy == ImportStrategyTypes.NoDup) {
			database.getPuzzleListCursor().forEach { c -> existingPuzzles[c.originalValues] = c.gameId }
		} else {
			val folderId = ensureFolder(folderInfo)
			existingPuzzles.clear()
			database.getPuzzleListCursor(folderId).forEach { c -> existingPuzzles[c.originalValues] = c.gameId }
		}

		// loop over puzzle list
		val numberOfDecimalPlaces = newPuzzlesOriginalValues.size.toString().length
		for ((index,originalValues) in newPuzzlesOriginalValues.withIndex()) {
			if (isCancelled) return
			val (newBoard, _ /* dataVersion */) = SudokuBoard.deserialize(originalValues, false)
			SudokuGame(newBoard).run {
				state = GAME_STATE_NOT_STARTED
				created = fileCreated
				userNote = "#${(index+1).toString().padStart(numberOfDecimalPlaces, '0')}"

				id = existingPuzzles[board.originalValues] ?: -1L
				if (id == -1L) {
					// no puzzle found -> insert
					folderId = ensureFolder(folderInfo)
					val newId = database.insertPuzzle(this)
					importedCount += 1
					existingPuzzles[board.originalValues] = newId
				} else {
					// no update needed, due to the fact that no additional information
					// like note, progress state or history are available.
					duplicatesCount += 1  // = skipped
				}
			}
			currentValue += 1
		}
	}
}

