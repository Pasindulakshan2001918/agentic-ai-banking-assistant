package com.agenticbank.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.ActivityRegisterBinding
import com.agenticbank.ui.dashboard.MainActivity
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = BankRepository(this)
        session = SessionManager(this)

        supportActionBar?.title = "Create Account"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnRegister.setOnClickListener { doRegister() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun doRegister() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
            showToast("Please fill in all fields")
            return
        }
        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return
        }
        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }

        binding.progressBar.visible()
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = repo.register(username, password, email, fullName)
                if (response.isSuccessful) {
                    val auth = response.body()!!
                    session.saveSession(
                        token = auth.token,
                        username = auth.username,
                        accountId = auth.accountId,
                        userId = auth.userId,
                        role = auth.role
                    )
                    showToast("Account created successfully!")
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()
                } else {
                    showToast("Registration failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.btnRegister.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
