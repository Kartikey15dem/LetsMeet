package com.myworldtech.meet.presentation.componenets

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myworldtech.meet.R
import com.myworldtech.meet.data.preferences.getUserInfo
import kotlin.text.compareTo


@Composable
fun HomeScreen(
   navController: NavController
) {
    PermissionRequester()
    val context = LocalContext.current
    var joinCode by rememberSaveable { mutableStateOf("") }
    var userName by rememberSaveable { mutableStateOf("") }
    var userPhotoUrl by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val userInfo = getUserInfo(context)
        userName = userInfo.first.toString()
        userPhotoUrl = userInfo.second.toString()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Let’s meet others",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = userPhotoUrl,
                contentDescription = "User Photo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                placeholder = painterResource(id = R.drawable.account_circle_24)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Button(
            onClick = {
                val meetingCode = Uri.encode(generateMeetingCode())
                val name = Uri.encode(userName)
                val photoUrl = Uri.encode(userPhotoUrl)
                val isAskToJoin = false.toString()
                navController.navigate("meetingPreview/$meetingCode/$name/$photoUrl/$isAskToJoin")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.video_camera), contentDescription = "New Meeting", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New meeting", color = Color.White)
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it },
                placeholder = { Text("Enter a code or link") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(50)
            )

            TextButton(
                onClick = {
                    if (joinCode.isNotBlank()) {
                        val meetingCode = Uri.encode(joinCode)
                        val name = Uri.encode(userName)
                        val photoUrl = Uri.encode(userPhotoUrl)
                        val isAskToJoin = true.toString()
                        navController.navigate("meetingPreview/$meetingCode/$name/$photoUrl/$isAskToJoin")
                    }
                },
                enabled = joinCode.isNotBlank()
            ) {
                Text("Join")
            }
        }
    }
}


fun generateMeetingCode(): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    return List(3) {
        (1..3).map { chars.random() }.joinToString("")
    }.joinToString("-")
}


@Composable
fun PermissionRequester() {
    val context = LocalContext.current
    val activity = context as? Activity

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
     listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS

    )} else {
       listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allGranted = permissionsResult.all { it.value }
        if (allGranted) {

        } else {
            // You can show rationale or retry logic here
        }
    }

    LaunchedEffect(Unit) {
        // Ask only if not already granted
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {

        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

