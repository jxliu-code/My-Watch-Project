package com.example.lddc.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import coil.compose.AsyncImage
import com.example.lddc.common.models.enums.Source
import com.example.lddc.common.models.info.SongInfo
import com.example.lddc.presentation.components.designsystem.LDDCCard
import com.example.lddc.presentation.components.designsystem.LDDCEmptyState
import com.example.lddc.presentation.components.designsystem.LDDCFilledButton
import com.example.lddc.presentation.components.designsystem.LDDCLoadingState
import com.example.lddc.presentation.components.designsystem.LDDCOutlinedButton
import com.example.lddc.presentation.components.designsystem.LDDCSearchInput
import com.example.lddc.presentation.components.designsystem.LDDCSourceChip
import com.example.lddc.presentation.theme.KugouColor
import com.example.lddc.presentation.theme.NetEaseColor
import com.example.lddc.presentation.theme.QQMusicColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    initialQuery: String = "",
    initialFilters: SearchFilters = SearchFilters(),
    onSongClick: (SongInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isRound = context.resources.configuration.isScreenRound

    LaunchedEffect(Unit) {
        if (initialQuery.isNotEmpty() && !uiState.hasSearched) {
            viewModel.onKeywordChange(initialQuery)
            if (initialFilters.hasActiveFilters()) {
                viewModel.onFiltersChange(initialFilters)
            }
            viewModel.search()
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            searchFilters = uiState.filters,
            enabledSources = uiState.enabledSources,
            onFiltersChanged = viewModel::onFiltersChange,
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "搜索歌词",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isRound) TextAlign.Center else TextAlign.Start,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LDDCSearchInput(
                    value = uiState.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    placeholder = "搜索歌曲、歌手",
                    onSearch = viewModel::search,
                    modifier = Modifier.weight(1f)
                )

                val activeFilterCount = uiState.filters.getActiveFilterCount()
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            androidx.compose.material3.Badge {
                                Text(activeFilterCount.toString())
                            }
                        }
                    }
                ) {
                    LDDCOutlinedButton(
                        text = "筛选",
                        onClick = { showFilterDialog = true },
                        icon = Icons.Default.FilterList
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isSearching -> {
                        LDDCLoadingState(text = "搜索中...")
                    }

                    uiState.errorMessage != null -> {
                        LDDCEmptyState(
                            icon = Icons.Default.Search,
                            title = "搜索失败",
                            description = uiState.errorMessage!!,
                            action = {
                                LDDCFilledButton(
                                    text = "重试",
                                    onClick = viewModel::search
                                )
                            }
                        )
                    }

                    !uiState.hasSearched -> {
                        if (searchHistory.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SearchHistorySection(
                                    history = searchHistory,
                                    onItemClick = { viewModel.searchFromHistory(it) },
                                    onDeleteItem = { id -> viewModel.deleteSingleHistory(id) },
                                    onClear = { viewModel.clearAllSearchHistory() }
                                )
                            }
                        } else {
                            LDDCEmptyState(
                                icon = Icons.Default.Search,
                                title = "开始搜索",
                                description = "输入关键词搜索歌词"
                            )
                        }
                    }

                    uiState.searchResults.isEmpty() -> {
                        LDDCEmptyState(
                            icon = Icons.Default.Search,
                            title = "未找到结果",
                            description = "尝试使用其他关键词搜索"
                        )
                    }

                    else -> {
                        val displaySongs = uiState.filteredResults
                        val totalCount = uiState.searchResults.values.flatten().size

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (displaySongs.size != totalCount)
                                        "搜索结果 (${displaySongs.size}/$totalCount)"
                                    else
                                        "搜索结果 ($totalCount)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                if (uiState.filters.hasActiveFilters()) {
                                    Text(
                                        text = "清除筛选",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { viewModel.clearFilters() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (displaySongs.isEmpty()) {
                                LDDCEmptyState(
                                    icon = Icons.Default.Search,
                                    title = "没有符合条件的结果",
                                    description = "尝试调整筛选条件"
                                )
                            } else {
                                val configuration = LocalConfiguration.current
                                val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

                                if (isLandscape) {
                                    val gridState = rememberLazyGridState()
                                    var lastFirstVisibleItem by remember { mutableIntStateOf(0) }
                                    var lastFirstVisibleScrollOffset by remember { mutableIntStateOf(0) }

                                    LaunchedEffect(gridState, uiState.hasMoreData) {
                                        snapshotFlow {
                                            Triple(
                                                gridState.layoutInfo.visibleItemsInfo,
                                                gridState.firstVisibleItemIndex,
                                                gridState.firstVisibleItemScrollOffset
                                            )
                                        }
                                            .collect { (visibleItems, firstIndex, scrollOffset) ->
                                                if (!uiState.hasMoreData || uiState.isLoadingMore) return@collect
                                                val totalItems = gridState.layoutInfo.totalItemsCount
                                                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                                                val isNearBottom = lastVisibleItem >= totalItems - 2
                                                val isScrolling = firstIndex != lastFirstVisibleItem || scrollOffset != lastFirstVisibleScrollOffset
                                                lastFirstVisibleItem = firstIndex
                                                lastFirstVisibleScrollOffset = scrollOffset
                                                if (isNearBottom && isScrolling) viewModel.loadMore()
                                            }
                                    }

                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 320.dp),
                                        state = gridState,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        items(displaySongs) { song ->
                                            SearchResultGridItem(song = song, onClick = { onSongClick(song) })
                                        }
                                        if (uiState.isLoadingMore || uiState.hasMoreData) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                LoadMoreIndicator(
                                                    isLoading = uiState.isLoadingMore,
                                                    hasMoreData = uiState.hasMoreData,
                                                    onClick = { viewModel.loadMore() }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val listState = rememberLazyListState()
                                    var lastFirstVisibleItem by remember { mutableIntStateOf(0) }
                                    var lastFirstVisibleScrollOffset by remember { mutableIntStateOf(0) }

                                    LaunchedEffect(listState, uiState.hasMoreData) {
                                        snapshotFlow {
                                            Triple(
                                                listState.layoutInfo.visibleItemsInfo,
                                                listState.firstVisibleItemIndex,
                                                listState.firstVisibleItemScrollOffset
                                            )
                                        }
                                            .collect { (visibleItems, firstIndex, scrollOffset) ->
                                                if (!uiState.hasMoreData || uiState.isLoadingMore) return@collect
                                                val totalItems = listState.layoutInfo.totalItemsCount
                                                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                                                val isNearBottom = lastVisibleItem >= totalItems - 2
                                                val isScrolling = firstIndex != lastFirstVisibleItem || scrollOffset != lastFirstVisibleScrollOffset
                                                lastFirstVisibleItem = firstIndex
                                                lastFirstVisibleScrollOffset = scrollOffset
                                                if (isNearBottom && isScrolling) viewModel.loadMore()
                                            }
                                    }

                                    LazyColumn(
                                        state = listState,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        items(displaySongs) { song ->
                                            SearchResultItem(song = song, onClick = { onSongClick(song) })
                                        }
                                        if (uiState.isLoadingMore || uiState.hasMoreData) {
                                            item {
                                                LoadMoreIndicator(
                                                    isLoading = uiState.isLoadingMore,
                                                    hasMoreData = uiState.hasMoreData,
                                                    onClick = { viewModel.loadMore() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    history: List<SearchViewModel.SearchHistoryItem>,
    onItemClick: (String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "清空",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onClear() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            history.forEach { item ->
                SearchHistoryChip(
                    keyword = item.keyword,
                    onClick = { onItemClick(item.keyword) },
                    onDelete = { onDeleteItem(item.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryChip(
    keyword: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = keyword,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LoadMoreIndicator(
    isLoading: Boolean,
    hasMoreData: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "加载中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else if (hasMoreData) {
            TextButton(
                onClick = onClick,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(
                    text = "点击加载更多",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = "没有更多数据了",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    song: SongInfo,
    onClick: () -> Unit
) {
    LDDCCard(
        onClick = onClick,
        elevated = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!song.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "未知歌曲",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist?.toString() ?: "未知歌手",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            song.source?.let { source ->
                LDDCSourceChip(
                    text = getSourceShortName(source),
                    sourceColor = getSourceColor(source)
                )
            }
        }
    }
}

@Composable
private fun SearchResultGridItem(
    song: SongInfo,
    onClick: () -> Unit
) {
    LDDCCard(
        onClick = onClick,
        elevated = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!song.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "未知歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist?.toString() ?: "未知歌手",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            song.source?.let { source ->
                LDDCSourceChip(
                    text = getSourceShortName(source),
                    sourceColor = getSourceColor(source)
                )
            }
        }
    }
}

private fun getSourceShortName(source: Source): String {
    return when (source) {
        Source.QM -> "QQ"
        Source.NE -> "网易"
        Source.KG -> "酷狗"
        Source.MULTI -> "全部"
        else -> source.name
    }
}

@Composable
private fun getSourceColor(source: Source): androidx.compose.ui.graphics.Color {
    return when (source) {
        Source.QM -> QQMusicColor
        Source.NE -> NetEaseColor
        Source.KG -> KugouColor
        Source.MULTI -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}