package com.dasexperten.agents

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.ui.chat.ChatScreen
import com.dasexperten.agents.ui.components.UpdateBanner
import com.dasexperten.agents.ui.roster.RosterScreen
import com.dasexperten.agents.ui.theme.DasAgentsTheme
import com.dasexperten.agents.update.UpdateViewModel
import com.dasexperten.agents.viewmodel.ChatViewModel
import com.dasexperten.agents.viewmodel.RosterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DasAgentsTheme {
                AgentsRoot()
            }
        }
    }
}

private sealed interface Screen {
    data object Roster : Screen
    data class Chat(val agents: List<Agent>) : Screen
}

@Composable
private fun AgentsRoot() {
    val app = LocalContext.current.applicationContext as Application
    val rosterVm: RosterViewModel = viewModel(
        factory = RosterViewModel.factory(app),
    )
    val updateVm: UpdateViewModel = viewModel(
        factory = UpdateViewModel.factory(app),
    )
    val updateState by updateVm.state.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Roster) }
    var chatKey by rememberSaveable { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        UpdateBanner(
            state = updateState,
            onInstall = updateVm::installReady,
            onDismiss = updateVm::dismiss,
        )
        when (val s = screen) {
            Screen.Roster -> {
                RosterScreen(
                    viewModel = rosterVm,
                    onStartChat = { agents ->
                        if (agents.isNotEmpty()) {
                            chatKey += 1
                            screen = Screen.Chat(agents)
                        }
                    },
                )
            }
            is Screen.Chat -> {
                val chatVm: ChatViewModel = viewModel(
                    key = "chat-$chatKey-${s.agents.joinToString("-") { it.slug }}",
                    factory = ChatViewModel.factory(s.agents),
                )
                ChatScreen(
                    viewModel = chatVm,
                    onBack = { screen = Screen.Roster },
                )
            }
        }
    }
}
