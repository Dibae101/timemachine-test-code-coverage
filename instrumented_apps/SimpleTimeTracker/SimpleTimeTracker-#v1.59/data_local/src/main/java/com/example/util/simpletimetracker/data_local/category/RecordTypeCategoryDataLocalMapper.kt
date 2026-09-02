package com.example.util.simpletimetracker.data_local.category

import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import javax.inject.Inject

class RecordTypeCategoryDataLocalMapper @Inject constructor() {

    fun map(dbo: RecordTypeCategoryDBO): RecordTypeCategory {
        return RecordTypeCategory(
            recordTypeId = dbo.recordTypeId,
            categoryId = dbo.categoryId,
        )
    }

    fun map(typeId: Long, categoryId: Long): RecordTypeCategoryDBO {
        return RecordTypeCategoryDBO(
            recordTypeId = typeId,
            categoryId = categoryId,
        )
    }

    fun map(domain: RecordTypeCategory): RecordTypeCategoryDBO {
        return RecordTypeCategoryDBO(
            recordTypeId = domain.recordTypeId,
            categoryId = domain.categoryId,
        )
    }
}