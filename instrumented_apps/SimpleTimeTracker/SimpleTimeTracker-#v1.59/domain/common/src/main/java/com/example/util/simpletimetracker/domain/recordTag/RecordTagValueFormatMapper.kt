package com.example.util.simpletimetracker.domain.recordTag

import javax.inject.Inject

class RecordTagValueFormatMapper @Inject constructor() {

    fun map(value: Double): String {
        // TODO do better?
        return value.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}