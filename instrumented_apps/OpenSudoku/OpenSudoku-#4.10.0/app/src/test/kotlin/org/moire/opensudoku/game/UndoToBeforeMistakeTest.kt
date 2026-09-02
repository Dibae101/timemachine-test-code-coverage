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

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.moire.opensudoku.game.command.CommandStack
import org.moire.opensudoku.game.command.SetCellValueCommand
import kotlin.test.Test

class UndoToBeforeMistakeTest {

    @Test
    fun `should undo until board has no mistakes`() {
        val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val board = SudokuBoard.fromString(puzzle, false)
        val commandStack = CommandStack(board)
        
        // Ensure solution is calculated
        board.solutionCount shouldBe 1
        
        // Find an empty cell to play in. (0, 2) is empty in this puzzle.
        val targetCell = board.getCell(0, 2)
        val correctValue = targetCell.solution
        
        // 1. Valid move
        commandStack.execute(SetCellValueCommand(targetCell, correctValue), true)
        board.hasMistakes.shouldBeFalse()

        // 2. Mistake. (0, 3) is empty.
        val targetCell2 = board.getCell(0, 3)
        val wrongValue = if (targetCell2.solution == 1) 2 else 1
        commandStack.execute(SetCellValueCommand(targetCell2, wrongValue), true)
        board.hasMistakes.shouldBeTrue()

        // 3. Undo to solvable state
        commandStack.undoToSolvableState()

        // 4. Assert
        board.hasMistakes.shouldBeFalse()
        targetCell.value shouldBe correctValue
        targetCell2.value shouldBe 0
    }
}
