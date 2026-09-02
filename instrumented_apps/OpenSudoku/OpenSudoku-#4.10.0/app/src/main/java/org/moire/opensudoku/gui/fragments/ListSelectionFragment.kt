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
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import org.moire.opensudoku.R

/**
 * Full-screen fragment that displays a list of options for a ListPreference.
 */
class ListSelectionFragment : Fragment() {
	private var clickedDialogEntryIndex = 0
	private lateinit var entries: Array<CharSequence?>
	private lateinit var entryValues: Array<CharSequence>
	private lateinit var adapter: ListAdapter
	private var listState: Parcelable? = null
	private lateinit var listPreference: ListPreference
	private var preferenceKey: String? = null

	private val onItemClickListener = View.OnClickListener { v ->
		val viewHolder = v.tag as ListAdapter.ViewHolder
		val prevSelectedPosition = clickedDialogEntryIndex
		clickedDialogEntryIndex = viewHolder.absoluteAdapterPosition
		adapter.notifyItemChanged(prevSelectedPosition)
		adapter.notifyItemChanged(clickedDialogEntryIndex)
		val value = entryValues[clickedDialogEntryIndex] as String
		
		// Update the preference immediately
		if (listPreference.callChangeListener(value)) {
			listPreference.value = value
		}
		
		// Notify activity to update night mode if necessary
		(requireActivity() as? ListSelectionListener)?.onListSelectionChanged()
	}

	interface ListSelectionListener {
		fun onListSelectionChanged()
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
			activity?.onBackPressed()
		}

		if (savedInstanceState != null) {
			clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
			listState = savedInstanceState.getParcelable(SAVE_STATE_LIST_STATE)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.preference_dialog_sudoku_board_theme, container, false)
		// Hide board preview if it exists in the layout (reusing the same layout)
		view.findViewById<View>(R.id.preference_board_view)?.visibility = View.GONE
		
		val recyclerView = view.findViewById<RecyclerView>(R.id.theme_list)
		val layoutManager = LinearLayoutManager(context)
		recyclerView.layoutManager = layoutManager
		if (listState != null) {
			layoutManager.onRestoreInstanceState(listState)
			listState = null
		} else {
			layoutManager.scrollToPosition(clickedDialogEntryIndex)
		}
		adapter = ListAdapter(entries, requireContext())
		recyclerView.adapter = adapter
		adapter.onItemClickListener = onItemClickListener
		
		requireActivity().setTitle(listPreference.title)
		
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

	internal inner class ListAdapter(private val entries: Array<CharSequence?>, private val themedContext: Context) : RecyclerView.Adapter<ListAdapter.ViewHolder?>() {
		internal var onItemClickListener: View.OnClickListener? = null

		internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			val radioButton: MaterialRadioButton = itemView.findViewById(android.R.id.text1)
			val iconView: ImageView = itemView.findViewById(R.id.icon)

			init {
				radioButton.tag = this
				radioButton.setOnClickListener(onItemClickListener)
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val v = LayoutInflater.from(themedContext)
				.inflate(R.layout.preference_dialog_listitem, parent, false)
			return ViewHolder(v)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val button = holder.radioButton
			button.text = entries[position]
			button.isChecked = position == clickedDialogEntryIndex
			val value = entryValues[position] as String

			val iconView = holder.iconView
			val iconRes = when (value) {
				"light" -> R.drawable.ic_baseline_light_mode
				"dark" -> R.drawable.ic_baseline_dark_mode
				"system" -> R.drawable.ic_baseline_system_mode
				else -> 0
			}
			if (iconRes != 0) {
				iconView.setImageResource(iconRes)
				iconView.visibility = View.VISIBLE
			} else {
				iconView.visibility = View.GONE
			}
		}

		override fun getItemCount(): Int = entries.size
	}

	val currentValue: String?
		get() = if (::entryValues.isInitialized && clickedDialogEntryIndex >= 0) entryValues[clickedDialogEntryIndex] as String else null

	companion object {
		private const val ARG_KEY = "key"
		private const val SAVE_STATE_INDEX = "ListSelectionFragment.index"
		private const val SAVE_STATE_LIST_STATE = "ListSelectionFragment.listState"

		fun newInstance(key: String?): ListSelectionFragment {
			val fragment = ListSelectionFragment()
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}
