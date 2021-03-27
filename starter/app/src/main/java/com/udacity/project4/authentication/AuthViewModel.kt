package com.udacity.project4.authentication

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import timber.log.Timber

//class AuthViewModel(private val context: Context, private val sharedPrefs: SharedPreferences) :
//    ViewModel() {
//
//    enum class AuthState {
//        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
//    }
//
//    private val currentUser = MutableLiveData<FirebaseUser>()
//    private val authState = MutableLiveData<AuthState>()
//
//        init {
//            FirebaseAuth.getInstance()
//        }
//
//    fun authState(): AuthState {
//        var authenticated: AuthState = AuthState.UNAUTHENTICATED
//
//        //check auth state
//        FirebaseAuth.AuthStateListener { firebaseAuth ->
//            Timber.d("CurrentUser is: ${firebaseAuth.currentUser}")
//
//            authenticated = if (firebaseAuth.currentUser != null)// signed-in user
//                AuthState.AUTHENTICATED
//            else
//                AuthState.UNAUTHENTICATED
//            authState.value = authenticated
//        }
//        return authenticated
//    }
//
//    fun setUserAuth(value: Boolean) {
//        sharedPrefs.edit().putBoolean("user_authentication_state", value).apply()
//    }
//    fun isUserAuthenticated() = sharedPrefs.getBoolean("user_authentication_state", false)
//}
class AuthViewModel : ViewModel() {
    enum class AuthenticationState {
        AUTHENTICATED,
        UNAUTHENTICATED,
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}