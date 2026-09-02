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

package org.moire.opensudoku.gui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import net.margaritov.preference.colorpicker.ColorPickerDialog
import net.margaritov.preference.colorpicker.ColorPickerPreference
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.game.GameSettings.ThemeUtil
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.gui.ThemeColorPreference
import org.moire.opensudoku.utils.Colors
import org.moire.opensudoku.utils.HSL
import org.moire.opensudoku.utils.ThemeUtils
import org.moire.opensudoku.utils.Themes

/**
 * Preview and set a custom app theme.
 */

class CustomDarkThemeFragment : CustomThemeFragment(false)
class CustomLightThemeFragment : CustomThemeFragment(true)

private const val COLOR_PREFERENCE_PREFIX_LIGHT = "custom_light_theme_color"
private const val COLOR_PREFERENCE_PREFIX_DARK = "custom_dark_theme_color"

@Keep
open class CustomThemeFragment(val isLightTheme: Boolean) : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, MenuProvider {
	private lateinit var board: SudokuBoardView
	private var copyFromExistingThemeDialog: Dialog? = null
	private var settings: GameSettings? = null
	private val theme = if (isLightTheme) THEME_CUSTOM_LIGHT else THEME_CUSTOM_DARK
	private lateinit var defaultTheme: ThemeUtil

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(if (isLightTheme) R.xml.preferences_custom_theme_light else R.xml.preferences_custom_theme_dark, rootKey)
		setDefaultColorsIfNotSaved(preferenceScreen)
		updateEvenBoxColoringVisibility()
		settings = GameSettings(requireContext())

		val prefKey = if (isLightTheme) "custom_light_background_colors" else "custom_dark_background_colors"
		findPreference<androidx.preference.Preference>(prefKey)?.setOnPreferenceClickListener {
			BackgroundColorFragment.newInstance(isLightTheme).show(parentFragmentManager, "BackgroundColorDialog")
			true
		}

