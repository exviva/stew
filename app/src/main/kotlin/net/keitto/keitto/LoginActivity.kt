package net.keitto.keitto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.activity_login.logInButton
import kotlinx.android.synthetic.activity_login.passwordEditText
import kotlinx.android.synthetic.activity_login.userNameEditText

class LoginActivity() : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        logInButton.setOnClickListener {
            val userName = userNameEditText.getText().toString()
            val password = passwordEditText.getText().toString()
            val application = getApplication() as Application
            application.api.logIn(userName, password) { userIdCookie, sessionIdCookie, csrfToken ->
                application.setCurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
                val intent = Intent(this, javaClass<MainActivity>())
                startActivity(intent)
                finish()
            }
        }
    }

}