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
package org.moire.opensudoku.utils

import android.content.Context
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.gui.fragments.THEME_CUSTOM_DARK
import org.moire.opensudoku.gui.fragments.THEME_CUSTOM_LIGHT

object BackgroundColorPrefs {

	private val defaultHues = floatArrayOf(
		0f,   // Red
		30f,  // Orange
		60f,  // Yellow
		120f, // Green
		180f, // Cyan
		210f, // Light Blue
		240f, // Blue
		280f, // Purple
		320f  // Pink
	)

	fun getThemeKey(index: Int, isLightTheme: Boolean): String {
		return Colors.fromSuffix("Value$index").key(isLightTheme)
	}

	fun getColor(context: Context, index: Int, isLightThemeOverride: Boolean? = null): Int {
		val themeKey = GameSettings(context).theme
		val isLightTheme = isLightThemeOverride ?: !ThemeUtils.isDarkTheme(themeKey)
		val isCustomTheme = themeKey == THEME_CUSTOM_LIGHT || themeKey == THEME_CUSTOM_DARK

		if (isLightThemeOverride != null || isCustomTheme) {
			val prefs = PreferenceManager.getDefaultSharedPreferences(context)
			val key = getThemeKey(index, isLightTheme)
			if (prefs.contains(key)) {
				return prefs.getInt(key, 0)
			}
			
			// Fallback to old storage for migration
			val oldPrefs = context.getSharedPreferences("settings_background_colors", Context.MODE_PRIVATE)
			val oldKey = if (isLightTheme) "light_color_$index" else "dark_color_$index"
			if (oldPrefs.contains(oldKey)) {
				val color = oldPrefs.getInt(oldKey, 0)
				// Migrate
				prefs.edit().putInt(key, color).apply()
				return color
			}
			// Legacy global key
			val legacyKey = "color_$index"
			if (oldPrefs.contains(legacyKey)) {
				val color = oldPrefs.getInt(legacyKey, 0)
				// Migrate
				prefs.edit().putInt(key, color).apply()
				return color
			}
		}

		// Try to get from theme
		val attr = getAttrForIndex(index)
		val typedValue = TypedValue()
		if (context.theme.resolveAttribute(attr, typedValue, true)) {
			return if (typedValue.resourceId != 0) {
				context.getColor(typedValue.resourceId)
			} else {
				typedValue.data
			}
		}

		return getDefaultColor(context, index)
	}

	private fun getAttrForIndex(index: Int): Int {
		return when (index) {
			1 -> R.attr.colorValue1
			2 -> R.attr.colorValue2
			3 -> R.attr.colorValue3
			4 -> R.attr.colorValue4
			5 -> R.attr.colorValue5
			6 -> R.attr.colorValue6
			7 -> R.attr.colorValue7
			8 -> R.attr.colorValue8
			9 -> R.attr.colorValue9
			else -> com.google.android.material.R.attr.colorSurface
		}
	}

	fun setColor(context: Context, index: Int, color: Int, isLightThemeOverride: Boolean? = null) {
		val themeKey = GameSettings(context).theme
		val isLightTheme = isLightThemeOverride ?: !ThemeUtils.isDarkTheme(themeKey)
		val prefs = PreferenceManager.getDefaultSharedPreferences(context)
		prefs.edit().putInt(getThemeKey(index, isLightTheme), color).apply()
	}

	fun resetColors(context: Context, isLightThemeOverride: Boolean? = null) {
		val themeKey = GameSettings(context).theme
		val isLightTheme = isLightThemeOverride ?: !ThemeUtils.isDarkTheme(themeKey)
		val prefs = PreferenceManager.getDefaultSharedPreferences(context)
		val editor = prefs.edit()
		for (i in 1..9) {
			editor.remove(getThemeKey(i, isLightTheme))
		}
		editor.apply()
	}

	private fun getDefaultColor(context: Context, index: Int): Int {
		val backgroundColor = ThemeUtils.getContextThemeColor(context, com.google.android.material.R.attr.colorSurface)
		val isDark = ColorUtils.calculateLuminance(backgroundColor) < 0.5

		val hue = defaultHues[index - 1]
		return if (isDark) {
			// For dark themes: darker, more saturated colors
			ColorUtils.HSLToColor(floatArrayOf(hue, 0.6f, 0.2f))
		} else {
			// For light themes: very light, pastel colors
			ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.9f))
		}
	}
}
