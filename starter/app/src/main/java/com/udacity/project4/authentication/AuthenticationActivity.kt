package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.Observer
import com.udacity.project4.R
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.utils.Variables
import kotlinx.android.synthetic.main.activity_authentication.*
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
//sign-in code (Random value)
private val SIGN_IN_REQUEST_CODE = 1030
class AuthenticationActivity : AppCompatActivity() {

    private val authenticationViewModel: AuthViewModel by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        auth_login_button.setOnClickListener {
            launchSignInFlow()
        }
        authenticationViewModel.authenticationState.observe(this, Observer<AuthViewModel.AuthenticationState>{ authenticationState ->
            when(authenticationState) {
                AuthViewModel.AuthenticationState.AUTHENTICATED -> startActivity(Intent(this, RemindersActivity::class.java))
                AuthViewModel.AuthenticationState.UNAUTHENTICATED -> Timber.e("Not authenticated!")
                else -> Timber.e(
                    "New $authenticationState state that doesn't require any UI change"
                )
            }
        })
    }

//          TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout



    private fun launchSignInFlow() {
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())

        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build(), SIGN_IN_REQUEST_CODE)
    }

}
