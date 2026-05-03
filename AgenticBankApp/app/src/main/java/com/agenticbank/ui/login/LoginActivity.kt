package com.agenticbank.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agenticbank.data.api.RetrofitClient
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.ActivityLoginBinding
import com.agenticbank.ui.dashboard.MainActivity
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = BankRepository(this)
        session = SessionManager(this)

        // Auto-login if token exists
        if (session.isLoggedIn()) {
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Please enter username and password")
            return
        }

        binding.progressBar.visible()
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = repo.login(username, password)
                if (response.isSuccessful) {
                    val auth = response.body()!!
                    session.saveSession(
                        token = auth.token,
                        username = auth.username,
                        accountId = auth.accountId,
                        userId = auth.userId,
                        role = auth.role
                    )
                    goToMain()
                } else {
                    showToast("Login failed: Invalid credentials")
                }
            } catch (e: Exception) {
                showToast("Connection error: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
