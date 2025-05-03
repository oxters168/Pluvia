package com.OxGames.Pluvia.ui.screen.friends

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.enums.DialogType
import com.OxGames.Pluvia.ui.component.BBCodeText
import com.OxGames.Pluvia.ui.component.EmptyScreen
import com.OxGames.Pluvia.ui.component.LoadingScreen
import com.OxGames.Pluvia.ui.component.data.fakeSteamFriends
import com.OxGames.Pluvia.ui.component.dialog.GamesListDialog
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.ui.component.dialog.WebViewDialog
import com.OxGames.Pluvia.ui.component.dialog.state.MessageDialogState
import com.OxGames.Pluvia.ui.component.topbar.AccountButton
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.OxGames.Pluvia.ui.screen.friends.components.FriendItem
import com.OxGames.Pluvia.ui.screen.friends.components.ProfileButton
import com.OxGames.Pluvia.ui.screen.friends.components.StickyHeaderItem
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.utils.SteamUtils
import com.materialkolor.ktx.isLight
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.javasteam.types.SteamID
import java.util.Date
import kotlinx.coroutines.launch

// TODO pressing back wont make the selected profile go to the initial details screen.

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = hiltViewModel(),
    onChat: (Long) -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Unit>()
    val state by viewModel.friendsState.collectAsStateWithLifecycle()

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    FriendsScreenContent(
        navigator = navigator,
        state = state,
        onBack = { onBackPressedDispatcher?.onBackPressed() },
        onChat = onChat,
        onBlock = viewModel::onBlock,
        onRemove = viewModel::onRemove,
        onFriendClick = viewModel::observeSelectedFriend,
        onHeaderAction = viewModel::onHeaderAction,
        onLogout = onLogout,
        onNickName = viewModel::onNickName,
        onSettings = onSettings,
        onAlias = viewModel::onAlias,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun FriendsScreenContent(
    navigator: ThreePaneScaffoldNavigator<Unit>,
    state: FriendsState,
    onBack: () -> Unit,
    onChat: (Long) -> Unit,
    onBlock: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onFriendClick: (Long) -> Unit,
    onHeaderAction: (String) -> Unit,
    onLogout: () -> Unit,
    onNickName: (String) -> Unit,
    onSettings: () -> Unit,
    onAlias: () -> Unit,
) {
    val listState = rememberLazyListState() // Hoisted high to preserve state
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showGamesDialog by rememberSaveable { mutableStateOf(false) }

    GamesListDialog(
        visible = showGamesDialog,
        list = state.profileFriendGames,
        onDismissRequest = {
            showGamesDialog = false
        },
    )

    // Pretty much the same as 'NavigableListDetailPaneScaffold'
    BackHandler(navigator.canNavigateBack(BackNavigationBehavior.PopUntilContentChange)) {
        scope.launch {
            navigator.navigateBack(BackNavigationBehavior.PopUntilContentChange)
        }
    }

    ListDetailPaneScaffold(
        modifier = Modifier.displayCutoutPadding(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                FriendsListPane(
                    state = state,
                    listState = listState,
                    snackbarHost = snackbarHost,
                    onBack = onBack,
                    onChat = onChat,
                    onFriendClick = {
                        scope.launch {
                            onFriendClick(it.id)
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        }
                    },
                    onHeaderAction = onHeaderAction,
                    onLogout = onLogout,
                    onSettings = onSettings,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                FriendsDetailPane(
                    state = state,
                    onBack = onBack,
                    onChat = onChat,
                    onBlock = onBlock,
                    onRemove = onRemove,
                    onNickName = onNickName,
                    onShowGames = {
                        showGamesDialog = true
                    },
                    onAlias = onAlias,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FriendsListPane(
    state: FriendsState,
    snackbarHost: SnackbarHostState,
    listState: LazyListState,
    onBack: () -> Unit,
    onChat: (Long) -> Unit,
    onFriendClick: (SteamFriend) -> Unit,
    onHeaderAction: (String) -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.title_friends)) },
                actions = {
                    AccountButton(
                        onSettings = onSettings,
                        onLogout = onLogout,
                    )
                },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        val context = LocalContext.current

        LazyColumn(
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                )
                .fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            state.friendsList.forEach { (key, value) ->
                val header = context.getString(key)
                stickyHeader {
                    StickyHeaderItem(
                        isCollapsed = header in state.collapsedListSections,
                        header = header,
                        count = value.size,
                        onHeaderAction = { onHeaderAction(header) },
                    )
                }

                if (header !in state.collapsedListSections) {
                    itemsIndexed(value, key = { _, item -> item.id }) { idx, friend ->
                        FriendItem(
                            modifier = Modifier.animateItem(),
                            friend = friend,
                            onClick = { onChat(friend.id) },
                            onLongClick = { onFriendClick(friend) },
                        )

                        if (idx < value.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsDetailPane(
    state: FriendsState,
    onBack: () -> Unit,
    onChat: (Long) -> Unit,
    onBlock: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onNickName: (String) -> Unit,
    onShowGames: () -> Unit,
    onAlias: () -> Unit,
) {
    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = {
                if (state.profileFriend == null) {
                    EmptyScreen(message = stringResource(R.string.friend_no_selection))
                } else {
                    ProfileDetailsScreen(
                        state = state,
                        onBack = onBack,
                        onChat = onChat,
                        onBlock = onBlock,
                        onRemove = onRemove,
                        onNickName = onNickName,
                        onShowGames = onShowGames,
                        onAlias = onAlias,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDetailsScreen(
    state: FriendsState,
    onBack: () -> Unit,
    onChat: (Long) -> Unit,
    onBlock: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onNickName: (String) -> Unit,
    onShowGames: () -> Unit,
    onAlias: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val windowWidth = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val context = LocalContext.current

    var msgDialogState by rememberSaveable(stateSaver = MessageDialogState.Saver) {
        mutableStateOf(MessageDialogState(false))
    }

    val onDismissRequest: (() -> Unit)?
    val onDismissClick: (() -> Unit)?
    val onConfirmClick: (() -> Unit)?

    when (msgDialogState.type) {
        DialogType.FRIEND_BLOCK -> {
            onConfirmClick = {
                onBlock(state.profileFriend!!.id)
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        DialogType.FRIEND_REMOVE -> {
            onConfirmClick = {
                onRemove(state.profileFriend!!.id)
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        DialogType.FRIEND_FAVORITE -> {
            onConfirmClick = {
                Toast.makeText(context, "Favorite TODO", Toast.LENGTH_SHORT).show()
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        DialogType.FRIEND_UN_FAVORITE -> {
            onConfirmClick = {
                Toast.makeText(context, "Un-Favorite TODO", Toast.LENGTH_SHORT).show()
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(visible = false) }
            onDismissClick = { msgDialogState = MessageDialogState(visible = false) }
        }

        else -> {
            onDismissRequest = null
            onDismissClick = null
            onConfirmClick = null
        }
    }

    MessageDialog(
        visible = msgDialogState.visible,
        onDismissRequest = onDismissRequest,
        onConfirmClick = onConfirmClick,
        confirmBtnText = msgDialogState.confirmBtnText,
        onDismissClick = onDismissClick,
        dismissBtnText = msgDialogState.dismissBtnText,
        icon = msgDialogState.type.icon,
        title = msgDialogState.title,
        message = msgDialogState.message,
    )

    var setNickNameDialog by rememberSaveable { mutableStateOf(false) }
    var newNickName by rememberSaveable(state.profileFriend!!.nickname) {
        mutableStateOf(state.profileFriend.nickname)
    }
    if (setNickNameDialog) {
        AlertDialog(
            onDismissRequest = {
                setNickNameDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.dialog_title_friend_set_nickname),
                )
            },
            title = { Text(text = stringResource(R.string.dialog_title_friend_set_nickname)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.dialog_message_friend_set_nickname, state.profileFriend!!.name))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newNickName,
                        onValueChange = { newNickName = it },
                        label = { Text(text = stringResource(R.string.nickname)) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setNickNameDialog = false
                        onNickName(newNickName)
                    },
                    content = { Text(text = stringResource(R.string.confirm)) },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { setNickNameDialog = false },
                    content = { Text(text = stringResource(R.string.cancel)) },
                )
            },
        )
    }

    var showPreviousAliasDialog by rememberSaveable { mutableStateOf(false) }
    if (showPreviousAliasDialog) {
        AlertDialog(
            onDismissRequest = {
                showPreviousAliasDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.dialog_title_friend_past_aliases),
                )
            },
            title = { Text(text = stringResource(R.string.dialog_title_friend_past_aliases)) },
            text = {
                LazyColumn {
                    items(state.profileFriendAlias) { alias ->
                        Text(text = alias)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (state.profileFriendAlias.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = stringResource(R.string.dialog_message_friend_no_past_aliases))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPreviousAliasDialog = false },
                    content = { Text(text = stringResource(R.string.close)) },
                )
            },
        )
    }

    var showInternalBrowserDialog by rememberSaveable { mutableStateOf(false) }
    WebViewDialog(
        isVisible = showInternalBrowserDialog,
        url = SteamUtils.getProfileUrl(state.profileFriend!!.id),
        onDismissRequest = {
            showInternalBrowserDialog = false
        },
    )

    Scaffold(
        topBar = {
            // Show Top App Bar when in Compact or Medium screen space.
            if (windowWidth == WindowWidthSizeClass.COMPACT || windowWidth == WindowWidthSizeClass.MEDIUM) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_profile),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                )
            }
        },
    ) { paddingValues ->
        val uriHandler = LocalUriHandler.current
        val isLight = MaterialTheme.colorScheme.background.isLight()
        var moreExpanded by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            CoilImage(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .size(92.dp),
                imageModel = { SteamUtils.getAvatarURL(state.profileFriend.avatarHash) },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                ),
                loading = { CircularProgressIndicator() },
                failure = { Icon(Icons.Filled.QuestionMark, null) },
                previewPlaceholder = painterResource(R.drawable.ic_logo_color),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = state.profileFriend.nameOrNickname,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineLarge,
            )

            Text(
                text = state.profileFriend.isPlayingGameName,
                color = if (isLight) Color.Unspecified else state.profileFriend.statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileButton(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    text = stringResource(R.string.chat),
                    onClick = { onChat(state.profileFriend.id) },
                )
                Spacer(modifier = Modifier.width(16.dp))
                ProfileButton(
                    icon = Icons.Outlined.Person,
                    text = stringResource(R.string.profile),
                    onClick = {
                        if (PrefManager.openWebLinksExternally) {
                            uriHandler.openUri(SteamUtils.getProfileUrl(state.profileFriend.id))
                        } else {
                            showInternalBrowserDialog = true
                        }
                    },
                )
                Spacer(modifier = Modifier.width(16.dp))
                ProfileButton(
                    icon = Icons.Outlined.Games,
                    text = stringResource(R.string.games),
                    onClick = onShowGames,
                )
                Spacer(modifier = Modifier.width(16.dp))
                ProfileButton(
                    icon = Icons.Outlined.MoreVert,
                    text = stringResource(R.string.more),
                    onClick = { moreExpanded = !moreExpanded },
                )
            }

            AnimatedVisibility(visible = moreExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileButton(
                            icon = Icons.Outlined.History,
                            text = stringResource(R.string.friend_view_aliases),
                            onClick = {
                                onAlias()
                                showPreviousAliasDialog = true
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.Edit,
                            text = stringResource(R.string.friend_set_nickname),
                            onClick = {
                                setNickNameDialog = true
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.PersonOff,
                            text = stringResource(R.string.friend_block),
                            onClick = {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.FRIEND_BLOCK,
                                    confirmBtnText = R.string.block,
                                    dismissBtnText = R.string.cancel,
                                    title = R.string.dialog_title_friend_block,
                                    message = context.getString(R.string.dialog_message_friend_block, state.profileFriend.nameOrNickname),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.PersonRemove,
                            text = stringResource(R.string.dialog_title_friend_remove),
                            onClick = {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.FRIEND_REMOVE,
                                    confirmBtnText = R.string.remove,
                                    dismissBtnText = R.string.cancel,
                                    title = R.string.friend_remove,
                                    message = context.getString(R.string.dialog_message_friend_remove, state.profileFriend.nameOrNickname),
                                )
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileButton(
                            icon = Icons.Outlined.Favorite,
                            text = stringResource(R.string.friend_add_favorite),
                            onClick = {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.FRIEND_FAVORITE,
                                    confirmBtnText = R.string.favorite,
                                    dismissBtnText = R.string.cancel,
                                    title = R.string.dialog_title_friend_favorite,
                                    message = context.getString(
                                        R.string.dialog_message_friend_favorite,
                                        state.profileFriend.nameOrNickname,
                                    ),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ProfileButton(
                            icon = Icons.Outlined.Notifications,
                            text = stringResource(R.string.friend_set_alerts),
                            onClick = {
                                Toast.makeText(context, "Notifications TODO", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (state.profileFriendInfo == null) {
                        LoadingScreen()
                    } else {
                        // 'headline' doesn't seem to be used anymore
                        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                            // Meh...
                            with(state.profileFriendInfo) {
                                // Steam launch: Sept 12, 2003
                                val isValid = timeCreated.after(Date(1063267200000L))
                                if (isValid) {
                                    if (realName.isNotEmpty()) {
                                        Text(text = stringResource(R.string.friend_profile_name, realName))
                                    }
                                    if (cityName.isNotEmpty()) {
                                        Text(text = stringResource(R.string.friend_profile_city, cityName))
                                    }
                                    if (stateName.isNotEmpty()) {
                                        Text(text = stringResource(R.string.friend_profile_state, stateName))
                                    }
                                    if (stateName.isNotEmpty()) {
                                        Text(text = stringResource(R.string.friend_profile_country, countryName))
                                    }
                                    Text(text = stringResource(R.string.friend_profile_created, timeCreated))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = stringResource(R.string.friend_profile_summary))
                                    BBCodeText(text = summary)
                                } else {
                                    Text(stringResource(R.string.friend_profile_private))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom scroll padding
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

internal class FriendsScreenPreview : PreviewParameterProvider<ThreePaneScaffoldRole> {
    override val values = sequenceOf(ListDetailPaneScaffoldRole.List, ListDetailPaneScaffoldRole.Detail)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:parent=pixel_5,orientation=landscape",
)
@Composable
private fun Preview_FriendsScreenContent(
    @PreviewParameter(FriendsScreenPreview::class) state: ThreePaneScaffoldRole,
) {
    val context = LocalContext.current
    PrefManager.init(context)

    val navigator = rememberListDetailPaneScaffoldNavigator(
        initialDestinationHistory = listOf(ThreePaneScaffoldDestinationItem<Unit>(state)),
    )

    PluviaTheme {
        Surface {
            FriendsScreenContent(
                navigator = navigator,
                state = FriendsState(
                    friendsList = mapOf(
                        R.string.friend_sticky_game to fakeSteamFriends(),
                        R.string.friend_sticky_online to fakeSteamFriends(id = 5, inGame = false),
                        R.string.friend_sticky_offline to fakeSteamFriends(id = 10, online = false, inGame = false),
                    ),
                    profileFriend = fakeSteamFriends()[1],
                    profileFriendInfo = ProfileInfoCallback(
                        result = EResult.OK,
                        steamID = SteamID(123L),
                        timeCreated = Date(9988776655 * 1000L),
                        realName = "Friend Name",
                        cityName = "Friend Town",
                        stateName = "Friend State",
                        countryName = "Friend Country",
                        headline = "",
                        summary = "[emoticon]roar[/emoticon] Very nice profile! ːsteamboredː ːsteamthisː",
                    ),
                ),
                onFriendClick = { },
                onHeaderAction = { },
                onBack = { },
                onSettings = { },
                onLogout = { },
                onChat = { },
                onBlock = { },
                onRemove = { },
                onNickName = { },
                onAlias = { },
            )
        }
    }
}
