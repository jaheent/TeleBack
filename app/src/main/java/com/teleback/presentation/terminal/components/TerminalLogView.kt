package com.teleback.presentation.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleback.core.cli.LogLevel
import com.teleback.core.cli.TerminalLog

@Composable
fun TerminalLogView(
    logs: List<TerminalLog>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= (total - 3).coerceAtLeast(0)
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && isNearBottom) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    val scrollInfo by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo.size
            if (totalItems == 0 || visibleItems >= totalItems) {
                null
            } else {
                val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                val thumbRatio = (visibleItems.toFloat() / totalItems.toFloat()).coerceIn(0.1f, 1f)
                val scrollRatio = (firstVisibleItem.toFloat() / (totalItems - visibleItems).toFloat()).coerceIn(0f, 1f)
                thumbRatio to scrollRatio
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs, key = { it.id }) { log ->
                LogItemRow(log = log)
            }
        }

        // Compact Retro Scrollbar
        scrollInfo?.let { (thumbRatio, scrollRatio) ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(1.5.dp))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val totalHeight = maxHeight
                    val thumbHeight = (totalHeight * thumbRatio).coerceAtLeast(16.dp)
                    val availableOffset = totalHeight - thumbHeight
                    val offsetY = availableOffset * scrollRatio

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(thumbHeight)
                            .graphicsLayer {
                                translationY = offsetY.toPx()
                            }
                            .background(colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(log: TerminalLog) {
    val colorScheme = MaterialTheme.colorScheme
    val tagColor = when (log.level) {
        LogLevel.OK, LogLevel.CMD -> colorScheme.primary
        LogLevel.INFO, LogLevel.SYNC -> colorScheme.secondary
        LogLevel.SNAP -> colorScheme.secondary
        else -> log.level.color
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = log.formattedTime,
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = log.level.tag,
                color = tagColor,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = log.message,
                color = colorScheme.onSurface,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }

        if (log.progress != null) {
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { log.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        }
    }
}
