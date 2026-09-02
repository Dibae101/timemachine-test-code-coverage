package com.example.util.simpletimetracker.domain.favourite.interactor

import com.example.util.simpletimetracker.domain.favourite.model.RecordTypeToFavouriteComment
import com.example.util.simpletimetracker.domain.favourite.repo.RecordTypeToFavouriteCommentRepo
import javax.inject.Inject

class RecordTypeToFavouriteCommentInteractor @Inject constructor(
    private val repo: RecordTypeToFavouriteCommentRepo,
) {

    suspend fun getAll(): List<RecordTypeToFavouriteComment> {
        return repo.getAll()
    }

    suspend fun getTypes(commentId: Long): Set<Long> {
        return repo.getTypeIdsByComment(commentId)
    }

    suspend fun addTypes(commentId: Long, typeIds: List<Long>) {
        repo.addTypes(commentId, typeIds)
    }

    suspend fun removeTypes(commentId: Long, typeIds: List<Long>) {
        repo.removeTypes(commentId, typeIds)
    }

    suspend fun removeAll(commentId: Long) {
        repo.removeAll(commentId)
    }
}