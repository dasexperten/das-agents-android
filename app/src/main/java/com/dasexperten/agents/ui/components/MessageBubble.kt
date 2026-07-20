package com.dasexperten.agents.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dasexperten.agents.model.ChatMessage
import com.dasexperten.agents.model.ChatRole

@Composable
fun MessageBubble(
    message: ChatMessage,
    photoUrl: String?,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.User
    val isError = message.role == ChatRole.Error
    val isTyping = message.role == ChatRole.Typing

    val bubbleColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        isError -> MaterialTheme.colorScheme.errorContainer
        isTyping -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        highlighted -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = message.agentName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = message.initials ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            if (!isUser && !message.initials.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(message.initials, message.agentName).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp,
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}
