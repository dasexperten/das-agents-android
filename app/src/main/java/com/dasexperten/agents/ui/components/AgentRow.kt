package com.dasexperten.agents.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dasexperten.agents.model.Agent

@Composable
fun AgentTile(
    agent: Agent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (selected) 0.96f else 1f, label = "press")
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = agent.photoUrl,
                contentDescription = agent.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(3.dp, borderColor, CircleShape),
            )
            Text(
                text = agent.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
