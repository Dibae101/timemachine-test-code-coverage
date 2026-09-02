package com.example.util.simpletimetracker.feature_statistics_detail

import com.example.util.simpletimetracker.feature_statistics_detail.interactor.FillEmptyBarsWithPreviousValueInteractor
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartBarDataDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FillEmptyBarsWithPreviousValueInteractorTest {

    private val interactor = FillEmptyBarsWithPreviousValueInteractor()

    @Test
    fun returnsEmptyWhenDataIsEmpty() {
        // When
        val result = interactor.invoke(
            data = emptyList(),
            previousRangeData = listOf(chartBar(0, listOf(1L to 1))),
        )

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun fillsEmptyFromPreviousRange() {
        // Given
        val now = System.currentTimeMillis()
        val previousDuration = listOf(1L to 100)
        val previous = chartBar(now - 10_000, previousDuration)
        val data = listOf(chartBar(now - 9_000, emptyList()))

        // When
        val result = interactor.invoke(
            data = data,
            previousRangeData = listOf(previous),
        )

        // Then
        assertEquals(previousDuration, result[0].durations)
    }

    @Test
    fun fillsFromMostRecentPreviousNonEmptyBar() {
        // Given
        val now = System.currentTimeMillis()
        val oldestDuration = listOf(1L to 100)
        val newestDuration = listOf(2L to 200)
        val data = listOf(chartBar(now - 9_000, emptyList()))

        // When
        val result = interactor.invoke(
            data = data,
            previousRangeData = listOf(
                chartBar(now - 20_000, oldestDuration),
                chartBar(now - 18_000, emptyList()),
                chartBar(now - 16_000, newestDuration),
            ),
        )

        // Then
        assertEquals(newestDuration, result[0].durations)
    }

    @Test
    fun fillsConsecutiveEmptyBars() {
        // Given
        val now = System.currentTimeMillis()
        val firstDurations = listOf(2L to 200)
        val data = listOf(
            chartBar(now - 12_000, firstDurations),
            chartBar(now - 10_000, emptyList()),
            chartBar(now - 8_000, emptyList()),
        )

        // When
        val result = interactor.invoke(
            data = data,
            previousRangeData = null,
        )

        // Then
        assertEquals(firstDurations, result[1].durations)
        assertEquals(firstDurations, result[2].durations)
    }

    @Test
    fun doesNotFillFutureEmptyBars() {
        // Given
        val now = System.currentTimeMillis()
        val upcomingBar = chartBar(now + 10_000, emptyList())

        // When
        val result = interactor.invoke(
            listOf(element = upcomingBar),
            listOf(chartBar(now - 20_000, listOf(3L to 300))),
        )

        // Then
        assertTrue(result[0].durations.isEmpty())
    }

    private fun chartBar(
        rangeStart: Long,
        durations: List<Pair<Long, Int>>,
    ): ChartBarDataDuration {
        return ChartBarDataDuration(
            rangeStart = rangeStart,
            legend = "legend",
            durations = durations,
        )
    }
}