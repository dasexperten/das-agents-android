package com.dasexperten.agents.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.ui.components.InitialsStrip
import com.dasexperten.agents.ui.components.MessageBubble
import com.dasexperten.agents.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val visible = viewModel.visibleMessages()
    val agentsBySlug = remember(state.agents) { state.agents.associateBy { it.slug } }
    val focusAgent: Agent? = state.agents.firstOrNull { it.slug == state.focusSlug }
        ?: state.agents.firstOrNull()
    val bgUrl = focusAgent?.fullPhotoUrl ?: focusAgent?.photoUrl

    LaunchedEffect(visible.size, state.sending) {
        if (visible.isNotEmpty()) {
            listState.animateScrollToItem(visible.lastIndex)
        }
    }

    LaunchedEffect(state.focusSlug, state.filterMode) {
        val focus = state.focusSlug ?: return@LaunchedEffect
        val idx = visible.indexOfLast { it.agentSlug == focus }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    Box(Modifier.fillMaxSize()) {
        // Background: agent photo + white semi-transparent scrim
        if (bgUrl != null) {
            AsyncImage(
                model = bgUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.72f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Surface(color = Color.White.copy(alpha = 0.55f)) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                        Text(
                            text = state.agents.joinToString(" + ") { it.name },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.loadingHistory) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                    InitialsStrip(
                        agents = state.agents,
                        focusSlug = state.focusSlug,
                        filterMode = state.filterMode,
                        pendingSlugs = state.pendingSlugs,
                        onSelect = viewModel::focusAgent,
                    )
                    if (state.filterMode && state.focusSlug != null) {
                        Text(
                            text = "фильтр · ${focusAgent?.initials ?: ""} · ещё раз для всех",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                        )
                    }
                }
            }

            if (state.error != null && visible.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(visible, key = { it.id }) { msg ->
                        val photo = msg.agentSlug?.let { agentsBySlug[it]?.photoUrl }
                        MessageBubble(
                            message = msg,
                            photoUrl = photo,
                            highlighted = state.focusSlug != null && msg.agentSlug == state.focusSlug,
                        )
                    }
                }
            }

            // Input: ~4 lines, send on the right
            Surface(
                color = Color.White.copy(alpha = 0.88f),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = viewModel::onDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp, max = 120.dp),
                        placeholder = { Text("Сообщение…") },
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.9f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.75f),
                        ),
                    )
                    IconButton(
                        onClick = viewModel::send,
                        enabled = state.draft.isNotBlank() && !state.sending,
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 4.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.draft.isNotBlank() && !state.sending) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (state.draft.isNotBlank() && !state.sending) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
