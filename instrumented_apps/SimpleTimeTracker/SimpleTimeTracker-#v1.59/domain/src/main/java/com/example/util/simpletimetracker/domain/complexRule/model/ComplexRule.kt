package com.example.util.simpletimetracker.domain.complexRule.model

import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.record.model.RecordBase

data class ComplexRule(
    val id: Long = 0L,
    val disabled: Boolean,
    val action: Action,
    val actionDisallowOnlyPrevious: Boolean,
    val actionAssignTagValues: List<RecordBase.Tag>,
    val actionAssignTagValueOnStartIds: Set<Long>,
    val conditionStartingTypeIds: Set<Long>,
    val conditionCurrentTypeIds: Set<Long>,
    val conditionDaysOfWeek: Set<DayOfWeek>,
) {

    sealed interface Action {
        object AllowMultitasking : Action
        object DisallowMultitasking : Action
        object AssignTag : Action
    }

    sealed interface Condition {
        object StartingType : Condition
        object CurrentType : Condition
        object DaysOfWeek : Condition
    }

    val conditions: List<Condition>
        get() = listOfNotNull(
            Condition.StartingType.takeIf { conditionStartingTypeIds.isNotEmpty() },
            Condition.CurrentType.takeIf { conditionCurrentTypeIds.isNotEmpty() },
            Condition.DaysOfWeek.takeIf { conditionDaysOfWeek.isNotEmpty() },
        )

    val hasConditions: Boolean
        get() = conditions.isNotEmpty()

    val actionAssignTagIds: Set<Long>
        get() = actionAssignTagValues.map(RecordBase.Tag::tagId).toSet()

    val hasActions: Boolean
        get() = action !is Action.AssignTag || actionAssignTagValues.isNotEmpty()
}