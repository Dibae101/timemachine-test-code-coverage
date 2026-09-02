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

package org.moire.opensudoku.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import org.moire.opensudoku.game.GameSettings

class FontSelector(private val context: Context) {

	private var _fonts: CharArray = "123456789".toCharArray()
	private var _default: Boolean = false

	// relative width/height for aspect ratio
	private var maxHeight: Float = 10f
	private var maxWidth: Float = 10f

	/** the widest char in the set */
	 var maxChar = 'X'

	init {
		refresh()
	}

	fun refresh() {
		setFonts(GameSettings(context).selectedFont)
	}

	/** how to set textSize to fit into the desired square */
	fun requiredRatio(): Float {
	/*
	chinese        95       92        '九' 20061           0.968421053
    latin          52       74        '4'                  1.423076923
    letter         63       74        'A'                  1.174603175
    arabic         41       43        '٧' 1639             1.048780488
	*/
		val default_width: Int = 52
		val default_height: Int = 74
		var wr: Float = maxWidth/default_width
		var hr: Float = maxHeight/default_height

		val r = if (wr > hr) hr/wr else wr/hr
		return r
	}

	 private fun setFonts(selectedFont: String) {
		_default = (selectedFont == "123456789")
		if (selectedFont.length == 9) {
			_fonts = selectedFont.toCharArray()

			// calculate approximate max height/width and find the widest char
			val paint = Paint()
			paint.textSize = 100f
			val result = Rect()
			var w = 0f
			for (i in 0..8) {
				paint.getTextBounds(_fonts[i].toString(), 0, 1, result)
				if (result.width() > w) {
					w = result.width().toFloat()
					maxChar = _fonts[i]
				}
			}
			maxWidth = w
			paint.getTextBounds(_fonts, 0, 9, result)
			maxHeight = result.height().toFloat()
		}

	 }

	/** convert the digit to the appropriate char */
	fun convert(digit: Int): String {
		if (digit in 1..9)
			return _fonts[digit-1].toString()
		return "X"
	}

	fun getSymbolSet(): String {
		return _fonts.concatToString()
	}

	/** replace digits in the hint dialog(s) */
	fun replaceDigitsInBraces(input: String): String {
		if (_default) return input

		val out = StringBuilder(input.length)
		var inBraces = false
		for (ch in input) {
			when {
				ch == '{' -> {
					inBraces = true
					out.append(ch)
				}

				ch == '}' -> {
					inBraces = false
					out.append(ch)
				}

				inBraces && ch.isDigit() -> {
					out.append(_fonts[ch - '1'])
				}

				else -> out.append(ch)
			}
		}
		return out.toString()
	}
}
