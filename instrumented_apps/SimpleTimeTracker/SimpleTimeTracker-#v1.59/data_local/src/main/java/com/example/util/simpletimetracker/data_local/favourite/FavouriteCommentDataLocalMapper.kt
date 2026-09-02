package com.example.util.simpletimetracker.data_local.favourite

import com.example.util.simpletimetracker.domain.favourite.model.FavouriteComment
import javax.inject.Inject

class FavouriteCommentDataLocalMapper @Inject constructor() {

    fun map(dbo: FavouriteCommentDBO): FavouriteComment {
        return FavouriteComment(
            id = dbo.id,
            comment = dbo.comment,
        )
    }

    fun map(domain: FavouriteComment): FavouriteCommentDBO {
        return FavouriteCommentDBO(
            id = domain.id,
            comment = domain.comment,
        )
    }
}