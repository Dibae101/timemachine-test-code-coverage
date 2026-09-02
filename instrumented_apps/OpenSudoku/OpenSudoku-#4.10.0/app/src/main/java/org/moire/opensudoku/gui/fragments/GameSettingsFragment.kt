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

package org.moire.opensudoku.gui.fragments


import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.moire.opensudoku.R
import org.moire.opensudoku.game.DARK_THEME_KEY
import org.moire.opensudoku.game.LIGHT_THEME_KEY
import org.moire.opensudoku.game.THEME_AMOLED
import org.moire.opensudoku.game.THEME_OPENSUDOKU
import org.moire.opensudoku.game.UI_MODE_DARK
import org.moire.opensudoku.game.UI_MODE_LIGHT
import org.moire.opensudoku.game.UI_MODE_SYSTEM
import org.moire.opensudoku.utils.ThemeUtils
import org.moire.opensudoku.utils.Themes

const val UI_MODE_KEY = "ui_mode"
const val EXPERIMENTAL_FEATURES_ENABLED = "experimental_features_v2"
const val BACKGROUND_COLOR_ENABLED = "background_color_enabled"
const val BACKGROUND_COLORS = "background_colors"
const val THEME_CUSTOM_LIGHT = "custom_light"
const val THEME_CUSTOM_DARK = "custom_dark"

class GameSettingsFragment : PreferenceFragmentCompat(), TargetFragment, OnSharedPreferenceChangeListener {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.preferences, rootKey)
		updateThemeKeysVisibility()
		updateExperimentalFeaturesVisibility()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		requireActivity().setTitle(R.string.game_settings)
		return super.onCreateView(inflater, container, savedInstanceState)
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference.key != LIGHT_THEME_KEY && preference.key != DARK_THEME_KEY && preference.key != UI_MODE_KEY) {
			super.onDisplayPreferenceDialog(preference)
			return
		}

		val fragmentClass = if (preference.key == UI_MODE_KEY) {
			ListSelectionFragment::class.java.name
		} else {
			ThemeSelectionFragment::class.java.name
		}

		val pref = Preference(requireContext()).apply {
			fragment = fragmentClass
			key = preference.key
			title = preference.title
			extras.putString("key", preference.key)
		}
		@Suppress("DEPRECATION")
		(requireActivity() as? PreferenceFragmentCompat.OnPreferenceStartFragmentCallback)
			?.onPreferenceStartFragment(this, pref)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		when (key) {
			UI_MODE_KEY -> {
				val uiMode = sharedPreferences.getString(key, UI_MODE_SYSTEM)
				when (uiMode) {
					UI_MODE_LIGHT -> {
						AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
					}

					UI_MODE_DARK -> {
						AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
					}

					// Default behaviour (including if the value is unrecognised) is to follow
					// the system.
					else -> {
						AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
					}
				}
				updateThemeKeysVisibility()
			}

			LIGHT_THEME_KEY -> {
				ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
				activity?.recreate()
			}

			DARK_THEME_KEY -> {
				ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
				activity?.recreate()
			}

			EXPERIMENTAL_FEATURES_ENABLED -> {
				updateExperimentalFeaturesVisibility()
			}
		}
	}

	private fun updateExperimentalFeaturesVisibility() {
		val experimentalFeatures = preferenceManager.sharedPreferences?.getStringSet(EXPERIMENTAL_FEATURES_ENABLED, emptySet())
		val isCellColoringEnabled = experimentalFeatures?.contains("cell_coloring") == true

		findPreference<Preference>(BACKGROUND_COLOR_ENABLED)?.isVisible = isCellColoringEnabled
		findPreference<Preference>(BACKGROUND_COLORS)?.isVisible = isCellColoringEnabled
	}

	// update visibility of theme preference buttons
	private fun updateThemeKeysVisibility() {
		val uiMode = findPreference<ListPreference>(UI_MODE_KEY)?.value
		val isLightThemeEnabled = uiMode != UI_MODE_DARK
		val isDarkThemeEnabled = uiMode != UI_MODE_LIGHT

		val lightThemePref = findPreference<ListPreference>(LIGHT_THEME_KEY)!!
		if (!Themes.isLightKey(lightThemePref.value)) lightThemePref.value = THEME_OPENSUDOKU
		lightThemePref.isVisible = isLightThemeEnabled

		val darkThemePref = findPreference<ListPreference>(DARK_THEME_KEY)!!
		if (!Themes.isDarkKey(darkThemePref.value)) darkThemePref.value = THEME_AMOLED
		darkThemePref.isVisible = isDarkThemeEnabled
	}

	override fun onResume() {
		super.onResume()
		preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
		updateThemeKeysVisibility()
	}

	override fun onPause() {
		super.onPause()
		preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
	}
}
