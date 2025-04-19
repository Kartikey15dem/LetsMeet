package com.myworldtech.meet.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
//import com.microsoft.identity.client.AuthenticationCallback
//import com.microsoft.identity.client.IAuthenticationResult
//import com.microsoft.identity.client.PublicClientApplication
//import com.microsoft.identity.client.IPublicClientApplication
//import com.microsoft.identity.client.ISingleAccountPublicClientApplication
//import com.microsoft.identity.client.exception.MsalException
import com.myworldtech.meet.R

class AuthService(private val activity: Activity){
    private val auth = FirebaseAuth.getInstance()
    fun createAccountWithEmail(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Account creation successful
                    onSuccess()
                } else {
                    // Account creation failed
                    onFailure(task.exception?.message ?: "Unknown error occurred")
                }
            }
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleGoogleSignInResult(
        account: GoogleSignInAccount?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Google Sign-In failed")
                }
            }
    }
//    private lateinit var publicClientApp: PublicClientApplication

//    fun configureMicrosoftSignIn(context: Context) {
//        PublicClientApplication.createSingleAccountPublicClientApplication(
//            context,
//            R.raw.auth_config_single_account, // Replace with your JSON configuration file
//            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
//                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
//                    publicClientApp = application as PublicClientApplication
//                }
//
//                override fun onError(exception: MsalException) {
//                    // Handle initialization error
//                    exception.printStackTrace()
//                }
//            }
//        )
//    }
//    fun createAccountWithMicrosoft(
//        onSuccess: (String) -> Unit,
//        onFailure: (String) -> Unit
//    ) {
//        val scopes = arrayOf("User.Read") // Define the required scopes
//        publicClientApp.acquireToken(activity, scopes, object : AuthenticationCallback {
//            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
//                val accessToken = authenticationResult?.accessToken
//                if (accessToken != null) {
//                    // Use the access token to authenticate with your backend or Firebase
//                    onSuccess("Microsoft account linked successfully")
//                } else {
//                    onFailure("Access token is null")
//                }
//            }
//
//            override fun onError(exception: MsalException?) {
//                onFailure(exception?.message ?: "Microsoft Sign-In failed")
//            }
//
//            override fun onCancel() {
//                onFailure("Microsoft Sign-In canceled")
//            }
//        })
//    }
    fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login successful
                    onSuccess()
                } else {
                    // Login failed
                    onFailure(task.exception?.message ?: "Unknown error occurred")
                }
            }
    }

}