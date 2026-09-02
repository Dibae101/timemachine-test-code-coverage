package com.example.util.simpletimetracker.data_local.recordTag

import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordShortcutToRecordTag
import javax.inject.Inject

class RecordShortcutToRecordTagDataLocalMapper @Inject constructor() {

    fun map(dbo: RecordShortcutToRecordTagDBO): RecordShortcutToRecordTag {
        return RecordShortcutToRecordTag(
            shortcutId = dbo.shortcutId,
            recordTagId = dbo.recordTagId,
            recordTagNumericValue = dbo.recordTagNumericValue,
        )
    }

    fun map(shortcutId: Long, recordTag: RecordBase.Tag): RecordShortcutToRecordTagDBO {
        return RecordShortcutToRecordTagDBO(
            shortcutId = shortcutId,
            recordTagId = recordTag.tagId,
            recordTagNumericValue = recordTag.numericValue,
        )
    }

    fun map(domain: RecordShortcutToRecordTag): RecordShortcutToRecordTagDBO {
        return RecordShortcutToRecordTagDBO(
            shortcutId = domain.shortcutId,
            recordTagId = domain.recordTagId,
            recordTagNumericValue = domain.recordTagNumericValue,
        )
    }
}