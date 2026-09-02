/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2025-2026 by Open Sudoku authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.moire.opensudoku.game

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.moire.opensudoku.R
import org.moire.opensudoku.gui.Tag
import org.moire.opensudoku.gui.fragments.UI_MODE_KEY
import org.moire.opensudoku.gui.importing.ImportStrategyTypes
import org.moire.opensudoku.gui.inputmethod.InputMethod
import org.moire.opensudoku.gui.screen.puzzle_list.PuzzleListSorter
import org.moire.opensudoku.utils.Colors

@Suppress("HardCodedStringLiteral")

const val MOST_RECENTLY_PLAYED_PUZZLE_ID = "most_recently_played_puzzle_id"

private const val DOUBLE_MARKS_MODE_KEY = "double_marks_mode"
private const val EVEN_BOX_COLORING_ENABLED_LIGHT = "pref_enable_even_box_coloring_light"
private const val EVEN_BOX_COLORING_ENABLED_DARK = "pref_enable_even_box_coloring_dark"
private const val HIGHLIGHT_WRONG_VALUES = "highlight_wrong_values_mode"
private const val HIGHLIGHT_TOUCHED_CELL = "highlight_touched_cell"
private const val HIGHLIGHT_COMPLETED_VALUES = "highlight_completed_values"
private const val SHOW_NUMBER_TOTALS = "show_number_totals"
private const val FILTER_STATE_NOT_STARTED = "filter" + SudokuGame.GAME_STATE_NOT_STARTED
private const val FILTER_STATE_PLAYING = "filter" + SudokuGame.GAME_STATE_PLAYING
private const val FILTER_STATE_SOLVED = "filter" + SudokuGame.GAME_STATE_COMPLETED
private const val FILTER_STATE_WITH_MISTAKES = "filter_with_mistakes"
private const val FILTER_STATE_WITH_HINTS = "filter_with_hints"
private const val RANDOM_ONLY_UNSOLVED = "random_only_unsolved"
private const val RANDOM_ONLY_WITH_MISTAKES = "random_only_with_mistakes"
private const val RANDOM_ONLY_WITH_HINTS = "random_only_with_hints"
private const val SORT_TYPE = "sort_type"
private const val SORT_ORDER = "sort_order"
private const val SHOW_TIME = "show_time"
private const val SHOW_MISTAKE_COUNTER = "show_mistake_counter"
private const val SHOW_PUZZLE_LISTS_ON_STARTUP = "show_puzzle_lists_on_startup"
private const val SCHEMA_VERSION = "schema_version"
private const val SCREEN_BORDER_SIZE = "screen_border_size"
private const val HIGHLIGHT_SIMILAR_CELLS = "highlight_similar_cells"
private const val FILL_IN_VALID_MARKS_ENABLED = "fill_in_marks_enabled"
private const val VALIDATE_PENCIL_MARKS = "validate_pencil_marks"
private const val BELL_ENABLED = "bell_enabled"
private const val REMOVE_MARKS_ON_INPUT = "remove_marks_on_input"
private const val IM_MOVE_RIGHT_ON_INSERT = "im_move_right_on_insert_move_right"
private const val SELECTED_INPUT_MODES = "selected_input_modes"
private const val IM_POPUP = "im_popup"
private const val INSERT_ON_TAP = "insert_on_tap"
private const val BIDIRECTIONAL_SELECTION = "bidirectional_selection"
private const val HIGHLIGHT_SIMILAR = "highlight_similar"
private const val SELECT_ON_TAP = "select_on_tap"
private const val BACKGROUND_COLOR_ENABLED = "background_color_enabled"
private const val SHOW_HINTS = "show_hints"
private const val EXPERIMENTAL_FEATURES_ENABLED = "experimental_features_v2"
const val LIGHT_THEME_KEY = "light_theme"
const val DARK_THEME_KEY = "dark_theme"
const val CUSTOM_LIGHT_BASE_THEME_KEY = "custom_light_base_theme"
const val CUSTOM_DARK_BASE_THEME_KEY = "custom_dark_base_theme"
const val THEME_OPENSUDOKU = "opensudoku"
const val THEME_AMOLED = "amoled"
private const val HIGHLIGHT_SIMILAR_MARKS_MODE = "highlight_similar_marks_mode"
private const val VALUES = "values"
const val UI_MODE_SYSTEM = "system"
const val UI_MODE_LIGHT = "light"
const val UI_MODE_DARK = "dark"
private const val ACTIVE_INPUT_METHOD_INDEX = "active_input_method_index"
private const val LAST_GAME_SELECTED_ROW = "last_game_selected_row"
private const val LAST_GAME_SELECTED_COLUMN = "last_game_selected_column"
private const val INPUT_METHOD_SELECTED_NUMBER = "input_method_selected_number"
private const val INPUT_METHOD_EDIT_MODE = "input_method_edit_mode"
private const val OLD_COLOR_PREFERENCE_PREFIX = "custom_theme_color"
private const val FONT_SELECTOR = "display_font_selector"

