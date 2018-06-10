package net.stew.stew

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.logInButton
import kotlinx.android.synthetic.main.activity_login.passwordEditText
import kotlinx.android.synthetic.main.activity_login.userNameEditText

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        userNameEditText.setText((application as Application).previousUserName)
        userNameEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        logInButton.setOnClickListener { logIn() }

        if (userNameEditText.text.isNotEmpty()) {
            passwordEditText.requestFocus()
        }
    }

    private fun logIn() {
        val userName = userNameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val application = application as Application
        val errorListener: (ResponseStatus) -> Unit = {
            val details = if (it is ResponseStatus.ServerError) it.error.message else ""
            val msg = getString(R.string.network_error, details)
            handleResponseError(msg)
        }
        logInButton.isEnabled = false
        logInButton.setText(R.string.logging_in)
        application.api.logIn(userName, password, errorListener) { userIdCookie, sessionIdCookie, csrfToken ->
            if (userIdCookie != null && sessionIdCookie != null && csrfToken != null) {
                application.setCurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                handleResponseError(getString(R.string.invalid_credentials))
            }
        }
    }

    private fun handleResponseError(message: String) {
        logInButton.isEnabled = true
        logInButton.setText(R.string.log_in)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val textWatcher = object: TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val userNamePresent = userNameEditText.text.isNotEmpty()
            val passwordPresent = passwordEditText.text.isNotEmpty()
            logInButton.isEnabled = userNamePresent && passwordPresent
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

}