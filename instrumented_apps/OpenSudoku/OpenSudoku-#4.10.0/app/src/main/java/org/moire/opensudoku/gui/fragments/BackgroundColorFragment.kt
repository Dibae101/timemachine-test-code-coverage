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

package org.moire.opensudoku.gui.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import net.margaritov.preference.colorpicker.ColorPickerDialog
import org.moire.opensudoku.R
import org.moire.opensudoku.gui.NumberButton
import org.moire.opensudoku.utils.BackgroundColorPrefs
import org.moire.opensudoku.utils.FontSelector

open class BackgroundColorFragment : DialogFragment(), ColorPickerDialog.OnColorChangedListener {
	private var currentButtonIndex: Int = -1
	private val btns = arrayOfNulls<NumberButton>(10)
	var isLightTheme: Boolean = true
		private set

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		isLightTheme = arguments?.getBoolean("isLightTheme", true) ?: true
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val ctx = requireContext()
		val view = layoutInflater.inflate(R.layout.background_color, null)
		val fontSet = FontSelector(ctx)

		// initialize buttons and colors
		val buttonIds = intArrayOf(
			R.id.btn_color_1, R.id.btn_color_2, R.id.btn_color_3,
			R.id.btn_color_4, R.id.btn_color_5, R.id.btn_color_6,
			R.id.btn_color_7, R.id.btn_color_8, R.id.btn_color_9
		)

		for (i in 1..9) {
			val resId = buttonIds[i - 1]
			val btn = view.findViewById<NumberButton>(resId)
			btn.tag = i
			btn.symbolToDisplay = fontSet.convert(i)
			btn.maxChar = fontSet.maxChar
			btn.scalingMultiplier = fontSet.requiredRatio()
			val color = BackgroundColorPrefs.getColor(ctx, i, isLightTheme)
			updateButtonAppearance(btn, color)
			btn.setOnClickListener {
				currentButtonIndex = i
				openColorPicker(i, color)
			}
			btns[i] = btn
		}

		return AlertDialog.Builder(ctx)
			.setTitle(R.string.background_colors_title)
			.setView(view)
			.setPositiveButton(R.string.close, null)
			.setNeutralButton(R.string.reset, null)
			.create()
	}

	override fun onStart() {
		super.onStart()
		val dialog = dialog as? AlertDialog
		dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
			val ctx = requireContext()
			BackgroundColorPrefs.resetColors(ctx, isLightTheme)
			for (i in 1..9) {
				val color = BackgroundColorPrefs.getColor(ctx, i, isLightTheme)
				btns[i]?.let { updateButtonAppearance(it, color) }
			}
		}
	}

	private fun updateButtonAppearance(btn: NumberButton, color: Int) {
		btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
		val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, color)
		val blackContrast = ColorUtils.calculateContrast(Color.BLACK, color)
		btn.setTextColor(if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK)
	}

	private fun openColorPicker(index: Int, initialColor: Int) {
		val fontSet = FontSelector(requireContext())
		val dialog = ColorPickerDialog(requireContext(), initialColor, getString(R.string.choose_background_color_for, fontSet.convert(index)))
		dialog.setOnColorChangedListener(this)
		dialog.show()
	}

	private fun setColor(index: Int, color: Int) {
		BackgroundColorPrefs.setColor(requireContext(), index, color, isLightTheme)
	}

	override fun onColorChanged(color: Int) {
		val idx = currentButtonIndex
		if (idx < 0) return
		setColor(idx, color)
		// update button background immediately
		btns[idx]?.let { updateButtonAppearance(it, color) }
	}

	companion object {
		fun newInstance(isLightTheme: Boolean): BackgroundColorFragment {
			val fragment = BackgroundColorFragment()
			val args = Bundle()
			args.putBoolean("isLightTheme", isLightTheme)
			fragment.arguments = args
			return fragment
		}
	}
}

class LightBackgroundColorFragment : BackgroundColorFragment()
class DarkBackgroundColorFragment : BackgroundColorFragment()
