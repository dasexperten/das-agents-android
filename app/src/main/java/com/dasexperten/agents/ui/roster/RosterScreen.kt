package com.dasexperten.agents.ui.roster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasexperten.agents.R
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.ui.components.AgentTile
import com.dasexperten.agents.viewmodel.RosterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    viewModel: RosterViewModel,
    onStartChat: (List<Agent>) -> Unit,
    onOpenDigest: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedCount = state.selectedSlugs.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.roster_title))
                        if (state.lastSyncLabel != null) {
                            Text(
                                text = stringResource(
                                    R.string.roster_subtitle_sync,
                                    state.lastSyncLabel ?: "",
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onOpenDigest) {
                        Text(stringResource(R.string.roster_digest))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (selectedCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Button(
                        onClick = { onStartChat(viewModel.selectedAgents()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(
                            text = when {
                                selectedCount == 1 -> stringResource(R.string.roster_chat_one)
                                selectedCount in 2..4 -> stringResource(
                                    R.string.roster_chat_few,
                                    selectedCount,
                                )
                                else -> stringResource(
                                    R.string.roster_chat_many,
                                    selectedCount,
                                )
                            }
                        )
                    }
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading && state.agents.isNotEmpty(),
            onRefresh = { viewModel.refresh(silent = false) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading && state.agents.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.agents.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            state.error ?: stringResource(R.string.roster_load_error),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.roster_retry))
                        }
                    }
                }
                else -> {
                    // 2 equal columns; each tile is fixed height + always 2 name lines.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = true,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.agents,
                            key = { it.slug },
                        ) { agent ->
                            AgentTile(
                                agent = agent,
                                selected = agent.slug in state.selectedSlugs,
                                onClick = { viewModel.toggle(agent.slug) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
