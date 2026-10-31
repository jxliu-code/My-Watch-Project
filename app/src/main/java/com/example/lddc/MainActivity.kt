package com.example.lddc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lddc.common.models.info.SongInfo
import com.example.lddc.presentation.screens.search.LyricsDetailScreen
import com.example.lddc.presentation.screens.search.LyricsDetailViewModel
import com.example.lddc.presentation.screens.search.SearchFilters
import com.example.lddc.presentation.screens.search.SearchScreen
import com.example.lddc.presentation.screens.search.SearchViewModel
import com.example.lddc.presentation.screens.settings.SettingsScreen
import com.example.lddc.presentation.screens.settings.SettingsViewModel
import com.example.lddc.presentation.theme.LDDCTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Search : Screen(
        "search?initialQuery={initialQuery}&songName={songName}&artist={artist}&album={album}",
        "搜索",
        Icons.Default.Home
    )

    object Settings : Screen("settings", "设置", Icons.Default.Settings)

    object LyricsDetail : Screen("lyrics_detail/{songJson}", "歌词详情", Icons.Default.Home) {
        fun createRoute(song: SongInfo): String {
            val songJson = Json.encodeToString(song)
            val encoded = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
            return "lyrics_detail/$encoded"
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LDDCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LDDCApp()
                }
            }
        }
    }
}

@Composable
fun LDDCApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isRound = context.resources.configuration.isScreenRound

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val autoHide = settingsUiState.autoHideNavBar

    var isNavBarVisible by remember { mutableStateOf(true) }
    var interactionKey by remember { mutableIntStateOf(0) }

    // 监听触摸、滑动
    val pointerModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    if (!isNavBarVisible) isNavBarVisible = true
                    interactionKey++
                } else if (event.type == PointerEventType.Move) {
                    val deltaY = event.changes.firstOrNull()?.positionChange()?.y ?: 0f
                    // 如果向上滑动超过 5px，说明是上滑操作
                    if (deltaY < -5f) {
                        if (!isNavBarVisible) isNavBarVisible = true
                        interactionKey++ // 重置隐藏计时
                    }
                }
            }
        }
    }

    // 3秒倒计时隐藏逻辑
    LaunchedEffect(autoHide, isNavBarVisible, interactionKey) {
        if (autoHide && isNavBarVisible) {
            delay(3000)
            isNavBarVisible = false
        }
    }

    Scaffold(
        bottomBar = {
            if (!isRound) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().then(pointerModifier)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Search.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (isRound) 0.dp else innerPadding.calculateBottomPadding()
                    )
            ) {
                composable(
                    route = Screen.Search.route,
                    arguments = listOf(
                        navArgument("initialQuery") { type = NavType.StringType; defaultValue = "" },
                        navArgument("songName") { type = NavType.StringType; defaultValue = "" },
                        navArgument("artist") { type = NavType.StringType; defaultValue = "" },
                        navArgument("album") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val viewModel: SearchViewModel = hiltViewModel()
                    val initialQuery = backStackEntry.arguments?.getString("initialQuery") ?: ""
                    val songName = backStackEntry.arguments?.getString("songName") ?: ""
                    val artist = backStackEntry.arguments?.getString("artist") ?: ""
                    val album = backStackEntry.arguments?.getString("album") ?: ""
                    SearchScreen(
                        viewModel = viewModel,
                        initialQuery = initialQuery,
                        initialFilters = SearchFilters(songName, artist, album),
                        onSongClick = { song ->
                            navController.navigate(Screen.LyricsDetail.createRoute(song))
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = settingsViewModel)
                }

                composable(
                    route = Screen.LyricsDetail.route,
                    arguments = listOf(navArgument("songJson") { type = NavType.StringType })
                ) { backStackEntry ->
                    val viewModel: LyricsDetailViewModel = hiltViewModel()
                    val songJson = backStackEntry.arguments?.getString("songJson") ?: ""
                    val decodedJson = URLDecoder.decode(songJson, StandardCharsets.UTF_8.toString())
                    val song = try { Json.decodeFromString<SongInfo>(decodedJson) } catch (e: Exception) { null }

                    LyricsDetailScreen(
                        viewModel = viewModel,
                        song = song,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            if (isRound) {
                // 底栏隐藏时的极小字提示
                AnimatedVisibility(
                    visible = !isNavBarVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "上滑恢复导航",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // 悬浮底栏
                AnimatedVisibility(
                    visible = isNavBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    BottomNavigationBar(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val items = listOf(Screen.Search, Screen.Settings)
    val context = LocalContext.current
    val isRound = context.resources.configuration.isScreenRound
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (isRound) {
        Row(
            modifier = modifier
                .width(200.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        .padding(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        NavigationBar(modifier = modifier, windowInsets = WindowInsets(0, 0, 0, 0)) {
            items.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                    label = {
                        Text(
                            text = screen.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}