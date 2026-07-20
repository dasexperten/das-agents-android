package com.dasexperten.agents.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dasexperten.agents.update.UpdateUiState

@Composable
fun UpdateBanner(
    state: UpdateUiState,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.available && !state.checking && !state.downloading && state.error == null) return

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            when {
                state.checking -> Text("Проверка обновления…")
                state.downloading -> {
                    Text("Скачиваю обновление ${state.versionName}…")
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
                state.available -> {
                    Text(
                        text = "Доступна версия ${state.versionName}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (state.notes.isNotBlank()) {
                        Text(
                            text = state.notes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = onInstall, enabled = state.readyPath != null) {
                            Text(if (state.readyPath != null) "Установить" else "Готовится…")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Позже")
                        }
                    }
                }
                state.error != null -> {
                    Text(
                        text = "Обновление: ${state.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
