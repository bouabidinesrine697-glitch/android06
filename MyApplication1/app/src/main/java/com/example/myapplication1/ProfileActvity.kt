package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.databinding.ActivityProfilBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadProfile()
        loadStats()
        setupClickListeners()
    }

    private fun loadProfile() {
        val firstName = TokenManager.getFirstName(this) ?: ""
        val lastName  = TokenManager.getLastName(this)  ?: ""

        binding.tvProfileName.text  = "$firstName $lastName"
        binding.tvProfileEmail.text = TokenManager.getEmail(this) ?: "-"

        val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
            .uppercase()
        binding.tvAvatar.text = initials.ifEmpty { "?" }
    }

    private fun loadStats() {
        val stations = StationRepository.getStations()
        binding.tvStationsCount.text = "${stations.size}"
        binding.tvBikesCount.text    = "${stations.sumOf { it.bikes }}"
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vraiment vous déconnecter ?")
            .setPositiveButton("Déconnecter") { _, _ ->
                TokenManager.logout(this)
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}