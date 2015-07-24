package net.stew.stew

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import kotlinx.android.synthetic.activity_login.logInButton
import kotlinx.android.synthetic.activity_login.passwordEditText
import kotlinx.android.synthetic.activity_login.userNameEditText

class LoginActivity() : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        userNameEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        logInButton.setOnClickListener { logIn() }
    }

    private fun logIn() {
        val userName = userNameEditText.getText().toString()
        val password = passwordEditText.getText().toString()
        val application = getApplication() as Application
        val errorListener = {
            Toast.makeText(this, R.string.network_error_toast, Toast.LENGTH_SHORT).show()
        }
        application.api.logIn(userName, password, errorListener) { userIdCookie, sessionIdCookie, csrfToken ->
            application.setCurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
            val intent = Intent(this, javaClass<MainActivity>())
            startActivity(intent)
            finish()
        }
    }

    private val textWatcher = object: TextWatcher {

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val userNamePresent = userNameEditText.getText().length() > 0
            val passwordPresent = passwordEditText.getText().length() > 0
            logInButton.setEnabled(userNamePresent && passwordPresent)
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

}