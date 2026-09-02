package com.example.util.simpletimetracker.data_local.recordTag

import com.example.util.simpletimetracker.data_local.base.logDataAccess
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordShortcutToRecordTag
import com.example.util.simpletimetracker.domain.recordTag.repo.RecordShortcutToRecordTagRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordShortcutToRecordTagRepoImpl @Inject constructor(
    private val dao: RecordShortcutToRecordTagDao,
    private val mapper: RecordShortcutToRecordTagDataLocalMapper,
) : RecordShortcutToRecordTagRepo {

    override suspend fun getAll(): List<RecordShortcutToRecordTag> =
        withContext(Dispatchers.IO) {
            logDataAccess("get all")
            dao.getAll().map(mapper::map)
        }

    override suspend fun add(recordShortcutToRecordTag: RecordShortcutToRecordTag) =
        withContext(Dispatchers.IO) {
            logDataAccess("add")
            recordShortcutToRecordTag
                .let(mapper::map)
                .let {
                    dao.insert(listOf(it))
                }
        }

    override suspend fun addRecordTags(shortcutId: Long, tags: List<RecordBase.Tag>) =
        withContext(Dispatchers.IO) {
            logDataAccess("add record tags")
            tags.map {
                mapper.map(shortcutId = shortcutId, recordTag = it)
            }.let {
                dao.insert(it)
            }
        }

    override suspend fun removeAllByTagId(tagId: Long) =
        withContext(Dispatchers.IO) {
            logDataAccess("remove all by tagId")
            dao.deleteAllByTagId(tagId)
        }

    override suspend fun removeAllByShortcutId(shortcutId: Long) =
        withContext(Dispatchers.IO) {
            logDataAccess("remove all by shortcutId")
            dao.deleteAllByShortcutId(shortcutId)
        }

    override suspend fun clear() =
        withContext(Dispatchers.IO) {
            logDataAccess("clear")
            dao.clear()
        }
}