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

package org.moire.opensudoku.gui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import net.margaritov.preference.colorpicker.AlphaPatternDrawable

class ThemeColorPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var previewColor: Int = Color.TRANSPARENT
    private val density = context.resources.displayMetrics.density

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetFrameView = holder.findViewById(android.R.id.widget_frame) as? LinearLayout
        if (widgetFrameView != null) {
            widgetFrameView.visibility = View.VISIBLE
            widgetFrameView.setPadding(
                widgetFrameView.paddingLeft,
                widgetFrameView.paddingTop,
                (density * 8).toInt(),
                widgetFrameView.paddingBottom
            )
            // remove already created preview image
            widgetFrameView.removeAllViews()

            val d = (density * 31).toInt()
            val iView = ImageView(context)
            val params = LinearLayout.LayoutParams(d, d)
            params.gravity = android.view.Gravity.CENTER_VERTICAL
            iView.layoutParams = params
            
            widgetFrameView.addView(iView)
            widgetFrameView.minimumWidth = 0
            iView.background = AlphaPatternDrawable((5 * density).toInt())
            iView.setImageBitmap(getPreviewBitmap())
        }
    }

    private fun getPreviewBitmap(): Bitmap {
        val d = (density * 31).toInt()
        val bm = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888)
        val w = bm.width
        val h = bm.height
        for (i in 0 until w) {
            for (j in i until h) {
                // Border logic from ColorPickerPreference.java
                val isBorder = i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2
                val c = if (isBorder) Color.GRAY else previewColor
                bm.setPixel(i, j, c)
                if (i != j) {
                    bm.setPixel(j, i, c)
                }
            }
        }
        return bm
    }

    fun setPreviewColor(color: Int) {
        previewColor = color
        notifyChanged()
    }
}
