package org.andstatus.game2048

import korlibs.concurrent.thread.NativeThread.Companion.current
import korlibs.time.Stopwatch
import korlibs.time.milliseconds
import kotlinx.atomicfu.atomic

val isTestRun = atomic(false)
val gameStopWatch = Stopwatch().start()
val gameIsLoading = atomic(false)

fun myLogInTest(messageSupplier: () -> String): Unit {
    if (isTestRun.value) myLog(messageSupplier())
}

fun myLog(message: Any?) =
    println("game2048.log ${gameStopWatch.elapsed.milliseconds.toInt()} [${current.id}] $message")

inline fun <T> myMeasured(message: Any?, measuredAction: () -> T): T =
    Stopwatch().start().let { stopWatch ->
        measuredAction().also {
            myLog("$message in ${stopWatch.elapsed.milliseconds.toInt()} ms")
        }
    }

inline fun <T> myMeasuredIt(message: Any?, measuredAction: () -> T): T =
    Stopwatch().start().let { stopWatch ->
        measuredAction().also {
            myLog("$message in ${stopWatch.elapsed.milliseconds.toInt()} ms: $it")
        }
    }
