package com.example.util.simpletimetracker.domain.activityFilter.interactor

import com.example.util.simpletimetracker.domain.activityFilter.repo.ActivityFilterRepo
import com.example.util.simpletimetracker.domain.activityFilter.model.ActivityFilter
import java.util.Locale
import javax.inject.Inject

class ActivityFilterInteractor @Inject constructor(
    private val activityFilterRepo: ActivityFilterRepo,
) {

    suspend fun getAll(): List<ActivityFilter> {
        return activityFilterRepo.getAll().let(::sort)
    }

    suspend fun get(id: Long): ActivityFilter? {
        return activityFilterRepo.get(id)
    }

    suspend fun getByTypeId(id: Long): List<ActivityFilter> {
        return activityFilterRepo.getByTypeId(id)
    }

    suspend fun add(record: ActivityFilter) {
        activityFilterRepo.add(record)
    }

    suspend fun changeSelected(id: Long, selected: Boolean) {
        activityFilterRepo.changeSelected(id, selected)
    }

    suspend fun disableAll() {
        activityFilterRepo.changeSelectedAll(selected = false)
    }

    suspend fun remove(id: Long) {
        activityFilterRepo.remove(id)
    }

    suspend fun removeTypeId(id: Long) {
        getByTypeId(id).forEach { filter ->
            val newFilter = filter.copy(
                selectedIds = filter.selectedIds
                    .toMutableSet()
                    .apply { removeAll { it == id } },
            )
            add(newFilter)
        }
    }

    fun sort(
        data: List<ActivityFilter>,
    ): List<ActivityFilter> {
        return data.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
}