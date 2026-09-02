package com.example.util.simpletimetracker.domain.mapper

import com.example.util.simpletimetracker.domain.recordTag.interactor.FilterSelectableTagsInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTypeToTag
import org.junit.Assert
import org.junit.Test

class FilterSelectableSelectedTagsInteractorTest {

    private val subject = FilterSelectableTagsInteractor()

    @Test
    fun onlyGeneral_some() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
            ),
            typeIds = emptyList(),
        )

        // Then
        Assert.assertArrayEquals(listOf(1L, 3L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun onlyGeneral_none() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = emptyList(),
        )

        // Then
        Assert.assertArrayEquals(emptyList<Long>().toTypedArray(), result.toTypedArray())
    }

    @Test
    fun onlyTyped_some() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = listOf(11),
        )

        // Then
        Assert.assertArrayEquals(listOf(2L, 3L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun onlyTyped_some_severalTypes() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = listOf(11, 22),
        )

        // Then
        Assert.assertArrayEquals(listOf(1L, 2L, 3L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun onlyTyped_none() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = listOf(33),
        )

        // Then
        Assert.assertArrayEquals(emptyList<Long>().toTypedArray(), result.toTypedArray())
    }

    @Test
    fun all1() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3, 4),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = listOf(33),
        )

        // Then
        Assert.assertArrayEquals(listOf(4L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun all2() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3, 4),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = listOf(11),
        )

        // Then
        Assert.assertArrayEquals(listOf(2L, 3L, 4L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun byAllTypeIds1() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3, 4),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = emptyList(),
            byAllTypeIds = listOf(11),
        )

        // Then
        Assert.assertArrayEquals(listOf(2L, 3L, 4L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun byAllTypeIds2() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3, 4),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
            ),
            typeIds = emptyList(),
            byAllTypeIds = listOf(11, 22),
        )

        // Then
        Assert.assertArrayEquals(listOf(4L).toTypedArray(), result.toTypedArray())
    }

    @Test
    fun byAllTypeIds3() {
        // When
        val result = subject.execute(
            tagIds = listOf(1, 2, 3, 4),
            typesToTags = listOf(
                RecordTypeToTag(recordTypeId = 11, tagId = 2),
                RecordTypeToTag(recordTypeId = 11, tagId = 3),
                RecordTypeToTag(recordTypeId = 22, tagId = 1),
                RecordTypeToTag(recordTypeId = 22, tagId = 2),
            ),
            typeIds = emptyList(),
            byAllTypeIds = listOf(11, 22),
        )

        // Then
        Assert.assertArrayEquals(listOf(2L, 4L).toTypedArray(), result.toTypedArray())
    }
}