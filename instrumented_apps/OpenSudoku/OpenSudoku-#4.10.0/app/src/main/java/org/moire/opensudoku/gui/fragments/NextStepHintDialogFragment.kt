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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.moire.opensudoku.R
import org.moire.opensudoku.game.nextstep.NextStep


class NextStepHintDialogFragment : DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
		builder.setTitle(R.string.hint)
		if (message != null) {
			builder.setMessage(message)
		}
		builder.setNeutralButton(android.R.string.ok) { _: DialogInterface?, _: Int -> onDialogFinished?.invoke() }
		if (showApplyButton)
			builder.setPositiveButton(R.string.hint_apply_nextstep) {
				_: DialogInterface?,
				_: Int -> onDialogApplyNextStep?.invoke()
			}
		return builder.create()
	}

	override fun onStart() {
		super.onStart()
		val window = dialog!!.window
		val layoutParams = window!!.attributes
		layoutParams.gravity = Gravity.BOTTOM
		layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
		window.attributes = layoutParams
	}

	override fun onCancel(dialog: DialogInterface) {
		super.onCancel(dialog)
		onDialogFinished?.invoke()
	}

	fun showMessage(manager: FragmentManager, message: String,
					showApplyButton: Boolean,
					nextStep: NextStep) {
		Companion.message = message
		Companion.showApplyButton = showApplyButton
		Companion.nextStep = nextStep
		show(manager, this.javaClass.simpleName)
	}

	companion object {
		var onDialogFinished: (() -> Unit)? = null
		var onDialogApplyNextStep: (() -> Unit)? = null
		var message: String? = null
		var showApplyButton: Boolean = false
		@SuppressLint("StaticFieldLeak")
		var nextStep: NextStep? = null
	}
}
