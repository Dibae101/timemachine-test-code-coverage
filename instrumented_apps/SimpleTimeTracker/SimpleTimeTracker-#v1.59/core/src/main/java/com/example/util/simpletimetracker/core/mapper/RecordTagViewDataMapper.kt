package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import javax.inject.Inject

class RecordTagViewDataMapper @Inject constructor() {

    fun mapIcon(
        tag: RecordTag,
        types: Map<Long, RecordType>,
    ): String? {
        return mapIcon(
            tag = tag,
            type = types[tag.iconColorSource],
        )
    }

    fun mapIcon(
        tag: RecordTag,
        type: RecordType?,
    ): String? {
        return type?.icon ?: tag.icon.takeIf { it.isNotEmpty() }
    }

    fun mapColor(
        tag: RecordTag,
        types: Map<Long, RecordType>,
    ): AppColor {
        return mapColor(
            tag = tag,
            type = types[tag.iconColorSource],
        )
    }

    fun mapColor(
        tag: RecordTag,
        type: RecordType?,
    ): AppColor {
        return type?.color ?: tag.color
    }
}