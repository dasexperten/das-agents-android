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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.ui.components.AgentTile
import com.dasexperten.agents.viewmodel.RosterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    viewModel: RosterViewModel,
    onStartChat: (List<Agent>) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedCount = state.selectedSlugs.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Агенты") },
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
                            text = if (selectedCount == 1) {
                                "Чат · 1 агент"
                            } else {
                                "Чат · $selectedCount агента"
                            }
                        )
                    }
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading && state.agents.isNotEmpty(),
            onRefresh = viewModel::refresh,
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
                        Text(state.error ?: "", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = viewModel::refresh) {
                            Text("Повторить")
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.agents, key = { it.slug }) { agent ->
                            AgentTile(
                                agent = agent,
                                selected = agent.slug in state.selectedSlugs,
                                onClick = { viewModel.toggle(agent.slug) },
                            )
                        }
                    }
                }
            }
        }
    }
}
