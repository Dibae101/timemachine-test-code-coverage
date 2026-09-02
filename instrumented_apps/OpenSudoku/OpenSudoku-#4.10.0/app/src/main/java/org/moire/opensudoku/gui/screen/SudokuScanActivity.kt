/*
 * This file is part of Open Sudoku - an open-source Sudoku game.
 * Copyright (C) 2009-2026 by Open Sudoku authors.
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
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.graphics.Color
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import android.view.ViewGroup.MarginLayoutParams
import org.moire.opensudoku.R
import org.moire.opensudoku.game.SudokuBoard
import org.moire.opensudoku.game.SudokuBoard.Companion.SUDOKU_SIZE
import org.moire.opensudoku.game.SudokuBoard.Companion.fromArray
import org.moire.opensudoku.game.SudokuSolver
import org.moire.opensudoku.gui.Tag
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc
import kotlin.math.min

const val NUMBER_OF_REQUIRED_VALID_PUZZLE_RECOGNITIONS = 3

/**
 * Uses the device's camera to try and recognise a sudoku.
 */
class SudokuScanActivity : CameraActivity(), CvCameraViewListener2 {
	private lateinit var openCvCameraView: CameraBridgeViewBase
	private lateinit var digitRecognizer: DigitRecognizer
	private lateinit var imageViewFinder: Mat
	private var imageCorrected: Mat? = null
	private var lastGridParams: GridParams? = null
	private val contours: MutableList<MatOfPoint> = ArrayList()
	private var isDuringOCR = false
	private var lastBoardContour: MatOfPoint2f? = null
	private var lastBoardContourVisual: MatOfPoint? = null
	private var contourPersistence = 0
	internal var lastRecognizedBoard: Array<IntArray>? = null
	private var successfulRecognitionCount = 0
	private var bestCollection: SudokuBoard? = null
	private var ocrCount = 0
	private var frameCount = 0

	public override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		window.statusBarColor = Color.TRANSPARENT
		window.navigationBarColor = Color.TRANSPARENT

		super.onCreate(savedInstanceState)

		if (!OpenCVLoader.initLocal()) {
			Log.e(TAG, "OpenCV initialization failed!")
			Toast.makeText(this, getString(R.string.camera_initialization_failed), Toast.LENGTH_LONG).show()
			finish()
			return
		}

		this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		this.setContentView(R.layout.puzzle_scan)

