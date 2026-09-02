package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.base.ARCHIVED_BUTTON_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNCATEGORIZED_ITEM_ID
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryAddViewData
import com.example.util.simpletimetracker.feature_base_adapter.category.CategoryViewData
import com.example.util.simpletimetracker.feature_base_adapter.empty.EmptyViewData
import com.example.util.simpletimetracker.feature_base_adapter.hint.HintViewData
import com.example.util.simpletimetracker.feature_base_adapter.hintBig.HintBigViewData
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon
import javax.inject.Inject

class CategoryViewDataMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    private val iconMapper: IconMapper,
    private val resourceRepo: ResourceRepo,
    private val recordTagViewDataMapper: RecordTagViewDataMapper,
    private val recordTagValueMapper: RecordTagValueMapper,
) {

    fun mapCategory(
        category: Category,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false,
    ): CategoryViewData.Category {
        return CategoryViewData.Category(
            id = category.id,
            name = category.name,
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            color = colorMapper.toFilteredColor(
                color = category.color,
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
        )
    }

    fun mapToUncategorizedItem(
        isFiltered: Boolean,
        isDarkTheme: Boolean,
    ): CategoryViewData.Category {
        return CategoryViewData.Category(
            id = UNCATEGORIZED_ITEM_ID,
            name = R.string.uncategorized_time_name
                .let(resourceRepo::getString),
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            color = colorMapper.toFilteredUntrackedColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
        )
    }

    fun mapToCategoryUntrackedItem(
        isFiltered: Boolean,
        isDarkTheme: Boolean,
    ): CategoryViewData {
        return CategoryViewData.Category(
            id = UNTRACKED_ITEM_ID,
            name = R.string.untracked_time_name
                .let(resourceRepo::getString),
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            color = colorMapper.toFilteredUntrackedColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
        )
    }

    fun mapRecordTag(
        tag: RecordTag,
        types: Map<Long, RecordType>,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false,
    ): CategoryViewData.Record.Tagged {
        val icon = recordTagViewDataMapper.mapIcon(tag, types)
            ?.let(iconMapper::mapIcon)
        val color = recordTagViewDataMapper.mapColor(tag, types)

        return CategoryViewData.Record.Tagged(
            id = tag.id,
            name = tag.name,
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            iconAlpha = colorMapper.toIconAlpha(icon, isFiltered),
            color = colorMapper.toFilteredColor(
                color = color,
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            icon = icon,
        )
    }

    fun mapRecordTagWithValue(
        tag: RecordTag,
        tagData: RecordBase.Tag?,
        types: Map<Long, RecordType>,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false,
        valueOnStartIds: List<Long> = emptyList(),
    ): CategoryViewData.Record {
        val viewData = mapRecordTag(
            tag = tag,
            types = types,
            isDarkTheme = isDarkTheme,
            isFiltered = isFiltered,
        )
        val newName = recordTagValueMapper.getName(
            tagId = tag.id,
            name = tag.name,
            value = tagData?.numericValue,
            valueSuffix = tag.valueSuffix,
            valueOnStartIds = valueOnStartIds,
        )
        return viewData.copy(name = newName)
    }

    fun groupToTagGroups(
        tags: List<RecordTag>,
    ): Map<String, List<RecordTag>> {
        // Sorted by group name.
        return tags.groupBy { mapRecordTagToTagGroupName(it) }.toSortedMap()
    }

    fun mapToUntaggedItem(
        isDarkTheme: Boolean,
        isFiltered: Boolean,
    ): CategoryViewData.Record.Untagged {
        return CategoryViewData.Record.Untagged(
            id = UNCATEGORIZED_ITEM_ID,
            name = R.string.change_record_untagged
                .let(resourceRepo::getString),
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            color = colorMapper.toFilteredUntrackedColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            icon = RecordTypeIcon.Image(R.drawable.untagged),
        )
    }

    fun mapToTagUntrackedItem(
        isFiltered: Boolean,
        isDarkTheme: Boolean,
    ): CategoryViewData {
        return CategoryViewData.Record.Tagged(
            id = UNTRACKED_ITEM_ID,
            name = R.string.untracked_time_name
                .let(resourceRepo::getString),
            icon = RecordTypeIcon.Image(R.drawable.unknown),
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
            color = colorMapper.toFilteredUntrackedColor(
                isDarkTheme = isDarkTheme,
                isFiltered = isFiltered,
            ),
        )
    }

    fun mapToTagArchiveItem(
        isEnabled: Boolean,
        isDarkTheme: Boolean,
    ): CategoryViewData {
        return CategoryViewData.Record.Tagged(
            id = ARCHIVED_BUTTON_ITEM_ID, // TODO use special item instead
            name = R.string.settings_archive
                .let(resourceRepo::getString),
            icon = RecordTypeIcon.Image(R.drawable.archive),
            iconColor = colorMapper.toIconColor(
                isDarkTheme = isDarkTheme,
                isFiltered = false,
            ),
            color = mapEnabledColor(isEnabled = isEnabled, isDarkTheme = isDarkTheme),
        )
    }

    fun mapToTypeTagAddItem(
        useShortName: Boolean,
        isDarkTheme: Boolean,
    ): CategoryAddViewData {
        return map(
            useShortName = useShortName,
            type = CategoryAddViewData.Type.AddCategory,
            isDarkTheme = isDarkTheme,
        )
    }

    fun mapToRecordTagAddItem(
        useShortName: Boolean,
        isDarkTheme: Boolean,
    ): CategoryAddViewData {
        return map(
            useShortName = useShortName,
            type = CategoryAddViewData.Type.AddTag,
            isDarkTheme = isDarkTheme,
        )
    }

    fun mapToRecordTagShowAllItem(
        isEnabled: Boolean,
        isDarkTheme: Boolean,
    ): CategoryAddViewData {
        return CategoryAddViewData(
            type = CategoryAddViewData.Type.ShowAll,
            name = resourceRepo.getString(R.string.types_filter_show_all),
            color = mapEnabledColor(isEnabled = isEnabled, isDarkTheme = isDarkTheme),
            icon = if (isEnabled) {
                RecordTypeIcon.Image(R.drawable.hide)
            } else {
                RecordTypeIcon.Image(R.drawable.show)
            },
        )
    }

    fun mapToTagSearchItem(
        isEnabled: Boolean,
        isDarkTheme: Boolean,
    ): CategoryAddViewData {
        return CategoryAddViewData(
            type = CategoryAddViewData.Type.EnableSearch,
            name = resourceRepo.getString(R.string.search_hint),
            color = mapEnabledColor(isEnabled = isEnabled, isDarkTheme = isDarkTheme),
            icon = RecordTypeIcon.Image(R.drawable.search),
        )
    }

    fun mapToRecordTagsEmpty(): ViewHolderType {
        return EmptyViewData(
            message = resourceRepo.getString(R.string.change_record_categories_empty),
        )
    }

    fun mapToCategoriesEmpty(): ViewHolderType {
        return EmptyViewData(
            message = resourceRepo.getString(R.string.change_record_type_categories_empty),
        )
    }

    fun mapToCategoryHint(): HintViewData = HintViewData(
        text = R.string.categories_record_type_hint
            .let(resourceRepo::getString),
    )

    fun mapToRecordTagHint(): HintViewData = HintViewData(
        text = R.string.categories_record_hint
            .let(resourceRepo::getString),
    )

    fun mapToCategoriesFirstHint(): ViewHolderType {
        return HintBigViewData(
            text = resourceRepo.getString(R.string.categories_record_type_hint),
            infoIconVisible = true,
            closeIconVisible = false,
        )
    }

    fun mapToTagsFirstHint(): ViewHolderType {
        return HintBigViewData(
            text = resourceRepo.getString(R.string.categories_record_hint),
            infoIconVisible = true,
            closeIconVisible = false,
        )
    }

    private fun map(
        useShortName: Boolean,
        type: CategoryAddViewData.Type,
        isDarkTheme: Boolean,
    ): CategoryAddViewData {
        val name = when {
            useShortName -> R.string.running_records_add_type
            type is CategoryAddViewData.Type.AddCategory -> R.string.categories_add_category
            type is CategoryAddViewData.Type.AddTag -> R.string.categories_add_record_tag
            else -> 0
        }.let(resourceRepo::getString)

        return CategoryAddViewData(
            type = type,
            name = name,
            color = colorMapper.toInactiveColor(isDarkTheme),
            icon = RecordTypeIcon.Image(R.drawable.add),
        )
    }

    private fun mapRecordTagToTagGroupName(
        tag: RecordTag,
    ): String {
        return tag.name.substringBefore("::", "")
    }

    private fun mapEnabledColor(
        isEnabled: Boolean,
        isDarkTheme: Boolean,
    ): Int {
        return if (isEnabled) {
            colorMapper.toActiveColor(isDarkTheme)
        } else {
            colorMapper.toInactiveColor(isDarkTheme)
        }
    }
}