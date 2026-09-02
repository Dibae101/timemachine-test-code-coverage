package com.example.util.simpletimetracker.data_local.recordTag

import com.example.util.simpletimetracker.data_local.base.logDataAccess
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.repo.RunningRecordToRecordTagRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunningRecordToRecordTagRepoImpl @Inject constructor(
    private val dao: RunningRecordToRecordTagDao,
    private val mapper: RunningRecordToRecordTagDataLocalMapper,
) : RunningRecordToRecordTagRepo {

    override suspend fun addRunningRecordTags(runningRecordId: Long, tags: List<RecordBase.Tag>) =
        withContext(Dispatchers.IO) {
            logDataAccess("add running record tags")
            tags.map {
                mapper.map(recordId = runningRecordId, recordTag = it)
            }.let {
                dao.insert(it)
            }
        }

    override suspend fun removeAllByTagId(tagId: Long) =
        withContext(Dispatchers.IO) {
            logDataAccess("remove all by tagId")
            dao.deleteAllByTagId(tagId)
        }

    override suspend fun removeAllByRunningRecordId(runningRecordId: Long) =
        withContext(Dispatchers.IO) {
            logDataAccess("remove all by runningRecordId")
            dao.deleteAllByRecordId(runningRecordId)
        }

    override suspend fun clear() =
        withContext(Dispatchers.IO) {
            logDataAccess("clear")
            dao.clear()
        }
}