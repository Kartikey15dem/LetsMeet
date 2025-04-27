package com.myworldtech.meet.presentation.componenets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column {
        Button(onClick = {}) {
            Text(text = "New Meeting")
        }
        Button(onClick = onClick) {
            Text(text = "Join Meeting")
        }
    }
}