package com.myworldtech.meet.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
//import com.microsoft.identity.client.AuthenticationCallback
//import com.microsoft.identity.client.IAuthenticationResult
//import com.microsoft.identity.client.PublicClientApplication
//import com.microsoft.identity.client.IPublicClientApplication
//import com.microsoft.identity.client.ISingleAccountPublicClientApplication
//import com.microsoft.identity.client.exception.MsalException
import com.myworldtech.meet.R
import com.myworldtech.meet.data.preferences.UserPrefsKeys
import com.myworldtech.meet.data.preferences.dataStore
import com.myworldtech.meet.data.preferences.saveUserLocally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                    if(account !=null){
                        val user = User(account.displayName.toString(), account.photoUrl.toString(),
                        account.email.toString()
                    )
                        Log.d("auth", "loginsucc")
                        saveUserToFirestoreAndLocal(activity, user, onSuccess, onFailure)
                    }
                } else {
                    onFailure(task.exception?.message ?: "Google Sign-In failed")
                }
            }
    }

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
    // User data class


    // Save user to Firestore and locally
    fun saveUserToFirestoreAndLocal(
        context: Context,
        user: User,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        Log.d("auth", "save")
        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentRef ->
                val peerId = documentRef.id
                // Save locally
                Log.d("auth", "Firestore")
                CoroutineScope(Dispatchers.IO).launch {
                    saveUserLocally(context, user.name,
                        user.profilePhotoUrl.toString(), peerId, user.email)
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.toString())
                Log.d("auth", "$e")
            }
    }


}
data class User(
    val name: String,
    val profilePhotoUrl: String?,
    val email: String
)