package com.example.util.simpletimetracker.core.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileWorkRepo @Inject constructor() {

    val inProgress: LiveData<Boolean> = MutableLiveData(false)
}