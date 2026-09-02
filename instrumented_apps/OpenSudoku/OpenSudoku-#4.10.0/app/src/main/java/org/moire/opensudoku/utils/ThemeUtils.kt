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

package org.moire.opensudoku.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.SameValueHighlightMode
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.THEME_AMOLED
import org.moire.opensudoku.game.THEME_OPENSUDOKU
import org.moire.opensudoku.game.UI_MODE_DARK
import org.moire.opensudoku.game.UI_MODE_LIGHT
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.gui.fragments.THEME_CUSTOM_DARK
import org.moire.opensudoku.gui.fragments.THEME_CUSTOM_LIGHT
import kotlin.math.abs
import kotlin.math.ceil
import com.google.android.material.R.attr as mattr

enum class Colors(val suffix: String, val attr: Int) {
	LINE("Line", mattr.colorOutline),
	SECTOR_LINE("SectorLine", mattr.colorOutline),

	// Editable cells: text, marks and background
	VALUE_TEXT("ValueText", mattr.colorOnSurface),
	MARKS_TEXT("MarksText", mattr.colorOnSurface),
	BACKGROUND("Background", mattr.colorSurface),

	// Not editable cells: text and background
	GIVEN_VALUE_TEXT("GivenValueText", mattr.colorOnPrimaryContainer),
	GIVEN_VALUE_BACKGROUND("GivenValueBackground", mattr.colorPrimaryContainer),

	// Touched cells highlighting: text, marks and background
	TOUCHED_TEXT("TouchedText", mattr.colorOnTertiaryContainer),
	TOUCHED_MARKS_TEXT("TouchedMarksText", mattr.colorOnTertiaryContainer),
	TOUCHED_BACKGROUND("TouchedBackground", mattr.colorTertiaryContainer),

	// Selected cell (color of the selection frame)
	SELECTED_BACKGROUND("SelectedBackground", mattr.colorSurfaceBright),

	// Highlighted cells: text, marks and background
	HIGHLIGHTED_TEXT("HighlightedText", mattr.colorOnSecondaryContainer),
	HIGHLIGHTED_BACKGROUND("HighlightedBackground", mattr.colorSecondaryContainer),
	HIGHLIGHTED_MARKS_TEXT("HighlightedMarksText", mattr.colorOnPrimary),
	HIGHLIGHTED_MARKS_BACKGROUND("HighlightedMarksBackground", android.R.attr.colorPrimary),

	// Invalid value cells: text and background
	TEXT_ERROR("TextError", mattr.colorOnErrorContainer),
	BACKGROUND_ERROR("BackgroundError", mattr.colorErrorContainer),

	// Even numbered box (editable cells): text, marks and background
	EVEN_TEXT("EvenText", mattr.colorOnSurfaceVariant),
	EVEN_MARKS_TEXT("EvenMarksText", mattr.colorOnSurfaceVariant),
	EVEN_BACKGROUND("EvenBackground", mattr.colorSurfaceVariant),
	EVEN_GIVEN_VALUE_TEXT("EvenGivenValueText", mattr.colorOnPrimaryContainer),
	EVEN_GIVEN_VALUE_BACKGROUND("EvenGivenValueBackground", mattr.colorPrimaryVariant),

	// Cell coloring custom colors
	VALUE_1("Value1", R.attr.colorValue1),
	VALUE_2("Value2", R.attr.colorValue2),
	VALUE_3("Value3", R.attr.colorValue3),
	VALUE_4("Value4", R.attr.colorValue4),
	VALUE_5("Value5", R.attr.colorValue5),
	VALUE_6("Value6", R.attr.colorValue6),
	VALUE_7("Value7", R.attr.colorValue7),
	VALUE_8("Value8", R.attr.colorValue8),
	VALUE_9("Value9", R.attr.colorValue9);

	fun key(isLightTheme: Boolean): String {
		return if (isLightTheme) "custom_light_theme_color$suffix" else "custom_dark_theme_color$suffix"
	}

	companion object {
		fun fromSuffix(suffix: String): Colors {
			for (color in entries) {
				if (color.suffix == suffix) {
					return color
				}
			}
			throw IllegalArgumentException("Unknown color suffix: $suffix")
		}
	}
}