		val editButton = findViewById<Button>(R.id.button_edit)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.puzzle_scan_root)) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val margin = (16 * resources.displayMetrics.density).toInt()
			editButton.updateLayoutParams<MarginLayoutParams> {
				bottomMargin = systemBars.bottom + margin
				rightMargin = systemBars.right + margin
			}
			insets
		}

		val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
		windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

		openCvCameraView = findViewById<CameraBridgeViewBase>(R.id.camera_view).apply {
			visibility = View.VISIBLE
			setCvCameraViewListener(this@SudokuScanActivity)
		}
		findViewById<Button>(R.id.button_edit).setOnClickListener {
			synchronized(this) {
				val board = lastRecognizedBoard?.let { fromArray(it, false) }
				val boardToSave = board ?: bestCollection
				if (boardToSave != null) {
					this.setResult(RESULT_OK, Intent().putExtra(Tag.CELL_COLLECTION, boardToSave.serialize()))
					finish()
				} else {
					Toast.makeText(this, "No Sudoku recognized yet", Toast.LENGTH_SHORT).show()
				}
			}
		}
		digitRecognizer = DigitRecognizer(this)
	}

	public override fun onPause() {
		super.onPause()
		openCvCameraView.disableView()
	}

	public override fun onResume() {
		super.onResume()
		openCvCameraView.enableView()
	}

	override fun getCameraViewList(): List<CameraBridgeViewBase> {
		return listOf(openCvCameraView)
	}

	public override fun onDestroy() {
		digitRecognizer.close()
		super.onDestroy()
	}

	override fun onCameraViewStarted(width: Int, height: Int) {
		Log.i(TAG, "onCameraViewStarted: ${width}x${height}")
		imageViewFinder = Mat.zeros(height, width, CvType.CV_8UC4)
		val squareSide = min(width, height) * 0.8
		val topLeft = Point((width - squareSide) / 2.0, (height - squareSide) / 2.0)
		val bottomRight = Point((width + squareSide) / 2.0, (height + squareSide) / 2.0)
		Imgproc.rectangle(imageViewFinder, topLeft, bottomRight, WHITE, Imgproc.FILLED)
	}

	override fun onCameraViewStopped() {
		imageViewFinder.release()
		synchronized(this) {
			imageCorrected?.release()
			imageCorrected = null
			lastBoardContour?.release()
			lastBoardContour = null
			lastBoardContourVisual?.release()
			lastBoardContourVisual = null
		}
	}

	override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
		val rgba = inputFrame.rgba()
		val gray = inputFrame.gray()
		frameCount++

		val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) display?.rotation else (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
		val isPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180

		val procRgba = if (isPortrait) { val r = Mat(); Core.rotate(rgba, r, Core.ROTATE_90_CLOCKWISE); r } else rgba
		val procGray = if (isPortrait) { val r = Mat(); Core.rotate(gray, r, Core.ROTATE_90_CLOCKWISE); r } else gray

		val boardContour = findSudokuBoardContour(procGray)
		if (boardContour != null) {
			val pts = MatOfPoint()
			boardContour.convertTo(pts, CvType.CV_32S)
			synchronized(this) {
				lastBoardContour?.release()
				lastBoardContourVisual?.release()
				lastBoardContour = boardContour
				lastBoardContourVisual = pts
				contourPersistence = 10 // Stay for 10 frames
			}
		} else {
			synchronized(this) {
				contourPersistence--
				if (contourPersistence <= 0) {
					lastBoardContour?.release()
					lastBoardContour = null
					lastBoardContourVisual?.release()
					lastBoardContourVisual = null
				}
			}
		}

		synchronized(this) {
			if (contourPersistence > 0 && lastBoardContourVisual != null) {
				Imgproc.drawContours(procRgba, listOf(lastBoardContourVisual), 0, DARK_GREEN, 6)
			}
		}

		if (isDuringOCR || frameCount % 3 != 0) {
			compositeViewFinder(procRgba, if (isPortrait) getUprightViewFinder() else imageViewFinder)
			compositeDebugInfo(procRgba); return finishFrame(procRgba, rgba, isPortrait, procGray)
		}

		val currentContour = synchronized(this) { 
			val c = lastBoardContour ?: return@synchronized null
			val copy = MatOfPoint2f()
			c.copyTo(copy)
			copy
		}
		if (currentContour == null) {
			compositeViewFinder(procRgba, if (isPortrait) getUprightViewFinder() else imageViewFinder)
			compositeDebugInfo(procRgba); return finishFrame(procRgba, rgba, isPortrait, procGray)
		}
		isDuringOCR = true

		val targetSize = 900.0 
		val imageToCorrect = Mat(targetSize.toInt(), targetSize.toInt(), CvType.CV_8UC1)
		removePerspective(procGray, imageToCorrect, currentContour, targetSize)
		currentContour.release()

		val refinedRect = findRefinedGridRect(imageToCorrect)
		val grid = imageToCorrect.getGridInfo(refinedRect)
		if (grid.gridWidth < 100 || grid.gridHeight < 100) {
			compositeViewFinder(procRgba, if (isPortrait) getUprightViewFinder() else imageViewFinder)
			compositeDebugInfo(procRgba); isDuringOCR = false; imageToCorrect.release(); return finishFrame(procRgba, rgba, isPortrait, procGray)
		}

		Imgproc.adaptiveThreshold(imageToCorrect, imageToCorrect, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)
		
		// Denoise
		val k = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
		Imgproc.morphologyEx(imageToCorrect, imageToCorrect, Imgproc.MORPH_OPEN, k); k.release()
		
		imageToCorrect.blankOutGrid(grid)
		val values = recognizeDigits(imageToCorrect, grid)
		analyzeOcrResult(values)
		isDuringOCR = false

		compositeViewFinder(procRgba, if (isPortrait) getUprightViewFinder() else imageViewFinder)
		synchronized(this) {
			imageCorrected?.release(); imageCorrected = imageToCorrect
		}
		compositeDebugInfo(procRgba)
		return finishFrame(procRgba, rgba, isPortrait, procGray)
	}

	private fun finishFrame(procRgba: Mat, originalRgba: Mat, isPortrait: Boolean, procGray: Mat): Mat {
		if (isPortrait) { Core.rotate(procRgba, originalRgba, Core.ROTATE_90_COUNTERCLOCKWISE); procRgba.release(); procGray.release(); return originalRgba }
		return procRgba
	}

	private var uprightViewFinder: Mat? = null
	private fun getUprightViewFinder(): Mat {
		if (uprightViewFinder == null) { uprightViewFinder = Mat(); Core.rotate(imageViewFinder, uprightViewFinder!!, Core.ROTATE_90_CLOCKWISE) }
		return uprightViewFinder!!
	}

	private fun recognizeDigits(image: Mat, grid: GridParams): Array<IntArray> {
		val values = Array(SUDOKU_SIZE) { IntArray(SUDOKU_SIZE) }
		val cleanBoard = Mat.zeros(image.size(), image.type())

		val cellW = (grid.gridWidth / SUDOKU_SIZE)
		val cellH = (grid.gridHeight / SUDOKU_SIZE)
		val padW = (cellW * 0.18).toInt()
		val padH = (cellH * 0.18).toInt()
		
		for (row in 0 until SUDOKU_SIZE) {
			for (col in 0 until SUDOKU_SIZE) {
				val x = (grid.getColX(col.toDouble()) + padW).toInt()
				val y = (grid.getRowY(row.toDouble()) + padH).toInt()
				val w = (cellW - 2 * padW).toInt()
				val h = (cellH - 2 * padH).toInt()

				if (x < 0 || y < 0 || x + w > image.width() || y + h > image.height()) continue
				val rawCell = image.submat(y, y + h, x, x + w)
				if (Core.countNonZero(rawCell) > (w * h * 0.01)) {
					val cell = cleanCell(rawCell)
					if (Core.countNonZero(cell) > (w * h * 0.015)) {
						values[row][col] = digitRecognizer.classify(cell)
						val target = cleanBoard.submat(y, y + h, x, x + w); cell.copyTo(target); target.release()
					}
					cell.release()
				}
				rawCell.release()
			}
		}
		synchronized(this) { lastGridParams = grid; cleanBoard.copyTo(image) }; cleanBoard.release()
		return values
	}

	private fun findRefinedGridRect(warpedGray: Mat): Rect {
		val thresh = Mat()
		Imgproc.adaptiveThreshold(warpedGray, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)
		val horizontal = thresh.clone(); val vertical = thresh.clone(); val scale = 20
		val horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(horizontal.cols() / scale.toDouble(), 1.0))
		Imgproc.erode(horizontal, horizontal, horizontalStructure); Imgproc.dilate(horizontal, horizontal, horizontalStructure)
		val verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.0, vertical.rows() / scale.toDouble()))
		Imgproc.erode(vertical, vertical, verticalStructure); Imgproc.dilate(vertical, vertical, verticalStructure)
		val grid = Mat(); Core.add(horizontal, vertical, grid)
		val contoursList = ArrayList<MatOfPoint>(); Imgproc.findContours(grid, contoursList, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
		var maxArea = 0.0; var bestRect = Rect(0, 0, warpedGray.cols(), warpedGray.rows())
		for (contour in contoursList) {
			val area = Geometry.contourArea(contour)
			if (area > maxArea) {
				val rect = Geometry.boundingRect(contour)
				val aspect = rect.width.toDouble() / rect.height
				if (aspect in 0.8..1.2 && rect.width > warpedGray.cols() * 0.4) { maxArea = area; bestRect = rect }
			}
		}
		thresh.release(); horizontal.release(); vertical.release(); grid.release()
		return bestRect
	}

	private fun cleanCell(cell: Mat): Mat {
		val labels = Mat(); val stats = Mat(); val centroids = Mat()
		val nLabels = Imgproc.connectedComponentsWithStats(cell, labels, stats, centroids)
		var bestLabel = -1; var maxArea = 0; val minArea = cell.width() * cell.height() * 0.02
		for (i in 1 until nLabels) {
			val area = stats.get(i, Imgproc.CC_STAT_AREA)[0].toInt()
			val w = stats.get(i, Imgproc.CC_STAT_WIDTH)[0].toInt()
			val h = stats.get(i, Imgproc.CC_STAT_HEIGHT)[0].toInt()
			val x = stats.get(i, Imgproc.CC_STAT_LEFT)[0].toInt()
			val y = stats.get(i, Imgproc.CC_STAT_TOP)[0].toInt()
			
			val centerX = x + w / 2.0; val centerY = y + h / 2.0
			val isCentral = Math.abs(centerX - cell.width() / 2.0) < cell.width() * 0.22 &&
					Math.abs(centerY - cell.height() / 2.0) < cell.height() * 0.22
			
			// Filter out frame remnants
			val touchesBorder = x <= 0 || y <= 0 || (x + w) >= cell.width() || (y + h) >= cell.height()

			if (isCentral && !touchesBorder && area > maxArea && area > minArea) {
				maxArea = area
				bestLabel = i
			}
		}
		val cleaned = Mat.zeros(cell.size(), cell.type())
		if (bestLabel != -1) Core.compare(labels, Scalar(bestLabel.toDouble()), cleaned, Core.CMP_EQ)
		labels.release(); stats.release(); centroids.release(); return cleaned
	}

	private fun analyzeOcrResult(values: Array<IntArray>) {
		ocrCount++; var recognized = 0
		for (r in 0 until SUDOKU_SIZE) for (c in 0 until SUDOKU_SIZE) if (values[r][c] != 0) recognized++
		synchronized(this) { lastRecognizedBoard = values }
		if (recognized < 17) return
		val board = fromArray(values, false)
		if (!board.validateAllCells() || SudokuSolver.countSolutions(board, 2) != 1) return
		synchronized(this) {
			successfulRecognitionCount++
			if (board.valuesCount > (bestCollection?.valuesCount ?: 0)) bestCollection = board
			if (successfulRecognitionCount >= NUMBER_OF_REQUIRED_VALID_PUZZLE_RECOGNITIONS) {
				this.setResult(RESULT_OK, Intent().putExtra(Tag.CELL_COLLECTION, bestCollection!!.serialize())); finish()
			}
		}
	}

	private fun compositeViewFinder(background: Mat, viewFinder: Mat) {
		if (background.size() != viewFinder.size()) return
		Core.addWeighted(background, 0.5, viewFinder, 0.3, 0.0, background)
		val y = (background.height() * 0.05).toInt(); val markerSize = 40
		var x = background.width() / 2 - (NUMBER_OF_REQUIRED_VALID_PUZZLE_RECOGNITIONS - 1) * markerSize * 2 / 2
		for (i in 1..NUMBER_OF_REQUIRED_VALID_PUZZLE_RECOGNITIONS) {
			Imgproc.drawMarker(background, Point(x.toDouble(), y.toDouble()), if (i <= successfulRecognitionCount) GREEN else RED, Imgproc.MARKER_SQUARE, markerSize)
			x += markerSize * 2
		}
	}

	private fun compositeDebugInfo(background: Mat) {
		val (outputImage, grid, recognizedBoard) = synchronized(this) {
			Triple(imageCorrected?.clone(), lastGridParams, lastRecognizedBoard)
		}
		if (outputImage == null || outputImage.empty()) {
			outputImage?.release(); return
		}

		if (outputImage.type() == CvType.CV_8UC1) Imgproc.cvtColor(outputImage, outputImage, Imgproc.COLOR_GRAY2BGRA)

		val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) display?.rotation else (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
		val isPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180

		val imageToShow: Mat
		// One is like showDebug = false (cleaned digits)
		val leftPart = outputImage.clone()

		// Other is "just the recognition part" (red overlay on black background)
		val rightPart = Mat.zeros(outputImage.size(), outputImage.type())
		rightPart.drawGrid(SUDOKU_SIZE, SUDOKU_SIZE, RED, 100, grid)
		recognizedBoard?.let { rightPart.printInValuesScaled(it, 1.0, isPortrait, grid) }

		val combined = Mat(outputImage.rows(), outputImage.cols() * 2, outputImage.type())
		val leftSub = combined.submat(0, outputImage.rows(), 0, outputImage.cols())
		leftPart.copyTo(leftSub); leftSub.release(); leftPart.release()

		val rightSub = combined.submat(0, outputImage.rows(), outputImage.cols(), outputImage.cols() * 2)
		rightPart.copyTo(rightSub); rightSub.release(); rightPart.release()

		imageToShow = combined
		outputImage.release()

		val scale = min(background.height() * 0.4 / imageToShow.height(), background.width() * 0.9 / imageToShow.width())
		val displayMat = Mat()
		val dw = (imageToShow.width() * scale).coerceAtLeast(10.0).toInt()
		val dh = (imageToShow.height() * scale).coerceAtLeast(10.0).toInt()
		Imgproc.resize(imageToShow, displayMat, Size(dw.toDouble(), dh.toDouble()))

		val startX = (background.cols() - dw) / 2
		val startY = background.rows() - dh - 50 // 50px padding from bottom
		val endX = startX + dw
		val endY = startY + dh

		if (startX >= 0 && startY >= 0 && endX <= background.cols() && endY <= background.rows()) {
			val sub = background.submat(startY, endY, startX, endX)
			Core.addWeighted(sub, 0.5, displayMat, 0.5, 0.0, sub)
			sub.release()
		}
		displayMat.release(); imageToShow.release()
	}

	private fun removePerspective(srcImage: Mat?, dstImage: Mat?, contour: MatOfPoint2f, targetSize: Double) {
		val cornerPoints = findCornerPoints(contour.toArray())
		val centerX = cornerPoints.map { it.x }.average(); val centerY = cornerPoints.map { it.y }.average()
		val shrunkPoints = cornerPoints.map { p -> Point(p.x + (centerX - p.x) * 0.05, p.y + (centerY - p.y) * 0.05) }.toTypedArray()
		val padding = targetSize / 60.0; val rect = MatOfPoint2f(*shrunkPoints); val dst = MatOfPoint2f(Point(padding, padding), Point(targetSize - padding, padding), Point(targetSize - padding, targetSize - padding), Point(padding, targetSize - padding))
		val transform = Geometry.getPerspectiveTransform(rect, dst); Imgproc.warpPerspective(srcImage, dstImage, transform, Size(targetSize, targetSize)); rect.release(); dst.release(); transform.release()
	}

	private fun findCornerPoints(points: Array<Point>): Array<Point> {
		var maxSum = Double.MIN_VALUE; var maxSumIdx = 0; var minSum = Double.MAX_VALUE; var minSumIdx = 0
		var maxDiff = Double.MIN_VALUE; var maxDiffIdx = 0; var minDiff = Double.MAX_VALUE; var minDiffIdx = 0
		for (i in points.indices) {
			val sum = points[i].x + points[i].y; val diff = points[i].y - points[i].x
			if (sum > maxSum) { maxSum = sum; maxSumIdx = i }; if (sum < minSum) { minSum = sum; minSumIdx = i }
			if (diff > maxDiff) { maxDiff = diff; maxDiffIdx = i }; if (diff < minDiff) { minDiff = diff; minDiffIdx = i }
		}
		return arrayOf(points[minSumIdx], points[minDiffIdx], points[maxSumIdx], points[maxDiffIdx])
	}

	private fun findSudokuBoardContour(detectionGray: Mat?): MatOfPoint2f? {
		val edges = Mat(); Imgproc.Canny(detectionGray, edges, 200.0, 255.0)
		contours.clear(); Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
		if (contours.isEmpty()) { edges.release(); return null }
		var maxA = 0.0; var imax = 0
		for (i in contours.indices) { val area = Geometry.contourArea(contours[i]); if (area > maxA) { maxA = area; imax = i } }
		val c2f = MatOfPoint2f(*contours[imax].toArray()); val approx = MatOfPoint2f()
		Geometry.approxPolyDP(c2f, approx, 0.01 * Geometry.arcLength(c2f, true), true); edges.release(); c2f.release(); return approx
	}

	companion object {
		private const val TAG = "SudokuScanActivity"
		val RED = Scalar(255.0, 0.0, 0.0); val GREEN = Scalar(0.0, 255.0, 0.0); val DARK_GREEN = Scalar(0.0, 127.0, 0.0); val WHITE = Scalar(255.0, 255.0, 255.0)
	}
}

