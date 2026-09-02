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

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import org.moire.opensudoku.R
import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.DARK_THEME_KEY
import org.moire.opensudoku.game.LIGHT_THEME_KEY
import org.moire.opensudoku.gui.SudokuBoardView
import org.moire.opensudoku.utils.ThemeUtils

/**
 * Full-screen fragment that displays a list of themes and a game board to
 * preview the theme.
 */
class ThemeSelectionFragment : Fragment() {
	private lateinit var boardView: SudokuBoardView
	var clickedDialogEntryIndex = 0
	private lateinit var entries: Array<CharSequence?>
	private lateinit var entryValues: Array<CharSequence>
	private lateinit var adapter: ThemeAdapter
	private var listState: Parcelable? = null
	lateinit var listPreference: ListPreference
	var preferenceKey: String? = null

	private val onItemClickListener = View.OnClickListener { v ->
		val viewHolder = v.tag as ThemeAdapter.ViewHolder
		val prevSelectedPosition = clickedDialogEntryIndex
		clickedDialogEntryIndex = viewHolder.absoluteAdapterPosition
		adapter.notifyItemChanged(prevSelectedPosition)
		adapter.notifyItemChanged(clickedDialogEntryIndex)
		val value = entryValues[clickedDialogEntryIndex] as String
		applyThemePreview(value)
		
		// Update the preference immediately
		if (listPreference.callChangeListener(value)) {
			listPreference.value = value
			ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
			requireActivity().recreate()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		preferenceKey = arguments?.getString(ARG_KEY)
		
		@Suppress("DEPRECATION")
		val target = targetFragment as? PreferenceFragmentCompat
		val pref = target?.findPreference<Preference>(preferenceKey ?: "") as? ListPreference
		if (pref != null) {
			listPreference = pref
			clickedDialogEntryIndex = listPreference.findIndexOfValue(listPreference.value)
			entries = listPreference.entries
			entryValues = listPreference.entryValues
		} else {
			// Fallback if target fragment is lost (e.g. on rotation if not handled)
			// In a real app we might want to reload from SharedPrefs
			activity?.onBackPressed()
		}

		if (savedInstanceState != null) {
			clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
			listState = savedInstanceState.getParcelable(SAVE_STATE_LIST_STATE)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.preference_dialog_sudoku_board_theme, container, false)
		boardView = view.findViewById(R.id.preference_board_view)
		val recyclerView = view.findViewById<RecyclerView>(R.id.theme_list)
		val layoutManager = LinearLayoutManager(context)
		recyclerView.layoutManager = layoutManager
		if (listState != null) {
			layoutManager.onRestoreInstanceState(listState)
			listState = null
		} else {
			layoutManager.scrollToPosition(clickedDialogEntryIndex)
		}
		adapter = ThemeAdapter(entries, requireContext())
		recyclerView.adapter = adapter
		adapter.onItemClickListener = onItemClickListener
		prepareBoardPreviewView("${entryValues[clickedDialogEntryIndex]}")
		
		requireActivity().setTitle(if (preferenceKey == LIGHT_THEME_KEY) R.string.light_theme else R.string.dark_theme)
		
		return view
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
		val recyclerView = view?.findViewById<RecyclerView>(R.id.theme_list)
		if (recyclerView != null) {
			outState.putParcelable(SAVE_STATE_LIST_STATE, recyclerView.layoutManager?.onSaveInstanceState())
		}
	}

	internal inner class ThemeAdapter(private val entries: Array<CharSequence?>, private val themedContext: Context) : RecyclerView.Adapter<ThemeAdapter.ViewHolder?>() {
		internal var onItemClickListener: View.OnClickListener? = null

		internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			val radioButton: MaterialRadioButton = itemView.findViewById(android.R.id.text1)
			val editButton: MaterialButton = itemView.findViewById(R.id.edit_button)

			init {
				radioButton.tag = this
				radioButton.setOnClickListener(onItemClickListener)
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val v = LayoutInflater.from(themedContext)
				.inflate(R.layout.preference_dialog_theme_listitem, parent, false)
			return ViewHolder(v)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val button = holder.radioButton
			button.text = entries[position]
			button.isChecked = position == clickedDialogEntryIndex
			val themeCode = entryValues[position] as String

			val editButton = holder.editButton
			if (themeCode == THEME_CUSTOM_LIGHT || themeCode == THEME_CUSTOM_DARK) {
				editButton.visibility = View.VISIBLE
				editButton.isEnabled = button.isChecked
				editButton.setOnClickListener {
					val fragmentClass = if (themeCode == THEME_CUSTOM_LIGHT) CustomLightThemeFragment::class.java.name else CustomDarkThemeFragment::class.java.name
					val pref = Preference(themedContext).apply {
						fragment = fragmentClass
						key = if (themeCode == THEME_CUSTOM_LIGHT) "light_theme_colors" else "dark_theme_colors"
					}
					@Suppress("DEPRECATION")
					val target = targetFragment as? PreferenceFragmentCompat
					if (target != null) {
						(requireActivity() as? PreferenceFragmentCompat.OnPreferenceStartFragmentCallback)
							?.onPreferenceStartFragment(target, pref)
					}
				}
			} else {
				editButton.visibility = View.GONE
			}
		}

		override fun getItemCount(): Int = entries.size
	}

	private fun prepareBoardPreviewView(initialTheme: String) {
		boardView.onSelectedCellUpdate = { cell: Cell? -> boardView.highlightedValue = cell?.value ?: 0 }
		ThemeUtils.prepareBoardPreviewView(boardView)
		applyThemePreview(initialTheme)
	}

	private fun applyThemePreview(theme: String) {
		ThemeUtils.applyThemeToSudokuBoardViewFromContext(theme, boardView, requireContext())
	}

	val isLightTheme: Boolean
		get() = preferenceKey == LIGHT_THEME_KEY

	companion object {
		private const val ARG_KEY = "key"
		private const val SAVE_STATE_INDEX = "ThemeSelectionFragment.index"
		private const val SAVE_STATE_LIST_STATE = "ThemeSelectionFragment.listState"

		fun newInstance(key: String?): ThemeSelectionFragment {
			val fragment = ThemeSelectionFragment()
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}
