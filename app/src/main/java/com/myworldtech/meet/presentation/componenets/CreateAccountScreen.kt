package com.myworldtech.meet.presentation.componenets

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.myworldtech.meet.R
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.presentation.viewmodels.AuthViewModel
import com.myworldtech.meet.ui.theme.MeetTheme

@Composable
fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel,
    authService: AuthService,
    onAccountCreated: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            authService.handleGoogleSignInResult(
                account = account,
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Google Sign-In Successful",
                        Toast.LENGTH_SHORT
                    ).show()
                    onAccountCreated.invoke()
                },
                onFailure = { error ->
                    Toast.makeText(
                        context,
                        "Google Sign-In Error: $error",
                        Toast.LENGTH_LONG
                    ).show()
                })
        } catch (e: ApiException) {
            // Handle error
        }
    }
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val userName by viewModel.userName.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false)}
    var buttonEnabled by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Create Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email Input

            OutlinedTextField(
                value = userName,
                onValueChange = {
                    viewModel.setUserName(it)
                    if(userName.isNotBlank()) buttonEnabled = true else false
                },
                label = { Text("UserName") },
                placeholder = { Text("Enter your email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),

            )

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = {
                    viewModel.setEmail(it)
                    if(email.isNotBlank() && isValidEmail(email)) buttonEnabled = true else false
                },
                label = { Text("Email") },
                placeholder = { Text("Enter your email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = {
                    viewModel.setPassword(it)
                    if(password.isNotBlank()) buttonEnabled = true else false
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) "Hide" else "Show"
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(icon)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Button(
                onClick = { viewModel.createThroughEmail(authService,
                    onSuccess = onAccountCreated,
                    onFailure = { error ->
                        // Handle account creation failure
                    }
                )},
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = buttonEnabled
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 16.sp
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ){

                CreateAccountButton(
                    onClick = {
                        val signInIntent = authService.getGoogleSignInIntent()
                        launcher.launch(signInIntent)
                    },
                    text = " Google",
                    painter = painterResource(id = R.drawable.googleicon)
                )
                CreateAccountButton(
                    onClick = { },
                    text = " Microsoft",
                    painter = painterResource(id = R.drawable.microsofticon)
                )

            }
        }
    }
}
@Composable
fun CreateAccountButton(
    onClick: () -> Unit,
    text : String,
    painter: Painter
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, Color.Black.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painter,
                contentDescription = "Logo",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                color = Color.Black,
            )
        }
    }
}
fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    return email.matches(Regex(emailRegex))
}



@Composable
@Preview
fun CreatePreview(modifier: Modifier = Modifier) {
    MeetTheme {
//        CreateAccountScreen()
    }
}