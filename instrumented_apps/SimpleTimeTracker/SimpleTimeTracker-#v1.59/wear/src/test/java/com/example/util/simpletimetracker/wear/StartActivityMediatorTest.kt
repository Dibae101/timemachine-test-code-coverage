/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.example.util.simpletimetracker.wear

import com.example.util.simpletimetracker.data.WearDataRepo
import com.example.util.simpletimetracker.domain.interactor.WearTagSelectionDataInteractor
import com.example.util.simpletimetracker.domain.mediator.StartActivityMediator
import com.example.util.simpletimetracker.domain.model.WearActivity
import com.example.util.simpletimetracker.domain.model.WearShouldShowTagSelectionResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

class StartActivityMediatorTest {
    private val wearDataRepo: WearDataRepo = mock()
    private val wearTagSelectionDataInteractor: WearTagSelectionDataInteractor = mock()

    private val mediator = StartActivityMediator(
        wearDataRepo = wearDataRepo,
        wearTagSelectionDataInteractor = wearTagSelectionDataInteractor,
    )

    private val sampleActivity = WearActivity(
        id = 1,
        name = "Sleep",
        icon = "🛏️",
        color = 0xFF123456,
    )

    @Before
    fun setup() {
        Mockito.reset(wearDataRepo)
    }

    @Test
    fun `tag selection disabled`() = runTest {
        // Given
        val result = WearShouldShowTagSelectionResult(
            shouldShow = false,
            preselectedTags = emptyList(),
            requiredTagValueSelectionTagIds = emptyList(),
        )
        Mockito.`when`(wearDataRepo.loadShouldShowTagSelection(sampleActivity.id))
            .thenReturn(Result.success(result))
        var onRequestTagSelectionCalled = false
        var onProgressChanged = false

        // When
        mediator.requestStart(
            activityId = sampleActivity.id,
            onRequestTagSelection = { onRequestTagSelectionCalled = true },
            onProgressChanged = { onProgressChanged = it },
        )

        // Then
        Mockito.verify(wearDataRepo).startActivity(
            id = sampleActivity.id,
            tags = emptyList(),
            useSelectedTags = false,
        )
        assertEquals(false, onRequestTagSelectionCalled)
        assertEquals(true, onProgressChanged)
    }

    @Test
    fun `tag selection enabled`() = runTest {
        // Given
        val result = WearShouldShowTagSelectionResult(
            shouldShow = true,
            preselectedTags = emptyList(),
            requiredTagValueSelectionTagIds = emptyList(),
        )
        Mockito.`when`(wearDataRepo.loadShouldShowTagSelection(sampleActivity.id))
            .thenReturn(Result.success(result))
        var onRequestTagSelectionCalled = false
        var onProgressChanged = false

        // When
        mediator.requestStart(
            activityId = sampleActivity.id,
            onRequestTagSelection = { onRequestTagSelectionCalled = true },
            onProgressChanged = { onProgressChanged = it },
        )

        // Then
        Mockito.verify(wearDataRepo, Mockito.never()).startActivity(
            id = sampleActivity.id,
            tags = emptyList(),
            useSelectedTags = false,
        )
        assertEquals(true, onRequestTagSelectionCalled)
        assertEquals(false, onProgressChanged)
    }
}