enum class Themes(val key: String, val attr: Int, val isLight: Boolean) {
	OPENSUDOKU(THEME_OPENSUDOKU, R.style.AppTheme_OpenSudoku, true),
	AMOLED(THEME_AMOLED, R.style.AppTheme_AMOLED, false),
	LATTE("latte", R.style.AppTheme_Latte, true),
	ESPRESSO("espresso", R.style.AppTheme_Espresso, false),
	SUNRISE("sunrise", R.style.AppTheme_Sunrise, true),
	HONEY_BEE("honey_bee", R.style.AppTheme_HoneyBee, false),
	CRYSTAL("crystal", R.style.AppTheme_Crystal, true),
	MIDNIGHT_BLUE("midnight_blue", R.style.AppTheme_MidnightBlue, false),
	EMERALD("emerald", R.style.AppTheme_Emerald, true),
	FOREST("forest", R.style.AppTheme_Forest, false),
	AMETHYST("amethyst", R.style.AppTheme_Amethyst, true),
	RUBY("ruby", R.style.AppTheme_Ruby, false),
	PAPER_LIGHT("paper_light", R.style.AppTheme_PaperLight, true),
	PAPER_DARK("paper_dark", R.style.AppTheme_PaperDark, false),
	GRAPH_PAPER_LIGHT("graph_paper_light", R.style.AppTheme_GraphPaperLight, true),
	GRAPH_PAPER_DARK("graph_paper_dark", R.style.AppTheme_GraphPaperDark, false),
	HIGH_CONTRAST_LIGHT("high_contrast_light", R.style.AppTheme_HighContrastLight, true),
	HIGH_CONTRAST_DARK("high_contrast_dark", R.style.AppTheme_HighContrastDark, false);

	companion object {
		fun fromKey(key: String): Themes {
			for (theme in entries) {
				if (theme.key == key) return theme
			}
			throw IllegalArgumentException("Unknown Theme key: $key")
		}

		fun getStyleResId(themeName: String): Int {
			return fromKey(themeName).attr
		}

		fun isLightKey(key: String): Boolean {
			if (key == THEME_CUSTOM_LIGHT) return true
			for (theme in entries) {
				if (theme.isLight && theme.key == key) return true
			}
			return false
		}

		fun isDarkKey(key: String): Boolean {
			if (key == THEME_CUSTOM_DARK) return true
			for (theme in entries) {
				if (!theme.isLight && theme.key == key) return true
			}
			return false
		}
	}
}

const val COLOR_DIM_FACTOR = 0.33

object ThemeUtils {
	var sTimestampOfLastThemeUpdate: Long = 0
	var nightModeOverride: Int? = null
	private val alphaShifted = ceil(COLOR_DIM_FACTOR * 0xFF).toInt() shl 24

	fun isDarkTheme(themeCode: String): Boolean {
		return when (themeCode) {
			THEME_CUSTOM_DARK -> return true
			THEME_CUSTOM_LIGHT -> return false
			else -> !Themes.fromKey(themeCode).isLight
		}
	}

	fun getDesiredNightMode(context: Context): Int {
		val settings = GameSettings(context)
		return when (settings.uiMode) {
			UI_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
			UI_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
			else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		}
	}

	fun setThemeFromPreferences(activity: Activity) {
		val settings = GameSettings(activity)

		val currentNightMode = AppCompatDelegate.getDefaultNightMode()
		// A dark theme overrides the UI mode, and is always in night mode. Otherwise, follow the user's preference.
		// If the UI mode has changed then signal the caller to recreate.
		val newNightMode: Int = nightModeOverride ?: getDesiredNightMode(activity)
		if (newNightMode != currentNightMode) {
			AppCompatDelegate.setDefaultNightMode(newNightMode)
		}

		val theme = settings.theme
		when (theme) {
			THEME_CUSTOM_LIGHT -> {
				val baseTheme = settings.getCustomBaseTheme(true)
				if (baseTheme != null) activity.setTheme(Themes.getStyleResId(baseTheme))
				else activity.setTheme(findClosestStyle(activity, true))
			}
			THEME_CUSTOM_DARK -> {
				val baseTheme = settings.getCustomBaseTheme(false)
				if (baseTheme != null) activity.setTheme(Themes.getStyleResId(baseTheme))
				else activity.setTheme(findClosestStyle(activity, false))
			}
			else -> activity.setTheme(Themes.getStyleResId(theme))
		}
	}

	fun getContextThemeColor(context: Context, colorAttribute: Int): Int {
		val attributes = intArrayOf(colorAttribute)
		val themeColors = context.theme.obtainStyledAttributes(attributes)
		val color = themeColors.getColor(0, Color.BLACK)
		themeColors.recycle()
		return color
	}

