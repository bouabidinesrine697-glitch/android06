package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication1.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) register()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }

        binding.etFirstName.setOnFocusChangeListener { _, _ -> binding.firstNameLayout.error = null }
        binding.etLastName.setOnFocusChangeListener  { _, _ -> binding.lastNameLayout.error  = null }
        binding.etEmail.setOnFocusChangeListener     { _, _ -> binding.emailLayout.error     = null }
        binding.etPhone.setOnFocusChangeListener     { _, _ -> binding.phoneLayout.error     = null }
        binding.etPassword.setOnFocusChangeListener  { _, _ -> binding.passwordLayout.error  = null }
        binding.etConfirm.setOnFocusChangeListener   { _, _ -> binding.confirmLayout.error   = null }
    }

    private fun register() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName  = binding.etLastName.text.toString().trim()
        val email     = binding.etEmail.text.toString().trim()
        val phone     = binding.etPhone.text.toString().trim()
        val password  = binding.etPassword.text.toString()

        setLoading(true)
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(
                        firstName = firstName,
                        lastName  = lastName,
                        email     = email,
                        phone     = phone,
                        password  = password
                    )
                )

                if (response.isSuccessful) {
                    response.body()?.let { registerData ->
                        TokenManager.saveAuth(this@RegisterActivity, registerData) // ✅ Fixed
                        navigateToMap()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string()
                    showError(errorMsg ?: "Erreur lors de l'inscription")
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

        val firstName = binding.etFirstName.text.toString().trim()
        val lastName  = binding.etLastName.text.toString().trim()
        val email     = binding.etEmail.text.toString().trim()
        val phone     = binding.etPhone.text.toString().trim()
        val password  = binding.etPassword.text.toString()
        val confirm   = binding.etConfirm.text.toString()

        when {
            firstName.isEmpty() -> {
                binding.firstNameLayout.error = "Prénom requis"
                isValid = false
            }
            firstName.length < 2 -> {
                binding.firstNameLayout.error = "Min 2 caractères"
                isValid = false
            }
            else -> binding.firstNameLayout.error = null
        }

        when {
            lastName.isEmpty() -> {
                binding.lastNameLayout.error = "Nom requis"
                isValid = false
            }
            lastName.length < 2 -> {
                binding.lastNameLayout.error = "Min 2 caractères"
                isValid = false
            }
            else -> binding.lastNameLayout.error = null
        }

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
            phone.isEmpty() -> {
                binding.phoneLayout.error = "Téléphone requis"
                isValid = false
            }
            !Patterns.PHONE.matcher(phone).matches() -> {
                binding.phoneLayout.error = "Numéro invalide"
                isValid = false
            }
            else -> binding.phoneLayout.error = null
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
            !password.any { it.isDigit() } -> {
                binding.passwordLayout.error = "Doit contenir un chiffre"
                isValid = false
            }
            else -> binding.passwordLayout.error = null
        }

        when {
            confirm.isEmpty() -> {
                binding.confirmLayout.error = "Confirmation requise"
                isValid = false
            }
            confirm != password -> {
                binding.confirmLayout.error = "Les mots de passe ne correspondent pas"
                isValid = false
            }
            else -> binding.confirmLayout.error = null
        }

        return isValid
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled  = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.text       = if (loading) "" else "CRÉER MON COMPTE"
        binding.btnRegister.icon       = if (loading) null else getDrawable(R.drawable.ic_person)
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