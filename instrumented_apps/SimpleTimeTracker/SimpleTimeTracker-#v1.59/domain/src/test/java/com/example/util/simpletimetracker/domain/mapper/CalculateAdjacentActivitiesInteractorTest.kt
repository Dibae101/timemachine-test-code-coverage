package com.example.util.simpletimetracker.domain.mapper

import com.example.util.simpletimetracker.domain.record.interactor.CalculateAdjacentActivitiesInteractor
import com.example.util.simpletimetracker.domain.record.interactor.CalculateAdjacentActivitiesInteractor.CalculationResult
import com.example.util.simpletimetracker.domain.record.interactor.GetMultitaskRecordsInteractor
import com.example.util.simpletimetracker.domain.record.model.Record
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock

@RunWith(Parameterized::class)
class CalculateAdjacentActivitiesInteractorTest(
    private val input: Pair<List<Long>, List<Record>>,
    private val output: Map<Long, List<CalculationResult>>,
) {

    private val getMultitaskRecordsInteractor: GetMultitaskRecordsInteractor = mock()
    private val subject = CalculateAdjacentActivitiesInteractor(
        getMultitaskRecordsInteractor = getMultitaskRecordsInteractor,
    )

    @Suppress("UNCHECKED_CAST")
    @Test
    fun map() {
        val expected = output
        val actual = subject.calculateNextActivities(
            typeIds = input.first,
            records = input.second,
            maxCount = 5,
        )

        assertEquals(
            "Test failed for params $input",
            expected,
            actual,
        )
    }

    companion object {
        private val record: Record = Record(
            id = 0L,
            typeId = 0L,
            timeStarted = 0L,
            timeEnded = 0L,
            comment = "",
            tags = emptyList(),
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            // Empty.
            arrayOf(
                listOf(0L) to emptyList<Record>(),
                emptyMap<Long, List<CalculationResult>>(),
            ),
            arrayOf(
                listOf(0L) to listOf(record),
                emptyMap<Long, List<CalculationResult>>(),
            ),
            arrayOf(
                listOf(0L) to listOf(
                    record.copy(typeId = 1),
                    record.copy(typeId = 2),
                ),
                emptyMap<Long, List<CalculationResult>>(),
            ),
            // Multitasked.
            arrayOf(
                listOf(0L) to listOf(
                    record.copy(typeId = 0, timeStarted = 1, timeEnded = 10),
                    record.copy(typeId = 1, timeStarted = 2, timeEnded = 3),
                    record.copy(typeId = 2, timeStarted = 4, timeEnded = 15),
                ),
                emptyMap<Long, List<CalculationResult>>(),
            ),
            // Only before.
            arrayOf(
                listOf(0L) to listOf(
                    record.copy(typeId = 1, timeStarted = 1, timeEnded = 2),
                    record.copy(typeId = 2, timeStarted = 2, timeEnded = 3),
                    record.copy(typeId = 0, timeStarted = 3, timeEnded = 4),
                ),
                emptyMap<Long, List<CalculationResult>>(),
            ),
            // One after.
            arrayOf(
                listOf(0L) to listOf(
                    record.copy(typeId = 0, timeStarted = 0, timeEnded = 1),
                    record.copy(typeId = 1, timeStarted = 2, timeEnded = 3),
                ),
                mapOf(
                    0L to listOf(
                        CalculationResult(1, 1),
                    ),
                ),
            ),
            // Several.
            arrayOf(
                listOf(0L) to listOf(
                    record.copy(typeId = 1, timeStarted = 0, timeEnded = 1),
                    record.copy(typeId = 0, timeStarted = 1, timeEnded = 2),
                    record.copy(typeId = 1, timeStarted = 2, timeEnded = 3),
                    record.copy(typeId = 2, timeStarted = 3, timeEnded = 4),
                    record.copy(typeId = 0, timeStarted = 4, timeEnded = 5),
                    record.copy(typeId = 2, timeStarted = 5, timeEnded = 6),
                    record.copy(typeId = 0, timeStarted = 6, timeEnded = 7),
                    record.copy(typeId = 1, timeStarted = 10, timeEnded = 11),
                ),
                mapOf(
                    0L to listOf(
                        CalculationResult(1, 2),
                        CalculationResult(2, 1),
                    ),
                ),
            ),
            // Several typeIds
            arrayOf(
                listOf(0L, 1L, 2L, 3L) to listOf(
                    record.copy(typeId = 1, timeStarted = 0, timeEnded = 1),
                    record.copy(typeId = 0, timeStarted = 1, timeEnded = 2),
                    record.copy(typeId = 1, timeStarted = 2, timeEnded = 3),
                    record.copy(typeId = 2, timeStarted = 3, timeEnded = 4),
                    record.copy(typeId = 0, timeStarted = 4, timeEnded = 5),
                    record.copy(typeId = 2, timeStarted = 5, timeEnded = 6),
                    record.copy(typeId = 0, timeStarted = 6, timeEnded = 7),
                    record.copy(typeId = 1, timeStarted = 10, timeEnded = 11),
                ),
                mapOf(
                    0L to listOf(
                        CalculationResult(1, 2),
                        CalculationResult(2, 1),
                    ),
                    1L to listOf(
                        CalculationResult(0, 1),
                        CalculationResult(2, 1),
                    ),
                    2L to listOf(
                        CalculationResult(0, 2),
                    ),
                ),
            ),
        )
    }
}