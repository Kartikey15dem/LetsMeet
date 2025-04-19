package com.myworldtech.meet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.presentation.componenets.CreateAccountScreen
import com.myworldtech.meet.presentation.viewmodels.AuthViewModel
import com.myworldtech.meet.ui.theme.MeetTheme
import androidx.activity.viewModels
import com.myworldtech.meet.presentation.componenets.AppNavigation

class MainActivity : ComponentActivity() {
    private lateinit var authService: AuthService
    private val authViewModel: AuthViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authService = AuthService(this)

        // Configure Google and Microsoft Sign-In
        authService.configureGoogleSignIn()
//        authService.configureMicrosoftSignIn(this)

        setContent {
            MeetTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    authService = authService
                )
            }
        }
    }

//    fun startMicrosoftSignIn(authService: AuthService) {
//        authService.createAccountWithMicrosoft(
//            onSuccess = { message ->
//                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
//            },
//            onFailure = { error ->
//                Toast.makeText(
//                    this@MainActivity,
//                    "Microsoft Sign-In Error: $error",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        )
//    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MeetTheme {
        Greeting("Android")
    }
}