	/**
	 * Updates the colors of `board` to use colors defined by the `custom_theme_...` preferences.
	 */
	fun applyCustomThemeToSudokuBoardViewFromSharedPreferences(context: Context, board: SudokuBoardView, isLightTheme: Boolean) {
		val customColors = GameSettings(context).customColors(isLightTheme)
		board.apply {
			line.color = customColors.colorLine
			sectorLine.color = customColors.colorSectorLine
			textValue.color = customColors.colorValueText
			textMarkColor = customColors.colorMarkText
			background.color = customColors.colorBackground
			textGivenValue.color = customColors.colorGivenValueText
			backgroundGivenValue.color = customColors.colorGivenValueBackground
			textTouched.color = customColors.colorTouchedText
			textTouchedMarksColor = customColors.colorTouchedMarksText
			backgroundTouched.color = customColors.colorTouchedBackground
			selectedCellFrame.color = customColors.colorSelectedCellFrame
			textHighlighted.color = customColors.colorHighlightedText
			backgroundHighlighted.color = customColors.colorHighlightedBackground
			textMarkHighlightedColor = customColors.colorHighlightedMarksText
			backgroundMarksHighlighted.color = customColors.colorHighlightedMarksBackground
			isEvenBoxColoringEnabled = customColors.colorEvenBoxColoringEnabled
			textEvenCells.color = customColors.colorEvenCellsText
			textEvenCellsMarksColor = customColors.colorEvenCellsMarksText
			backgroundEvenCells.color = customColors.colorEvenCellsBackground
			textEvenGivenValueCells.color = customColors.colorEvenGivenValueCellsText
			backgroundEvenGivenValueCells.color = customColors.colorEvenGivenValueCellsBackground
			textInvalid.color = customColors.colorInvalidText
			backgroundInvalid.color = customColors.colorInvalidBackground
			reloadCustomColors()
		}
	}

	fun applyConfiguredThemeToSudokuBoardView(boardView: SudokuBoardView, context: Context) {
		val theme = GameSettings(context).theme
		applyThemeToSudokuBoardViewFromContext(theme, boardView, context)
	}

