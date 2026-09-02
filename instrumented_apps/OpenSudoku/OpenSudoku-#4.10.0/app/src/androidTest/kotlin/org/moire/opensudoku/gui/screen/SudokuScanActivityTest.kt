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
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

const val FULL_PICTURE_FILE_NAME = "full_board_picture.jpg"

@RunWith(AndroidJUnit4::class)
class SudokuScanActivityTest {

    @Before
    fun setup() {
        OpenCVLoader.initLocal()
    }

    @Test
    fun testOnCameraFullFrame() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(FULL_PICTURE_FILE_NAME)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Simulate landscape camera frame
        val landscape = Mat()
        Core.rotate(mat, landscape, Core.ROTATE_90_COUNTERCLOCKWISE)
        
        val rgba = Mat()
        Imgproc.cvtColor(landscape, rgba, Imgproc.COLOR_RGB2RGBA)
        val gray = Mat()
        Imgproc.cvtColor(landscape, gray, Imgproc.COLOR_RGB2GRAY)

        val frame = object : CameraBridgeViewBase.CvCameraViewFrame {
            override fun rgba(): Mat = rgba
            override fun gray(): Mat = gray
            override fun release() {}
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

        ActivityScenario.launch(SudokuScanActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onCameraViewStarted(rgba.cols(), rgba.rows())
                
                // Process frame
                activity.onCameraFrame(frame)
                
                val actual = activity.lastRecognizedBoard
                assertNotNull("Board should have been recognized", actual)
                
                for (r in 0 until 9) {
                    val rowStr = actual!![r].joinToString(" ")
                    android.util.Log.i("SudokuScanActivityTest", "Row $r: $rowStr")
                }

                for (i in 0 until 9) {
                    assertArrayEquals("Row $i mismatch", expected[i], actual!![i])
                }
            }
        }

        rgba.release(); gray.release(); mat.release()
    }
}
