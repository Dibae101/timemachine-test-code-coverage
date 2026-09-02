package com.example.util.simpletimetracker.feature_notification.core

import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.example.util.simpletimetracker.core.mapper.RecordTagFullNameMapper
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import javax.inject.Inject

class NotificationCommonMapper @Inject constructor(
    private val recordTagFullNameMapper: RecordTagFullNameMapper,
) {

    fun getNotificationText(
        recordType: RecordType,
        recordTags: List<RecordTag>,
        recordTagsData: List<RecordBase.Tag>,
    ): CharSequence {
        val tag = recordTagFullNameMapper.getFullName(
            tags = recordTags,
            tagData = recordTagsData,
        )

        return buildSpannedString {
            bold { append(recordType.name) }
            if (tag.isNotEmpty()) {
                append(" - ")
                append(tag)
            }
        }
    }
}