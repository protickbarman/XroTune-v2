package com.xrotune.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.xrotune.app.data.model.SearchResultItem
import com.xrotune.app.ui.components.AddToPlaylistDialog
import com.xrotune.app.ui.components.CinematicLaunchContainer
import com.xrotune.app.ui.components.DownloadQualityPickerSheet
import com.xrotune.app.ui.components.FormatInspectorSheet
import com.xrotune.app.ui.components.IosMotionTokens
import com.xrotune.app.ui.components.MiniPlayer
import com.xrotune.app.ui.components.MusicCollectionSheet
import com.xrotune.app.ui.components.TrackOptionsSheet
import com.xrotune.app.ui.screens.HomeScreen
import com.xrotune.app.ui.screens.NowPlayingScreen
import com.xrotune.app.ui.screens.PlaylistsScreen
import com.xrotune.app.ui.screens.SearchScreen
import com.xrotune.app.ui.screens.SettingsScreen
import com.xrotune.app.ui.theme.TintedSurface
import com.xrotune.app.ui.theme.XroTuneTheme
import com.xrotune.app.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private var openPlayerFromIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            XroTuneTheme {
                CinematicLaunchContainer {
                    MainAppScreen(
                        openPlayerTrigger = openPlayerFromIntent,
                        onConsumeOpenPlayerTrigger = { openPlayerFromIntent = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
            openPlayerFromIntent = true
            intent.removeExtra(EXTRA_OPEN_NOW_PLAYING)
        }
    }

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "extra_open_now_playing"
    }
}

sealed class Screen(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    data object Search : Screen("search", R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search)
    data object Playlists : Screen("playlists", R.string.nav_playlists, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MainAppScreen(
    openPlayerTrigger: Boolean = false,
    onConsumeOpenPlayerTrigger: () -> Unit = {},
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Search, Screen.Playlists, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val playlists by viewModel.playlistSummaries.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.videoId }.toSet() }
    val downloadedIds = remember(downloadedSongs) { downloadedSongs.map { it.videoId }.toSet() }

    var pendingPlaylistAddSong by remember { mutableStateOf<SearchResultItem?>(null) }
    var collectionOptionsTrack by remember { mutableStateOf<SearchResultItem?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(viewModel.shouldRequestNotificationPermission) {
        if (viewModel.shouldRequestNotificationPermission) {
            viewModel.shouldRequestNotificationPermission = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    LaunchedEffect(openPlayerTrigger) {
        if (openPlayerTrigger) {
            if (viewModel.playerManager.currentSong != null) {
                viewModel.showNowPlayingSheet = true
            }
            onConsumeOpenPlayerTrigger()
        }
    }

    LaunchedEffect(viewModel.statusMessage) {
        val msg = viewModel.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val isNowPlayingPopupVisible = viewModel.showNowPlayingSheet && viewModel.playerManager.currentSong != null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val popupMaxHeight = (maxHeight * 0.90f).coerceAtLeast(480.dp)

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                Surface(
                    color = TintedSurface,
                    shadowElevation = 12.dp,
                    tonalElevation = 0.dp
                ) {
                    NavigationRail(
                        containerColor = TintedSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        screens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val label = stringResource(screen.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(screen.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = TintedSurface,
                        shadowElevation = 16.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AnimatedVisibility(
                                visible = viewModel.playerManager.currentSong != null,
                                enter = slideInVertically(
                                    animationSpec = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 360f),
                                    initialOffsetY = { it }
                                ) + fadeIn(tween(200)),
                                exit = slideOutVertically(
                                    animationSpec = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 380f),
                                    targetOffsetY = { it }
                                ) + fadeOut(tween(160))
                            ) {
                                MiniPlayer(viewModel = viewModel)
                            }
                            if (!isWideScreen) {
                                NavigationBar(
                                    containerColor = TintedSurface,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    screens.forEach { screen ->
                                        val selected = currentRoute == screen.route
                                        val label = stringResource(screen.labelRes)
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(screen.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                    contentDescription = label
                                                )
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(180)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = { navigateTo(Screen.Search.route) },
                            onNavigateToPlaylists = { navigateTo(Screen.Playlists.route) }
                        )
                    }
                    composable(Screen.Search.route) {
                        SearchScreen(viewModel = viewModel)
                    }
                    composable(Screen.Playlists.route) {
                        PlaylistsScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = { navigateTo(Screen.Search.route) }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Dimmed scrim behind floating rounded bottom Now Playing popup
        AnimatedVisibility(
            visible = isNowPlayingPopupVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.showNowPlayingSheet = false }
                    )
            )
        }

        // Floating rounded bottom Now Playing popup sliding up with iOS spring
        AnimatedVisibility(
            visible = isNowPlayingPopupVisible,
            enter = slideInVertically(
                animationSpec = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 360f),
                initialOffsetY = { it }
            ) + scaleIn(
                animationSpec = IosMotionTokens.SheetSpring,
                initialScale = 0.95f,
                transformOrigin = TransformOrigin(0.5f, 1.0f)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 400f),
                targetOffsetY = { it }
            ) + scaleOut(
                animationSpec = IosMotionTokens.SheetSpring,
                targetScale = 0.96f,
                transformOrigin = TransformOrigin(0.5f, 1.0f)
            ) + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .heightIn(max = popupMaxHeight)
                .zIndex(10f)
        ) {
            NowPlayingScreen(viewModel = viewModel)
        }

        MusicCollectionSheet(
            viewModel = viewModel,
            onRequestTrackOptions = { collectionOptionsTrack = it }
        )

        collectionOptionsTrack?.let { song ->
            val vid = song.videoId ?: ""
            TrackOptionsSheet(
                song = song,
                isFavorite = vid in favoriteIds,
                isDownloaded = vid in downloadedIds,
                onDismiss = { collectionOptionsTrack = null },
                onPlayNow = {
                    viewModel.closeCollectionSheet()
                    viewModel.playTrack(song, openPlayerSheet = true)
                },
                onToggleFavorite = { viewModel.toggleFavorite(song) },
                onAddToPlaylist = { pendingPlaylistAddSong = song },
                onInspectFormats = { if (vid.isNotBlank()) viewModel.openFormatInspector(vid) },
                onDownloadToggle = {
                    if (vid in downloadedIds) viewModel.removeOfflineDownload(vid)
                    else viewModel.requestDownloadTrack(song)
                }
            )
        }

        FormatInspectorSheet(
            viewModel = viewModel,
            onAddToPlaylist = { song ->
                viewModel.closeFormatInspector()
                pendingPlaylistAddSong = song
            }
        )

        DownloadQualityPickerSheet(viewModel = viewModel)

        pendingPlaylistAddSong?.let { song ->
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { pendingPlaylistAddSong = null },
                onSelectPlaylist = { plId ->
                    viewModel.addSongToPlaylist(plId, song)
                    pendingPlaylistAddSong = null
                },
                onCreateAndAdd = { newName ->
                    viewModel.createPlaylistAndAddSong(newName, song)
                    pendingPlaylistAddSong = null
                }
            )
        }
    }
}
