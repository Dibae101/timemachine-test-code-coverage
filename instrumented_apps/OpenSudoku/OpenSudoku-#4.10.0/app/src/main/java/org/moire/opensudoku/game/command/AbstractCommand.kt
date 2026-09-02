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
import java.util.StringTokenizer

/**
 * Generic interface for command in application.
 */
abstract class AbstractCommand {
	protected open fun deserialize(data: StringTokenizer, dataVersion: Int) {}
	open fun serialize(data: StringBuilder) {
		val cmdLongName = commandClass
		for (cmdDef in commands) {
			if (cmdDef.longName == cmdLongName) {
				data.append(cmdDef.shortName)
					.append("|")
				return
			}
		}
		throw IllegalArgumentException("Unknown command class '$cmdLongName'.")
	}

	private val commandClass: String
		get() = javaClass.simpleName

	/**
	 * Executes the command.
	 */
	abstract fun execute()

	/**
	 * Undo this command.
	 */
	abstract fun undo(): Cell?
	private interface CommandCreatorFunction {
		fun create(): AbstractCommand
	}

	private class CommandDef(var longName: String, var shortName: String, var creator: CommandCreatorFunction) {
		fun create(): AbstractCommand = creator.create()
	}

	companion object {
		private val commands = arrayOf(
			CommandDef(ManualActionCommand::class.java.simpleName, "c0",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = ManualActionCommand()
				}),
			CommandDef(ClearAllMarksCommand::class.java.simpleName, "c1",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = ClearAllMarksCommand()
				}),
			CommandDef(EditCellPrimaryMarksCommand::class.java.simpleName, "c2",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = EditCellPrimaryMarksCommand()
				}),
			CommandDef(FillInMarksCommand::class.java.simpleName, "c3",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = FillInMarksCommand()
				}),
			CommandDef(SetCellValueCommand::class.java.simpleName, "c4",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = SetCellValueCommand()
				}),
			CommandDef(CheckpointCommand::class.java.simpleName, "c5",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = CheckpointCommand()
				}),
			CommandDef(SetCellValueAndRemoveMarksCommand::class.java.simpleName, "c6",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = SetCellValueAndRemoveMarksCommand()
				}),
			CommandDef(FillInMarksWithAllValuesCommand::class.java.simpleName, "c7",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = FillInMarksWithAllValuesCommand()
				}),
			CommandDef(EditCellSecondaryMarksCommand::class.java.simpleName, "c8",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = EditCellSecondaryMarksCommand()
				}),
			CommandDef(SetCellColorCommand::class.java.simpleName, "c9",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = SetCellColorCommand()
				}),
			CommandDef(ClearAllPrimaryMarksCommand::class.java.simpleName, "c10",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = ClearAllPrimaryMarksCommand()
				}),
			CommandDef(ClearAllSecondaryMarksCommand::class.java.simpleName, "c11",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = ClearAllSecondaryMarksCommand()
				}),
			CommandDef(ClearAllColorsCommand::class.java.simpleName, "c12",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = ClearAllColorsCommand()
				})
		)

		fun deserialize(data: StringTokenizer, dataVersion: Int): AbstractCommand {
			val cmdShortName = data.nextToken()
			for (cmdDef in commands) {
				if (cmdDef.shortName == cmdShortName) {
					val cmd = cmdDef.create()
					cmd.deserialize(data, dataVersion)
					return cmd
				}
			}
			throw IllegalArgumentException("Unknown command class '$cmdShortName'.")
		}
	}
}
