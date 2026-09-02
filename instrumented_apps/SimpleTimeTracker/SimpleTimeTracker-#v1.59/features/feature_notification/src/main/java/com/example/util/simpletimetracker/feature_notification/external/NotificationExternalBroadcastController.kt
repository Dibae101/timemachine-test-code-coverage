package com.example.util.simpletimetracker.feature_notification.external

import com.example.util.simpletimetracker.core.extension.allowDiskRead
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationExternalBroadcastController @Inject constructor(
    private val externalBroadcastInteractor: ExternalBroadcastInteractor,
) {

    private val mutex = Mutex()

    fun onActionExternalActivityStart(
        name: String?,
        comment: String?,
        tagNames: List<String>,
        timeStarted: String?,
    ) = allowDiskRead { MainScope() }.launch {
        name ?: return@launch
        mutex.withLock {
            externalBroadcastInteractor.onActionActivityStart(
                name = name,
                comment = comment,
                tagNames = tagNames,
                timeStarted = timeStarted,
            )
        }
    }

    fun onActionExternalActivityStop(
        name: String?,
        timeEnded: String?,
    ) = allowDiskRead { MainScope() }.launch {
        name ?: return@launch
        mutex.withLock {
            externalBroadcastInteractor.onActionActivityStopByName(
                name = name,
                timeEnded = timeEnded,
            )
        }
    }

    fun onActionExternalActivityStopAll() = allowDiskRead { MainScope() }.launch {
        mutex.withLock {
            externalBroadcastInteractor.onActionActivityStopAll()
        }
    }

    fun onActionExternalActivityStopShortest() = allowDiskRead { MainScope() }.launch {
        mutex.withLock {
            externalBroadcastInteractor.onActionActivityStopShortest()
        }
    }

    fun onActionExternalActivityStopLongest() = allowDiskRead { MainScope() }.launch {
        mutex.withLock {
            externalBroadcastInteractor.onActionActivityStopLongest()
        }
    }

    fun onActionExternalActivityRestart(
        comment: String?,
        tagNames: List<String>,
    ) = allowDiskRead { MainScope() }.launch {
        mutex.withLock {
            externalBroadcastInteractor.onActionActivityRestart(
                comment = comment, tagNames = tagNames,
            )
        }
    }

    fun onActionExternalRecordAdd(
        name: String?,
        timeStarted: String?,
        timeEnded: String?,
        comment: String?,
        tagNames: List<String>,
    ) = allowDiskRead { MainScope() }.launch {
        name ?: return@launch
        timeStarted ?: return@launch
        timeEnded ?: return@launch
        mutex.withLock {
            externalBroadcastInteractor.onRecordAdd(
                name = name,
                timeStarted = timeStarted,
                timeEnded = timeEnded,
                comment = comment,
                tagNames = tagNames,
            )
        }
    }

    fun onActionExternalRecordChange(
        findMode: String?,
        name: String?,
        comment: String?,
        commentMode: String?,
    ) = allowDiskRead { MainScope() }.launch {
        mutex.withLock {
            externalBroadcastInteractor.onRecordChange(
                findModeData = findMode,
                name = name,
                comment = comment,
                commentModeData = commentMode,
            )
        }
    }

    fun onActionExternalRecordTagAdd(
        name: String?,
        icon: String?,
    ) = allowDiskRead { MainScope() }.launch {
        mutex.withLock {
            externalBroadcastInteractor.onRecordTagAdd(
                name = name,
                icon = icon,
            )
        }
    }
}