package org.moire.opensudoku.rule

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.moire.opensudoku.utils.TimeProvider
import java.time.LocalDateTime

class FixedTimeRule : TestRule {
	override fun apply(base: Statement, description: Description): Statement {
		return object : Statement() {
			override fun evaluate() {
				val time = getTimeFromTestMethod(description)
				TimeProvider.overrideCurrentTime(time)
				try {
					base.evaluate()
				} finally {
					TimeProvider.overrideCurrentTime(null)
				}
			}
		}
	}

	private fun getTimeFromTestMethod(description: Description): LocalDateTime? {
		val fixedTimeAnnotation = description.getAnnotation(FixedTime::class.java)
		return fixedTimeAnnotation?.let { LocalDateTime.parse(it.time) }
	}
}

@Retention(AnnotationRetention.RUNTIME)
annotation class FixedTime(val time: String)
