package org.moire.opensudoku.rule

import androidx.test.ext.junit.rules.activityScenarioRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.moire.opensudoku.gui.screen.title.TitleScreenActivity

fun applicationLaunchRule(): TestRule {
	return RuleChain.outerRule(FixedTimeRule())
		.around(ClearStoredDataRule())
		.around(activityScenarioRule<TitleScreenActivity>())
}