internal fun Mat.blankOutGrid(grid: GridParams) {
	val thickness = (grid.gridWidth / 80.0).toInt().coerceAtLeast(2)
	for (i in 0..SUDOKU_SIZE) {
		Imgproc.line(this, Point(grid.getColX(i.toDouble()), grid.minY), Point(grid.getColX(i.toDouble()), grid.maxY), Scalar(0.0), thickness)
		Imgproc.line(this, Point(grid.minX, grid.getRowY(i.toDouble())), Point(grid.maxX, grid.getRowY(i.toDouble())), Scalar(0.0), thickness)
	}
}

internal fun Mat.printInValuesScaled(lastRecognizedBoard: Array<IntArray>, fontScaleBase: Double, rotate90CW: Boolean = false, gridParams: GridParams? = null) {
	val targetMat = if (rotate90CW) { val r = Mat(); Core.rotate(this, r, Core.ROTATE_90_COUNTERCLOCKWISE); r } else this
	val grid = gridParams ?: targetMat.getGridInfo()
	for (row in 0 until SUDOKU_SIZE) {
		for (col in 0 until SUDOKU_SIZE) {
			val value = if (rotate90CW) lastRecognizedBoard[col][SUDOKU_SIZE - 1 - row] else lastRecognizedBoard[row][col]
			if (value != 0) {
				val text = "$value"; val font = Imgproc.FONT_HERSHEY_DUPLEX; val fontScale = 2.2 * fontScaleBase; val thickness = (3 * fontScaleBase).toInt().coerceAtLeast(1)
				val textSize = Imgproc.getTextSize(text, font, fontScale, thickness, null)
				val textLocation = Point(grid.getColX(col + 0.5) - textSize.width / 2, grid.getRowY(row + 0.5) + textSize.height / 2)
				Imgproc.putText(targetMat, text, textLocation, font, fontScale, SudokuScanActivity.RED, thickness)
			}
		}
	}
	if (rotate90CW) { Core.rotate(targetMat, this, Core.ROTATE_90_CLOCKWISE); targetMat.release() }
}

