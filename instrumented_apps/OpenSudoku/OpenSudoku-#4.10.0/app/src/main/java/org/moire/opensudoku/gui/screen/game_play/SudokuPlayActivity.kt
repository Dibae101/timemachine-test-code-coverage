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
@file:Suppress("HardCodedStringLiteral")

package org.moire.opensudoku.gui.screen.game_play

import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.DoubleMarksMode
import org.moire.opensudoku.game.HintHighlight
import org.moire.opensudoku.game.NextStepHint
import org.moire.opensudoku.game.SameValueHighlightMode
import org.moire.opensudoku.game.SudokuGame
import org.moire.opensudoku.game.WrongValueHighlightMode
import org.moire.opensudoku.game.nextstep.HintLevels
import org.moire.opensudoku.game.nextstep.NextStepStates
import org.moire.opensudoku.game.nextstep.StrategyIds
import org.moire.opensudoku.game.nextstep.StrategyLevelIds
import org.moire.opensudoku.gui.screen.GameSettingsActivity
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.gui.SuidGenerator3F
import org.moire.opensudoku.gui.Tag.IS_RANDOM_PUZZLE
import org.moire.opensudoku.gui.Tag.PUZZLE_ID
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.DialogParams
import org.moire.opensudoku.gui.fragments.HintLevelDialogFragment
import org.moire.opensudoku.gui.fragments.NextStepHintDialogFragment
import org.moire.opensudoku.gui.fragments.SimpleDialog
import org.moire.opensudoku.gui.inputmethod.IMControlPanel
import org.moire.opensudoku.gui.inputmethod.IMInsertOnTap
import org.moire.opensudoku.gui.inputmethod.IMPopup
import org.moire.opensudoku.gui.inputmethod.IMSelectOnTap
import org.moire.opensudoku.utils.OptionsMenuItem
import org.moire.opensudoku.utils.ThemeUtils
import org.moire.opensudoku.utils.addAll
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.getValue

class SudokuPlayActivity : ThemedActivity() {
	val viewModel: SudokuPlayViewModel by viewModels()
	private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
	private lateinit var rootLayout: ViewGroup
	private lateinit var sudokuBoard: SudokuBoardView
	private lateinit var optionsMenu: Menu
	private lateinit var imControlPanel: IMControlPanel
	private lateinit var imPopup: IMPopup
	private lateinit var imInsertOnTap: IMInsertOnTap
	private lateinit var imSelectOnTap: IMSelectOnTap
	private var isShowTime = true
	private var isShowMistakeCounter = true
	private var isHighlightCompletedValues = true
	private var gameTimer: GameTimerUpdater? = null
	private val playingDurationFormatter = PlayingDurationFormat()
	private var fillInValidMarksEnabled = false
	private lateinit var hintsQueue: HintsQueue
	private lateinit var resetGameDialog: SimpleDialog
	private lateinit var clearAllMarksDialog: SimpleDialog
	private lateinit var undoToCheckpointDialog: SimpleDialog
	private lateinit var undoToBeforeMistakeDialog: SimpleDialog
	private lateinit var solvePuzzleDialog: SimpleDialog
	private lateinit var solveOneCellValue: SimpleDialog
	private lateinit var puzzleSolvedDialog: SimpleDialog
	private var isViewInitFinished = false
	private var isRandomPuzzle = false
	private var audioService: AudioManager? = null
	private var maxVolume = 0
	private var isAudioAvailable: Boolean? = null

