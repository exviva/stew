package net.keitto.keitto

import android.app.Activity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.activity_login.*

class LoginActivity() : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
    }

    fun logIn(v: View) {
        val userName = userNameEditText.getText().toString()
        val password = passwordEditText.getText().toString()
        val application = getApplication() as Application
        application.api.logIn(userName, password) { userIdCookie, sessionIdCookie, csrfToken ->
            application.setCurrentSession(userIdCookie, sessionIdCookie, csrfToken)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

}