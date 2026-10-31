package com.example.lddc.presentation.screens.search

import androidx.lifecycle.viewModelScope
import com.example.lddc.common.models.enums.SearchType
import com.example.lddc.common.models.enums.Source
import com.example.lddc.common.models.info.SongInfo
import com.example.lddc.data.repository.LyricsRepository
import com.example.lddc.domain.search.ClearSearchHistoryUseCase
import com.example.lddc.domain.search.DeleteSearchHistoryUseCase
import com.example.lddc.domain.search.GetSearchHistoryUseCase
import com.example.lddc.domain.search.SearchSongsUseCase
import com.example.lddc.domain.settings.GetSettingsUseCase
import com.example.lddc.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val deleteSearchHistoryUseCase: DeleteSearchHistoryUseCase,
    private val getSettingsUseCase: GetSettingsUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory.asStateFlow()

    init {
        loadSearchHistory()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                _uiState.update { it.copy(enabledSources = settings.enabledSources) }
            }
        }
    }

    fun onKeywordChange(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
    }

    fun onFiltersChange(filters: SearchFilters) {
        _uiState.update { currentState ->
            val filtered = applyFilters(currentState.searchResults, filters)
            currentState.copy(
                filters = filters,
                filteredResults = filtered
            )
        }
    }

    fun clearFilters() {
        _uiState.update { currentState ->
            val allSongs = currentState.searchResults.values.flatten()
            currentState.copy(
                filters = SearchFilters(),
                filteredResults = allSongs
            )
        }
    }

    private fun applyFilters(
        results: Map<Source, List<SongInfo>>,
        filters: SearchFilters
    ): List<SongInfo> {
        var songs = results.values.flatten()

        if (filters.songName.isNotBlank()) {
            songs = songs.filter { song ->
                song.title?.contains(filters.songName, ignoreCase = true) == true
            }
        }

        if (filters.artist.isNotBlank()) {
            songs = songs.filter { song ->
                song.artist?.toString()?.contains(filters.artist, ignoreCase = true) == true
            }
        }

        if (filters.album.isNotBlank()) {
            songs = songs.filter { song ->
                song.album?.contains(filters.album, ignoreCase = true) == true
            }
        }

        if (filters.platforms.isNotEmpty()) {
            songs = songs.filter { song ->
                song.source in filters.platforms
            }
        }

        songs = interleaveSongsBySource(songs)
        return songs
    }

    private fun interleaveSongsBySource(songs: List<SongInfo>): List<SongInfo> {
        val grouped = songs.groupBy { it.source }
        val sourceOrder = listOf(Source.QM, Source.KG, Source.NE)
        val result = mutableListOf<SongInfo>()
        var index = 0
        var hasMore = true

        while (hasMore) {
            hasMore = false
            for (source in sourceOrder) {
                val sourceSongs = grouped[source] ?: emptyList()
                if (index < sourceSongs.size) {
                    result.add(sourceSongs[index])
                    hasMore = true
                }
            }
            index++
        }
        return result
    }

    fun search() {
        val keyword = _uiState.value.keyword.trim()
        if (keyword.isEmpty()) return

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isSearching = true, errorMessage = null, currentPage = 1, hasMoreData = true) }

            searchSongsUseCase.saveSearchHistory(keyword, _uiState.value.searchType)

            searchSongsUseCase(
                keyword = keyword,
                source = _uiState.value.selectedSource,
                searchType = _uiState.value.searchType,
                enabledSources = _uiState.value.enabledSources,
                page = 1
            ).collectLatest { result ->
                when (result) {
                    is LyricsRepository.SearchResult.Loading -> {
                        _uiState.update { it.copy(isSearching = true) }
                    }
                    is LyricsRepository.SearchResult.Success -> {
                        _uiState.update { currentState ->
                            val filtered = applyFilters(result.results, currentState.filters)
                            currentState.copy(
                                isSearching = false,
                                searchResults = result.results,
                                filteredResults = filtered,
                                hasSearched = true,
                                hasMoreData = result.results.values.any { it.size >= 20 }
                            )
                        }
                    }
                    is LyricsRepository.SearchResult.Error -> {
                        _uiState.update {
                            it.copy(isSearching = false, errorMessage = result.message)
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val keyword = _uiState.value.keyword.trim()
        if (keyword.isEmpty() || _uiState.value.isLoadingMore || !_uiState.value.hasMoreData) return

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = _uiState.value.currentPage + 1

            searchSongsUseCase(
                keyword = keyword,
                source = _uiState.value.selectedSource,
                searchType = _uiState.value.searchType,
                enabledSources = _uiState.value.enabledSources,
                page = nextPage
            ).collectLatest { result ->
                when (result) {
                    is LyricsRepository.SearchResult.Loading -> {}
                    is LyricsRepository.SearchResult.Success -> {
                        _uiState.update { currentState ->
                            val newResults = currentState.searchResults.toMutableMap()
                            result.results.forEach { (source, songs) ->
                                val existingSongs = newResults.getOrDefault(source, emptyList())
                                newResults[source] = existingSongs + songs
                            }
                            
                            val filtered = applyFilters(newResults, currentState.filters)
                            currentState.copy(
                                isLoadingMore = false,
                                searchResults = newResults,
                                filteredResults = filtered,
                                currentPage = nextPage,
                                hasMoreData = result.results.values.any { it.size >= 20 }
                            )
                        }
                    }
                    is LyricsRepository.SearchResult.Error -> {
                        _uiState.update {
                            it.copy(isLoadingMore = false, errorMessage = result.message)
                        }
                    }
                }
            }
        }
    }

    fun searchFromHistory(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        search()
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            getSearchHistoryUseCase().collectLatest { history ->
                _searchHistory.value = history.map { item ->
                    SearchHistoryItem(
                        id = item.id,
                        keyword = item.keyword,
                        searchType = item.searchType
                    )
                }
            }
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }

    // 新增：删除单条历史记录
    fun deleteSingleHistory(id: Long) {
        viewModelScope.launch {
            deleteSearchHistoryUseCase(id)
        }
    }

    data class SearchUiState(
        val keyword: String = "",
        val selectedSource: Source = Source.MULTI,
        val searchType: SearchType = SearchType.SONG,
        val isSearching: Boolean = false,
        val isLoadingMore: Boolean = false,
        val searchResults: Map<Source, List<SongInfo>> = emptyMap(),
        val filteredResults: List<SongInfo> = emptyList(),
        val hasSearched: Boolean = false,
        val hasMoreData: Boolean = true,
        val currentPage: Int = 1,
        val errorMessage: String? = null,
        val enabledSources: List<Source> = listOf(Source.MULTI, Source.NE, Source.QM, Source.KG),
        val filters: SearchFilters = SearchFilters()
    )

    data class SearchHistoryItem(
        val id: Long,
        val keyword: String,
        val searchType: SearchType
    )
}