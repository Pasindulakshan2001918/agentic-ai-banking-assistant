package com.agenticbank.ui.bills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.data.models.Bill
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentBillsBinding
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class BillsFragment : Fragment() {

    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager
    private var bills: List<Bill> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        binding.rvBills.layoutManager = LinearLayoutManager(requireContext())
        loadBills()
        binding.swipeRefresh.setOnRefreshListener { loadBills() }
    }

    private fun loadBills() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            try {
                val response = repo.getBills()
                if (response.isSuccessful) {
                    bills = response.body()?.bills ?: emptyList()
                    if (bills.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvBills.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvBills.visible()
                        binding.rvBills.adapter = BillAdapter(bills) { bill ->
                            showPayDialog(bill)
                        }
                    }
                }
            } catch (e: Exception) {
                showToast("Error loading bills: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showPayDialog(bill: Bill) {
        val dialogView = layoutInflater.inflate(com.agenticbank.R.layout.dialog_pay_bill, null)
        val etAmount = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etAmount)
        val etOtp = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etOtp)
        val btnGetOtp = dialogView.findViewById<android.widget.Button>(com.agenticbank.R.id.btnGetOtp)

        // Pre-fill amount if available
        bill.amount?.let { etAmount.setText(it.toString()) }

        btnGetOtp.setOnClickListener {
            showToast("OTP sent to your registered contact")
            etOtp.visible()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Pay ${bill.provider}")
            .setMessage("${bill.billType} - Ref: ${bill.accountReference}")
            .setView(dialogView)
            .setPositiveButton("Pay") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull() ?: run {
                    showToast("Enter valid amount"); return@setPositiveButton
                }
                val otp = etOtp.text.toString().trim()
                if (otp.isEmpty()) { showToast("Enter OTP"); return@setPositiveButton }
                val accountId = session.getAccountId() ?: run {
                    showToast("No account found"); return@setPositiveButton
                }
                payBill(bill.id, amount, accountId, otp)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun payBill(billId: Long, amount: Double, accountId: Long, otp: String) {
        lifecycleScope.launch {
            try {
                val response = repo.payBill(billId, amount, accountId, otp)
                if (response.isSuccessful) {
                    val result = response.body()!!
                    showToast("✅ Bill paid! Receipt: ${result.receiptNumber ?: "N/A"}")
                    loadBills()
                } else {
                    showToast("Payment failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
