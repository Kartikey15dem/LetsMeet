package com.myworldtech.meet.presentation.componenets.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.myworldtech.meet.R
import com.myworldtech.meet.data.auth.AuthService
import com.myworldtech.meet.ui.theme.MeetTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    authService: AuthService,
    onEmailLogin: () -> Unit
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
//                    Toast.makeText(
//                        context,
//                        "Google Sign-In Successful",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    onLoginSuccess.invoke()
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
    val isDarkTheme = isSystemInDarkTheme()
    val darkComposition = rememberLottieComposition(LottieCompositionSpec.Asset("facetimeL.json"))
    val lightComposition = rememberLottieComposition(LottieCompositionSpec.Asset("facetimeL.json"))

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){

        LottieAnimation(
            modifier = Modifier
                .size(300.dp)
                .padding(8.dp),
            composition = if(isDarkTheme) darkComposition.value else lightComposition.value,
            iterations = Int.MAX_VALUE
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = "Let's Join In",
            fontSize = 24.sp,
            color = if (isSystemInDarkTheme()) Color.White else Color.Black,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(6.dp))
        SignInButton(
            onClick = {
                val signInIntent = authService.getGoogleSignInIntent()
                launcher.launch(signInIntent)
            },
            text = "Login with Google",
            painter = painterResource(id = R.drawable.googleicon)
        )
        Spacer(modifier = Modifier.size(6.dp))
        SignInButton(
            onClick = onEmailLogin,
            text = "Login with Email",
            painter = painterResource(id = R.drawable.baseline_email_24)
        )
        Spacer(modifier = Modifier.size(6.dp))
        SignInButton(
            onClick = {

            },
            text = "Login with Microsoft",
            painter = painterResource(id = R.drawable.microsofticon)
        )
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = "Don't have an account?",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(8.dp))
        Button(
            onClick =  onCreateAccount ,
            modifier = Modifier.padding(8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 16.sp
            )
        }


    }
}

@Composable
fun SignInButton(
    onClick: () -> Unit,
    text : String,
    painter: Painter
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(8.dp)
            .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
                ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, Color.Black.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painter,
                contentDescription = "Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                color = Color.Black,
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun Login(modifier: Modifier = Modifier) {
    MeetTheme {
//        LoginScreen() { }
    }
}