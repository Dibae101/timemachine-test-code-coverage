package org.moire.opensudoku.utils

import androidx.annotation.VisibleForTesting
import java.time.LocalDateTime

object TimeProvider {

	private var fixedTime: LocalDateTime? = null

	fun getCurrentLocalTime(): LocalDateTime {
		return fixedTime ?: LocalDateTime.now()
	}

	@VisibleForTesting
	fun overrideCurrentTime(newTime: LocalDateTime?) {
		fixedTime = newTime
	}
}
