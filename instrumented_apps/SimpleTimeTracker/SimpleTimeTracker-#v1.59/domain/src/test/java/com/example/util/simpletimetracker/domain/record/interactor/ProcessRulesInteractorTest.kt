package com.example.util.simpletimetracker.domain.record.interactor

import com.example.util.simpletimetracker.domain.base.ResultContainer
import com.example.util.simpletimetracker.domain.base.suspendLazy
import com.example.util.simpletimetracker.domain.complexRule.interactor.ComplexRuleProcessActionInteractor
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTypeToDefaultTagInteractor
import com.example.util.simpletimetracker.domain.utils.recordTag
import com.example.util.simpletimetracker.domain.utils.tag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class ProcessRulesInteractorTest {

    private val recordTagInteractor: RecordTagInteractor = Mockito.mock()
    private val runningRecordInteractor: RunningRecordInteractor = mock()
    private val complexRuleProcessActionInteractor: ComplexRuleProcessActionInteractor = mock()
    private val recordTypeToDefaultTagInteractor: RecordTypeToDefaultTagInteractor = mock()

    private val subject = ProcessRulesInteractor(
        recordTagInteractor = recordTagInteractor,
        runningRecordInteractor = runningRecordInteractor,
        complexRuleProcessActionInteractor = complexRuleProcessActionInteractor,
        recordTypeToDefaultTagInteractor = recordTypeToDefaultTagInteractor,
    )

    private fun recordWithType(typeId: Long): Record {
        return Record(
            id = typeId,
            typeId = typeId,
            timeStarted = 1L,
            timeEnded = 2L,
            comment = "",
            tags = emptyList(),
        )
    }

    @Test
    fun getRulesResultForStart_retroactiveMergeSkipsProcessing(): Unit = runBlocking {
        // Given
        val typeId = 11L
        val prevRecords = suspendLazy { listOf(recordWithType(typeId)) }

        val defaultResult = ComplexRuleProcessActionInteractor.Result(
            isMultitaskingAllowed = ResultContainer.Undefined,
            disallowOnlyPreviousTypeIds = emptySet(),
            tags = emptyList(),
            tagIdsToSelectValueOnStart = emptySet(),
        )

        // When
        val result = subject.getRulesResultForStart(
            typeId = typeId,
            timeStarted = 10L,
            prevRecords = prevRecords,
            retroactiveTrackingMode = true,
        )

        // Then
        assertEquals(defaultResult, result)
        verify(complexRuleProcessActionInteractor, never()).processRules(any(), any(), any())
    }

    @Test
    fun getRulesResultForStart_executesRulesWhenNeeded(): Unit = runBlocking {
        // Given
        val typeId = 11L
        val timeStarted = 10L
        val prevRecord = recordWithType(typeId = typeId + 1)
        val prevRecords = suspendLazy { listOf(prevRecord) }

        val expectedResult = ComplexRuleProcessActionInteractor.Result(
            isMultitaskingAllowed = ResultContainer.Defined(true),
            disallowOnlyPreviousTypeIds = setOf(2L, 3L),
            tags = listOf(tag(5L, 1.0)),
            tagIdsToSelectValueOnStart = setOf(6L),
        )

        whenever(complexRuleProcessActionInteractor.hasRules()).thenReturn(true)
        whenever(runningRecordInteractor.getAll()).thenReturn(emptyList())
        whenever(
            complexRuleProcessActionInteractor.processRules(
                timeStarted = timeStarted,
                startingTypeId = typeId,
                currentTypeIds = setOf(prevRecord.typeId),
            ),
        ).thenReturn(expectedResult)

        // When
        val result = subject.getRulesResultForStart(
            typeId = typeId,
            timeStarted = timeStarted,
            prevRecords = prevRecords,
            retroactiveTrackingMode = false,
        )

        // Then
        assertSame(expectedResult, result)
        verify(complexRuleProcessActionInteractor).processRules(
            timeStarted = timeStarted,
            startingTypeId = typeId,
            currentTypeIds = setOf(prevRecord.typeId),
        )
    }

    @Test
    fun getAllTags_mergesTagsWithPriority(): Unit = runBlocking {
        // Given
        val typeId = 99L
        val currentTags = listOf(tag(1L, 10.0))
        val tagValuesFromRules = listOf(tag(2L, 5.0), tag(4L, 7.0))

        whenever(recordTagInteractor.getAll()).thenReturn(
            listOf(recordTag(1L), recordTag(2L), recordTag(3L), recordTag(4L)),
        )
        whenever(recordTypeToDefaultTagInteractor.getTags(typeId)).thenReturn(
            setOf(1L, 2L, 3L),
        )

        // When
        val result = subject.getAllTags(typeId, currentTags, tagValuesFromRules)

        // Then
        assertEquals(
            listOf(
                tag(1L, 10.0),
                tag(2L, 5.0),
                tag(3L, null),
                tag(4L, 7.0),
            ),
            result,
        )
    }
}
