package com.example.util.simpletimetracker.domain.favourite.interactor

import javax.inject.Inject

class IsCommentFavouriteInteractor @Inject constructor(
    private val favouriteCommentInteractor: FavouriteCommentInteractor,
    private val filterFavouriteCommentsInteractor: FilterFavouriteCommentsInteractor,
) {

    suspend fun execute(
        comment: String,
        typeId: Long,
    ): Boolean {
        val favouriteComment = favouriteCommentInteractor.get(comment)
        return if (favouriteComment != null) {
            filterFavouriteCommentsInteractor.execute(
                typeId = typeId,
                comments = listOf(favouriteComment),
            ).isNotEmpty()
        } else {
            false
        }
    }
}