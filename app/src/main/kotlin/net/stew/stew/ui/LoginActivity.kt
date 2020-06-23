package net.stew.stew.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.stew.stew.Api
import net.stew.stew.Application
import net.stew.stew.R
import net.stew.stew.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater).apply {
            userNameEditText.setText((application as Application).previousUserName)
            userNameEditText.addTextChangedListener(textWatcher)
            passwordEditText.addTextChangedListener(textWatcher)
            logInButton.setOnClickListener { logIn() }

            if (userNameEditText.text.isNotEmpty()) {
                passwordEditText.requestFocus()
            }

            setContentView(root)
        }
    }

    private fun logIn() = binding.apply {
        val userName = userNameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val application = application as Application

        logInButton.isEnabled = false
        logInButton.setText(R.string.logging_in)

        lifecycleScope.launch {
            when (val res = application.api.logIn(userName, password)) {
                is Api.Response.Success -> {
                    val (userIdCookie, sessionIdCookie, csrfToken) = res.data

                    if (userIdCookie != null && sessionIdCookie != null && csrfToken != null) {
                        application.setCurrentSession(userName, userIdCookie, sessionIdCookie, csrfToken)
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        handleResponseError(getString(R.string.invalid_credentials))
                    }
                }
                is Api.Response.Error -> handleResponseError(getString(R.string.network_error, res.details))
            }
        }
    }

    private fun handleResponseError(message: String) = binding.apply {
        logInButton.isEnabled = true
        logInButton.setText(R.string.log_in)
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
    }

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            binding.apply {
                val userNamePresent = userNameEditText.text.isNotEmpty()
                val passwordPresent = passwordEditText.text.isNotEmpty()
                logInButton.isEnabled = userNamePresent && passwordPresent
            }
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

}