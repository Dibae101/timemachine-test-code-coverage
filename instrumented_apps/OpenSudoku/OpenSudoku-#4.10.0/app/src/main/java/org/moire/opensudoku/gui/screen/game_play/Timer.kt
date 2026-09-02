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
package org.moire.opensudoku.gui.screen.game_play

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * This class implements a simple periodic timer. Construct a periodic timer with a given tick interval.
 * @param tickInterval The tick interval in ms.
 */
abstract class Timer(private var tickInterval: Long) : Handler(Looper.getMainLooper()) {
	/**
	 * Query whether this Timer is running.
	 *
	 * @return true iff we're running.
	 */
	var isRunning = false
		private set

	// Number of times step() has been called.
	private var tickCount = 0

	// Time at which to execute the next step.  We schedule each
	// step at this plus x ms; this gives us an even execution rate.
	private var nextStepTime: Long = 0

	/**
	 * Get the accumulated time of this Timer.
	 *
	 * @return How long this timer has been running, in ms.
	 */
	// The accumulated time in ms for which this timer has been running.
	// Increments between start() and stop(); start(true) resets it.
	var time: Long = 0
		private set

	// The time at which we last added to `time`.
	private var lastLogTime: Long = 0

	// ******************************************************************** //
	// Handlers.
	// ******************************************************************** //
	/**
	 * Handle a step of the animation.
	 */
	private val runner: Runnable = object : Runnable {
		override fun run() {
			if (isRunning) {
				val now = SystemClock.uptimeMillis()

				// Add up the time since the last step.
				time += now - lastLogTime
				lastLogTime = now
				tickCount++
				if (!step()) {
					// Schedule the next.  If we've got behind, schedule
					// it for a tick after now.  (Otherwise we'd end
					// up with a zillion events queued.)
					nextStepTime += tickInterval
					if (nextStepTime <= now) {
						nextStepTime += tickInterval
					}
					postAtTime(this, nextStepTime)
				} else {
					isRunning = false
				}
			}
		}
	}
	// ******************************************************************** //
	// Implementation.
	// ******************************************************************** //
	/**
	 * Start the timer.  step() will be called at regular intervals
	 * until it returns true; then done() will be called.
	 *
	 * Subclasses may override this to do their own setup; but they
	 * must then call super.start().
	 */
	fun start() {
		if (isRunning) return
		isRunning = true
		val now = SystemClock.uptimeMillis()

		// Start accumulating time again.
		lastLogTime = now

		// Schedule the first event at once.
		nextStepTime = now
		postAtTime(runner, nextStepTime)
	}
	// ******************************************************************** //
	// State Save/Restore.
	// ******************************************************************** //
	/**
	 * Stop the timer.  step() will not be called again until it is
	 * restarted.
	 *
	 * Subclasses may override this to do their own setup; but they
	 * must then call super.stop().
	 */
	fun stop() {
		if (isRunning) {
			isRunning = false
			val now = SystemClock.uptimeMillis()
			time += now - lastLogTime
			lastLogTime = now
		}
	}

	/**
	 * Subclasses override this to handle a timer tick.
	 *
	 * @return true if the timer should stop; this will
	 * trigger a call to done().  false otherwise;
	 * we will continue calling step().
	 */
	protected abstract fun step(): Boolean
}
