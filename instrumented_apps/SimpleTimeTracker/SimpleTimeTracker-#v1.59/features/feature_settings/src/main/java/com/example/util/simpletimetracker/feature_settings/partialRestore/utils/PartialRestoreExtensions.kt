package com.example.util.simpletimetracker.feature_settings.partialRestore.utils

import com.example.util.simpletimetracker.domain.backup.model.PartialBackupRestoreData
import com.example.util.simpletimetracker.feature_settings.partialRestore.model.PartialRestoreFilterType

fun PartialBackupRestoreData.getIds(
    filter: PartialRestoreFilterType,
    existing: Boolean,
): Set<Long> {
    return when (filter) {
        is PartialRestoreFilterType.Activities -> types
        is PartialRestoreFilterType.Categories -> categories
        is PartialRestoreFilterType.Tags -> tags
        is PartialRestoreFilterType.Records -> records
        is PartialRestoreFilterType.RecordShortcuts -> recordShortcuts
        is PartialRestoreFilterType.ActivityFilters -> activityFilters
        is PartialRestoreFilterType.FavouriteComments -> favouriteComments
        is PartialRestoreFilterType.FavouriteColors -> favouriteColors
        is PartialRestoreFilterType.FavouriteIcons -> favouriteIcon
        is PartialRestoreFilterType.ComplexRules -> rules
        is PartialRestoreFilterType.ActivitySuggestions -> activitySuggestions
    }.filter {
        if (existing) it.value.exist else !it.value.exist
    }.keys
}
