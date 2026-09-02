package com.example.util.simpletimetracker.domain.recordType.extension

import com.example.util.simpletimetracker.domain.recordType.model.RecordTypeGoal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsExtensionsTest {

    @Test
    fun goalReachedAtExactValue() {
        val actual = RecordTypeGoal.Subtype.Goal.isReached(current = 4, goalValue = 4)
        assertTrue(actual)
    }

    @Test
    fun goalNotReachedBelowValue() {
        val actual = RecordTypeGoal.Subtype.Goal.isReached(current = 3, goalValue = 4)
        assertFalse(actual)
    }

    @Test
    fun limitNotReachedBelowValue() {
        val actual = RecordTypeGoal.Subtype.Limit.isReached(current = 3, goalValue = 4)
        assertFalse(actual)
    }

    @Test
    fun limitNotReachedAtExactValue() {
        val actual = RecordTypeGoal.Subtype.Limit.isReached(current = 4, goalValue = 4)
        assertFalse(actual)
    }

    @Test
    fun limitReachedAboveValue() {
        val actual = RecordTypeGoal.Subtype.Limit.isReached(current = 5, goalValue = 4)
        assertTrue(actual)
    }

    @Test
    fun goalSuccessfulAtExactValue() {
        val actual = RecordTypeGoal.Subtype.Goal.isSuccessful(current = 4, goalValue = 4)
        assertTrue(actual)
    }

    @Test
    fun goalNotSuccessfulBelowValue() {
        val actual = RecordTypeGoal.Subtype.Goal.isSuccessful(current = 3, goalValue = 4)
        assertFalse(actual)
    }

    @Test
    fun limitSuccessfulBelowValue() {
        val actual = RecordTypeGoal.Subtype.Limit.isSuccessful(current = 3, goalValue = 4)
        assertTrue(actual)
    }

    @Test
    fun limitSuccessfulAtExactValue() {
        val actual = RecordTypeGoal.Subtype.Limit.isSuccessful(current = 4, goalValue = 4)
        assertTrue(actual)
    }

    @Test
    fun limitNotSuccessfulAboveValue() {
        val actual = RecordTypeGoal.Subtype.Limit.isSuccessful(current = 5, goalValue = 4)
        assertFalse(actual)
    }
}