		val baseThemePrefKey = if (isLightTheme) "custom_light_base_theme" else "custom_dark_base_theme"
		findPreference<ThemeColorPreference>(baseThemePrefKey)?.apply {
			setOnPreferenceClickListener {
				val dialog = BaseThemeColorFragment.newInstance(isLightTheme)
				dialog.onThemeSelected = { themeKey ->
					settings?.setCustomBaseTheme(isLightTheme, themeKey)

					updateBaseThemePreference(this, themeKey)
					ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
					requireActivity().recreate()
				}
				dialog.show(parentFragmentManager, "BaseThemeColorDialog")
				true
			}
			updateBaseThemePreference(this, settings?.getCustomBaseTheme(isLightTheme))
		}
	}

	private fun updateBaseThemePreference(preference: ThemeColorPreference, themeKey: String?) {
		val themeResId = if (themeKey != null) {
			Themes.getStyleResId(themeKey)
		} else {
			ThemeUtils.findClosestStyle(requireContext(), isLightTheme)
		}
		val themeWrapper = ContextThemeWrapper(requireContext(), themeResId)
		val colorPrimary = ThemeUtils.getContextThemeColor(themeWrapper, android.R.attr.colorPrimary)
		preference.setPreviewColor(colorPrimary)
	}

	private fun setDefaultColorsIfNotSaved(preferenceGroup: PreferenceGroup) {
		val prefix = if (isLightTheme) COLOR_PREFERENCE_PREFIX_LIGHT else COLOR_PREFERENCE_PREFIX_DARK
		defaultTheme = ThemeUtil(requireContext(), if (isLightTheme) R.style.AppTheme_OpenSudoku else R.style.AppTheme_AMOLED)
		for (i in 0 until preferenceGroup.preferenceCount) {
			val preference = preferenceGroup.getPreference(i)
			if (preference is PreferenceGroup) {
				setDefaultColorsIfNotSaved(preference) // Recurse
				continue
			}
			if (preference.key != null && preference.key.startsWith(prefix)) {
				require(preference is ColorPickerPreference)
				val colorValue = preference.sharedPreferences!!.getInt(
					preference.key,
					getDefaultColor(preference.key.drop(prefix.length))
				)
				preference.onColorChanged(colorValue)
			}
		}
	}

	private fun getDefaultColor(colorKey: String): Int {
		val color = Colors.fromSuffix(colorKey)
		return defaultTheme.getColor(color.attr)
	}

	private fun updateEvenBoxColoringVisibility() {
		val checkboxKey = if (isLightTheme) "pref_enable_even_box_coloring_light" else "pref_enable_even_box_coloring_dark"
		val isEnabled = preferenceManager.sharedPreferences?.getBoolean(checkboxKey, false) ?: false

		val evenColors = listOf(
			Colors.EVEN_TEXT,
			Colors.EVEN_MARKS_TEXT,
			Colors.EVEN_BACKGROUND,
			Colors.EVEN_GIVEN_VALUE_TEXT,
			Colors.EVEN_GIVEN_VALUE_BACKGROUND
		)

		for (color in evenColors) {
			findPreference<androidx.preference.Preference>(color.key(isLightTheme))?.isVisible = isEnabled
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		// Customise the view to include the game board preview. Do this by inflating the
		// default view and the desired view (as a ViewGroup), and then adding the default view
		// to the view group.
		val defaultView = super.onCreateView(inflater, container, savedInstanceState)
		val viewGroup = inflater.inflate(R.layout.preference_custom_theme, container, false) as ViewGroup
		viewGroup.addView(defaultView)

		// The normal preferences layout (i.e., the layout that defaultView is using) forces the
		// width to match the parent. In landscape mode this shrinks the width of the board to 0.
		//
		// To fix, wait until after initialView has been added to viewGroup (so the layout params
		// are the correct type), then override the width and weight to take up 50% of the screen.
		if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			val layoutParams = defaultView.layoutParams as LinearLayout.LayoutParams
			layoutParams.width = 0
			layoutParams.weight = 1f
			defaultView.layoutParams = layoutParams
		}
		requireActivity().setTitle(if (isLightTheme) R.string.light_theme_colors_title else R.string.dark_theme_colors_title)
		return viewGroup
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		board = view.findViewById(R.id.board_view)
		prepareGamePreviewView(board)
		val menuHost: MenuHost = requireActivity()
		menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
	}

	private fun prepareGamePreviewView(board: SudokuBoardView) {
		board.sameValueHighlightMode = settings!!.sameValueHighlightMode
		board.onSelectedCellUpdate = { cell: Cell? -> board.highlightedValue = cell?.value ?: 0 }
		ThemeUtils.prepareBoardPreviewView(board)
		updateThemePreview()
	}

	private fun updateThemePreview() {
		ThemeUtils.applyThemeToSudokuBoardViewFromContext(theme, board, requireContext())
	}

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.custom_theme, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.copy_from_theme -> {
				showCopyFromExistingThemeDialog()
				true
			}

			R.id.create_from_color -> {
				showCreateFromSingleColorDialog()
				true
			}

			else -> false
		}
	}

	private fun showCopyFromExistingThemeDialog() {
		val builder = AlertDialog.Builder(requireContext())
		builder.setTitle(R.string.select_theme)
		builder.setNegativeButton(android.R.string.cancel, null)
		val themeNames = requireContext().resources.getStringArray(if (isLightTheme) R.array.light_theme_names else R.array.dark_theme_names)
		val themeNamesWithoutCustomTheme = themeNames.copyOfRange(0, themeNames.size - 1)
		builder.setItems(themeNamesWithoutCustomTheme) { _: DialogInterface?, which: Int ->
			copyFromExistingThemeIndex(which)
			copyFromExistingThemeDialog?.dismiss()
		}
		val copyFromExistingThemeDialog = builder.create()
		copyFromExistingThemeDialog.setOnDismissListener { this.copyFromExistingThemeDialog = null }
		copyFromExistingThemeDialog.show()
		this.copyFromExistingThemeDialog = copyFromExistingThemeDialog
	}

	private fun showCreateFromSingleColorDialog() {
		val colorDialog = ColorPickerDialog(requireContext(), settings!!.customColors(isLightTheme).colorPrimary, "Choose the base color")
		colorDialog.alphaSliderVisible = false
		colorDialog.hexValueEnabled = true
		colorDialog.setOnColorChangedListener(::createCustomThemeFromSingleColor)
		colorDialog.show()
	}

	private fun copyFromExistingThemeIndex(which: Int) {
		val context = requireContext()
		val themeCode = context.resources.getStringArray(if (isLightTheme) R.array.light_theme_codes else R.array.dark_theme_codes)[which]
		val themeWrapper = ContextThemeWrapper(context, Themes.getStyleResId(themeCode))

		// Copy these attributes from a theme...
		val attributes = Colors.entries.map { it.attr }.toIntArray()

		// ... and set them as the value of these preferences
		val preferenceKeys = Colors.entries.map { it.key(isLightTheme) }.toTypedArray()

		val themeColors = themeWrapper.theme.obtainStyledAttributes(attributes)
		for (i in attributes.indices) {
			findPreference<ColorPickerPreference>(preferenceKeys[i])?.onColorChanged(themeColors.getColor(i, Color.GRAY))
		}
		themeColors.recycle()
	}

	private fun createCustomThemeFromSingleColor(@ColorInt colorPrimary: Int) {
		// generate other colors from the provided color
		val colorSecondary = HSL(colorPrimary).hue(340).color
		val colorTertiary = HSL(colorPrimary).lightness(if (isLightTheme) -0.2f else 0.2f).color
		val colorValueText = if (isLightTheme) Color.BLACK else Color.WHITE
		val colorBackground = if (isLightTheme) Color.WHITE else Color.BLACK
		val colorGivenValueBackground = HSL(colorPrimary).saturation(-0.4f).lightness(if (isLightTheme) 0.4f else -0.4f).color
		val colorGivenValueText = colorOn(colorGivenValueBackground)
		val colorTouchedText = colorOn(colorSecondary)
		val colorHighlightedText = colorOn(colorPrimary)

		// set the colors
		val colorMapping = mutableMapOf(
			Colors.LINE to colorTertiary,
			Colors.SECTOR_LINE to colorTertiary,
			Colors.VALUE_TEXT to colorValueText,
			Colors.MARKS_TEXT to colorValueText,
			Colors.BACKGROUND to colorBackground,
			Colors.GIVEN_VALUE_TEXT to colorGivenValueText,
			Colors.GIVEN_VALUE_BACKGROUND to colorGivenValueBackground,
			Colors.TOUCHED_TEXT to colorTouchedText,
			Colors.TOUCHED_MARKS_TEXT to colorTouchedText,
			Colors.TOUCHED_BACKGROUND to colorSecondary,
			Colors.SELECTED_BACKGROUND to colorTertiary,
			Colors.HIGHLIGHTED_TEXT to colorHighlightedText,
			Colors.HIGHLIGHTED_BACKGROUND to colorPrimary,
			Colors.HIGHLIGHTED_MARKS_TEXT to colorHighlightedText,
			Colors.HIGHLIGHTED_MARKS_BACKGROUND to colorPrimary,
			Colors.EVEN_TEXT to colorValueText,
			Colors.EVEN_MARKS_TEXT to colorValueText,
			Colors.EVEN_BACKGROUND to Color.TRANSPARENT,
			Colors.EVEN_GIVEN_VALUE_TEXT to Color.TRANSPARENT,
			Colors.EVEN_GIVEN_VALUE_BACKGROUND to Color.TRANSPARENT,
		)

		for (i in 1..9) {
			val hue = (i - 1) * 40f // simple spread
			val color = if (isLightTheme) {
				ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.9f))
			} else {
				ColorUtils.HSLToColor(floatArrayOf(hue, 0.6f, 0.2f))
			}
			colorMapping[Colors.fromSuffix("Value$i")] = color
		}

		for ((color, newValue) in colorMapping) {
			findPreference<ColorPickerPreference>(color.key(isLightTheme))?.onColorChanged(newValue)
		}
	}

	/**
	 * @param background background color
	 * @return a color suitable for using "on" the given background color.
	 */
	@ColorInt
	private fun colorOn(@ColorInt background: Int): Int {
		val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, background)
		val blackContrast = ColorUtils.calculateContrast(Color.BLACK, background)
		return if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		updateThemePreview()
		updateEvenBoxColoringVisibility()
		ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
	}

	override fun onResume() {
		super.onResume()
		settings!!.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause() {
		super.onPause()
		settings!!.unregisterOnSharedPreferenceChangeListener(this)
	}
}
