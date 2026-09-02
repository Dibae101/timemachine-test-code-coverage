package com.example.util.simpletimetracker.core.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.util.simpletimetracker.core.manager.KeyboardVisibilityManager

// Can update padding or margin, which would be more appropriate.
fun View.doOnApplyWindowInsetsListener(block: View.(WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        // All setOnApplyWindowInsetsListener listeners should call Keyboard manager.
        KeyboardVisibilityManager.onInsetsChanged(windowInsets)
        view.block(windowInsets)
        windowInsets
    }

    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.removeOnAttachStateChangeListener(this)
                    ViewCompat.requestApplyInsets(view)
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            },
        )
    }
}

fun View.applyStatusBarInsets() {
    doOnApplyWindowInsetsListener { updatePadding(top = it.getStatusBarInsetsTop()) }
}

fun View.applyNavBarInsets() {
    doOnApplyWindowInsetsListener { updatePadding(bottom = it.getNavBarInsetsBottom()) }
}

fun WindowInsetsCompat.getStatusBarInsetsTop(): Int {
    return getInsets(WindowInsetsCompat.Type.statusBars()).top
}

fun WindowInsetsCompat.getNavBarInsetsBottom(): Int {
    return getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
}

// Need windowSoftInputMode="adjustResize" on activity in order to work on api < 30.
fun WindowInsetsCompat.isKeyboardInsetsVisible(): Boolean {
    return isVisible(WindowInsetsCompat.Type.ime())
}

sealed interface InsetConfiguration {
    object DoNotApply : InsetConfiguration
    data class ApplyToView(val view: () -> View) : InsetConfiguration
}
