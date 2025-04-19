package com.myworldtech.meet.presentation.componenets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column {
        Button(onClick = { /* Handle button click */ }) {
            Text(text = "New Meeting")
        }
        Button(onClick = { /* Handle button click */ }) {
            Text(text = "Join Meeting")
        }
    }
}