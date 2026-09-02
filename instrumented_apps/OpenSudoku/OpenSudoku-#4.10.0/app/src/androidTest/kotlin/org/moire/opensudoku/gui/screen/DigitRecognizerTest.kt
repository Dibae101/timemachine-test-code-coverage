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

package org.moire.opensudoku.gui.screen

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.geometry.Geometry
import org.moire.opensudoku.game.SudokuBoard.Companion.SUDOKU_SIZE
import org.opencv.imgproc.Imgproc

@RunWith(AndroidJUnit4::class)
class DigitRecognizerTest {

    private lateinit var digitRecognizer: DigitRecognizer

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OpenCVLoader.initLocal()
        digitRecognizer = DigitRecognizer(context)
    }

    @Test
    fun testRecognitionOnDebugBoard() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open("debug_board.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val gray = Mat()
        if (mat.channels() > 1) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        } else {
            mat.copyTo(gray)
        }

        val expected = arrayOf(
            intArrayOf(0, 3, 0, 0, 0, 9, 7, 0, 5),
            intArrayOf(0, 5, 1, 0, 0, 7, 0, 9, 0),
            intArrayOf(7, 9, 8, 5, 0, 3, 0, 6, 0),
            intArrayOf(3, 6, 4, 7, 5, 2, 9, 1, 8),
            intArrayOf(9, 1, 0, 4, 6, 8, 0, 3, 0),
            intArrayOf(8, 2, 0, 9, 3, 1, 0, 0, 0),
            intArrayOf(5, 0, 0, 0, 9, 4, 0, 0, 0),
            intArrayOf(0, 4, 3, 8, 7, 6, 0, 5, 9),
            intArrayOf(0, 0, 9, 3, 0, 5, 4, 0, 0)
        )

        val cellWidth = gray.width() / SUDOKU_SIZE
        val cellHeight = gray.height() / SUDOKU_SIZE

        var total = 0
        var correct = 0
        val failures = StringBuilder()

        for (row in 0 until SUDOKU_SIZE) {
            for (col in 0 until SUDOKU_SIZE) {
                val x = col * cellWidth
                val y = row * cellHeight
                val w = if (col == SUDOKU_SIZE - 1) gray.width() - x else cellWidth
                val h = if (row == SUDOKU_SIZE - 1) gray.height() - y else cellHeight
                
                val pad = 1
                if (w <= 2 * pad || h <= 2 * pad) continue
                val cell = gray.submat(y + pad, y + h - pad, x + pad, x + w - pad)
                
                val predicted = digitRecognizer.classify(cell)
                val expectedValue = expected[row][col]
                
                if (expectedValue != 0) {
                    total++
                    if (predicted == expectedValue) {
                        correct++
                    } else {
                        failures.append("at row $row col $col expected $expectedValue but was $predicted; ")
                    }
                }
                cell.release()
            }
        }

        gray.release(); mat.release()
        val accuracy = if (total > 0) (correct.toFloat() / total) * 100 else 0f
        android.util.Log.i("DigitRecognizerTest", "Accuracy: $correct/$total ($accuracy%)")
        assertEquals("Recognition accuracy on the test board is too low! Failures: $failures", total, correct)
    }

    @Test
    fun testRecognitionOnFullPicture() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = try {
            context.assets.open(FULL_PICTURE_FILE_NAME)
        } catch (e: Exception) {
            null
        }
        if (inputStream == null) {
            android.util.Log.w("DigitRecognizerTest", "$FULL_PICTURE_FILE_NAME not found, skipping test")
            return
        }
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        // Find the board boundaries
        val nonZero = Mat()
        Core.findNonZero(gray, nonZero)
        if (nonZero.empty()) {
            android.util.Log.w("DigitRecognizerTest", "No non-zero pixels in $FULL_PICTURE_FILE_NAME")
            return
        }
        val boardRect = Geometry.boundingRect(nonZero)
        nonZero.release()
        
        val board = gray.submat(boardRect)

        val expected = Array(SUDOKU_SIZE) { IntArray(SUDOKU_SIZE) }

        val cellWidth = board.width() / SUDOKU_SIZE
        val cellHeight = board.height() / SUDOKU_SIZE

        for (row in 0 until SUDOKU_SIZE) {
            val rowLog = StringBuilder("Row $row: ")
            for (col in 0 until SUDOKU_SIZE) {
                val x = col * cellWidth
                val y = row * cellHeight
                val w = if (col == SUDOKU_SIZE - 1) board.width() - x else cellWidth
                val h = if (row == SUDOKU_SIZE - 1) board.height() - y else cellHeight
                
                val pad = 1
                if (w <= 2 * pad || h <= 2 * pad) continue
                val cell = board.submat(y + pad, y + h - pad, x + pad, x + w - pad)
                
                val predicted = digitRecognizer.classify(cell)
                rowLog.append(if (predicted == 0) ". " else "$predicted ")
                cell.release()
            }
            android.util.Log.i("DigitRecognizerTest", rowLog.toString())
        }

        gray.release(); mat.release()
    }
}
