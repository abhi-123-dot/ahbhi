package com.example.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ChatBubble
import com.example.ui.components.ChatSidebarContent
import com.example.ui.components.QuickPromptChips
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onVoiceInputClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val bookmarkedMessages by viewModel.bookmarkedMessages.collectAsStateWithLifecycle()
    val recentTitles by viewModel.recentChatTitles.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Filter messages by active search query
    val displayMessages = remember(allMessages, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            allMessages
        } else {
            allMessages.filter { it.text.contains(uiState.searchQuery, ignoreCase = true) }
        }
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    // Show floating scroll-to-bottom button when scrolled up
    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) false
            else {
                val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItemIndex < totalItems - 2
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatSidebarContent(
                recentTitles = recentTitles,
                searchQuery = uiState.searchQuery,
                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                onNewChatClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel.startNewChat()
                    }
                },
                onRecentTitleClick = { title ->
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel.sendMessage(overrideText = title)
                    }
                },
                onOpenBookmarks = {
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel.setShowBookmarkDialog(true)
                    }
                },
                onOpenSettings = {
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel.setShowSettingsDialog(true)
                    }
                },
                onOpenAbout = {
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel.setShowAboutDialog(true)
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open sidebar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_abhi_ai_icon),
                                contentDescription = "ABHI AI Logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ABHI AI",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00F0FF))
                                    )
                                }
                                Text(
                                    text = "Ask Anything. Learn Everything.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // New Chat Quick Action Button
                        IconButton(
                            onClick = { viewModel.startNewChat() },
                            modifier = Modifier.testTag("topbar_new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { viewModel.setShowSettingsDialog(true) },
                            modifier = Modifier.testTag("topbar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Bookmarks Button
                        IconButton(
                            onClick = { viewModel.setShowBookmarkDialog(true) },
                            modifier = Modifier.testTag("bookmarks_action_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (bookmarkedMessages.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${bookmarkedMessages.size}",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "Saved answers"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Category & Quick Prompt Suggestions
                QuickPromptChips(
                    selectedCategoryName = uiState.selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) },
                    onPromptClick = { prompt, category ->
                        viewModel.sendQuickPrompt(prompt, category)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Scrollable List of Messages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chat_message_list")
                    ) {
                        // Hero Banner shown on fresh or minimal chat history
                        if (displayMessages.size <= 2 && uiState.searchQuery.isBlank()) {
                            item {
                                BrandHeroBanner(
                                    onFeatureClick = { prompt, category ->
                                        viewModel.sendQuickPrompt(prompt, category)
                                    }
                                )
                            }
                        }

                        items(displayMessages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                isSpeaking = uiState.speakingMessageId == message.id,
                                fontSizeScale = uiState.fontSizeScale,
                                onToggleBookmark = { viewModel.toggleBookmark(message) },
                                onSpeak = { viewModel.speakMessage(message) },
                                onDelete = { viewModel.deleteMessage(message.id) }
                            )
                        }

                        // Typing / Thinking Indicator
                        if (uiState.isLoading) {
                            item {
                                TypingIndicator()
                            }
                        }
                    }

                    // Floating Scroll-to-bottom FAB
                    if (showScrollToBottom && displayMessages.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(displayMessages.size - 1)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 12.dp)
                                .testTag("scroll_to_bottom_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to newest message"
                            )
                        }
                    }
                }

                // Chat Input Bar
                ChatInputBar(
                    inputText = uiState.inputText,
                    isLoading = uiState.isLoading,
                    onTextChanged = { viewModel.onInputTextChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    onVoiceInputClick = onVoiceInputClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onToggleDarkMode = { viewModel.toggleDarkMode() },
            onFontSizeSelected = { viewModel.setFontSizeScale(it) },
            onThemeSelected = { viewModel.setAccentTheme(it) },
            onExportChat = { viewModel.exportChat(context) },
            onClearChat = {
                viewModel.setShowSettingsDialog(false)
                showClearConfirmDialog = true
            },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    // Bookmarks Dialog
    if (uiState.showBookmarkDialog) {
        BookmarksDialog(
            bookmarkedMessages = bookmarkedMessages,
            onDismiss = { viewModel.setShowBookmarkDialog(false) },
            onRemoveBookmark = { viewModel.toggleBookmark(it) }
        )
    }

    // About Dialog
    if (uiState.showAboutDialog) {
        AboutDialog(
            onDismiss = { viewModel.setShowAboutDialog(false) }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This will permanently remove all current messages and reset the conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showClearConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Clear All", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "pulse_typing")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_abhi_ai_icon),
            contentDescription = "Abhi AI Typing",
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Abhi AI is typing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Animated glowing dots
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00F0FF).copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = (alpha + 0.3f).coerceAtMost(1f)))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA855F7).copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInputClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChanged,
            placeholder = {
                Text(
                    text = "Ask Abhi AI anything...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("chat_input_field"),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { onSend() }
            ),
            trailingIcon = {
                // Mic voice button
                IconButton(
                    onClick = onVoiceInputClick,
                    modifier = Modifier.testTag("mic_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank() && !isLoading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .testTag("send_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (inputText.isNotBlank() && !isLoading) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BrandHeroBanner(
    onFeatureClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("brand_hero_banner")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_abhi_ai_banner),
                    contentDescription = "ABHI AI Hero Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Cyber neon gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ABHI AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Ask Anything. Learn Everything.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Text(
                text = "Modern AI assistant for studies, coding, gaming, creators, fitness, research & daily productivity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
