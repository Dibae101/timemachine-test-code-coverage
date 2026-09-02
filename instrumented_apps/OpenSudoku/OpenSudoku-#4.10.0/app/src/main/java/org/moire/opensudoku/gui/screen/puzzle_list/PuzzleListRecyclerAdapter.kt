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

package org.moire.opensudoku.gui.screen.puzzle_list

import android.R.attr.colorError
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moire.opensudoku.R
import org.moire.opensudoku.db.RawGameData
import org.moire.opensudoku.db.SudokuDatabase
import org.moire.opensudoku.db.extractSudokuGameFromRawData
import org.moire.opensudoku.db.getRawGameData
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.WrongValueHighlightMode
import org.moire.opensudoku.game.nextstep.StrategyLevelIds
import org.moire.opensudoku.gui.screen.game_play.PlayingDurationFormat
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.gui.SuidGenerator3F
import org.moire.opensudoku.utils.Colors
import org.moire.opensudoku.utils.ThemeUtils
import org.moire.opensudoku.utils.ThemeUtils.dimmedColor
import org.moire.opensudoku.utils.addAll
import java.io.Closeable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PuzzleListRecyclerAdapter(
	private val context: Context,
	private var puzzlesCursor: Cursor,
	private val onClickListener: (Long) -> Unit,
	private var isShowTime: Boolean,
	private var isShowMistakesCount: Boolean,
	private var highlightWrongValuesMode: WrongValueHighlightMode,
	private val db: SudokuDatabase? = null,
) : RecyclerView.Adapter<PuzzleListRecyclerAdapter.ViewHolder?>(), Closeable {
	private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
	private val suidCache = mutableMapOf<Long, String?>()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.sudoku_list_item, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = puzzlesCursor.count

	@SuppressLint("NotifyDataSetChanged")
	fun updateGameList(newGames: Cursor, isShowTime: Boolean, isShowMistakesCount: Boolean, wrongValuesHighlightMode: WrongValueHighlightMode) {
		this.isShowTime = isShowTime
		this.isShowMistakesCount = isShowMistakesCount
		highlightWrongValuesMode = wrongValuesHighlightMode
		puzzlesCursor.close()
		puzzlesCursor = newGames
		suidCache.clear()
		notifyDataSetChanged()
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.contentDescription = context.getString(R.string.puzzle_selection_position, position)

		//          R.id.board_view
		holder.boardView.isReadOnlyPreview = true
		holder.boardView.isFocusable = false
		holder.boardView.highlightDirectlyWrongValues = highlightWrongValuesMode != WrongValueHighlightMode.OFF
		holder.boardView.highlightIndirectlyWrongValues = highlightWrongValuesMode == WrongValueHighlightMode.INDIRECT
		ThemeUtils.applyConfiguredThemeToSudokuBoardView(holder.boardView, context)

		puzzlesCursor.moveToPosition(position)
		backgroundViewUpdates(holder, getRawGameData(puzzlesCursor))
	}

	override fun onViewRecycled(holder: ViewHolder) {
		super.onViewRecycled(holder)
		holder.job?.cancel()
		holder.job = null
	}

	@SuppressLint("SetTextI18n")
	fun backgroundViewUpdates(holder: ViewHolder, rawGameData: RawGameData?) {
		holder.job?.cancel()
		holder.job = adapterScope.launch {
			// data processing
			val game: SudokuGame? = withContext(Dispatchers.Default) {
				extractSudokuGameFromRawData(rawGameData, false)
			}
			if (game == null) {
				Toast.makeText(context, R.string.error_could_not_find_puzzle_in_database, Toast.LENGTH_LONG).show()
				return@launch
			}

			// precalculate SUID out of UI scope for slow devices
			val suid = if (game.suid != null) {
				"SUID: ${game.suid}"
			} else {
				suidCache[game.id] ?: withContext(Dispatchers.Default) {
					if (game.solutionCount == 1) {
						val encoded = SuidGenerator3F().encode(game.board)
						db?.updatePuzzleSuid(game.id, encoded)
						if (game.solution == null) {
							val sol = game.board.serializeSolution()
							db?.updatePuzzleSolution(game.id, sol)
							game.solution = sol
						}
						"SUID: $encoded"
					} else if (game.solutionCount == 0) {
						if (game.solution == null) {
							db?.updatePuzzleSolution(game.id, "none")
							game.solution = "none"
						}
						null
					} else null
				}.also { suidCache[game.id] = it }
			}

			holder.itemView.setOnClickListener { onClickListener(game.id) }
			holder.itemView.setOnCreateContextMenuListener { menu, _, _ ->
				selectedGameId = game.id
				menu?.run { addAll(PuzzleListActivity.ContextMenuItems.entries) }
			}

			holder.boardView.board = game.board

			//          R.id.state
			holder.state.apply {
				when (game.state) {
					SudokuGame.GAME_STATE_COMPLETED -> {
						text = context.getString(R.string.solved)
						setTextColor(ThemeUtils.getContextThemeColor(holder.userNote.context, Colors.GIVEN_VALUE_TEXT.attr))
					}

					SudokuGame.GAME_STATE_PLAYING -> {
						text = context.getString(R.string.playing)
						setTextColor(ThemeUtils.getContextThemeColor(holder.userNote.context, Colors.LINE.attr))
					}

					else -> {
						text = context.getString(R.string.not_started)
						setTextColor(ThemeUtils.getContextThemeColor(holder.userNote.context, Colors.VALUE_TEXT.attr))
					}
				}
			}

			holder.mistakesAndDuration.text = if (game.playingDuration != 0L) {
				val durationText = playingDurationFormatter.format(game.playingDuration)
				(if (isShowMistakesCount) "\u270B${game.hintUsage ?: "?"}\t\u2757${game.mistakeCounter ?: "?"}\t" else "") + (if (isShowTime) "\u231A${durationText}" else "")
			} else ""

			holder.lastPlayed.text = if (game.lastPlayed != 0L) context.getString(R.string.last_played_at, getDateAndTimeForHumans(game.lastPlayed)) else ""
			holder.created.text = if (game.created != 0L) context.getString(R.string.created_at, getDateAndTimeForHumans(game.created)) else ""
			holder.userNote.text = if (game.userNote != "") context.getString(R.string.note) + " " + game.userNote else ""
			holder.suid.apply {
				if (suid != null) {
					text = suid
					setTextColor(dimmedColor(ThemeUtils.getContextThemeColor(holder.userNote.context, android.R.attr.textColor)))
				} else {
					text = context.getString(R.string.invalid_puzzle)
					setTextColor(ThemeUtils.getContextThemeColor(holder.userNote.context, colorError))
				}
			}
			// rating
			if (game.ratingLevel < 1) {
				holder.rating.text = ""
			} else {
				var ratingText = context.getString(R.string.rating) + ":  "
				val ratingLevelText = StrategyLevelIds
					.getStrategyLevel(game.ratingLevel)
					.getStrategyLevelName(context)
				ratingText += ratingLevelText
				if (game.ratingValue > 0) {
					ratingText += "  ...  ${game.ratingValue}"
				}
				holder.rating.text = ratingText
			}
		}
	}

	private fun getDateAndTimeForHumans(epochMilliseconds: Long): String {
		val dateTime = LocalDateTime.ofEpochSecond(epochMilliseconds / 1000, 0, localZoneOffset)
		val today = LocalDate.now()
		val yesterday = today.minusDays(1)
		return if (dateTime.isAfter(today.atStartOfDay())) {
			context.getString(R.string.today_at_time, dateTime.toLocalTime().format(timeFormatter))
		} else if (dateTime.isAfter(yesterday.atStartOfDay())) {
			context.getString(R.string.yesterday_at_time, dateTime.toLocalTime().format(timeFormatter))
		} else {
			context.getString(R.string.on_date, dateTime.format(dateTimeFormatter))
		}
	}

	internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val boardView: SudokuBoardView = itemView.findViewById(R.id.board_view)
		val state: TextView = itemView.findViewById(R.id.state)
		val mistakesAndDuration: TextView = itemView.findViewById(R.id.mistakes_and_time)
		val lastPlayed: TextView = itemView.findViewById(R.id.last_played)
		val created: TextView = itemView.findViewById(R.id.created)
		val rating: TextView = itemView.findViewById(R.id.rating)
		val userNote: TextView = itemView.findViewById(R.id.user_note)
		val suid: TextView = itemView.findViewById(R.id.suid)
		var job: Job? = null
	}

	override fun close() {
		adapterScope.cancel()
		puzzlesCursor.close()
	}

	companion object {
		var selectedGameId: Long = 0

		private val localZoneOffset: ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
		private val playingDurationFormatter = PlayingDurationFormat()
		private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
		private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
	}
}
