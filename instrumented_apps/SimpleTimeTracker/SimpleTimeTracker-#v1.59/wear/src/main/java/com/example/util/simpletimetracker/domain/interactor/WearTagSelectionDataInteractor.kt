package com.example.util.simpletimetracker.domain.interactor

import com.example.util.simpletimetracker.domain.model.WearShouldShowTagSelectionResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearTagSelectionDataInteractor @Inject constructor() {

    val data: MutableMap<Long, WearShouldShowTagSelectionResult> = mutableMapOf()
}