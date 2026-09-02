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

package org.moire.opensudoku.game.command

import org.moire.opensudoku.game.Cell
import org.moire.opensudoku.game.SudokuBoard
import java.util.Stack
import java.util.StringTokenizer

class CommandStack(private val board: SudokuBoard) {

	private val commandStack = Stack<AbstractCommand>()
	var onEmptyChangeListener: (() -> Unit)? = null
	var isEmpty = true // true if no commands are recorded on this CommandStack
		set(value) {
			if (field != value) {
				field = value
				onEmptyChangeListener?.invoke()
			}
		}

	fun serialize(): String {
		val sb = StringBuilder()
		serialize(sb)
		return "$sb"
	}

	fun serialize(data: StringBuilder) {
		data.append(commandStack.size).append("|")
		commandStack.forEach { it.serialize(data) }
	}

	fun execute(command: AbstractCommand, isManual: Boolean) {
		board.beginBatchUpdate()
		if (isManual) {
			push(ManualActionCommand())
		}
		push(command)
		command.execute()
		board.endBatchUpdate()
	}

	fun undo(): Cell? {
		if (isEmpty) {
			return null
		}

		board.beginBatchUpdate()
		var lastCommand: AbstractCommand
		var cellUndone: Cell? = null

		do {
			lastCommand = pop()
			if (lastCommand !is ManualActionCommand && lastCommand !is CheckpointCommand) {
				cellUndone = lastCommand.undo()
			}
		} while (!isEmpty && lastCommand !is ManualActionCommand)

		board.endBatchUpdate()
		board.validateAllCells()
		return cellUndone
	}

	fun setCheckpoint() {
		if (!isEmpty && commandStack.peek() is CheckpointCommand) return // the checkpoint is already there
		push(CheckpointCommand())
	}

	fun hasCheckpoint(): Boolean = commandStack.any { it is CheckpointCommand }

	fun undoToCheckpoint() {
		if (isEmpty) return

		board.beginBatchUpdate()
		var c: AbstractCommand
		do {
			c = pop()
			c.undo()
		} while (!isEmpty && commandStack.peek() !is CheckpointCommand)
		board.endBatchUpdate()

		board.validateAllCells()
	}

	fun undoToSolvableState() {
		require(board.solutionCount == 1) { "This puzzle has " + board.solutionCount + " solutions" }
		board.beginBatchUpdate()
		while (!isEmpty && board.hasMistakes) {
			undo()
		}
		board.endBatchUpdate()
		board.validateAllCells()
	}

	val lastCommandCell: Cell?
		get() {
			val iterator: ListIterator<AbstractCommand> = commandStack.listIterator(commandStack.size)
			while (iterator.hasPrevious()) {
				when (val o = iterator.previous()) {
					is AbstractSingleCellCommand -> {
						return o.cell
					}

					is SetCellValueAndRemoveMarksCommand -> {
						return o.cell
					}
				}
			}
			return null
		}

	private fun push(command: AbstractCommand) {
		if (command is AbstractCellCommand) {
			command.cells = board
		}
		commandStack.push(command)
		if (command !is CheckpointCommand) {
			isEmpty = false
		}
	}

	private fun pop(): AbstractCommand {
		val lastCommand = commandStack.pop()
		@Suppress("UsePropertyAccessSyntax")
		isEmpty = commandStack.isEmpty()
		return lastCommand
	}

	fun deserialize(data: String?, dataVersion: Int) {
		if (data == null || data == "") {
			return
		}
		val st = StringTokenizer(data, "|")
		clean()
		val stackSize = st.nextToken().toInt()
		(0..<stackSize).forEach { i ->
			val command: AbstractCommand = AbstractCommand.deserialize(st, dataVersion)
			if (dataVersion < SudokuBoard.DATA_VERSION_4 && command !is ManualActionCommand) {
				push(ManualActionCommand())
			}
			push(command)
		}
	}

	fun clean() {
		commandStack.empty()
		isEmpty = true
	}
}
