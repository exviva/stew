package net.keitto.keitto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
    }

}