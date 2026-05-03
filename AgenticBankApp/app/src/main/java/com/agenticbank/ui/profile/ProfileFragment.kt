package com.agenticbank.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentProfileBinding
import com.agenticbank.ui.login.LoginActivity
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        loadProfile()

        binding.btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadProfile() {
        binding.progressBar.visible()
        // Show what we know from session first
        binding.tvUsername.text = session.getUsername() ?: "User"
        binding.tvAccountId.text = "Account ID: ${session.getAccountId() ?: "N/A"}"
        binding.tvRole.text = "Role: ${session.getRole() ?: "CUSTOMER"}"

        lifecycleScope.launch {
            try {
                val response = repo.getProfile()
                if (response.isSuccessful) {
                    val profile = response.body()!!
                    binding.tvUsername.text = profile.username
                    binding.tvEmail.text = profile.email
                    binding.tvFullName.text = profile.fullName ?: ""
                    binding.tvRole.text = "Role: ${profile.roles.joinToString()}"
                }
            } catch (e: Exception) {
                // Profile endpoint might require Keycloak for admins
                // Session data is sufficient for customers
            } finally {
                binding.progressBar.gone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
