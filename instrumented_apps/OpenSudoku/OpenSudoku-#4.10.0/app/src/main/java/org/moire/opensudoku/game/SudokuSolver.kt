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

package org.moire.opensudoku.game

private const val numRows = 9
private const val numCols = 9
private const val numValues = 9
private const val numConstraints = 4
private const val numCells = numRows * numCols

class SudokuSolver private constructor(board: SudokuBoard, val saveSolution: Boolean, val maxSolutions: Int) {
	private var validSolution = ArrayList<Array<Int>>()
	private lateinit var linkedList: Array<Array<Node?>>
	private lateinit var head: Node
	private var temporaryResult: ArrayList<Node> = ArrayList()

	init {
		initializeNodesMatrix()
		initializeNodesLinks()
		setPuzzle(board)
	}

	/**
	 * Modifies linked list based on the original state of the board
	 */
	private fun setPuzzle(board: SudokuBoard) {
		val cells = board.cells
		for (rowIndex in 0..8) {
			for (columnIndex in 0..8) {
				val cell = cells[rowIndex][columnIndex]
				if (cell.value == 0) continue

				val matrixRow = cellToRow(rowIndex, columnIndex, cell.value - 1)
				val matrixCol = 9 * rowIndex + columnIndex // calculates column of node based on cell constraint
				val rowNode = linkedList[matrixRow][matrixCol] ?: continue // should not happen, only if imported sudoku has errors
				var rightNode = rowNode
				do {
					cover(rightNode)
					rightNode = rightNode.right
				} while (rightNode !== rowNode)
			}
		}
	}

	private fun initializeNodesMatrix() {
		linkedList = Array(numRows * numCols * numValues + 1) { arrayOfNulls(numCells * numConstraints) }

		// add row of Nodes for column headers
		linkedList[0] = Array(numCells * numConstraints) { Node() }

		// calculate column where constraint will go
		val rowShift = numCells
		val colShift = numCells * 2
		val blockShift = numCells * 3
		var cellColumn = 0
		var rowColumn = 0
		var blockColumn = 0
		for (row in 0..<numRows) {
			var colColumn = 0
			for (col in 0..<numCols) {
				for (value in 0..<numValues) {
					val matrixRow = cellToRow(row, col, value)

					// cell
					linkedList[matrixRow][cellColumn] = Node()

					// row
					linkedList[matrixRow][rowColumn + rowShift] = Node()
					rowColumn++

					// col
					linkedList[matrixRow][colColumn + colShift] = Node()
					colColumn++

					// block
					linkedList[matrixRow][blockColumn + blockShift] = Node()
					blockColumn++
				}
				cellColumn++
				rowColumn -= numCols
				if (col % 3 != 2) {
					blockColumn -= numCols
				}
			}
			rowColumn += numCols
			if (row % 3 != 2) {
				blockColumn -= numCols * 3
			}
		}
	}

	private fun initializeNodesLinks() {
		head = Node()
		val rows = linkedList.size
		val cols = linkedList[0].size

		// link nodes in linkedList
		for (row in 0..<rows) {
			for (col in 0..<cols) {
				val currentNode = linkedList[row][col]
				if (currentNode == null) continue

				var rowToLink: Int
				var colToLink: Int

				// link left
				rowToLink = row
				colToLink = col
				do { // skip nulls
					colToLink = moveLeft(colToLink, cols)
				} while (linkedList[rowToLink][colToLink] == null)
				currentNode.left = linkedList[rowToLink][colToLink]!!

				// link right
				rowToLink = row
				colToLink = col
				do { // skip nulls
					colToLink = moveRight(colToLink, cols)
				} while (linkedList[rowToLink][colToLink] == null)
				currentNode.right = linkedList[rowToLink][colToLink]!!

				// link up
				rowToLink = row
				colToLink = col
				do { // skip nulls
					rowToLink = moveUp(rowToLink, rows)
				} while (linkedList[rowToLink][colToLink] == null)
				currentNode.up = linkedList[rowToLink][colToLink]!!

				// link up
				rowToLink = row
				colToLink = col
				do { // skip nulls
					rowToLink = moveDown(rowToLink, rows)
				} while (linkedList[rowToLink][colToLink] == null)
				currentNode.down = linkedList[rowToLink][colToLink]!!

				// initialize remaining node info
				currentNode.columnHeader = linkedList[0][col]!!
				currentNode.rowID = row
			}
		}

		// link head node
		head.right = linkedList[0][0]!!
		head.left = linkedList[0][cols - 1]!!
		linkedList[0][0]!!.left = head
		linkedList[0][cols - 1]!!.right = head
	}

