package com.myworldtech.meet.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.myworldtech.meet.data.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {
    // Private mutable variables
    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _userName = MutableStateFlow("")

    val email: StateFlow<String> get() = _email
    val password: StateFlow<String> get() = _password
    val userName: StateFlow<String> get() = _userName

    fun setEmail(newEmail: String) {
        _email.value = newEmail
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
    }

    fun setUserName(username: String) {
        _userName.value = username
    }

    fun createThroughEmail(authService: AuthService, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        authService.createAccountWithEmail(_email.value, _password.value,onSuccess, onFailure)
    }
    fun loginThroughEmail(authService: AuthService, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        authService.loginWithEmail(_email.value, _password.value,onSuccess, onFailure)
    }

}