	fun applyThemeToSudokuBoardViewFromContext(themeKey: String, board: SudokuBoardView, context: Context) {
		if (themeKey == THEME_CUSTOM_LIGHT || themeKey == THEME_CUSTOM_DARK) {
			applyCustomThemeToSudokuBoardViewFromSharedPreferences(context, board, themeKey == THEME_CUSTOM_LIGHT)
		} else {
			// If the theme implies dark mode then show the dark mode colours. Do this by constructing a new context with a `Configuration` that forces UI_MODE_NIGHT_YES.
			// This will cause the later colour lookups to use the version of the theme from values-night/.
			// Doing this with AppCompatDelegate.setDefaultNightMode() is destructive -- the activity/fragment/dialog would be destroyed and recreated whenever
			// the user previews a new theme.
			val config = if (isDarkTheme(themeKey)) {
				context.resources.configuration.apply { uiMode = Configuration.UI_MODE_NIGHT_YES or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) }
			} else {
				context.resources.configuration.apply { uiMode = Configuration.UI_MODE_NIGHT_NO or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) }
			}
			val configContext = context.createConfigurationContext(config)
			val themeWrapper = ContextThemeWrapper(configContext, Themes.getStyleResId(themeKey))
			board.setAllColorsFromThemedContext(themeWrapper)
		}
		board.invalidate()
	}

	fun prepareBoardPreviewView(boardView: SudokuBoardView) {
		boardView.apply {
			isFocusable = false
			// This provides a sample of an in-progress game that will demonstrate all of the possible scenarios that
			// have different theme colors applied to them.
			val boardData = SudokuBoard.deserialize(
				"version: 4\n" +
						"9|32|0|0|0|42|10|1|3|0|0|1|0|2|2|1|7|0|0|0|5|32|0|1|0|40|8|1|8|0|0|0|1|0|0|1|" +
						"5|32|0|0|0|42|10|1|8|0|0|1|0|6|2|1|1|0|0|0|0|36|32|1|0|40|8|1|7|0|0|0|9|0|0|1|" +
						"0|32|0|1|7|32|0|1|1|0|0|1|9|0|0|0|4|0|0|1|8|32|32|1|3|32|0|1|2|0|0|1|5|0|0|0|" +
						"9|128|0|1|1|0|0|0|5|0|0|1|6|128|0|1|2|128|0|0|9|0|0|1|0|192|128|1|3|0|0|1|0|192|128|1|" +
						"0|160|160|1|0|32|32|1|7|0|0|1|0|133|128|1|5|128|0|1|0|5|0|1|2|0|0|1|9|0|0|0|4|128|0|0|" +
						"2|128|128|1|3|0|0|1|9|0|0|1|7|128|0|1|0|128|128|1|4|0|0|0|1|0|0|1|5|0|0|0|6|128|0|0|" +
						"0|64|0|1|8|0|0|0|4|0|0|1|5|0|0|1|6|0|0|0|2|64|0|0|9|64|0|0|1|0|0|1|3|64|0|1|" +
						"0|65|1|1|9|0|0|1|2|0|0|0|4|129|0|0|3|128|128|1|0|65|64|1|5|192|0|1|6|0|0|1|0|192|128|1|" +
						"3|65|1|1|5|0|0|0|6|0|0|0|0|129|128|1|9|128|0|1|0|65|64|1|0|192|128|1|4|0|0|1|2|192|0|1|",
				false
			).first

			if (GameSettings(context).isCellColoringExperimentalEnabled) {
				var colorIndex = 1
				for (r in 3..5) {
					for (c in 6..8) {
						val cell = boardData.getCell(r, c)
						cell.color = colorIndex++
						cell.value = 0
						cell.isEditable = true
					}
				}
			}

			board = boardData
			board.validateAllCells()
			moveCellSelectionTo(2, 1)
			touchedCell = boardView.board.getCell(1, 3)
			sameValueHighlightMode = SameValueHighlightMode.NUMBERS_AND_MARK_VALUES
			highlightDirectlyWrongValues = true
			highlightInvalidPencilMarks = true
		}
	}

	fun findClosestStyle(context: Context, isLightTheme: Boolean): Int {
		var minDifference = Float.MAX_VALUE
		var closestTheme = Themes.OPENSUDOKU
		var difference: Float
		for (theme in Themes.entries) {
			if (theme.isLight != isLightTheme) continue
			difference = getThemeDistance(context, theme, isLightTheme)
			if (difference < minDifference) {
				minDifference = difference
				closestTheme = theme
			}
		}
		return closestTheme.attr
	}

	private fun getThemeDistance(context: Context, theme: Themes, isLightTheme: Boolean): Float {
		val settings = GameSettings(context)
		val themeWrapper = ContextThemeWrapper(context, theme.attr)

		val primaryDistance = getColorDistance(
			getContextThemeColor(themeWrapper, Colors.HIGHLIGHTED_BACKGROUND.attr),
			settings.customColors(isLightTheme).colorHighlightedBackground,
			isLightTheme
		)
		val secondaryDistance = getColorDistance(
			getContextThemeColor(themeWrapper, Colors.LINE.attr),
			settings.customColors(isLightTheme).colorLine,
			isLightTheme
		)
		val tertiaryDistance = getColorDistance(
			getContextThemeColor(themeWrapper, Colors.GIVEN_VALUE_BACKGROUND.attr),
			settings.customColors(isLightTheme).colorGivenValueBackground,
			isLightTheme
		)

		return 4 * primaryDistance + 2 * secondaryDistance + tertiaryDistance
	}

	private fun getColorDistance(themeColor: Int, customColor: Int, isLightTheme: Boolean): Float {
		val themeHSL = HSL(themeColor).colorAsHSL
		val customHSL = HSL(customColor).colorAsHSL
		var lightnessDiff = abs(themeHSL[2] - customHSL[2])
		if ((isLightTheme && themeHSL[2] < customHSL[2]) || (!isLightTheme && themeHSL[2] > customHSL[2])) {
			lightnessDiff *= 2 // for dark theme lighter color makes more difference
		}

		// weights: the most important factor is hue distance (0-360), then lightness (0-120) and saturation (0-40)
		return abs (themeHSL[0] - customHSL[0]) + abs(themeHSL[1] - customHSL[1]) * 40f + lightnessDiff * 120f
	}

	/**
	 * Adjusts the alpha channel of the given color.
	 *
	 * @param color color to adjust
	 * @return The adjusted color
	 */
	@ColorInt
	fun dimmedColor(@ColorInt color: Int): Int {
		val colorOnly = color and 0x00ffffff
		return colorOnly or alphaShifted
	}
}

class HSL(@ColorInt color: Int) {
	val colorAsHSL = FloatArray(3)
	val color
		get() = ColorUtils.HSLToColor(colorAsHSL)
	init {
		ColorUtils.colorToHSL(color, colorAsHSL)
	}
	fun hue(shift: Int): HSL {
		colorAsHSL[0] = (colorAsHSL[0] + shift.toFloat()) % 360f
		return this
	}
	fun saturation(shift: Float): HSL {
		colorAsHSL[1] = (colorAsHSL[1] + shift).coerceIn(0f, 1f)
		return this
	}
	fun lightness(shift: Float): HSL {
		colorAsHSL[2] = (colorAsHSL[2] + shift).coerceIn(0f, 1f)
		return this
	}
}