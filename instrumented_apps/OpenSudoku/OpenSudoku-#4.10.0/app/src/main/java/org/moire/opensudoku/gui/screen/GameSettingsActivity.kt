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

package org.moire.opensudoku.gui.screen

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import org.moire.opensudoku.R
import org.moire.opensudoku.gui.ThemedActivity
import org.moire.opensudoku.gui.fragments.BackgroundColorFragment
import org.moire.opensudoku.gui.fragments.CustomThemeFragment
import org.moire.opensudoku.gui.fragments.GameSettingsFragment
import org.moire.opensudoku.gui.fragments.ListSelectionFragment
import org.moire.opensudoku.gui.fragments.ThemeSelectionFragment
import org.moire.opensudoku.utils.ThemeUtils

class GameSettingsActivity : ThemedActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, FragmentManager.OnBackStackChangedListener, ListSelectionFragment.ListSelectionListener {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		theme.applyStyle(R.style.NoActionBarOverlay, true)
		setContentView(R.layout.preferences_host)

		val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val appBarLayout = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.app_bar_layout)
		val preferencesContent = findViewById<android.view.View>(R.id.preferences_content)
		val root = findViewById<android.view.View>(R.id.preferences_host_root)
		ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			appBarLayout.updatePadding(left = systemBars.left, top = systemBars.top, right = systemBars.right)
			preferencesContent.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
			insets
		}


		supportFragmentManager.addOnBackStackChangedListener(this)
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.preferences_content, GameSettingsFragment())
				.commit()
		} else {
			// Fragments are restored, update override if necessary
			updateNightModeOverride()
		}
	}

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val args = pref.extras
		val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment ?: return true)
		fragment.arguments = args

		@Suppress("DEPRECATION")    // known bug in Preferences library https://stackoverflow.com/a/74230035/7926219
		fragment.setTargetFragment(caller, 0)

		supportFragmentManager.beginTransaction()
			.replace(R.id.preferences_content, fragment)
			.addToBackStack(null)
			.commit()
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onBackStackChanged() {
		recreateActivityIfThemeChanged()
		updateNightModeOverride()
	}

	override fun onListSelectionChanged() {
		updateNightModeOverride()
	}

	private fun updateNightModeOverride() {
		val fragment = supportFragmentManager.findFragmentById(R.id.preferences_content)
		val desiredMode = when (fragment) {
			is ThemeSelectionFragment -> {
				if (fragment.isLightTheme) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
			}
			is CustomThemeFragment -> {
				if (fragment.isLightTheme) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
			}
			is BackgroundColorFragment -> {
				if (fragment.isLightTheme) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
			}
			is ListSelectionFragment -> {
				when (fragment.currentValue) {
					"light" -> AppCompatDelegate.MODE_NIGHT_NO
					"dark" -> AppCompatDelegate.MODE_NIGHT_YES
					"system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
					else -> null
				}
			}
			else -> null
		}

		if (ThemeUtils.nightModeOverride != desiredMode) {
			ThemeUtils.nightModeOverride = desiredMode
			val modeToSet = desiredMode ?: ThemeUtils.getDesiredNightMode(this)
			AppCompatDelegate.setDefaultNightMode(modeToSet)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		// Clear override when activity is destroyed to not affect other parts of the app
		// unless we are just recreating.
		if (isFinishing) {
			ThemeUtils.nightModeOverride = null
		}
	}
}
