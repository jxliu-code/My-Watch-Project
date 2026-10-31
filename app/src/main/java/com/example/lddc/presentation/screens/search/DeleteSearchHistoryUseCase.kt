package com.example.lddc.domain.search

import com.example.lddc.data.repository.SearchHistoryRepository

class DeleteSearchHistoryUseCase(
    private val repository: SearchHistoryRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteHistory(id)
    }
}