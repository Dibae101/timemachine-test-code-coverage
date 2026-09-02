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

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.moire.opensudoku.gui.DbKeeperViewModel

const val progressUpdateIntervalMs: Long = 300

data class ProgressUiState(
    var folderName: String? = null,
    var maxValue: Int = Int.MIN_VALUE,
    var currentValue: Int = Int.MIN_VALUE,
    val corruptedCount: Int = 0,
)

data class TaskFinishedState(
    val isFinished: Boolean = false,
    val folderId: Long = 0,
    val isSuccess: Boolean = false,
    val corruptedCount: Int = 0,
    val message: String = "",
)

class FolderTaskViewModel(application: Application) : DbKeeperViewModel(application) {
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState = _uiState.asStateFlow()
    private val _taskFinished = MutableStateFlow(TaskFinishedState())
    val taskFinished = _taskFinished.asStateFlow()

    private var task: FolderTaskModel? = null
    private val handler = Handler(Looper.getMainLooper())

    fun init(context: Context, task: FolderTaskModel) {
        this.task = task

        viewModelScope.launch {
            task.run(context, ::onTaskFinished)
        }

        handler.postDelayed(object : Runnable {
            override fun run() {
                progressUpdate()
                handler.postDelayed(this, progressUpdateIntervalMs)
            }
        }, progressUpdateIntervalMs)
    }

    fun onTaskFinished(folderId: Long, isSuccess: Boolean, message: String) {
        progressUpdate()
        _taskFinished.update {
            it.copy(
                isFinished = true,
                folderId = folderId,
                isSuccess = isSuccess,
                message = message,
                corruptedCount = task?.corruptedCount ?: 0
            )
        }
    }

    internal fun progressUpdate() {
        _uiState.update {
            it.copy(
                folderName = task?.titleParam,
                maxValue = task?.maxValue ?: 0,
                currentValue = task?.currentValue ?: 0,
                corruptedCount = task?.corruptedCount ?: 0,
            )
        }
    }

    private fun stopUpdatingProgress() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCleared() {
        super.onCleared()
        stopUpdatingProgress()
        task?.cancel()
    }
}
