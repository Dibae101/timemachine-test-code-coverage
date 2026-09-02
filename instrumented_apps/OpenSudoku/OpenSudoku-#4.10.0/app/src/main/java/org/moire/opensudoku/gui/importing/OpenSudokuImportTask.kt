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
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.moire.opensudoku.R
import org.moire.opensudoku.db.forEach
import org.moire.opensudoku.db.gameCreated
import org.moire.opensudoku.db.gameId
import org.moire.opensudoku.db.originalValues
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.SudokuGame.Companion.GAME_STATE_NOT_STARTED
import org.moire.opensudoku.gui.Tag
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Handles import of .opensudoku files.
 */
class OpenSudokuImportTask(private val uri: Uri, importStrategy: ImportStrategyTypes) : ImportTask(importStrategy) {
	override suspend fun processImport(context: Context) {
		val streamReader: InputStreamReader = if (uri.scheme == "content") {
			val contentResolver = context.contentResolver
			InputStreamReader(contentResolver.openInputStream(uri))
		} else {
			val juri = URI(uri.scheme, uri.schemeSpecificPart, uri.fragment)
			InputStreamReader(withContext(Dispatchers.IO) {
				juri.toURL().openStream()
			})
		}
		streamReader.use { importXml(context, it) }
	}

	private fun importXml(context: Context, reader: Reader) {
		val inBR = BufferedReader(reader)
		val parserFactory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
		parserFactory.isNamespaceAware = false
		val parser: XmlPullParser = parserFactory.newPullParser()
		parser.setInput(inBR)
		var eventType = parser.eventType
		var rootTag: String

		while (eventType != XmlPullParser.END_DOCUMENT && !isCancelled) {
			if (eventType == XmlPullParser.START_TAG) {
				rootTag = parser.name
				if (rootTag == "opensudoku") {
					when (parser.getAttributeValue(null, "version")) {
						null -> {
							importOpenSudokuV1Puzzles(context, parser)
						}

						"2" -> {
							importOpenSudokuV2Puzzles(context, parser)
						}

						"3" -> {
							importOpenSudokuV3Puzzles(context, parser)
						}

						else -> {
							importError = context.getString(R.string.invalid_format)
						}
					}
				} else {
					importError = context.getString(R.string.invalid_format)
					return
				}
			}
			eventType = parser.next()
		}
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun importOpenSudokuV3Puzzles(context: Context, parser: XmlPullParser) {
		var eventType = parser.eventType
		var lastTag: String
		val existingPuzzles = HashMap<String, Long>()
		var folderInfo = ImportFolderInfo("${context.getString(R.string.import_title)} ${DateFormat.format("yyyy-MM-dd HH:mm:ss", Date())}")
		titleParam = folderInfo.name

		if (importStrategy == ImportStrategyTypes.NoDup) {
			database.getPuzzleListCursor().forEach { c -> existingPuzzles[c.originalValues] = c.gameId }
		}

		while (eventType != XmlPullParser.END_DOCUMENT && !isCancelled) {
			if (eventType == XmlPullParser.START_TAG) {
				lastTag = parser.name
				if (lastTag == Tag.FOLDER) {
					folderInfo = ImportFolderInfo(
						parser.getAttributeValue(null, Tag.NAME),
						parser.getAttributeValue(null, Tag.CREATED).toLong()
					)
					titleParam = folderInfo.name
					if (importStrategy != ImportStrategyTypes.NoDup) {
						val folderId = ensureFolder(folderInfo)
						existingPuzzles.clear()
						database.getPuzzleListCursor(folderId).forEach { c ->
								existingPuzzles[c.gameCreated.toString() + '#' + c.originalValues] = c.gameId }
					}
				} else if (lastTag == Tag.GAME) {
					val (newBoard, dataVersion) = SudokuBoard.deserialize(parser.getAttributeValue(null, Tag.CELLS_DATA), false)
					SudokuGame(newBoard).run {
						created = parser.getAttributeValue(null, Tag.CREATED).toLong()
						lastPlayed = parser.getAttributeValue(null, Tag.LAST_PLAYED).toLong()
						state = parser.getAttributeValue(null, Tag.STATE).toInt()
						mistakeCounter = parser.getAttributeValue(null, Tag.MISTAKE_COUNTER)?.toInt()
						hintUsage = parser.getAttributeValue(null, Tag.HINT_USAGE)?.toIntOrNull()?:0
						playingDuration = parser.getAttributeValue(null, Tag.TIME).toLong()
						userNote = parser.getAttributeValue(null, Tag.USER_NOTE)
						commandStack.deserialize(parser.getAttributeValue(null, Tag.COMMAND_STACK), dataVersion)
						ratingLevel = parser.getAttributeValue(null, Tag.RATING_LEVEL)?.toIntOrNull()?:0
						ratingValue = if (ratingLevel > 0)
							parser.getAttributeValue(null, Tag.RATING_VALUE)?.toIntOrNull()?:0 else 0

						when(importStrategy){
							ImportStrategyTypes.NoDup ->
								updateDatabase4ImportStrategyNoDup(this,existingPuzzles,folderInfo)
							ImportStrategyTypes.Clean ->
								updateDatabase4ImportStrategyClean(this,existingPuzzles,folderInfo)
							ImportStrategyTypes.Sync ->
								updateDatabase4ImportStrategySync(this,existingPuzzles,folderInfo)
						}
					}
					currentValue = importedCount + updatedCount + duplicatesCount
				}
			}
			eventType = parser.next()
		}
	}


	private fun updateDatabase4ImportStrategyNoDup(game: SudokuGame,existingPuzzles: HashMap<String, Long>,folderInfo: ImportFolderInfo){
		game.id = existingPuzzles[game.board.originalValues] ?: -1L
		if (game.id == -1L) {
			// no puzzle found -> insert
			game.folderId = ensureFolder(folderInfo)
			val newId = database.insertPuzzle(game)
			importedCount += 1
			existingPuzzles[game.board.originalValues] = newId
		} else {
			// puzzle found -> check if update is allowed
			// Update only if puzzle to import is started -> overwrite existing puzzle
			if (game.state != GAME_STATE_NOT_STARTED) {
				game.folderId = ensureFolder(folderInfo)
				database.updatePuzzle(game)
				updatedCount += 1
			} else {
				duplicatesCount += 1
			}
		}
	}

	private fun updateDatabase4ImportStrategyClean(game: SudokuGame,existingPuzzles: HashMap<String, Long>,folderInfo: ImportFolderInfo){
		game.reset() // prepare data for import strategy CLEAN - do not import progress and history information
		game.id = existingPuzzles[game.created.toString() + '#' + game.board.originalValues] ?: -1L
		if (game.id == -1L) {
			// no puzzle found -> insert
			game.folderId = ensureFolder(folderInfo)
			val newId = database.insertPuzzle(game)
			importedCount += 1
			existingPuzzles[game.created.toString() + '#' + game.board.originalValues] = newId
		} else {
			// puzzle found -> check if update is allowed
			// Update only if existing puzzle is not started -> update only untouched puzzles
			val existingSudokuGame = database.getPuzzle(game.id, false)
			if (existingSudokuGame?.state == GAME_STATE_NOT_STARTED) {
				game.folderId = ensureFolder(folderInfo)
				database.updatePuzzle(game)
				updatedCount += 1
			} else {
				duplicatesCount += 1
			}
		}
	}

	private fun updateDatabase4ImportStrategySync(game: SudokuGame,existingPuzzles: HashMap<String, Long>,folderInfo: ImportFolderInfo){
		game.id = existingPuzzles[game.created.toString() + '#' + game.board.originalValues] ?: -1L
		if (game.id == -1L) {
			// no puzzle found -> insert
			game.folderId = ensureFolder(folderInfo)
			val newId = database.insertPuzzle(game)
			importedCount += 1
			existingPuzzles[game.created.toString() + '#' + game.board.originalValues] = newId
		} else {
			// puzzle found -> check if update is allowed
			// Update every puzzle -> overwrite everything
			game.folderId = ensureFolder(folderInfo)
			database.updatePuzzle(game)
			updatedCount += 1
		}
	}


	@Throws(XmlPullParserException::class, IOException::class)
	private fun importOpenSudokuV2Puzzles(context: Context, parser: XmlPullParser) {
		var eventType = parser.eventType
		var lastTag: String
		val existingPuzzles = HashMap<String, Long>()
		var folderInfo = ImportFolderInfo("${context.getString(R.string.import_title)} ${DateFormat.format("yyyy-MM-dd HH:mm:ss", Date())}")

		if (importStrategy == ImportStrategyTypes.NoDup) {
			database.getPuzzleListCursor().forEach { c -> existingPuzzles[c.originalValues] = c.gameId }
		}

		while (eventType != XmlPullParser.END_DOCUMENT && !isCancelled) {
			if (eventType == XmlPullParser.START_TAG) {
				lastTag = parser.name
				if (lastTag == Tag.FOLDER) {
					val name = parser.getAttributeValue(null, Tag.NAME)
					val created = parser.getAttributeValue(null, Tag.CREATED).toLong()
					folderInfo = ImportFolderInfo(name, created)
					if (importStrategy != ImportStrategyTypes.NoDup) {
						val folderId = ensureFolder(folderInfo)
						existingPuzzles.clear()
						database.getPuzzleListCursor(folderId).forEach { c ->
								existingPuzzles[c.gameCreated.toString() + '#' + c.originalValues] = c.gameId }
					}
				} else if (lastTag == Tag.GAME) {
					parser.getAttributeValue(null, "data")?.run {
						val (newBoard, dataVersion) = SudokuBoard.deserialize(this, false)
						SudokuGame(newBoard).run {
							state = parser.getAttributeValue(null, Tag.STATE)?.toInt() ?: GAME_STATE_NOT_STARTED
							playingDuration = parser.getAttributeValue(null, Tag.TIME)?.toLong() ?: 0
							created = parser.getAttributeValue(null, Tag.CREATED)?.toLong() ?: 0
							lastPlayed = parser.getAttributeValue(null, Tag.LAST_PLAYED)?.toLong() ?: 0
							userNote = parser.getAttributeValue(null, "note") ?: ""
							commandStack.deserialize(parser.getAttributeValue(null, Tag.COMMAND_STACK), dataVersion)

							when(importStrategy){
								ImportStrategyTypes.NoDup ->
									updateDatabase4ImportStrategyNoDup(this,existingPuzzles,folderInfo)
								ImportStrategyTypes.Clean ->
									updateDatabase4ImportStrategyClean(this,existingPuzzles,folderInfo)
								ImportStrategyTypes.Sync ->
									updateDatabase4ImportStrategySync(this,existingPuzzles,folderInfo)
							}

						}
					}
					currentValue = importedCount + updatedCount + duplicatesCount
				}
			}
			eventType = parser.next()
		}
	}

	@Throws(XmlPullParserException::class, IOException::class)
	private fun importOpenSudokuV1Puzzles(context: Context, parser: XmlPullParser) {
		var eventType = parser.eventType
		var lastTag = ""
		val existingPuzzles = HashMap<String, Long>()
		var folderInfo = ImportFolderInfo("${context.getString(R.string.import_title)} ${DateFormat.format("yyyy-MM-dd HH:mm:ss", Date())}")
		var fileCreated: Long
		val newPuzzlesOriginalValues = LinkedHashSet<String>()

		// the import file contains the following tags of interest:
		//   - name = name of the folder
		//   - created = date of the creation of the import file,
		//               will be used for the games as creation timestamp
		//   - game = original values of the puzzle

		// default value for the case that the 'created' tag is missing
		fileCreated = Instant.now().toEpochMilli()

		// read all data from import file
		while (eventType != XmlPullParser.END_DOCUMENT && !isCancelled) {
			if (eventType == XmlPullParser.START_TAG) {
				lastTag = parser.name
				if (lastTag == "game") {
					val cellsValues = parser.getAttributeValue(null, "data")
					if (cellsValues.length == 81 && cellsValues.isDigitsOnly()) {
						newPuzzlesOriginalValues.add(cellsValues)
						maxValue = newPuzzlesOriginalValues.size
					}
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				lastTag = ""
			} else if (eventType == XmlPullParser.TEXT) {
				if (lastTag == "name") {
					folderInfo = ImportFolderInfo(parser.text)
				}
				if (lastTag == "created") {
					fileCreated = try {
						LocalDate.parse(parser.text, DateTimeFormatter.ISO_DATE)
							.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond * 1000
					} catch (_: Exception) {
						0L
					}
				}
			}
			eventType = parser.next()
		}

		if (newPuzzlesOriginalValues.isEmpty()) return // nothing to do

		// read the existing puzzles
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
			if (isCancelled) break
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
