package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.domain.notifications.interactor.UpdateExternalViewsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.feature_settings.api.OnSettingChangedInteractor
import javax.inject.Inject

class OnSettingChangedInteractorImpl @Inject constructor(
    private val runningRecordInteractor: RunningRecordInteractor,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
    private val externalViewsInteractor: UpdateExternalViewsInteractor,
) : OnSettingChangedInteractor {

    override suspend fun onAllowMultitaskingChange() {
        externalViewsInteractor.onAllowMultitaskingChange()
    }

    override suspend fun onRetroactiveTrackingModeChange() {
        runningRecordInteractor.getAll().forEach {
            removeRunningRecordMediator.removeWithRecordAdd(it)
        }
        // TODO do not update widgets if there was running records?
        externalViewsInteractor.onRetroactiveTrackingModeChange()
    }
}