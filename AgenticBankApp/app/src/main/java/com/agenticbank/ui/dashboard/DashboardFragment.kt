package com.agenticbank.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.R
import com.agenticbank.data.models.Transaction
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentDashboardBinding
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.toFormattedCurrency
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        binding.tvWelcome.text = "Welcome, ${session.getUsername() ?: "User"}!"

        setupQuickActions()
        loadData()

        binding.swipeRefresh.setOnRefreshListener { loadData() }
    }

    private fun setupQuickActions() {
        binding.btnTransfer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_transfer)
        }
        binding.btnBeneficiary.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_beneficiaries)
        }
        binding.btnBills.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_bills)
        }
        binding.btnCards.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_cards)
        }
        binding.btnScheduled.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_scheduled)
        }
        binding.btnInsights.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_insights)
        }
        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_transactions)
        }
    }

    private fun loadData() {
        val accountId = session.getAccountId() ?: return
        binding.progressBar.visible()

        lifecycleScope.launch {
            try {
                // Load balance
                val balanceResponse = repo.getBalance(accountId)
                if (balanceResponse.isSuccessful) {
                    val balance = balanceResponse.body()!!
                    binding.tvBalance.text = balance.balance.toFormattedCurrency(balance.currency)
                    binding.tvAccountNumber.text = "Acc: ${balance.accountNumber}"
                    binding.tvAccountStatus.text = balance.status
                }

                // Load recent transactions
                val txResponse = repo.getTransactions(accountId, 0, 3)
                if (txResponse.isSuccessful) {
                    val transactions = txResponse.body()?.transactions ?: emptyList()
                    setupTransactionsRecycler(transactions)
                }
            } catch (e: Exception) {
                showToast("Error loading data: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupTransactionsRecycler(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            binding.tvNoTransactions.visible()
            binding.rvRecentTransactions.gone()
            return
        }
        binding.tvNoTransactions.gone()
        binding.rvRecentTransactions.visible()
        binding.rvRecentTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentTransactions.adapter = TransactionAdapter(transactions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
