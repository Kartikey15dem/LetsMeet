package com.myworldtech.meet.presentation.componenets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myworldtech.meet.presentation.model.Participant

@Composable
fun ParticipantGridCell(
    modifier: Modifier,
    participant: Participant
) {
    // Using key ensures recomposition only for this specific participant
    key(participant) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Display participant video or placeholder based on videoEnabled
            if (participant.videoEnabled) {
                // Show video stream (placeholder for now)
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "${participant.name}'s Video")
                }
            } else {
                // Show avatar/placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = participant.name.first().toString())
                }
            }

            // Status indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                // Audio indicator
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(24.dp)
                        .height(24.dp)
                ) {
                    Text(text = if (participant.audioEnabled) "🎤" else "🔇")
                }

                // Video indicator
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(24.dp)
                        .height(24.dp)
                ) {
                    Text(text = if (participant.videoEnabled) "📹" else "🚫")
                }
            }
        }
    }
}