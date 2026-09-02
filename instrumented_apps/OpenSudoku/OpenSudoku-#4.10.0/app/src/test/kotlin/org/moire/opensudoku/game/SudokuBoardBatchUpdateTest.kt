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

package org.moire.opensudoku.game

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.moire.opensudoku.game.command.CommandStack
import org.moire.opensudoku.game.command.SetCellValueCommand
import kotlin.test.Test

class SudokuBoardBatchUpdateTest {

    @Test
    fun `should count excessive onChange calls during batch undo`() {
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = SudokuBoard.fromString(puzzle, false)
        val commandStack = CommandStack(board)
        
        // Trigger solution calculation
        board.solutionCount shouldBe 1

        var onChangeCount = 0
        board.ensureOnChangeListener { onChangeCount++ }

        // Find empty cells
        val emptyCells = mutableListOf<Cell>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board.getCell(r, c).value == 0) {
                    emptyCells.add(board.getCell(r, c))
                }
            }
        }

        // Execute 20 mistakes
        for (i in 0 until 20) {
            val cell = emptyCells[i]
            val wrongValue = if (cell.solution == 1) 2 else 1
            commandStack.execute(SetCellValueCommand(cell, wrongValue), true)
        }

        // Reset counter before batch undo
        onChangeCount = 0

        // Batch undo
        commandStack.undoToSolvableState()

        // Expectation: only 1 notification for the entire batch.
        onChangeCount shouldBe 1
        
        println("onChangeCount after fix: $onChangeCount")
    }

    @Test
    fun `should only trigger one notification for complex individual commands`() {
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = SudokuBoard.fromString(puzzle, false)
        val commandStack = CommandStack(board)
        
        var onChangeCount = 0
        board.ensureOnChangeListener { onChangeCount++ }

        // A command that modifies many cells: fillInPrimaryMarks()
        // We'll simulate this with a custom command if needed, but fillInPrimaryMarks() itself
        // is now wrapped in begin/end batch updates.
        
        onChangeCount = 0
        board.fillInPrimaryMarks()
        onChangeCount shouldBe 1

        onChangeCount = 0
        board.fillInPrimaryMarksWithAllValues()
        onChangeCount shouldBe 1
    }
}