	private var bellEnabled: Boolean = false
		set(newValue) {
			if (newValue && isAudioAvailable == null) {
				isAudioAvailable = false
				audioService = audioService ?: getSystemService(AUDIO_SERVICE) as? AudioManager
				audioService?.apply {
					try {
						maxVolume = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
						isAudioAvailable = maxVolume > 0 && (ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MIN_VOLUME).audioSessionId != 0)
					} catch (_: RuntimeException) { // some devices do not support TG and throw RuntimeException from the constructor
					}
				}
			}
			if (isAudioAvailable == false) {
				Toast.makeText(applicationContext, R.string.audio_service_is_not_available, Toast.LENGTH_LONG).show()
				field = false
			} else {
				field = newValue
			}
		}

	/** nextStepLastHintLevel & nextStepLastSignature
	 * Both variables are used to store level and signature of the next step.
	 * The content are used to control the behavior of the counter "hint usage"
	 */
	private var nextStepLastHintLevel = HintLevels.LEVEL1
	private var nextStepLastSignature = ""

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.sudoku_play)

		val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		rootLayout = findViewById(R.id.play_root_layout)
		sudokuBoard = findViewById(R.id.play_board_view)
		imControlPanel = findViewById(R.id.input_methods)

		val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_layout)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val screenPadding = viewModel.settings.screenBorderSize
			appBarLayout.updatePadding(
				left = systemBars.left,
				top = systemBars.top,
				right = systemBars.right
			)
			rootLayout.updatePadding(
				left = systemBars.left + screenPadding,
				right = systemBars.right + screenPadding,
				bottom = systemBars.bottom + screenPadding
			)
			insets
		}

		hintsQueue = HintsQueue(this)
		gameTimer = GameTimerUpdater(::updateTime)

		settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { restartActivity() }

		if (!viewModel.initStarted) { // activity runs for the first time, read game from the database
			val sudokuGameID = intent.getLongExtra(PUZZLE_ID, 0)
			isRandomPuzzle = intent.getBooleanExtra(IS_RANDOM_PUZZLE, false)
			viewModel.init(sudokuGameID)
		}

		if (!viewModel.initFinished.value) lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.initFinished.collect { finished ->
					if (finished) {
						if (isRandomPuzzle) {
							val folderName = viewModel.getFolderName() ?: ""
							Toast.makeText(
								applicationContext,
								getString(R.string.playing_random_puzzle_toast, folderName),
								Toast.LENGTH_LONG
							).show()
							isRandomPuzzle = false // Show only once
						}
						alertIfGameIsNotSolvable()
						viewModel.game?.let { game ->
							if (game.solution == null) {
								if (game.board.solutionCount == 1) {
									game.solution = game.board.serializeSolution()
									viewModel.updatePuzzleInDB()
								} else if (game.board.solutionCount == 0) {
									game.solution = "none"
									viewModel.updatePuzzleInDB()
								}
							}
						}
					}
				}
			}
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.uiState.collect { uiState ->
					val playedGame = uiState.playedGame ?: return@collect

					if (playedGame.state == SudokuGame.GAME_STATE_COMPLETED) {
						sudokuBoard.isReadOnlyPreview = true
					}
					sudokuBoard.setGame(playedGame)
					sudokuBoard.onReadonlyChangeListener = ::updateUndoButtonState
					playedGame.onHasUndoChangedListener = ::updateUndoButtonState
					playedGame.onPuzzleSolvedListener = ::onSolvedListener
					playedGame.onDigitFinishedManuallyListener = ::onDigitFinishedListener
					playedGame.onInvalidDigitEnteredListener = ::onInvalidDigitEntryListener
					hintsQueue.showOneTimeHint("welcome", R.string.welcome, R.string.first_run_hint)
					imControlPanel.initialize(sudokuBoard, playedGame, hintsQueue)
					imPopup = imControlPanel.imPopup
					imInsertOnTap = imControlPanel.imInsertOnTap
					imSelectOnTap = imControlPanel.imSelectOnTap

					if (!sudokuBoard.isReadOnlyPreview) {
						val previousCellRow = viewModel.settings.lastGameSelectedRow
						val previousCellColumn = viewModel.settings.lastGameSelectedColumn
						if (previousCellRow != -1) {
							sudokuBoard.moveCellSelectionTo(previousCellRow, previousCellColumn)
						} else playedGame.lastCommandCell?.let { selectCell(it) }
					}

					registerDialogsListeners()

					isViewInitFinished = true
					resumeGame()
					updateView()
				}
			}
		}
	}

	private fun alertIfGameIsNotSolvable(): Boolean {
		val sudokuGame = viewModel.game ?: return true
		if (sudokuGame.solutionCount > 1) {
			SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_multiple_solutions)
			return true
		} else if (sudokuGame.solutionCount < 1) {
			SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_no_solution)
			return true
		}
		return false
	}

	private fun registerDialogsListeners() {
		val game = viewModel.game!!
		resetGameDialog = SimpleDialog(DialogParams().apply {
			resultKey = "ResetGame"
			iconId = R.drawable.ic_restore
			messageId = R.string.restart_confirm
		}).apply { registerPositiveButtonCallback(this@SudokuPlayActivity, ::restartGame) }

		clearAllMarksDialog = SimpleDialog(DialogParams().apply {
			resultKey = "ClearAllMarks"
			iconId = R.drawable.ic_delete
			messageId = R.string.clear_all_marks_confirm
		}).apply { registerPositiveButtonCallback(this@SudokuPlayActivity) { game.clearAllMarksManual() } }

		undoToCheckpointDialog = SimpleDialog(DialogParams().apply {
			resultKey = "UndoToCheckpoint"
			iconId = R.drawable.ic_undo
			messageId = R.string.undo_to_checkpoint_confirm
		}).apply {
			registerPositiveButtonCallback(this@SudokuPlayActivity) {
				game.undoToCheckpoint()
				selectLastCommandCell()
			}
		}

		undoToBeforeMistakeDialog = SimpleDialog(DialogParams().apply {
			resultKey = "UndoToBeforeMistake"
			iconId = R.drawable.ic_undo
			messageId = R.string.undo_to_before_mistake_confirm
		}).apply {
			registerPositiveButtonCallback(this@SudokuPlayActivity) {
				game.undoToBeforeMistake()
				selectLastCommandCell()
			}
		}

		solvePuzzleDialog = SimpleDialog(DialogParams().apply {
			resultKey = "SolvePuzzle"
			messageId = R.string.solve_puzzle_confirm
		}).apply {
			registerPositiveButtonCallback(this@SudokuPlayActivity) {
				val numberOfSolutions = game.solve()
				if (numberOfSolutions == 0) {
					SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_no_solution)
				} else if (numberOfSolutions > 1) {
					SimpleDialog().show(supportFragmentManager, R.string.puzzle_has_multiple_solutions)
				}
			}
		}

		solveOneCellValue = SimpleDialog(DialogParams().apply {
			resultKey = "SolveCell"
			messageId = R.string.hint_confirm
		}).apply {
			registerPositiveButtonCallback(this@SudokuPlayActivity) {
				val cell = sudokuBoard.selectedCell
				val game = game
				if (cell != null && cell.isEditable) {
					if (!alertIfGameIsNotSolvable()) {
						game.solveCell(cell)
					}
				} else {
					SimpleDialog().show(supportFragmentManager, R.string.you_cant_modify_this_cell)
				}
			}
		}

		puzzleSolvedDialog = SimpleDialog(DialogParams().apply {
			resultKey = "PuzzleSolved"
			iconId = R.drawable.ic_info
			titleId = R.string.well_done
		}).apply { registerPositiveButtonCallback(this@SudokuPlayActivity, ::finish) }

		// HINT - next step
		HintLevelDialogFragment.setListener(this) { checkedItemIndex ->
			// get hint level
			val hintLevelTitle = resources.getStringArray(R.array.hint_level_title_list)[checkedItemIndex]
			val hintLevel = when(hintLevelTitle) {
				getString(R.string.hint_level_1_title) -> HintLevels.LEVEL1
				getString(R.string.hint_level_2_title) -> HintLevels.LEVEL2
				getString(R.string.hint_level_3_title) -> HintLevels.LEVEL3
				getString(R.string.hint_level_4_title) -> HintLevels.LEVEL4
				else -> HintLevels.LEVEL4
			}
			// try to get a next step
			val nextStepHint = NextStepHint(applicationContext, game.board, hintLevel)
			// get the result of the search for a next step
			val nextStep = nextStepHint.getNextStepHint()
			highlightCellsAndSelectFirst(nextStep.cellsToHighlight)
			// hint usage
			if (nextStep.nextStepStrategyId == StrategyIds.NO_NEXT_STEP_FOUND) {
				// no_step_found will not increase the counter
				nextStepLastHintLevel = HintLevels.LEVEL1
				nextStepLastSignature = ""
			} else {
				if (game.hintUsage == null) game.hintUsage = 0
				var hintUsage = game.hintUsage!!
				val nextStepSignature = nextStep.getNextStepSignature()
				if ( nextStepSignature == nextStepLastSignature) {
					// same next step found as before
					if (hintLevel > nextStepLastHintLevel) {
						// hint level is higher, the usage counter will be increased
						// only by the delta depending on the hint level last and current
						hintUsage += (hintLevel.getHintUsageValue()
							- nextStepLastHintLevel.getHintUsageValue())
						nextStepLastHintLevel = hintLevel
					}
				} else {
					// new next step, the counter will be increased depending on the hint level
					hintUsage += hintLevel.getHintUsageValue()
					nextStepLastHintLevel = hintLevel
					nextStepLastSignature = nextStepSignature
				}
				game.hintUsage = hintUsage
			}
			// show the next step dialog with the hint message
			// and the possibility to execute the next step
			val hintText = sudokuBoard.fontSet.replaceDigitsInBraces(nextStep.nextStepText)
			val applyNextStepPossible = (hintLevel == HintLevels.LEVEL4) &&
				(nextStep.nextStepState == NextStepStates.STEP_FOUND)
			NextStepHintDialogFragment().showMessage(supportFragmentManager,
				hintText,
				applyNextStepPossible,
				nextStep)
		}
		NextStepHintDialogFragment.onDialogFinished = ::disableHighlight
		NextStepHintDialogFragment.onDialogApplyNextStep = ::applyNextStep
	}

	private fun updateUndoButtonState() {
		optionsMenu.findItem(OptionsMenuItems.UNDO_ACTION.id)?.isEnabled = !sudokuBoard.isReadOnlyPreview && (viewModel.game?.hasSomethingToUndo() == true)
	}

	override fun onResume() {
		super.onResume()
		updateView()
	}

	fun updateView() {
		if (!isViewInitFinished) return
		// read game settings
		fillInValidMarksEnabled = viewModel.settings.fillInMarksEnabled
		bellEnabled = viewModel.settings.bellEnabled
		ThemeUtils.applyConfiguredThemeToSudokuBoardView(sudokuBoard, this)
		val wrongValuesHighlightMode = viewModel.settings.highlightWrongValues
		val isCheckDirectlyWrongValues = wrongValuesHighlightMode != WrongValueHighlightMode.OFF
		sudokuBoard.highlightDirectlyWrongValues = isCheckDirectlyWrongValues
		val isCheckIndirectlyWrongValues = wrongValuesHighlightMode == WrongValueHighlightMode.INDIRECT
		sudokuBoard.highlightIndirectlyWrongValues = isCheckIndirectlyWrongValues
		sudokuBoard.highlightInvalidPencilMarks = viewModel.settings.isValidatePencilMarks
		sudokuBoard.doubleMarksMode = viewModel.settings.doubleMarksMode
		sudokuBoard.highlightTouchedCell = viewModel.settings.highlightTouchedCell
		sudokuBoard.sameValueHighlightMode = viewModel.settings.sameValueHighlightMode
		viewModel.game?.isRemoveMarksOnEntry = viewModel.settings.isRemoveMarksOnInput
		viewModel.game?.isCheckDirectErrors = isCheckDirectlyWrongValues
		viewModel.game?.isCheckIndirectErrors = isCheckIndirectlyWrongValues
		isShowTime = viewModel.settings.isShowTime
		isShowMistakeCounter = viewModel.settings.isShowMistakeCounter
		isHighlightCompletedValues = viewModel.settings.isHighlightCompletedValues

		if (viewModel.game?.state == SudokuGame.GAME_STATE_PLAYING) resumeGame()

		val moveCellSelectionOnPress = viewModel.settings.isMoveCellSelectionOnPress
		sudokuBoard.moveCellSelectionOnPress = moveCellSelectionOnPress

		imPopup.isEnabled = viewModel.settings.isPopupEnabled

		imInsertOnTap.isEnabled = viewModel.settings.isInsertOnTapEnabled
		imInsertOnTap.bidirectionalSelection = viewModel.settings.isBidirectionalSelection
		imInsertOnTap.highlightSimilar = viewModel.settings.isHighlightSimilar
		imInsertOnTap.onSelectedNumberChangedListener = ::onSelectedNumberChangedListener

		imSelectOnTap.isEnabled = viewModel.settings.isSelectOnTapEnabled
		imSelectOnTap.isMoveCellSelectionOnPress = moveCellSelectionOnPress

		imControlPanel.showDigitCount = viewModel.settings.isShowNumberTotals
		imControlPanel.highlightCompletedValues = isHighlightCompletedValues
		imControlPanel.isDoubleMarksEnabled = viewModel.settings.doubleMarksMode != DoubleMarksMode.SINGLE
		imControlPanel.isBackgroundColorEnabled = viewModel.settings.isBackgroundColorEnabled
		imControlPanel.activateFirstInputMethod() // make sure that some input method is activated
		viewModel.restoreInputState(imControlPanel)
		if (!sudokuBoard.isReadOnlyPreview) {
			sudokuBoard.onSelectedCellUpdate(sudokuBoard.selectedCell)
		}

		sudokuBoard.fontSet.refresh()
		updateTime()
		viewModel.highlightedCells?.let { highlightCellsAndSelectFirst(it) }
	}

	override fun onPause() {
		super.onPause()

		pauseGame()
		viewModel.updatePuzzleInDB()
		imControlPanel.pause()

		if (isViewInitFinished) {
			sudokuBoard.selectedCell?.let { selectedCell ->
				viewModel.settings.lastGameSelectedRow = selectedCell.rowIndex
				viewModel.settings.lastGameSelectedColumn = selectedCell.columnIndex
			}
			viewModel.saveInputState(imControlPanel)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		optionsMenu = menu

		menu.addAll(OptionsMenuItems.entries)
		updateUndoButtonState()
		optionsMenu.findItem(OptionsMenuItems.SOUND_ACTION.id)?.setIcon(if (bellEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
		optionsMenu.findItem(OptionsMenuItems.FILL_IN_MAIN_MARKS_WITH_ALL_VALUES.id)?.isVisible = !fillInValidMarksEnabled
		optionsMenu.findItem(OptionsMenuItems.FILL_IN_MAIN_MARKS_WITH_VALID_VALUES.id)?.isVisible = fillInValidMarksEnabled

		// Generate any additional actions that can be performed on the
		// overall list.  In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0, ComponentName(this, SudokuPlayActivity::class.java), null, intent, 0, null
		)
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		super.onPrepareOptionsMenu(menu)

		if (!isViewInitFinished) return false
		val isPlaying = (viewModel.game?.state == SudokuGame.GAME_STATE_PLAYING) // just in case the puzzle is already solved
		menu.findItem(OptionsMenuItems.FILL_IN_MAIN_MARKS_WITH_ALL_VALUES.id)?.isEnabled = isPlaying
		menu.findItem(OptionsMenuItems.CLEAR_ALL_MARKS.id)?.isEnabled = isPlaying
		menu.findItem(OptionsMenuItems.FILL_IN_MAIN_MARKS_WITH_VALID_VALUES.id)?.isEnabled = isPlaying && fillInValidMarksEnabled
		menu.findItem(OptionsMenuItems.SET_CHECKPOINT.id)?.isEnabled = isPlaying
		menu.findItem(OptionsMenuItems.UNDO_TO_CHECKPOINT.id)?.isEnabled = isPlaying && (viewModel.game?.hasUndoCheckpoint() == true)
		menu.findItem(OptionsMenuItems.UNDO_TO_BEFORE_MISTAKE.id)?.isEnabled = isPlaying
		menu.findItem(OptionsMenuItems.HINT.id)?.isEnabled = isPlaying
		menu.findItem(OptionsMenuItems.SOLVE_CELL.id)?.isEnabled = isPlaying
		menu.findItem(OptionsMenuItems.SOLVE_PUZZLE.id)?.isEnabled = isPlaying
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		if (!isViewInitFinished) return super.onOptionsItemSelected(item)

		when (item.itemId) {
			OptionsMenuItems.RESET.id -> {
				resetGameDialog.show(supportFragmentManager)
				return true
			}

			OptionsMenuItems.CLEAR_ALL_MARKS.id -> {
				clearAllMarksDialog.show(supportFragmentManager)
				return true
			}

			OptionsMenuItems.FILL_IN_MAIN_MARKS_WITH_VALID_VALUES.id -> {
				viewModel.game?.fillInMarksManual()
				return true
			}

			OptionsMenuItems.FILL_IN_MAIN_MARKS_WITH_ALL_VALUES.id -> {
				viewModel.game?.fillInMarksWithAllValuesManual()
				return true
			}

			OptionsMenuItems.UNDO_ACTION.id -> {
				val undoneCell = viewModel.game?.undo()
				undoneCell?.let {
					selectCell(it)
					sudokuBoard.onSelectedCellUpdate(undoneCell) // we need additional call in case the undone cell is the same as previous, in this case calling selectCell won't update highlighted value
				}
				return true
			}

			OptionsMenuItems.SOUND_ACTION.id -> {
				bellEnabled = !bellEnabled
				viewModel.settings.bellEnabled = bellEnabled
				item.setIcon(if (bellEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
				return true
			}

			OptionsMenuItems.SETTINGS.id -> {
				settingsLauncher.launch(Intent(this, GameSettingsActivity::class.java))
				return true
			}

			OptionsMenuItems.PUZZLE_INFO.id -> {
				showPuzzleInfo()
				return true
			}

			OptionsMenuItems.HELP.id -> {
				hintsQueue.showHint(R.string.help, R.string.help_text)
				return true
			}

			OptionsMenuItems.SET_CHECKPOINT.id -> {
				viewModel.game?.setUndoCheckpoint()
				return true
			}

			OptionsMenuItems.UNDO_TO_CHECKPOINT.id -> {
				undoToCheckpointDialog.show(supportFragmentManager)
				return true
			}

			OptionsMenuItems.UNDO_TO_BEFORE_MISTAKE.id -> {
				val sudokuGame = viewModel.game ?: return true
				if (!alertIfGameIsNotSolvable()) {
					if (!sudokuGame.board.hasMistakes) {
						SimpleDialog().show(supportFragmentManager, R.string.no_mistakes_found)
					} else {
						undoToBeforeMistakeDialog.show(supportFragmentManager)
					}
				}
				return true
			}

			OptionsMenuItems.SOLVE_PUZZLE.id -> {
				solvePuzzleDialog.show(supportFragmentManager)
				return true
			}

			OptionsMenuItems.SOLVE_CELL.id -> {
				solveOneCellValue.show(supportFragmentManager)
				return true
			}

			OptionsMenuItems.HINT.id -> {
				HintLevelDialogFragment().show(supportFragmentManager, OptionsMenuItems.HINT.name)
				return true
			}
		}

		return super.onOptionsItemSelected(item)
	}

	fun pauseGame() {
		gameTimer?.stop()
		viewModel.game?.apply {
			if (state == SudokuGame.GAME_STATE_PLAYING) pause()
		}
	}

	fun resumeGame() {
		if (viewModel.game?.state == SudokuGame.GAME_STATE_NOT_STARTED) {
			viewModel.game?.start()
		} else if (viewModel.game?.state == SudokuGame.GAME_STATE_PLAYING) {
			viewModel.game?.resume()
		}
		gameTimer?.start()
	}

	private fun highlightCellsAndSelectFirst(cellsToHighlight: Map<HintHighlight, List<Pair<Int, Int>>>) {
		sudokuBoard.sameValueHighlightMode = SameValueHighlightMode.HINTS_ONLY

		sudokuBoard.clearCellSelection()
		cellsToHighlight[HintHighlight.TARGET]?.firstOrNull()?.let { sudokuBoard.moveCellSelectionTo(it.first, it.second) }
		cellsToHighlight.forEach { (highlight, cellList) ->
			cellList.forEach {
				sudokuBoard.board.getCell(it.first, it.second).hintHighlight = highlight // cell object could be recreated in the meantime
			}
		}

		viewModel.highlightedCells = cellsToHighlight
	}

	private fun disableHighlight() {
		val configuredHighlightMode = viewModel.settings.sameValueHighlightMode
		viewModel.highlightedCells?.let {
			it.forEach { (_, cellList) ->
				cellList.forEach {
					sudokuBoard.board.getCell(it.first, it.second).hintHighlight = HintHighlight.NONE // cell object could be recreated in the meantime
				}
			}
			sudokuBoard.sameValueHighlightMode = configuredHighlightMode
		}
		viewModel.highlightedCells = null
	}

	/*
	 * apply next step, will be called from the hint dialog by the user
	 */
	private fun applyNextStep() {
		// get stored next step from the dialog
		val nextStep = NextStepHintDialogFragment.nextStep
		if (nextStep!!.nextStepState == NextStepStates.STEP_FOUND) {
			// set new values in game
			if (nextStep.nextStepActionSetValues.isNotEmpty()) {
				for (valueCells in nextStep.nextStepActionSetValues) {
					for (cell in valueCells.value) {
						viewModel.game?.setCellValue(cell, valueCells.key, false)
					}
				}
			}
			// remove candidates in game
			if (nextStep.nextStepActionRemoveCandidates.isNotEmpty()) {
				for (candidateCells in nextStep.nextStepActionRemoveCandidates) {
					for (cell in candidateCells.value) {
						viewModel.game?.removeNumberFromCellPrimaryMarks(
							cell,
							candidateCells.key,
							false
						)
					}
				}
			}
		}
		disableHighlight()
		// Create a "clean" board and input panel
		sudokuBoard.highlightedValue = 0
		imControlPanel.selectedNumber = 0
		imControlPanel.currentInputMethod?.update()
	}

	private fun restartGame() {
		// Restart game
		pauseGame()
		viewModel.game?.reset()
		sudokuBoard.isReadOnlyPreview = false
		resumeGame()
		optionsMenu.findItem(OptionsMenuItems.SOLVE_PUZZLE.id)?.isEnabled = true
		optionsMenu.findItem(OptionsMenuItems.SOLVE_CELL.id)?.isEnabled = true
		optionsMenu.findItem(OptionsMenuItems.HINT.id)?.isEnabled = true
		updateUndoButtonState()
	}

	/**
	 * Restarts whole activity.
	 */
	private fun restartActivity() {
		startActivity(intent)
		finish()
	}

	private fun selectLastCommandCell() {
		viewModel.lastCommandCell?.let(::selectCell)
	}

	private fun selectCell(cell: Cell) {
		sudokuBoard.moveCellSelectionTo(cell.rowIndex, cell.columnIndex)
	}

	private fun showPuzzleInfo() {
		val game = viewModel.game ?: return
		val folderName = viewModel.getFolderName() ?: ""

		val sb = StringBuilder()
		if (folderName.isNotEmpty()) {
			sb.append(getString(R.string.folder_name)).append(": ").append(folderName).append("\n")
		}

		if (game.created != 0L) {
			sb.append(getString(R.string.created_at, getDateAndTimeForHumans(game.created))).append("\n")
		}

		sb.append(getString(R.string.congrats_mistakes, game.mistakeCounter ?: 0).trim()).append("\n")
		sb.append(getString(R.string.congrats_hints, game.hintUsage ?: 0).trim()).append("\n")

		if (game.userNote.isNotEmpty()) {
			sb.append(getString(R.string.note)).append(" ").append(game.userNote).append("\n")
		}

		if (game.ratingLevel >= 1) {
			sb.append(getString(R.string.rating)).append(":  ")
			sb.append(StrategyLevelIds.getStrategyLevel(game.ratingLevel).getStrategyLevelName(this))
			if (game.ratingValue > 0) {
				sb.append("  ...  ").append(game.ratingValue)
			}
			sb.append("\n")
		}

		val suid = game.suid ?: if (game.solutionCount == 1) SuidGenerator3F().encode(game.board) else null
		if (suid != null) {
			sb.append(getString(R.string.suid_prefix)).append(suid).append("\n")
		}

		SimpleDialog(DialogParams().apply {
			iconId = R.drawable.ic_info
			titleId = R.string.puzzle_info
			message = sb.toString().trim()
		}).show(supportFragmentManager)
	}

	private fun getDateAndTimeForHumans(epochMilliseconds: Long): String {
		val dateTime = LocalDateTime.ofEpochSecond(epochMilliseconds / 1000, 0, localZoneOffset)
		val today = LocalDate.now()
		val yesterday = today.minusDays(1)
		return if (dateTime.isAfter(today.atStartOfDay())) {
			getString(R.string.today_at_time, dateTime.toLocalTime().format(timeFormatter))
		} else if (dateTime.isAfter(yesterday.atStartOfDay())) {
			getString(R.string.yesterday_at_time, dateTime.toLocalTime().format(timeFormatter))
		} else {
			getString(R.string.on_date, dateTime.format(dateTimeFormatter))
		}
	}

	/**
	 * Update the time of game-play.
	 */
	fun updateTime() {
		val playingDuration = viewModel.game?.playingDuration
		if (isShowTime && playingDuration != null) {
			title = playingDurationFormatter.format(playingDuration)
		} else {
			setTitle(R.string.app_name)
		}
	}

	private fun playTone(toneType: Int) {
		if (!bellEnabled) {
			return
		}
		audioService?.apply {
			val volume = getStreamVolume(AudioManager.STREAM_MUSIC)
			if (volume > 0 && !isStreamMute(AudioManager.STREAM_MUSIC) && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
				lifecycleScope.launch {
					val volume = 100 * volume / maxVolume
					ToneGenerator(AudioManager.STREAM_MUSIC, volume).apply {
						startTone(toneType, 400)
						delay(600)
						release()
					}
				}
			}
		}
	}

	/**
	 * Occurs when puzzle is solved.
	 */
	private fun onSolvedListener() {
		pauseGame()
		sudokuBoard.isReadOnlyPreview = true
		if (viewModel.game?.usedSolver() == true) {
			viewModel.game?.playingDuration = 0
			SimpleDialog().show(supportFragmentManager, R.string.used_solver)

		} else {
			var message = getString(R.string.congrats)
			if (isShowTime) message += getString(R.string.congrats_duration, playingDurationFormatter.format(viewModel.game?.playingDuration ?: 0))
			if (isShowMistakeCounter) message = message +
					(viewModel.game?.mistakeCounter?.let { getString(R.string.congrats_mistakes, it) } ?: "") +
					(viewModel.game?.hintUsage?.let { getString(R.string.congrats_hints, it) } ?: "" )
			puzzleSolvedDialog.show(supportFragmentManager, message)
		}
	}

	/**
	 * Occurs when valid 9 entries of a digit are placed.
	 */
	private fun onDigitFinishedListener(digit: Int) {
		if (isHighlightCompletedValues) sudokuBoard.blinkValue(digit)
		playTone(ToneGenerator.TONE_PROP_ACK)
	}

	private fun onInvalidDigitEntryListener() {
		playTone(ToneGenerator.TONE_SUP_ERROR)
	}

	private fun onSelectedNumberChangedListener(value: Int) {
		sudokuBoard.highlightedValue = value
	}

	companion object {
		enum class OptionsMenuItems(
			@StringRes override val titleRes: Int,
			@DrawableRes override val iconRes: Int = 0,
			override val shortcutKey: Char = Char(0),
			override val isAction: Boolean = false
		) : OptionsMenuItem {
			SOUND_ACTION(R.string.sound, R.drawable.ic_sound_off, Char(0), true),
			UNDO_ACTION(R.string.undo, R.drawable.ic_undo, Char(0), true),
			FILL_IN_MAIN_MARKS_WITH_VALID_VALUES(R.string.fill_in_valid_marks, R.drawable.ic_edit),
			FILL_IN_MAIN_MARKS_WITH_ALL_VALUES(R.string.fill_all_marks, R.drawable.ic_edit),
			CLEAR_ALL_MARKS(R.string.clear_all_marks, R.drawable.ic_delete),
			SET_CHECKPOINT(R.string.set_checkpoint),
			UNDO_TO_CHECKPOINT(R.string.undo_to_checkpoint),
			UNDO_TO_BEFORE_MISTAKE(R.string.undo_to_before_mistake),
			HINT(R.string.hint),
			SOLVE_CELL(R.string.solve_cell),
			SOLVE_PUZZLE(R.string.solve_puzzle),
			RESET(R.string.restart, R.drawable.ic_restore, 'r'),
			SETTINGS(R.string.settings, R.drawable.ic_settings, 's'),
			PUZZLE_INFO(R.string.puzzle_info, R.drawable.ic_info),
			HELP(R.string.help, R.drawable.ic_help, 'h');

			override val id = ordinal + Menu.FIRST
		}

		private val localZoneOffset: ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
		private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
		private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

		// This class implements the game clock.  All it does is update the status each tick.
		class GameTimerUpdater(private val onEachStep: () -> Unit) : Timer(1000) {
			override fun step(): Boolean {
				onEachStep()
				return false // Run until explicitly stopped.
			}
		}
	}
}
