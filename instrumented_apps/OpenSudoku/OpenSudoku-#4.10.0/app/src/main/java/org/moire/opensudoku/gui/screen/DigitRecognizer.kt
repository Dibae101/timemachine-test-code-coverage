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

import android.content.Context
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Recognizes digits in a Sudoku cell using a TensorFlow Lite MNIST model with structural refinement.
 */
class DigitRecognizer(context: Context, modelName: String = "mnist.tflite") {
    private val interpreter: Interpreter
    private val inputBuffer = ByteBuffer.allocateDirect(4 * 28 * 28).apply { order(ByteOrder.nativeOrder()) }
    private val pixelData = ByteArray(28 * 28)

    init {
        val modelFile = loadModelFile(context, modelName)
        val options = Interpreter.Options()
        interpreter = Interpreter(modelFile, options)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        context.assets.openFd(modelName).use { fileDescriptor ->
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    fun classify(cellMat: Mat): Int {
        val nonZero = Mat()
        Core.findNonZero(cellMat, nonZero)
        if (nonZero.empty()) {
            nonZero.release()
            return 0
        }
        val rect = Geometry.boundingRect(nonZero)
        nonZero.release()
        
        if (rect.width <= 10 || rect.height <= 10) {
            return 0
        }

        val digit = cellMat.submat(rect)
        if (digit.empty()) {
            digit.release()
            return 0
        }
        
        // Better resizing: Preserve aspect ratio and fit into 20x20
        val digit20 = Mat.zeros(Size(20.0, 20.0), cellMat.type())
        val w = rect.width.toDouble()
        val h = rect.height.toDouble()
        val scale = 20.0 / Math.max(w, h)
        val nw = (w * scale).toInt().coerceIn(1, 20)
        val nh = (h * scale).toInt().coerceIn(1, 20)
        val resized = Mat()
        Imgproc.resize(digit, resized, Size(nw.toDouble(), nh.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
        
        val dx = (20 - nw) / 2
        val dy = (20 - nh) / 2
        val destRoi = digit20.submat(dy, dy + nh, dx, dx + nw)
        resized.copyTo(destRoi)
        resized.release(); destRoi.release()

        // MNIST Normalization: Center of mass should be at (14, 14) in a 28x28 image
        val moments = Geometry.moments(digit20)
        val massX = if (moments.m00 > 0.1) moments.m10 / moments.m00 else 10.0
        val massY = if (moments.m00 > 0.1) moments.m01 / moments.m00 else 10.0
        
        val normalized = Mat.zeros(Size(28.0, 28.0), cellMat.type())
        val tx = 14.0 - massX
        val ty = 14.0 - massY
        
        val transMat = Mat(2, 3, org.opencv.core.CvType.CV_32F)
        transMat.put(0, 0, 1.0, 0.0, tx)
        transMat.put(1, 0, 0.0, 1.0, ty)
        Imgproc.warpAffine(digit20, normalized, transMat, Size(28.0, 28.0))
        transMat.release()

        inputBuffer.rewind()
        normalized.get(0, 0, pixelData)
        for (pixel in pixelData) {
            inputBuffer.putFloat((pixel.toInt() and 0xFF) / 255.0f)
        }

        val output = Array(1) { FloatArray(10) }
        interpreter.run(inputBuffer, output)
        val probabilities = output[0]
        val mnistIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val mnistProb = probabilities[mnistIndex]
        var result = mnistIndex
        
        val aspect = rect.height.toDouble() / rect.width.coerceAtLeast(1)

        // Structural Analysis
        val tempContours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(digit20, tempContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)
        var holes = 0
        var holeCenterY = 0.0
        val holeAreas = mutableListOf<Double>()
        if (!hierarchy.empty()) {
            for (i in 0 until tempContours.size) {
                if (hierarchy.get(0, i)[3] != -1.0) {
                    val area = Geometry.contourArea(tempContours[i])
                    if (area > 4.5) { 
                        holes++
                        holeAreas.add(area)
                        val m = Geometry.moments(tempContours[i])
                        if (m.m00 > 0) holeCenterY += (m.m01 / m.m00)
                    }
                }
            }
        }
        if (holes > 0) holeCenterY /= holes

        // HEURISTICS
        if (holes >= 2) {
             val sorted = holeAreas.sortedDescending()
             if (mnistProb > 0.9 && (mnistIndex == 9 || mnistIndex == 6)) result = mnistIndex
             else if (sorted.size >= 2 && sorted[1] < sorted[0] * 0.3) result = if (holeCenterY < 10) 9 else 6
             else result = 8
        } else if (holes == 1) {
             val yRel = holeCenterY / 20.0
             if (mnistIndex == 4) result = 4
             else if (mnistIndex == 9 && mnistProb > 0.6) result = 9
             else if (mnistIndex == 0 && mnistProb > 0.7) result = 0
             else if (mnistIndex == 9) {
                  val topMidCol = digit20.submat(0, 6, 8, 12)
                  if (Core.countNonZero(topMidCol) < 3) result = 4 else result = 9
                  topMidCol.release()
             } else {
                  result = if (yRel < 0.5) 9 else 6
             }
        } else {
             // 0 holes. 
             val topPart = digit20.submat(0, 6, 0, 20)
             var first = -1; var last = -1
             for (c in 0 until 20) { if (Core.countNonZero(topPart.col(c)) > 0) { if (first == -1) first = c; last = c } }
             val topSpan = last - first; topPart.release()

             if (mnistProb > 0.95 && mnistIndex != 1 && mnistIndex != 7) {
                  result = mnistIndex
             } else if (aspect > 2.8) {
                  result = 1
             } else {
                  val bot = digit20.submat(15, 20, 0, 20); val botA = Core.countNonZero(bot); bot.release()
                  
                  if (topSpan > 13 && aspect < 1.8 && botA < 15) result = 7
                  else if (mnistIndex == 2 || mnistIndex == 5) result = mnistIndex
                  else if (aspect > 1.3) {
                       val ml = digit20.submat(8, 12, 0, 8); val mlA = Core.countNonZero(ml); ml.release()
                       if (mlA < 15) result = 1 else result = mnistIndex
                  } else {
                       result = mnistIndex
                  }
             }
        }

        normalized.release(); digit20.release(); digit.release()
        hierarchy.release()
        for (c in tempContours) c.release()
        return if (result in 1..9 && (mnistProb > 0.1 || result == 1)) result else 0
    }

    fun close() {
        interpreter.close()
    }
}