class GameSettings(val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val customLightColors = CustomColorsPreferences(true)
    val customDarkColors = CustomColorsPreferences(false)

    var schemaVersion
        get() = preferences.getInt(SCHEMA_VERSION, 20240205)
        set(value) = preferences.edit { putInt(SCHEMA_VERSION, value) }
    val screenBorderSize
        get() = preferences.getInt(SCREEN_BORDER_SIZE, 0)
    var sortType
        get() = preferences.getInt(SORT_TYPE, PuzzleListSorter.SORT_BY_CREATED)
        set(value) = preferences.edit { putInt(SORT_TYPE, value) }
    var lastGameSelectedRow
        get() = preferences.getInt(LAST_GAME_SELECTED_ROW, -1)
        set(value) = preferences.edit { putInt(LAST_GAME_SELECTED_ROW, value) }
    var lastGameSelectedColumn
        get() = preferences.getInt(LAST_GAME_SELECTED_COLUMN, -1)
        set(value) = preferences.edit { putInt(LAST_GAME_SELECTED_COLUMN, value) }
    var selectedInputMethod
        get() = preferences.getInt(ACTIVE_INPUT_METHOD_INDEX, -1)
        set(value) = preferences.edit { putInt(ACTIVE_INPUT_METHOD_INDEX, value) }
    var lastGameKeyboardSelectedNumber
        get() = preferences.getInt(INPUT_METHOD_SELECTED_NUMBER, 0)
        set(value) = preferences.edit { putInt(INPUT_METHOD_SELECTED_NUMBER, value) }
    var lastGameKeyboardEditMode
        get() = preferences.getInt(INPUT_METHOD_EDIT_MODE, InputMethod.MODE_EDIT_VALUE)
        set(value) = preferences.edit { putInt(INPUT_METHOD_EDIT_MODE, value) }

    // Long
    var lastGamePuzzleId: Long
		get() = preferences.getLong(MOST_RECENTLY_PLAYED_PUZZLE_ID, 0)
        set(value) {
            if (value != lastGamePuzzleId) {
                preferences.edit { putLong(MOST_RECENTLY_PLAYED_PUZZLE_ID, value) }
                lastGameSelectedRow = -1
                lastGameSelectedColumn = -1
                lastGameKeyboardEditMode = InputMethod.MODE_EDIT_VALUE
                lastGameKeyboardSelectedNumber = 0
            }
		}

    // Boolean
    val fillInMarksEnabled
        get() = preferences.getBoolean(FILL_IN_VALID_MARKS_ENABLED, false)
    val isValidatePencilMarks
        get() = preferences.getBoolean(VALIDATE_PENCIL_MARKS, false)
    var bellEnabled
        get() = preferences.getBoolean(BELL_ENABLED, false)
        set(value) = preferences.edit { putBoolean(BELL_ENABLED, value) }
    val highlightWrongValues
        get() = WrongValueHighlightMode.fromString(preferences.getString(HIGHLIGHT_WRONG_VALUES, ""))
    val highlightTouchedCell
        get() = preferences.getBoolean(HIGHLIGHT_TOUCHED_CELL, true)
    val isRemoveMarksOnInput
        get() = preferences.getBoolean(REMOVE_MARKS_ON_INPUT, true)
    val isShowTime
        get() = preferences.getBoolean(SHOW_TIME, true)
    val isShowMistakeCounter
        get() = preferences.getBoolean(SHOW_MISTAKE_COUNTER, true)
    val isHighlightCompletedValues
        get() = preferences.getBoolean(HIGHLIGHT_COMPLETED_VALUES, true)
    val isMoveCellSelectionOnPress
        get() = preferences.getBoolean(IM_MOVE_RIGHT_ON_INSERT, false)
    val selectedInputModes
        get() = preferences.getStringSet(SELECTED_INPUT_MODES, setOf(IM_POPUP, INSERT_ON_TAP, SELECT_ON_TAP))
    val isPopupEnabled
        get() = selectedInputModes?.contains(IM_POPUP) != false
    val isInsertOnTapEnabled
        get() = selectedInputModes?.contains(INSERT_ON_TAP) != false
    val isSelectOnTapEnabled
        get() = selectedInputModes?.contains(SELECT_ON_TAP) != false
    val isBidirectionalSelection
        get() = preferences.getBoolean(BIDIRECTIONAL_SELECTION, true)
    val isHighlightSimilar
        get() = preferences.getBoolean(HIGHLIGHT_SIMILAR, true)
    val isShowNumberTotals
        get() = preferences.getBoolean(SHOW_NUMBER_TOTALS, false)
    val doubleMarksMode
        get() = DoubleMarksMode.fromString(preferences.getString(DOUBLE_MARKS_MODE_KEY, ""))
    val isBackgroundColorEnabled
        get() = isCellColoringExperimentalEnabled && preferences.getBoolean(BACKGROUND_COLOR_ENABLED, true)
    val isShowHints
        get() = preferences.getBoolean(SHOW_HINTS, true)
    val experimentalFeatures
        get() = preferences.getStringSet(EXPERIMENTAL_FEATURES_ENABLED, emptySet())
    val isScanPuzzleEnabled
        get() = experimentalFeatures?.contains("scan_puzzle") == true
    val isCellColoringExperimentalEnabled
        get() = experimentalFeatures?.contains("cell_coloring") == true

    // puzzle filter saved checkboxes states
    var isShowNotStarted
        get() = preferences.getBoolean(FILTER_STATE_NOT_STARTED, true)
        set(value) = preferences.edit { putBoolean(FILTER_STATE_NOT_STARTED, value) }
    var isShowPlaying
        get() = preferences.getBoolean(FILTER_STATE_PLAYING, true)
        set(value) = preferences.edit { putBoolean(FILTER_STATE_PLAYING, value) }
    var isShowCompleted
        get() = preferences.getBoolean(FILTER_STATE_SOLVED, true)
        set(value) = preferences.edit { putBoolean(FILTER_STATE_SOLVED, value) }
    var isShowWithMistakes
        get() = preferences.getBoolean(FILTER_STATE_WITH_MISTAKES, false)
        set(value) = preferences.edit { putBoolean(FILTER_STATE_WITH_MISTAKES, value) }
    var isShowWithHints
        get() = preferences.getBoolean(FILTER_STATE_WITH_HINTS, false)
        set(value) = preferences.edit { putBoolean(FILTER_STATE_WITH_HINTS, value) }

    // random puzzle filter saved checkboxes states
    var randomOnlyUnsolved
        get() = preferences.getBoolean(RANDOM_ONLY_UNSOLVED, true)
        set(value) = preferences.edit { putBoolean(RANDOM_ONLY_UNSOLVED, value) }
    var randomOnlyWithMistakes
        get() = preferences.getBoolean(RANDOM_ONLY_WITH_MISTAKES, false)
        set(value) = preferences.edit { putBoolean(RANDOM_ONLY_WITH_MISTAKES, value) }
    var randomOnlyWithHints
        get() = preferences.getBoolean(RANDOM_ONLY_WITH_HINTS, false)
        set(value) = preferences.edit { putBoolean(RANDOM_ONLY_WITH_HINTS, value) }

    var isSortAscending
        get() = preferences.getBoolean(SORT_ORDER, false)
        set(value) = preferences.edit { putBoolean(SORT_ORDER, value) }
    val isShowPuzzleListOnStartup
        get() = preferences.getBoolean(SHOW_PUZZLE_LISTS_ON_STARTUP, false)

    val theme: String
		get() {
            val currentUIMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return if (currentUIMode != Configuration.UI_MODE_NIGHT_YES) preferences.getString(LIGHT_THEME_KEY, THEME_OPENSUDOKU)!!
                else preferences.getString(DARK_THEME_KEY, THEME_AMOLED) ?: THEME_AMOLED
		}
    var uiMode
        get() = preferences.getString(UI_MODE_KEY, UI_MODE_SYSTEM)!!
        set(value) = preferences.edit { putString(UI_MODE_KEY, value) }

	var selectedFont: String
		get() = preferences.getString(FONT_SELECTOR, "123456789")!!
		set(value) = preferences.edit { putString(FONT_SELECTOR,value) }

    // Custom types
    val importStrategy
        get() = ImportStrategyTypes.convertKey2Type(preferences.getString(Tag.IMPORT_STRATEGY, ""))

    val sameValueHighlightMode: SameValueHighlightMode
        get() {
            return if (preferences.getBoolean(HIGHLIGHT_SIMILAR_CELLS, true)) {
                SameValueHighlightMode.fromString(preferences.getString(HIGHLIGHT_SIMILAR_MARKS_MODE, VALUES))
            } else {
                SameValueHighlightMode.NONE
            }
        }

    fun registerOnSharedPreferenceChangeListener(function: SharedPreferences. OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(function)
    }

    fun unregisterOnSharedPreferenceChangeListener(function: SharedPreferences. OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(function)
    }

    fun customColors(isLightTheme: Boolean): CustomColorsPreferences {
        return if (isLightTheme) customLightColors else customDarkColors
    }

    fun getCustomBaseTheme(isLightTheme: Boolean): String? {
        return preferences.getString(if (isLightTheme) CUSTOM_LIGHT_BASE_THEME_KEY else CUSTOM_DARK_BASE_THEME_KEY, null)
    }

    fun setCustomBaseTheme(isLightTheme: Boolean, themeKey: String) {
        preferences.edit { putString(if (isLightTheme) CUSTOM_LIGHT_BASE_THEME_KEY else CUSTOM_DARK_BASE_THEME_KEY, themeKey) }
    }

    inner class CustomColorsPreferences(val isLightTheme: Boolean) {
        val defaultTheme = ThemeUtil(context, if (isLightTheme) R.style.AppTheme_OpenSudoku else R.style.AppTheme_AMOLED)
        val colorEvenBoxColoringEnabled
            get() = isEvenBoxColoringEnabled(isLightTheme)

        private fun getColor(color: Colors): Int {
            return preferences.getInt(color.key(isLightTheme), defaultTheme.getColor(color.attr))
        }

        val colorPrimary
            get() = colorHighlightedBackground
        val colorSecondary
            get() = colorLine
        val colorTertiary
            get() = colorTouchedBackground
        val colorLine
            get() = getColor(Colors.LINE)
        val colorSectorLine
            get() = getColor(Colors.SECTOR_LINE)
        val colorValueText
            get() = getColor(Colors.VALUE_TEXT)
        val colorMarkText
            get() = getColor(Colors.MARKS_TEXT)
        val colorBackground
            get() = getColor(Colors.BACKGROUND)
        val colorGivenValueText
            get() = getColor(Colors.GIVEN_VALUE_TEXT)
        val colorGivenValueBackground
            get() = getColor(Colors.GIVEN_VALUE_BACKGROUND)
        val colorTouchedText
            get() = getColor(Colors.TOUCHED_TEXT)
        val colorTouchedMarksText
            get() = getColor(Colors.TOUCHED_MARKS_TEXT)
        val colorTouchedBackground
            get() = getColor(Colors.TOUCHED_BACKGROUND)
        val colorSelectedCellFrame
            get() = getColor(Colors.SELECTED_BACKGROUND)
        val colorHighlightedText
            get() = getColor(Colors.HIGHLIGHTED_TEXT)
        val colorHighlightedBackground
            get() = getColor(Colors.HIGHLIGHTED_BACKGROUND)
        val colorHighlightedMarksText
            get() = getColor(Colors.HIGHLIGHTED_MARKS_TEXT)
        val colorHighlightedMarksBackground
            get() = getColor(Colors.HIGHLIGHTED_MARKS_BACKGROUND)
        val colorEvenCellsText
            get() = getColor(Colors.EVEN_TEXT)
        val colorEvenCellsMarksText
            get() = getColor(Colors.EVEN_MARKS_TEXT)
        val colorEvenCellsBackground
            get() = getColor(Colors.EVEN_BACKGROUND)
        val colorEvenGivenValueCellsText
            get() = getColor(Colors.EVEN_GIVEN_VALUE_TEXT)
        val colorEvenGivenValueCellsBackground
            get() = getColor(Colors.EVEN_GIVEN_VALUE_BACKGROUND)
        val colorInvalidText
            get() = getColor(Colors.TEXT_ERROR)
        val colorInvalidBackground
            get() = getColor(Colors.BACKGROUND_ERROR)
    }

    fun isEvenBoxColoringEnabled(isLightTheme: Boolean): Boolean {
        return preferences.getBoolean(if (isLightTheme) EVEN_BOX_COLORING_ENABLED_LIGHT else EVEN_BOX_COLORING_ENABLED_DARK, false)
    }

    fun hasPreference(preferenceKey: String): Boolean {
        return preferences.contains(preferenceKey)
    }

    fun removePreference(preferenceKey: String) {
        preferences.edit { remove(preferenceKey) }
    }

    fun setIntPreference(preferenceKey: String, value: Int) {
        preferences.edit { putInt(preferenceKey, value) }
    }

    fun setBoolPreference(preferenceKey: String, value: Boolean) {
        preferences.edit { putBoolean(preferenceKey, value) }
    }

    fun getIntPreference(preferenceKey: String): Int {
        return preferences.getInt(preferenceKey, 0)
    }

    fun restoreAllAvailableOldCustomColors() {
        var isEvenColoringEnabled = false
        for (color in Colors.entries) {
            val oldSuffix = when (color) { // color keys renamed
				Colors.GIVEN_VALUE_TEXT -> "ReadOnlyText"
				Colors.GIVEN_VALUE_BACKGROUND -> "ReadOnlyBackground"
				else -> color.suffix
			}

            if (hasPreference("$OLD_COLOR_PREFERENCE_PREFIX${oldSuffix}")) {
                val oldValue = getIntPreference("$OLD_COLOR_PREFERENCE_PREFIX${oldSuffix}")
                setIntPreference(color.key(true), oldValue)
                setIntPreference(color.key(false), oldValue)

				when (color) { // new colors that previously were using the same as the current color value
					Colors.HIGHLIGHTED_BACKGROUND -> {
						setIntPreference(Colors.HIGHLIGHTED_MARKS_BACKGROUND.key(true), oldValue)
						setIntPreference(Colors.HIGHLIGHTED_MARKS_BACKGROUND.key(false), oldValue)
					}
					Colors.GIVEN_VALUE_TEXT -> {
						setIntPreference(Colors.EVEN_GIVEN_VALUE_TEXT.key(true), oldValue)
						setIntPreference(Colors.EVEN_GIVEN_VALUE_TEXT.key(false), oldValue)
					}
					Colors.GIVEN_VALUE_BACKGROUND -> {
						setIntPreference(Colors.EVEN_GIVEN_VALUE_BACKGROUND.key(true), oldValue)
						setIntPreference(Colors.EVEN_GIVEN_VALUE_BACKGROUND.key(false), oldValue)
					}
                    else -> {} // no op
                }

                // previously the even coloring was decided by EVEN_BACKGROUND being transparent, now it's a separate checkbox
                if (color == Colors.EVEN_BACKGROUND && oldValue != Color.TRANSPARENT) {
                    isEvenColoringEnabled = true
                }
            }
        }
        setBoolPreference(EVEN_BOX_COLORING_ENABLED_LIGHT, isEvenColoringEnabled)
        setBoolPreference(EVEN_BOX_COLORING_ENABLED_DARK, isEvenColoringEnabled)

        removeOldCustomColorsSettings()
    }

    fun removeOldCustomColorsSettings() {
        for (color in Colors.entries) {
            removePreference("$OLD_COLOR_PREFERENCE_PREFIX${color.suffix}")
        }
        removePreference("${OLD_COLOR_PREFERENCE_PREFIX}ReadOnlyText")
        removePreference("${OLD_COLOR_PREFERENCE_PREFIX}ReadOnlyBackground")
    }

    fun hasOldCustomColorsSettings(): Boolean {
        return hasPreference("$OLD_COLOR_PREFERENCE_PREFIX${Colors.LINE.suffix}")
    }

    fun updateDoublePencilMarksSetting() {
        if (hasPreference("double_marks_enabled")) {
            if (preferences.getBoolean("double_marks_enabled", true)) {
                preferences.edit { putString(DOUBLE_MARKS_MODE_KEY, DoubleMarksMode.WITH_CORNER.name) }
            } else {
                preferences.edit { putString(DOUBLE_MARKS_MODE_KEY, DoubleMarksMode.SINGLE.name) }
            }
            removePreference("double_marks_enabled")
        }
    }

    class ThemeUtil(val context: Context, val themeResId: Int) {
        val themeContext = ContextThemeWrapper(context, themeResId)
        val typedValue = TypedValue()

        @ColorInt
        fun getColor(attrResId: Int): Int {
            if (themeContext.theme.resolveAttribute(attrResId, typedValue, true)) {
                // Check if the resolved value is a color
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return typedValue.data
                } else if (typedValue.type == TypedValue.TYPE_STRING) { // typedValue.resourceId may be the ID of @color resource
                    try {
                        return typedValue.data // If it's a reference typedValue.data should already hold the resolved color reference
                    } catch (e: Exception) {
                        Log.e("ThemeUtil", "Attribute $attrResId was a reference, but failed to resolve color: ${e.message}")
                    }
                } else {
                    Log.w("ThemeUtil", "Attribute $attrResId found but was not a color type: ${typedValue.type}")
                }
            }

            Log.e("ThemeUtil", "Attribute $attrResId not found or not a color in theme $themeResId")
            return Color.BLACK
        }
    }
}

enum class WrongValueHighlightMode {
    OFF,
    DIRECT,
    INDIRECT;

    companion object {
        fun fromString(string: String?): WrongValueHighlightMode {
            return when (string) {
                "off" -> OFF
                "direct" -> DIRECT
                else -> INDIRECT
            }
        }
    }
}

enum class SameValueHighlightMode {
    NONE,
    NUMBERS,
    NUMBERS_AND_MARK_VALUES,
    NUMBERS_AND_MARK_CELLS,
    HINTS_ONLY; // this is used only for temporarily highlighting cells related to displayed hint (not to be used after selecting the "Hint" button)

    companion object {
        fun fromString(string: String?): SameValueHighlightMode {
            return when (string) {
                "values" -> NUMBERS_AND_MARK_VALUES
                "cells" -> NUMBERS_AND_MARK_CELLS
                else -> NUMBERS
            }
        }
    }
}

enum class DoubleMarksMode {
    SINGLE,
    WITH_CORNER,
    WITH_POSITIONAL;

    companion object {
        fun fromString(string: String?): DoubleMarksMode {
            return when (string) {
                "single" -> SINGLE
                "positional" -> WITH_POSITIONAL
                else -> WITH_CORNER
            }
        }
    }
}
