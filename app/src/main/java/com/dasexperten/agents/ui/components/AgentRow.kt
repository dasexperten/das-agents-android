package com.dasexperten.agents.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dasexperten.agents.model.Agent

/**
 * Every card is the same size.
 * Name block is always exactly 2 lines (short names leave empty second line).
 */
private val CardHeight = 172.dp
private val PhotoSize = 80.dp
private val NameLineHeight = 20.sp
private val NameBlockHeight = 40.dp // exactly 2 lines

@Composable
fun AgentTile(
    agent: Agent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
    val nameStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = NameLineHeight,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(CardHeight)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = agent.photoUrl,
                    contentDescription = agent.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PhotoSize)
                        .clip(CircleShape)
                        .border(3.dp, borderColor, CircleShape),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Always 2 lines for every agent — short names keep blank second line.
            Text(
                text = agent.name,
                style = nameStyle,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NameBlockHeight),
            )
        }
    }
}
