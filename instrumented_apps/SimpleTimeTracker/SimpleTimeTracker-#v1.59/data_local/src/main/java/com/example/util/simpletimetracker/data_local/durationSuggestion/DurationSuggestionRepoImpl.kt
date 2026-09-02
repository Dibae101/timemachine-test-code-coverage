package com.example.util.simpletimetracker.data_local.durationSuggestion

import com.example.util.simpletimetracker.data_local.base.logDataAccess
import com.example.util.simpletimetracker.domain.durationSuggestion.model.DurationSuggestion
import com.example.util.simpletimetracker.domain.durationSuggestion.repo.DurationSuggestionRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DurationSuggestionRepoImpl @Inject constructor(
    private val dao: DurationSuggestionDao,
    private val mapper: DurationSuggestionDataLocalMapper,
) : DurationSuggestionRepo {

    override suspend fun getAll(): List<DurationSuggestion> = withContext(Dispatchers.IO) {
        logDataAccess("getAll")
        dao.getAll().map(mapper::map)
    }

    override suspend fun add(data: DurationSuggestion): Long = withContext(Dispatchers.IO) {
        logDataAccess("add")
        return@withContext dao.insert(
            data.let(mapper::map),
        )
    }

    override suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        logDataAccess("remove")
        dao.delete(id)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        logDataAccess("clear")
        dao.clear()
    }
}