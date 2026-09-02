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

package org.moire.opensudoku.gui.screen.title

import android.app.Application
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.SudokuGame.Companion.GAME_STATE_NOT_STARTED
import org.moire.opensudoku.gui.DbKeeperViewModel
import org.moire.opensudoku.gui.fragments.RandomPuzzleDialogFragment.Companion.randomFilter
import org.moire.opensudoku.utils.TimeProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant

class TitleScreenViewModel(application: Application) : DbKeeperViewModel(application) {

	val gameSettings = GameSettings(application)

	fun initScreen(onComplete: () -> Unit) {
		initDb(true) { onComplete() }
	}

	fun getResumePuzzle(): Long? {
		val resumePuzzleID = gameSettings.lastGamePuzzleId
		val sudokuGame = db!!.getPuzzle(resumePuzzleID, false) ?: return null
		return if (sudokuGame.state != SudokuGame.GAME_STATE_COMPLETED) {
			resumePuzzleID
		} else null
	}

	fun getRandomPuzzle(): Long? {
		return db!!.getRandomPuzzleID(randomFilter)
	}

	fun getChallengePuzzle(): Long? {
		val now = TimeProvider.getCurrentLocalTime()
		if (now.month.value != 12) return null

		val xmasPuzzleValues = "690040013400792008000010000000409000004573200000020000000108000007000300041237890"
		val xmasPuzzle = db!!.findPuzzle(xmasPuzzleValues, false)
		if (xmasPuzzle != null) {
			return xmasPuzzle.id
		}

		val challengeFolderInfo = db!!.insertFolder(getString(R.string.challenge), 0)
		val (newBoard, _) = SudokuBoard.deserialize(xmasPuzzleValues, false)
		SudokuGame(newBoard).run {
			state = GAME_STATE_NOT_STARTED
			created = Instant.now().toEpochMilli()
			userNote = getString(R.string.xmas_challenge)
			folderId = challengeFolderInfo.id
			return@getChallengePuzzle db!!.insertPuzzle(this)
		}
	}

	fun shouldShowPuzzleListOnStartup(): Boolean {
		return gameSettings.isShowPuzzleListOnStartup
	}

	fun getLogs(): String {
		val log = StringBuilder()
		try {
			val process = Runtime.getRuntime().exec("logcat -d")
			val reader = BufferedReader(InputStreamReader(process.inputStream))
			var line: String?
			while (reader.readLine().also { line = it } != null) {
				log.append(line).append("\n")
			}
		} catch (e: Exception) {
			log.append("Error reading logcat: ${e.message}")
		}
		return log.toString()
	}

	private fun getString(resId: Int): String {
		return getApplication<Application>().getString(resId)
	}
}
