package com.example.util.simpletimetracker.domain.complexRule.interactor

import com.example.util.simpletimetracker.domain.base.ResultContainer
import com.example.util.simpletimetracker.domain.complexRule.model.ComplexRule
import com.example.util.simpletimetracker.domain.daysOfWeek.interactor.GetCurrentDayInteractor
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.utils.tag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class ComplexRuleProcessActionInteractorTest {

    private val complexRuleInteractor: ComplexRuleInteractor = mock()
    private val getCurrentDayInteractor: GetCurrentDayInteractor = mock()

    private val subject = ComplexRuleProcessActionInteractor(
        complexRuleInteractor = complexRuleInteractor,
        getCurrentDayInteractor = getCurrentDayInteractor,
    )

    private val startingTypeId = 1L
    private val currentTypeIds = setOf(2L)
    private val currentDay = DayOfWeek.MONDAY

    @Before
    fun before(): Unit = runBlocking {
        `when`(getCurrentDayInteractor.execute(any())).thenReturn(currentDay)
    }

    @Test
    fun mergesSelectOnStart(): Unit = runBlocking {
        // Given
        val rule1 = assignTagRule(
            actionAssignTagValues = listOf(tag(10L, null)),
            actionAssignTagValueOnStartIds = setOf(10L),
        )
        val rule2 = assignTagRule(
            actionAssignTagValues = listOf(tag(20L, null)),
            actionAssignTagValueOnStartIds = setOf(20L),
        )
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(rule1, rule2))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(setOf(10L, 20L), result.tagIdsToSelectValueOnStart)
    }

    @Test
    fun dropsSelectOnStartWhenNumericExists(): Unit = runBlocking {
        // Given
        val rule = assignTagRule(
            actionAssignTagValues = listOf(tag(30L, 3.5)),
            actionAssignTagValueOnStartIds = setOf(30L),
        )
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(rule))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(emptySet<Long>(), result.tagIdsToSelectValueOnStart)
    }

    @Test
    fun ignoresSelectOnStartWithoutMergedTag(): Unit = runBlocking {
        // Given
        val rule = assignTagRule(
            actionAssignTagValues = listOf(tag(40L, null)),
            actionAssignTagValueOnStartIds = setOf(40L, 99L),
        )
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(rule))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(setOf(40L), result.tagIdsToSelectValueOnStart)
    }

    @Test
    fun mergesAssignedTags(): Unit = runBlocking {
        // Given
        val rule1 = assignTagRule(
            actionAssignTagValues = listOf(tag(50L, null)),
            actionAssignTagValueOnStartIds = emptySet(),
        )
        val rule2 = assignTagRule(
            actionAssignTagValues = listOf(tag(60L, null)),
            actionAssignTagValueOnStartIds = emptySet(),
        )
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(rule1, rule2))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(
            listOf(tag(50L, null), tag(60L, null)),
            result.tags,
        )
    }

    @Test
    fun numericValuesOverrideEarlierNull(): Unit = runBlocking {
        // Given
        val ruleWithNull = assignTagRule(
            actionAssignTagValues = listOf(tag(70L, null)),
            actionAssignTagValueOnStartIds = emptySet(),
        )
        val ruleWithNumeric = assignTagRule(
            actionAssignTagValues = listOf(tag(70L, 9.0)),
            actionAssignTagValueOnStartIds = emptySet(),
        )
        val ruleWithLateNull = assignTagRule(
            actionAssignTagValues = listOf(tag(70L, null)),
            actionAssignTagValueOnStartIds = emptySet(),
        )
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(ruleWithNull, ruleWithNumeric, ruleWithLateNull))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(listOf(tag(70L, 9.0)), result.tags)
    }

    @Test
    fun allowsMultitaskingWhenAllowRuleExistsEvenWithDisallow(): Unit = runBlocking {
        // Given
        val disallowRule = multitaskingRule(ComplexRule.Action.DisallowMultitasking)
        val allowRule = multitaskingRule(ComplexRule.Action.AllowMultitasking)
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(disallowRule, allowRule))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(ResultContainer.Defined(true), result.isMultitaskingAllowed)
    }

    @Test
    fun disallowsMultitaskingWhenOnlyDisallowRulesRun(): Unit = runBlocking {
        // Given
        val rule = multitaskingRule(ComplexRule.Action.DisallowMultitasking)
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(rule))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(ResultContainer.Defined(false), result.isMultitaskingAllowed)
    }

    @Test
    fun leavesMultitaskingUndefinedWhenNoAllowOrDisallowRules(): Unit = runBlocking {
        // Given
        val rule = assignTagRule(
            actionAssignTagValues = listOf(tag(80L, null)),
            actionAssignTagValueOnStartIds = emptySet(),
        )
        `when`(complexRuleInteractor.getAll()).thenReturn(listOf(rule))

        // When
        val result = subject.processRules(
            timeStarted = 0L,
            startingTypeId = startingTypeId,
            currentTypeIds = currentTypeIds,
        )

        // Then
        assertEquals(ResultContainer.Undefined, result.isMultitaskingAllowed)
    }

    private fun assignTagRule(
        actionAssignTagValues: List<RecordBase.Tag>,
        actionAssignTagValueOnStartIds: Set<Long>,
    ): ComplexRule {
        return ComplexRule(
            id = 0L,
            disabled = false,
            action = ComplexRule.Action.AssignTag,
            actionDisallowOnlyPrevious = false,
            actionAssignTagValues = actionAssignTagValues,
            actionAssignTagValueOnStartIds = actionAssignTagValueOnStartIds,
            conditionStartingTypeIds = setOf(startingTypeId),
            conditionCurrentTypeIds = currentTypeIds,
            conditionDaysOfWeek = setOf(currentDay),
        )
    }

    private fun multitaskingRule(action: ComplexRule.Action): ComplexRule {
        return ComplexRule(
            id = 0L,
            disabled = false,
            action = action,
            actionDisallowOnlyPrevious = false,
            actionAssignTagValues = emptyList(),
            actionAssignTagValueOnStartIds = emptySet(),
            conditionStartingTypeIds = setOf(startingTypeId),
            conditionCurrentTypeIds = currentTypeIds,
            conditionDaysOfWeek = setOf(currentDay),
        )
    }
}
