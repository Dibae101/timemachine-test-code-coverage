package com.example.util.simpletimetracker.core.manager

import androidx.core.view.WindowInsetsCompat
import com.example.util.simpletimetracker.core.utils.isKeyboardInsetsVisible

object KeyboardVisibilityManager {
    private var isKeyboardVisible: Boolean = false
    private var observers: MutableMap<Any, Observer> = mutableMapOf()

    fun onInsetsChanged(insets: WindowInsetsCompat) {
        val isVisible = insets.isKeyboardInsetsVisible()
        if (isVisible != isKeyboardVisible) {
            isKeyboardVisible = isVisible
            observers.values.forEach { observer ->
                observer.onKeyboardVisibilityChanged(isVisible)
            }
        }
    }

    fun addObserver(tag: Any, observer: Observer) {
        observers[tag] = observer
    }

    fun removeObserver(tag: Any) {
        observers.remove(tag)
    }

    fun interface Observer {
        fun onKeyboardVisibilityChanged(isVisible: Boolean)
    }
}