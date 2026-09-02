package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.extension.shift
import com.example.util.simpletimetracker.domain.extension.orZero
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class CalendarShiftTest(
    private val input: Pair<Calendar, String>,
    private val output: String,
) {

    @Rule
    @JvmField
    var timeZoneRule = TimeZoneRule()

    @Test
    fun map() {
        val expected = output
        val shift = input.second.split(":").let {
            val negative = it.first().startsWith("-")
            val sign = if (negative) -1 else 1
            it.first().toIntOrNull().orZero() * hourInMs +
                sign * it.last().toIntOrNull().orZero() * minInMs
        }
        val actual = input.first.shift(shift).let {
            "${it.get(Calendar.HOUR_OF_DAY)}:${it.get(Calendar.MINUTE)}"
        }

        assertEquals(
            "Test failed for params $input",
            expected,
            actual,
        )
    }

    companion object {
        private val minInMs = TimeUnit.MINUTES.toMillis(1)
        private val hourInMs = TimeUnit.HOURS.toMillis(1)

        // Day of dst forward change in Germany.
        private fun getDstForward(time: String): Calendar {
            val hour = time.split(":").first().toIntOrNull().orZero()
            val min = time.split(":").last().toIntOrNull().orZero()
            return Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
                timeInMillis = 0
                set(Calendar.YEAR, 2025)
                set(Calendar.MONTH, 2)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.DAY_OF_MONTH, 30)
            }
        }

        // Day of dst forward change in Germany.
        private fun getDstBackward(time: String): Calendar {
            val hour = time.split(":").first().toIntOrNull().orZero()
            val min = time.split(":").last().toIntOrNull().orZero()
            return Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
                timeInMillis = 0
                set(Calendar.YEAR, 2025)
                set(Calendar.MONTH, 9)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.DAY_OF_MONTH, 26)
            }
        }

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            // Forward dst change.
            arrayOf(getDstForward("0:0") to "0:0", "0:0"),
            arrayOf(getDstForward("0:0") to "1:0", "1:0"),
            arrayOf(getDstForward("0:0") to "1:59", "1:59"),
            arrayOf(getDstForward("0:0") to "2:0", "3:0"),
            arrayOf(getDstForward("0:0") to "2:1", "3:1"),
            arrayOf(getDstForward("0:0") to "2:30", "3:30"),
            arrayOf(getDstForward("0:0") to "2:59", "3:59"),
            arrayOf(getDstForward("0:0") to "3:0", "3:0"),
            arrayOf(getDstForward("0:0") to "3:1", "3:1"),
            arrayOf(getDstForward("0:0") to "4:0", "4:0"),
            arrayOf(getDstForward("0:0") to "5:0", "5:0"),

            arrayOf(getDstForward("5:0") to "0:0", "5:0"),
            arrayOf(getDstForward("5:0") to "-1:0", "4:0"),
            arrayOf(getDstForward("5:0") to "-1:59", "3:1"),
            arrayOf(getDstForward("5:0") to "-2:0", "3:0"),
            arrayOf(getDstForward("5:0") to "-2:1", "1:59"),
            arrayOf(getDstForward("5:0") to "-2:30", "1:30"),
            arrayOf(getDstForward("5:0") to "-2:59", "1:1"),
            arrayOf(getDstForward("5:0") to "-3:0", "1:0"),
            arrayOf(getDstForward("5:0") to "-3:1", "1:59"),
            arrayOf(getDstForward("5:0") to "-4:0", "1:0"),
            arrayOf(getDstForward("5:0") to "-5:0", "0:0"),

            arrayOf(getDstForward("0:0") to "2:0", "3:0"),
            arrayOf(getDstForward("0:0") to "3:0", "3:0"),
            arrayOf(getDstForward("1:0") to "1:0", "3:0"),
            arrayOf(getDstForward("1:0") to "2:0", "3:0"),
            arrayOf(getDstForward("2:0") to "1:0", "4:0"),
            arrayOf(getDstForward("2:0") to "2:0", "5:0"),
            arrayOf(getDstForward("3:0") to "1:0", "4:0"),
            arrayOf(getDstForward("3:0") to "2:0", "5:0"),
            arrayOf(getDstForward("3:0") to "-1:0", "1:0"),
            arrayOf(getDstForward("3:0") to "-2:0", "1:0"),
            arrayOf(getDstForward("3:0") to "-3:0", "0:0"),
            arrayOf(getDstForward("2:0") to "-1:0", "1:0"),
            arrayOf(getDstForward("2:0") to "-2:0", "1:0"),
            arrayOf(getDstForward("1:0") to "-1:0", "0:0"),

            // Backward dst change.
            arrayOf(getDstBackward("0:0") to "0:0", "0:0"),
            arrayOf(getDstBackward("0:0") to "1:0", "1:0"),
            arrayOf(getDstBackward("0:0") to "2:0", "2:0"),
            arrayOf(getDstBackward("0:0") to "3:0", "3:0"),
            arrayOf(getDstBackward("0:0") to "4:0", "4:0"),
            arrayOf(getDstBackward("0:0") to "5:0", "5:0"),

            arrayOf(getDstBackward("5:0") to "0:0", "5:0"),
            arrayOf(getDstBackward("5:0") to "-1:0", "4:0"),
            arrayOf(getDstBackward("5:0") to "-2:0", "3:0"),
            arrayOf(getDstBackward("5:0") to "-3:0", "2:0"),
            arrayOf(getDstBackward("5:0") to "-4:0", "1:0"),
            arrayOf(getDstBackward("5:0") to "-5:0", "0:0"),

            arrayOf(getDstBackward("0:0") to "2:0", "2:0"),
            arrayOf(getDstBackward("0:0") to "3:0", "3:0"),
            arrayOf(getDstBackward("1:0") to "1:0", "2:0"),
            arrayOf(getDstBackward("1:0") to "2:0", "3:0"),
            arrayOf(getDstBackward("2:0") to "1:0", "3:0"),
            arrayOf(getDstBackward("2:0") to "2:0", "4:0"),
            arrayOf(getDstBackward("3:0") to "1:0", "4:0"),
            arrayOf(getDstBackward("3:0") to "2:0", "5:0"),
            arrayOf(getDstBackward("3:0") to "-1:0", "2:0"),
            arrayOf(getDstBackward("3:0") to "-2:0", "1:0"),
            arrayOf(getDstBackward("3:0") to "-3:0", "0:0"),
            arrayOf(getDstBackward("2:0") to "-1:0", "1:0"),
            arrayOf(getDstBackward("2:0") to "-2:0", "0:0"),
            arrayOf(getDstBackward("1:0") to "-1:0", "0:0"),
        )
    }

    class TimeZoneRule : TestWatcher() {
        private val origDefault: TimeZone = TimeZone.getDefault()

        override fun starting(description: Description?) {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        }

        override fun finished(description: Description?) {
            TimeZone.setDefault(origDefault)
        }
    }
}