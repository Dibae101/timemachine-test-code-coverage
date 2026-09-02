package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import javax.inject.Inject

class RecordTagFullNameMapper @Inject constructor(
    private val recordTagValueMapper: RecordTagValueMapper,
) {

    fun getFullName(
        tags: List<RecordTag>,
        tagData: List<RecordBase.Tag>,
    ): String {
        val tagDataMap = tagData.associateBy { it.tagId }
        return tags.joinToString(
            separator = ", ",
            transform = { tag ->
                tagDataMap[tag.id]?.numericValue?.let { value ->
                    recordTagValueMapper.getNameWithValue(
                        name = tag.name,
                        value = value,
                        valueSuffix = tag.valueSuffix,
                    )
                } ?: tag.name
            },
        )
    }
}