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

package org.moire.opensudoku.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import java.io.File
import java.io.Serializable

object AndroidUtils {
	/**
	 * Returns version code of OpenSudoku.
	 */
	fun getAppVersionCode(context: Context): Long {
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
			} else {
				@Suppress("DEPRECATION")
				context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
			}
		} catch (e: PackageManager.NameNotFoundException) {
			throw RuntimeException(e)
		}
	}

	/**
	 * Returns version name of OpenSudoku.
	 */
	fun getAppVersionName(context: Context): String {
		return try {
			context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
		} catch (e: PackageManager.NameNotFoundException) {
			throw RuntimeException(e)
		}
	}
}

@SuppressLint("UnsanitizedFilenameFromContentProvider")
internal fun Uri.getFileName(contentResolver: ContentResolver): String? {
	contentResolver.query(this, null, null, null, null)?.use { cursor ->
		if (cursor.moveToFirst()) {
			return@getFileName File(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))).name
		}
	}
	return lastPathSegment
}

interface ContextMenuItem {
	val ordinal: Int
	val id: Int
	val titleRes: Int
}

internal fun ContextMenu.addAll(entries: List<ContextMenuItem>) {
	for (entry in entries) {
		add(0, entry.id, entry.ordinal, entry.titleRes)
	}
}

interface OptionsMenuItem {
	val ordinal: Int
	val id: Int
	val titleRes: Int
	val iconRes: Int
	val shortcutKey: Char
	val isAction: Boolean
}

internal fun Menu.addAll(entries: List<OptionsMenuItem>) {
	for (entry in entries) {
		val item = add(0, entry.id, entry.ordinal, entry.titleRes)
		if (entry.iconRes != 0) {
			item.setIcon(entry.iconRes)
		}
		if (entry.shortcutKey != Char(0)) {
			item.setShortcut(entry.id.toChar(), entry.shortcutKey)
		}
		if (entry.isAction) {
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
		}
	}
}

inline fun <reified T : Serializable> Bundle.getSerializableUniversal(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getSerializable(key, T::class.java)
	} else {
		@Suppress("DEPRECATION")
		getSerializable(key) as? T
	}
}
