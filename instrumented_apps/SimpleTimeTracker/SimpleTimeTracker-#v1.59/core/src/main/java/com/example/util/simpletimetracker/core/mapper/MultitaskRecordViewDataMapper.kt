package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.domain.base.DurationFormat
import com.example.util.simpletimetracker.domain.record.model.MultitaskRecord
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.multitaskRecord.MultitaskRecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.multitaskRecord.customView.MultitaskRecordView
import javax.inject.Inject

class MultitaskRecordViewDataMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    private val recordViewDataMapper: RecordViewDataMapper,
) {

    fun map(
        multitaskRecord: MultitaskRecord,
        recordTypes: Map<Long, RecordType>,
        recordTags: List<RecordTag>,
        isDarkTheme: Boolean,
        useMilitaryTime: Boolean,
        durationFormat: DurationFormat,
        showSeconds: Boolean,
    ): MultitaskRecordViewData {
        val ids = multitaskRecord.records.map(Record::id)
        val records = multitaskRecord.records.mapNotNull { record ->
            recordViewDataMapper.map(
                record = record,
                recordType = recordTypes[record.typeId] ?: return@mapNotNull null,
                recordTags = recordTags,
                isDarkTheme = isDarkTheme,
                useMilitaryTime = useMilitaryTime,
                durationFormat = durationFormat,
                showSeconds = showSeconds,
            )
        }

        return MultitaskRecordViewData(
            ids = ids,
            data = MultitaskRecordView.ViewData(
                timeStarted = records.firstOrNull()?.timeStarted.orEmpty(),
                timeFinished = records.firstOrNull()?.timeFinished.orEmpty(),
                duration = records.firstOrNull()?.duration.orEmpty(),
                items = records.map {
                    MultitaskRecordView.ItemViewData(
                        name = it.name,
                        tagName = it.tagName,
                        iconId = it.iconId,
                        color = it.color,
                        comment = it.comment,
                    )
                },
            ),
        )
    }

    fun mapFiltered(
        viewData: MultitaskRecordViewData,
        isDarkTheme: Boolean,
        isFiltered: Boolean,
    ): MultitaskRecordViewData {
        return when {
            isFiltered -> {
                val newColor = colorMapper.toFilteredColor(isDarkTheme)
                viewData.copy(
                    data = viewData.data.copy(
                        items = viewData.data.items.map {
                            it.copy(color = newColor)
                        },
                    ),
                )
            }
            else -> viewData
        }
    }
}