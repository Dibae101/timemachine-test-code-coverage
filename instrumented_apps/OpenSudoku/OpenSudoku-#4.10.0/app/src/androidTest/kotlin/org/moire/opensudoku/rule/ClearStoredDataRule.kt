package org.moire.opensudoku.rule

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.moire.opensudoku.db.DATABASE_NAME

class ClearStoredDataRule : TestRule {

	override fun apply(base: Statement, description: Description): Statement {
		val context = InstrumentationRegistry.getInstrumentation().targetContext

		return object : Statement() {
			override fun evaluate() {
				clearAllSharedPreferences(context)
				clearDatabase(context)
				base.evaluate()
			}
		}
	}

	private fun clearAllSharedPreferences(context: Context) {
		val prefsDir = context.filesDir.parentFile!!.resolve("shared_prefs")
		val prefsFiles = prefsDir.listFiles { file -> file.isFile && file.extension == "xml" }
		prefsFiles!!.map { it.nameWithoutExtension }
			.forEach { context.deleteSharedPreferences(it) }
	}

	private fun clearDatabase(context: Context) {
		context.deleteDatabase(DATABASE_NAME)
	}
}
