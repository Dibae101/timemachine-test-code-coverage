package org.moire.opensudoku.pageobject

import androidx.test.espresso.Espresso.pressBack

abstract class BasePageObject {

	fun goBack() = apply {
		pressBack()
	}
}
