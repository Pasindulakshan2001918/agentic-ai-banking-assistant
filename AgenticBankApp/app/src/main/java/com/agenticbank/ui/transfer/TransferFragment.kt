package com.agenticbank.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.agenticbank.data.models.BeneficiaryResponse
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentTransferBinding
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class TransferFragment : Fragment() {

    private var _binding: FragmentTransferBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager
    private var beneficiaries: List<BeneficiaryResponse> = emptyList()
    private var transferReference: String? = null
    private var pendingToAccountId: Long? = null
    private var pendingAmount: Double? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        loadBeneficiaries()
        setupUI()
    }

    private fun loadBeneficiaries() {
        lifecycleScope.launch {
            try {
                val response = repo.getBeneficiaries()
                if (response.isSuccessful) {
                    beneficiaries = response.body()?.beneficiaries ?: emptyList()
                    val names = beneficiaries.map { "${it.nickname} (${it.accountNumber})" }
                    val adapter = ArrayAdapter(requireContext(),
                        android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerBeneficiary.adapter = adapter
                }
            } catch (e: Exception) {
                showToast("Could not load beneficiaries")
            }
        }
    }

    private fun setupUI() {
        // Toggle: beneficiary or direct account number
        binding.rgTransferType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbBeneficiary.id -> {
                    binding.layoutBeneficiary.visible()
                    binding.layoutDirectAccount.gone()
                }
                binding.rbDirect.id -> {
                    binding.layoutBeneficiary.gone()
                    binding.layoutDirectAccount.visible()
                }
            }
        }
        binding.rbBeneficiary.isChecked = true

        binding.btnRequestOtp.setOnClickListener { requestOtp() }
        binding.btnConfirmTransfer.setOnClickListener { confirmTransfer() }

        // OTP section hidden initially
        binding.layoutOtp.gone()
        binding.btnConfirmTransfer.gone()
    }

    private fun requestOtp() {
        val fromAccountId = session.getAccountId() ?: run {
            showToast("No account found"); return
        }
        val amountStr = binding.etAmount.text.toString()
        val amount = amountStr.toDoubleOrNull() ?: run {
            showToast("Enter a valid amount"); return
        }
        if (amount <= 0) { showToast("Amount must be greater than 0"); return }

        val toAccountId: Long = if (binding.rbBeneficiary.isChecked) {
            val selectedIndex = binding.spinnerBeneficiary.selectedItemPosition
            if (beneficiaries.isEmpty() || selectedIndex < 0) {
                showToast("Select a beneficiary"); return
            }
            // We need to get account ID from account number - use beneficiary id as proxy
            // Backend expects account ID, not beneficiary ID
            beneficiaries[selectedIndex].id // Will need adjustment per backend
        } else {
            binding.etAccountId.text.toString().toLongOrNull() ?: run {
                showToast("Enter a valid account ID"); return
            }
        }

        pendingToAccountId = toAccountId
        pendingAmount = amount

        binding.progressBar.visible()
        binding.btnRequestOtp.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = repo.requestTransferOtp(fromAccountId, toAccountId, amount)
                if (response.isSuccessful) {
                    val otpResp = response.body()!!
                    transferReference = otpResp.transferReference
                    // Show OTP in dev mode (remove in production)
                    val otpHint = if (!otpResp.otp.isNullOrEmpty()) " (Dev OTP: ${otpResp.otp})" else ""
                    showToast("OTP sent to your registered contact$otpHint")
                    binding.layoutOtp.visible()
                    binding.btnConfirmTransfer.visible()
                    binding.btnRequestOtp.text = "Resend OTP"
                } else {
                    showToast("Failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.btnRequestOtp.isEnabled = true
            }
        }
    }

    private fun confirmTransfer() {
        val otp = binding.etOtp.text.toString().trim()
        if (otp.isEmpty()) { showToast("Enter OTP"); return }
        val fromAccountId = session.getAccountId() ?: return
        val toAccountId = pendingToAccountId ?: return
        val amount = pendingAmount ?: return

        binding.progressBar.visible()
        binding.btnConfirmTransfer.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = repo.verifyTransferOtp(otp, fromAccountId, toAccountId, amount)
                if (response.isSuccessful) {
                    val result = response.body()!!
                    showToast("✅ Transfer of ${result.amount} successful!")
                    clearForm()
                } else {
                    showToast("Transfer failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.btnConfirmTransfer.isEnabled = true
            }
        }
    }

    private fun clearForm() {
        binding.etAmount.text?.clear()
        binding.etOtp.text?.clear()
        binding.etAccountId.text?.clear()
        binding.layoutOtp.gone()
        binding.btnConfirmTransfer.gone()
        binding.btnRequestOtp.text = "Request OTP"
        pendingToAccountId = null
        pendingAmount = null
        transferReference = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
