package com.dasexperten.agents.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dasexperten.agents.model.Agent

@Composable
fun InitialsStrip(
    agents: List<Agent>,
    focusSlug: String?,
    filterMode: Boolean,
    pendingSlugs: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        agents.forEach { agent ->
            val focused = focusSlug == agent.slug
            val pending = agent.slug in pendingSlugs
            val label = buildString {
                append(agent.initials)
                if (pending) append("…")
                if (filterMode && focused) append(" ·")
            }
            FilterChip(
                selected = focused,
                onClick = { onSelect(agent.slug) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (filterMode && focused) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    selectedLabelColor = if (filterMode && focused) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                ),
            )
        }
    }
}
