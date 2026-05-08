package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication1.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Already logged in → go straight to map
        if (TokenManager.isLoggedIn(this)) {
            navigateToMap()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) login()
        }

        binding.tvForgotPassword.setOnClickListener {
            // TODO: navigate to ForgotPasswordActivity
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogle.setOnClickListener {
            // TODO: Google Sign-In
        }

        // Clear errors on typing
        binding.etEmail.setOnFocusChangeListener { _, _ ->
            binding.emailLayout.error = null
        }
        binding.etPassword.setOnFocusChangeListener { _, _ ->
            binding.passwordLayout.error = null
        }
    }

    private fun login() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        setLoading(true)
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(email = email, password = password)
                )

                if (response.isSuccessful && response.body() != null) {
                    TokenManager.saveAuth(this@LoginActivity, response.body()!!)
                    navigateToMap()
                } else {
                    showError("Email ou mot de passe incorrect")
                }

            } catch (e: Exception) {
                showError("Erreur réseau : vérifiez votre connexion")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        when {
            email.isEmpty() -> {
                binding.emailLayout.error = "Email requis"
                isValid = false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailLayout.error = "Email invalide"
                isValid = false
            }
            else -> binding.emailLayout.error = null
        }

        when {
            password.isEmpty() -> {
                binding.passwordLayout.error = "Mot de passe requis"
                isValid = false
            }
            password.length < 6 -> {
                binding.passwordLayout.error = "Min 6 caractères"
                isValid = false
            }
            else -> binding.passwordLayout.error = null
        }

        return isValid
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled      = !loading
        binding.progressBar.visibility  = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.text           = if (loading) "" else "SE CONNECTER"
        binding.btnLogin.icon           = if (loading) null else
            getDrawable(R.drawable.ic_lock)
    }

    private fun showError(message: String) {
        binding.tvError.text       = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun navigateToMap() {
        startActivity(Intent(this, MapActivity::class.java))
        finish()
    }
}