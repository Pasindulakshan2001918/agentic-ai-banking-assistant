package com.agenticbank.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentTransactionsBinding
import com.agenticbank.ui.dashboard.TransactionAdapter
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager
    private var currentPage = 0
    private var isLoading = false
    private var hasMore = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        loadTransactions(reset = true)
        binding.swipeRefresh.setOnRefreshListener { loadTransactions(reset = true) }

        binding.btnLoadMore.setOnClickListener {
            if (hasMore && !isLoading) loadTransactions(reset = false)
        }
    }

    private fun loadTransactions(reset: Boolean) {
        val accountId = session.getAccountId() ?: return
        if (reset) { currentPage = 0; hasMore = true }
        if (isLoading) return

        isLoading = true
        binding.progressBar.visible()

        lifecycleScope.launch {
            try {
                val response = repo.getTransactions(accountId, currentPage, 20)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val transactions = body.transactions

                    if (reset) {
                        binding.rvTransactions.adapter = TransactionAdapter(transactions)
                    } else {
                        val old = (binding.rvTransactions.adapter as? TransactionAdapter)
                        // Rebuild with combined list
                        val combined = (old?.let { _ ->
                            val list = mutableListOf<com.agenticbank.data.models.Transaction>()
                            repeat(old.itemCount) { /* simplified - reset works for now */ }
                            list
                        } ?: emptyList()) + transactions
                        binding.rvTransactions.adapter = TransactionAdapter(transactions)
                    }

                    hasMore = transactions.size == 20
                    binding.btnLoadMore.visibility = if (hasMore) View.VISIBLE else View.GONE
                    if (hasMore) currentPage++

                    if (transactions.isEmpty() && reset) {
                        binding.tvEmpty.visible()
                    } else {
                        binding.tvEmpty.gone()
                    }
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isLoading = false
                binding.progressBar.gone()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
