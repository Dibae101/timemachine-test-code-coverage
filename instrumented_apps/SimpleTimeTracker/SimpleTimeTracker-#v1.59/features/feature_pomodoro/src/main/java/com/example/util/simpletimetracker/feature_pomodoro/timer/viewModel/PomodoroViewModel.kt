package com.example.util.simpletimetracker.feature_pomodoro.timer.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.domain.pomodoro.interactor.GetPomodoroSettingsInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.GetPomodoroStateInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroNextCycleInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroPauseInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroRestartCycleInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroStartInteractor
import com.example.util.simpletimetracker.domain.pomodoro.interactor.PomodoroStopInteractor
import com.example.util.simpletimetracker.feature_pomodoro.timer.mapper.PomodoroViewDataMapper
import com.example.util.simpletimetracker.feature_pomodoro.timer.model.PomodoroButtonState
import com.example.util.simpletimetracker.feature_pomodoro.timer.model.PomodoroTimerState
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.PomodoroSettingsParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val router: Router,
    private val pomodoroViewDataMapper: PomodoroViewDataMapper,
    private val prefsInteractor: PrefsInteractor,
    private val pomodoroStartInteractor: PomodoroStartInteractor,
    private val pomodoroStopInteractor: PomodoroStopInteractor,
    private val pomodoroNextCycleInteractor: PomodoroNextCycleInteractor,
    private val pomodoroRestartCycleInteractor: PomodoroRestartCycleInteractor,
    private val pomodoroPauseInteractor: PomodoroPauseInteractor,
    private val getPomodoroStateInteractor: GetPomodoroStateInteractor,
    private val getPomodoroSettingsInteractor: GetPomodoroSettingsInteractor,
) : BaseViewModel() {

    val buttonState: LiveData<PomodoroButtonState> = MutableLiveData()
    val timerState: LiveData<PomodoroTimerState> = MutableLiveData()

    private var timerJob: Job? = null

    fun onVisible() {
        startUpdate()
    }

    fun onHidden() {
        stopUpdate()
    }

    fun onSettingsClicked() {
        router.navigate(PomodoroSettingsParams)
    }

    fun onStartStopClicked() = viewModelScope.launch {
        val state = getPomodoroStateInteractor.execute()
        when (state) {
            is GetPomodoroStateInteractor.State.Running -> pomodoroStopInteractor.stop()
            is GetPomodoroStateInteractor.State.Paused -> pomodoroPauseInteractor.startAfterPause()
            is GetPomodoroStateInteractor.State.Stopped -> pomodoroStartInteractor.start()
        }
        updateButtonState()
        updateTimerState()
    }

    fun onRestartClicked() = viewModelScope.launch {
        pomodoroRestartCycleInteractor.execute()
        resetAnimation()
        updateTimerState()
    }

    fun onPauseClicked() = viewModelScope.launch {
        pomodoroPauseInteractor.pause()
        updateTimerState()
    }

    fun onPrevClicked() = viewModelScope.launch {
        pomodoroNextCycleInteractor.executePrev()
        resetAnimation()
        updateTimerState()
    }

    fun onNextClicked() = viewModelScope.launch {
        pomodoroNextCycleInteractor.executeNext()
        resetAnimation()
        updateTimerState()
    }

    private fun resetAnimation() {
        timerState.value?.copy(progress = 0)?.let(timerState::set)
    }

    private fun startUpdate() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                updateButtonState()
                updateTimerState()
                delay(TIMER_UPDATE_MS)
            }
        }
    }

    private fun stopUpdate() {
        timerJob?.cancel()
    }

    private suspend fun updateButtonState() {
        val data = loadButtonState()
        buttonState.set(data)
    }

    private suspend fun loadButtonState(): PomodoroButtonState {
        return pomodoroViewDataMapper.mapButtonState(
            showMoreControls = prefsInteractor.getPomodoroShowMoreControls(),
            state = getPomodoroStateInteractor.execute(),
        )
    }

    private suspend fun updateTimerState() {
        val data = loadTimerState()
        timerState.set(data)
    }

    private suspend fun loadTimerState(): PomodoroTimerState {
        return pomodoroViewDataMapper.mapTimerState(
            state = getPomodoroStateInteractor.execute(),
            timeStartedMs = prefsInteractor.getPomodoroModeStartedTimestampMs(),
            timePausedMs = prefsInteractor.getPomodoroModePausedTimestampMs(),
            timerUpdateMs = TIMER_UPDATE_MS,
            settings = getPomodoroSettingsInteractor.execute(),
        )
    }

    companion object {
        private const val TIMER_UPDATE_MS = 250L
    }
}
