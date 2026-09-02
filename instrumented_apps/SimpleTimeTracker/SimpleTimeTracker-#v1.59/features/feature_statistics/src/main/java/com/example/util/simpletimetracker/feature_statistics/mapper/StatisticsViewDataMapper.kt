package com.example.util.simpletimetracker.feature_statistics.mapper

import android.text.SpannableStringBuilder
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.empty.EmptyViewData
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_base_adapter.hintBig.HintBigViewData
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsViewData
import com.example.util.simpletimetracker.feature_statistics.R
import com.example.util.simpletimetracker.feature_statistics.adapter.StatisticsInfoViewData
import com.example.util.simpletimetracker.feature_views.TransitionNames
import com.example.util.simpletimetracker.feature_views.extension.setForegroundSpan
import com.example.util.simpletimetracker.feature_views.extension.toSpannableString
import javax.inject.Inject

class StatisticsViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
) {

    fun mapStatisticsTotalTracked(
        shift: Int,
        totalTracked: String,
        isDarkTheme: Boolean,
    ): ViewHolderType {
        val id = "statistics_total_tracked".hashCode().toLong()
        val transitionName = "${TransitionNames.STATISTICS_DETAIL}_shift${shift}_id$id"
        val data = StatisticsViewData(
            id = id,
            name = resourceRepo.getString(R.string.statistics_total_tracked),
            duration = totalTracked,
            percent = "",
            color = resourceRepo.getThemedAttr(R.attr.appCardBackgroundColor, isDarkTheme),
            icon = null,
            transitionName = transitionName,
        )
        return StatisticsInfoViewData(data)
    }

    fun mapToEmpty(): ViewHolderType {
        return EmptyViewData(
            message = R.string.no_data.let(resourceRepo::getString),
        )
    }

    fun mapToNoStatistics(): ViewHolderType {
        val emptyHint = resourceRepo.getString(R.string.no_statistics_exist)
        val dateHint = resourceRepo.getString(R.string.record_date_hint)
            .toSpannableString()
            .apply {
                setForegroundSpan(color = resourceRepo.getColor(R.color.textHintCommon))
            }

        return HintBigViewData(
            text = SpannableStringBuilder()
                .append(emptyHint)
                .append("\n")
                .append(dateHint),
            infoIconVisible = true,
            closeIconVisible = false,
        )
    }

    fun mapToHint(): ViewHolderType {
        return HintViewData(
            text = R.string.statistics_hint.let(resourceRepo::getString),
        )
    }

    fun mapToGoalHint(): ViewHolderType {
        return HintViewData(
            text = R.string.change_record_type_goal_time_hint.let(resourceRepo::getString),
        )
    }
}