internal fun Mat.drawGrid(rows: Int, cols: Int, color: Scalar, thicknessDivisor: Int, gridParams: GridParams? = null) {
	val grid = gridParams ?: getGridInfo()
	for (row in 0..rows) Imgproc.line(this, Point(grid.minX, grid.getRowY(row.toDouble())), Point(grid.maxX, grid.getRowY(row.toDouble())), color, (grid.gridHeight / thicknessDivisor).toInt().coerceAtLeast(1))
	for (col in 0..cols) Imgproc.line(this, Point(grid.getColX(col.toDouble()), grid.minY), Point(grid.getColX(col.toDouble()), grid.maxY), color, (grid.gridWidth / thicknessDivisor).toInt().coerceAtLeast(1))
}

internal fun Mat.getGridInfo(rect: Rect? = null): GridParams {
	return GridParams().apply {
		if (rect != null) { minX = rect.x.toDouble(); minY = rect.y.toDouble(); maxX = (rect.x + rect.width).toDouble(); maxY = (rect.y + rect.height).toDouble() }
		else { val p = width().toDouble() / 60; minX = p; minY = p; maxX = width().toDouble() - p; maxY = height().toDouble() - p }
		gridWidth = maxX - minX; gridHeight = maxY - minY; cellWidth = gridWidth / SUDOKU_SIZE; cellHeight = gridHeight / SUDOKU_SIZE
	}
}

class GridParams {
	var minX: Double = 0.0; var maxX: Double = 0.0; var minY: Double = 0.0; var maxY: Double = 0.0
	var gridHeight: Double = 0.0; var gridWidth: Double = 0.0; var cellHeight: Double = 0.0; var cellWidth: Double = 0.0
	fun getColX(col: Double): Double = minX + gridWidth * col / SUDOKU_SIZE
	fun getRowY(row: Double): Double = minY + gridHeight * row / SUDOKU_SIZE
}