	/**
	 * Dancing links algorithm
	 */
	private fun dlx(solutionCounter: Int): Int {
		if (head.right === head) { // all nodes covered
			if (saveSolution && solutionCounter == 0) saveSolution() // save the first solution found
			return solutionCounter + 1
		}

		var colNode = chooseLeastCountColumnNode()
		cover(colNode)

		var rowNode = colNode.down
		var newSolutionCounter = solutionCounter
		while (rowNode !== colNode) {
			temporaryResult.add(rowNode)
			var rightNode = rowNode.right
			while (rightNode !== rowNode) {
				cover(rightNode)
				rightNode = rightNode.right
			}

			newSolutionCounter = dlx(newSolutionCounter)
			if (newSolutionCounter >= maxSolutions) return  newSolutionCounter

			// undo operations and try the next row
			temporaryResult.removeAt(temporaryResult.size - 1)
			colNode = rowNode.columnHeader
			var leftNode = rowNode.left
			while (leftNode !== rowNode) {
				uncover(leftNode)
				leftNode = leftNode.left
			}
			rowNode = rowNode.down
		}
		uncover(colNode)
		return newSolutionCounter
	}

	private fun saveSolution() {
		temporaryResult.forEach { node ->
			val rowColVal = matrixRowToRowColumnValue(node.rowID)
			validSolution.add(rowColVal)
		}
	}

	/**
	 * Converts from puzzle cell to constraint matrix
	 *
	 * @param row             0-8 index
	 * @param col             0-8 index
	 * @param value           0-8 index (representing values 1-9)
	 * @return row in constraintMatrix corresponding to cell indices and value
	 */
	private fun cellToRow(row: Int, col: Int, value: Int): Int = 81 * row + 9 * col + value + 1

	private fun matrixRowToRowColumnValue(matrixRow: Int): Array<Int> {
		val matrixRowVal = matrixRow - 1
		return arrayOf(matrixRowVal / 81, matrixRowVal % 81 / 9, matrixRowVal % 9 + 1)
	}

	/**
	 * Functions to move cyclically through matrix
	 */
	private fun moveLeft(j: Int, numCols: Int): Int = if (j - 1 < 0) numCols - 1 else j - 1

	private fun moveRight(j: Int, numCols: Int): Int = (j + 1) % numCols

	private fun moveUp(i: Int, numRows: Int): Int = if (i - 1 < 0) numRows - 1 else i - 1

	private fun moveDown(i: Int, numRows: Int): Int = (i + 1) % numRows

	/**
	 * Unlinks node from linked list
	 */
	private fun cover(node: Node) {
		val colNode = node.columnHeader
		colNode.left.right = colNode.right
		colNode.right.left = colNode.left
		var rowNode = colNode.down
		while (rowNode !== colNode) {
			var rightNode = rowNode.right
			while (rightNode !== rowNode) {
				rightNode.up.down = rightNode.down
				rightNode.down.up = rightNode.up
				rightNode.columnHeader.count--
				rightNode = rightNode.right
			}
			rowNode = rowNode.down
		}
	}

	private fun uncover(node: Node) {
		val colNode = node.columnHeader
		var upNode = colNode.up
		while (upNode !== colNode) {
			var leftNode = upNode.left
			while (leftNode !== upNode) {
				leftNode.up.down = leftNode
				leftNode.down.up = leftNode
				leftNode.columnHeader.count++
				leftNode = leftNode.left
			}
			upNode = upNode.up
		}
		colNode.left.right = colNode
		colNode.right.left = colNode
	}

	/**
	 * Returns column node with lowest # of nodes
	 */
	private fun chooseLeastCountColumnNode(): Node {
		var bestNode = head.right
		var currentNode = bestNode.right
		while (currentNode !== head) {
			if (currentNode.count < bestNode.count) {
				bestNode = currentNode
			}
			currentNode = currentNode.right
		}
		return bestNode
	}

	companion object {
		/* ---------------PUBLIC FUNCTIONS--------------- */
		fun solve(board: SudokuBoard): Pair<Int, ArrayList<Array<Int>>?> {
			val originalBoard = if (board.isEditMode) board else board.copyOriginal(false)
			if (!originalBoard.isValid()) return Pair(0, null)

			val solver = SudokuSolver(originalBoard, true, 2)
			return Pair(solver.dlx(0), solver.validSolution)
		}

		fun countSolutions(board: SudokuBoard, maxCount: Int): Int {
			val originalBoard = if (board.isEditMode) board else board.copyOriginal(false)
			if (!originalBoard.isValid()) return 0

			val solver = SudokuSolver(originalBoard, false, maxCount)
			return solver.dlx(0)
		}
	}
}
