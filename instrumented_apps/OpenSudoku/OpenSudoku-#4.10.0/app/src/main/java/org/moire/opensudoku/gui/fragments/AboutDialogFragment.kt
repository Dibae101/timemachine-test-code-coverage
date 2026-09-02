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

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.moire.opensudoku.R
import org.moire.opensudoku.utils.AndroidUtils

class AboutDialogFragment : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val aboutView = layoutInflater.inflate(R.layout.about, null)
		val versionName = AndroidUtils.getAppVersionName(requireContext())

		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.mipmap.ic_launcher)
			.setTitle(getString(R.string.version, versionName))
			.setView(aboutView)
			.setPositiveButton(android.R.string.ok, null)

		return builder.create()
	}
}
