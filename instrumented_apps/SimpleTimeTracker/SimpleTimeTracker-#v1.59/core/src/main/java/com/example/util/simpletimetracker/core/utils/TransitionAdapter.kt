@file:Suppress("unused")

package com.example.util.simpletimetracker.core.utils

import androidx.transition.Transition

inline fun Transition.doOnEnd(
    crossinline action: (transition: Transition) -> Unit,
): Transition.TransitionListener = addListener(onEnd = action)

inline fun Transition.doOnStart(
    crossinline action: (transition: Transition) -> Unit,
): Transition.TransitionListener = addListener(onStart = action)

inline fun Transition.doOnCancel(
    crossinline action: (transition: Transition) -> Unit,
): Transition.TransitionListener = addListener(onCancel = action)

inline fun Transition.doOnResume(
    crossinline action: (transition: Transition) -> Unit,
): Transition.TransitionListener = addListener(onResume = action)

inline fun Transition.doOnPause(
    crossinline action: (transition: Transition) -> Unit,
): Transition.TransitionListener = addListener(onPause = action)

inline fun Transition.addListener(
    crossinline onEnd: (transition: Transition) -> Unit = {},
    crossinline onStart: (transition: Transition) -> Unit = {},
    crossinline onCancel: (transition: Transition) -> Unit = {},
    crossinline onResume: (transition: Transition) -> Unit = {},
    crossinline onPause: (transition: Transition) -> Unit = {},
): Transition.TransitionListener {
    val listener = object : Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition) = onEnd(transition)
        override fun onTransitionResume(transition: Transition) = onResume(transition)
        override fun onTransitionPause(transition: Transition) = onPause(transition)
        override fun onTransitionCancel(transition: Transition) = onCancel(transition)
        override fun onTransitionStart(transition: Transition) = onStart(transition)
    }
    addListener(listener)
    return listener
}
