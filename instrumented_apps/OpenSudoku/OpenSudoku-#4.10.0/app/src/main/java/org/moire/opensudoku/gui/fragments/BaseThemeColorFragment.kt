/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2026 by Open Sudoku authors.
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

import android.app.Dialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.moire.opensudoku.R
import org.moire.opensudoku.game.GameSettings
import org.moire.opensudoku.utils.ThemeUtils
import org.moire.opensudoku.utils.Themes

class BaseThemeColorFragment : DialogFragment() {
	private var isLightTheme: Boolean = true
	var onThemeSelected: ((String) -> Unit)? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		isLightTheme = arguments?.getBoolean("isLightTheme", true) ?: true
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val ctx = requireContext()
		val view = layoutInflater.inflate(R.layout.dialog_base_theme_color, null)
		val recyclerView = view.findViewById<RecyclerView>(R.id.theme_grid)

		val themes = Themes.entries.filter { it.isLight == isLightTheme }
		val themeData = themes.map { theme ->
			val themeWrapper = ContextThemeWrapper(ctx, theme.attr)
			val color = ThemeUtils.getContextThemeColor(themeWrapper, android.R.attr.colorPrimary)
			val hsl = FloatArray(3)
			ColorUtils.colorToHSL(color, hsl)
			ThemeItem(theme.key, color, hsl[0])
		}.sortedBy { it.hue }

		recyclerView.layoutManager = GridLayoutManager(ctx, 3)
		recyclerView.adapter = ThemeColorAdapter(themeData) { themeKey ->
			onThemeSelected?.invoke(themeKey)
			dismiss()
		}

		return AlertDialog.Builder(ctx)
			.setTitle(R.string.select_primary_color)
			.setView(view)
			.setNegativeButton(android.R.string.cancel, null)
			.create()
	}

	override fun onStart() {
		super.onStart()
		if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			dialog?.window?.let { window ->
				val params = window.attributes
				params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
				params.height = (resources.displayMetrics.heightPixels * 0.95).toInt()
				window.attributes = params
			}
		}
	}

	private data class ThemeItem(val key: String, val color: Int, val hue: Float)

	private inner class ThemeColorAdapter(
		private val items: List<ThemeItem>,
		private val onClick: (String) -> Unit
	) : RecyclerView.Adapter<ThemeColorAdapter.ViewHolder>() {

		inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val colorView: View = view.findViewById(R.id.color_view)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_color, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]
			holder.colorView.backgroundTintList = ColorStateList.valueOf(item.color)
			holder.itemView.setOnClickListener { onClick(item.key) }
		}

		override fun getItemCount() = items.size
	}

	companion object {
		fun newInstance(isLightTheme: Boolean): BaseThemeColorFragment {
			val fragment = BaseThemeColorFragment()
			val args = Bundle()
			args.putBoolean("isLightTheme", isLightTheme)
			fragment.arguments = args
			return fragment
		}
	}
}
