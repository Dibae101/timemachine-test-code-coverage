package com.example.util.simpletimetracker.domain.favourite.interactor

import com.example.util.simpletimetracker.domain.favourite.model.FavouriteComment
import javax.inject.Inject

class FilterFavouriteCommentsInteractor @Inject constructor(
    val recordTypeToFavouriteCommentInteractor: RecordTypeToFavouriteCommentInteractor,
) {

    suspend fun execute(
        typeId: Long,
        comments: List<FavouriteComment>,
    ): List<FavouriteComment> {
        val (included, excluded) = recordTypeToFavouriteCommentInteractor.getAll()
            .partition { it.recordTypeId == typeId }

        val includedIds = included.map { it.commentId }
        val excludedIds = excluded.map { it.commentId }

        // Assigned to this typeId or not assigned to other typeIds.
        return comments.filter { it.id in includedIds || it.id !in excludedIds }
    }
}