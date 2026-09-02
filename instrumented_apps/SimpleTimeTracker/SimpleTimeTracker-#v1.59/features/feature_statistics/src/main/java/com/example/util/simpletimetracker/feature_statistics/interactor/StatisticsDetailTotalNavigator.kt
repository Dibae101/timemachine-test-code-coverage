package com.example.util.simpletimetracker.feature_statistics.interactor

import com.example.util.simpletimetracker.core.extension.toParams
import com.example.util.simpletimetracker.core.interactor.GetStatisticsDetailRangeInteractor
import com.example.util.simpletimetracker.core.interactor.GetTotalStatisticsFilterInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.model.RecordsFilter
import com.example.util.simpletimetracker.feature_base_adapter.statistics.StatisticsViewData
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.StatisticsDetailParams
import javax.inject.Inject

class StatisticsDetailTotalNavigator @Inject constructor(
    private val router: Router,
    private val prefsInteractor: PrefsInteractor,
    private val getStatisticsDetailRangeInteractor: GetStatisticsDetailRangeInteractor,
    private val getTotalStatisticsFilterInteractor: GetTotalStatisticsFilterInteractor,
) {

    suspend fun execute(
        shift: Int,
        item: StatisticsViewData,
        sharedElements: Map<Any, String>,
    ) {
        val filter = getTotalStatisticsFilterInteractor.execute(
            filterType = prefsInteractor.getChartFilterType(),
        )

        router.navigate(
            data = StatisticsDetailParams(
                transitionName = item.transitionName.orEmpty(),
                filter = filter.let(::listOf).map(RecordsFilter::toParams),
                range = getStatisticsDetailRangeInteractor.execute(),
                shift = shift,
                preview = StatisticsDetailParams.Preview(
                    name = item.name,
                    iconId = item.icon?.toParams(),
                    color = item.color,
                ),
            ),
            sharedElements = sharedElements,
        )
    }
}