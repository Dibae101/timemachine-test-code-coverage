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

package org.moire.opensudoku.gui.screen.folder_list

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.moire.opensudoku.R
import org.moire.opensudoku.game.FolderInfo
import org.moire.opensudoku.utils.addAll

internal class FolderListRecyclerAdapter(
	private val context: Context,
	private val onClickListener: (Long) -> Unit
) : RecyclerView.Adapter<FolderListRecyclerAdapter.ViewHolder?>() {

	private var folders: List<FolderInfo> = emptyList()
	var selectedFolderId: Long = 0

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.folder_list_item, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = folders.size

	@SuppressLint("NotifyDataSetChanged")
	fun updateFoldersList(newFolders: List<FolderInfo>) {
		folders = newFolders
		notifyDataSetChanged()
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val folder: FolderInfo = folders[position]
		holder.itemView.setOnClickListener { onClickListener(folder.id) }
		holder.itemView.setOnCreateContextMenuListener { menu, _, _ ->
			selectedFolderId = folder.id
			menu?.run { addAll(FolderListActivity.Companion.ContextMenuItems.entries) }
		}

		holder.name.text = folder.name
		holder.detail.text = folder.getDetail(context)
	}

	internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val name: TextView = itemView.findViewById(R.id.name)
		val detail: TextView = itemView.findViewById(R.id.detail)
	}
}
