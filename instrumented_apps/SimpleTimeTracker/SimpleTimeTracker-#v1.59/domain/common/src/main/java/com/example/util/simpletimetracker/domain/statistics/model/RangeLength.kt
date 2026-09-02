package com.example.util.simpletimetracker.domain.statistics.model

import com.example.util.simpletimetracker.domain.record.model.Range

sealed class RangeLength {
    data object Day : RangeLength()
    data object Week : RangeLength()
    data object Month : RangeLength()
    data object Year : RangeLength()
    data object All : RangeLength()
    data class Custom(val range: Range) : RangeLength()
    data class Last(val days: Int) : RangeLength()
}