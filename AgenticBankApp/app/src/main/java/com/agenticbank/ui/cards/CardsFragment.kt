package com.agenticbank.ui.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.data.models.Card
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentCardsBinding
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class CardsFragment : Fragment() {

    private var _binding: FragmentCardsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        binding.rvCards.layoutManager = LinearLayoutManager(requireContext())
        loadCards()
        binding.swipeRefresh.setOnRefreshListener { loadCards() }
    }

    private fun loadCards() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            try {
                val response = repo.getCards()
                if (response.isSuccessful) {
                    val cards = response.body()?.cards ?: emptyList()
                    if (cards.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvCards.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvCards.visible()
                        binding.rvCards.adapter = CardAdapter(cards) { card, action ->
                            when (action) {
                                "BLOCK" -> confirmCardAction(card, "Block")
                                "UNBLOCK" -> confirmCardAction(card, "Unblock")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmCardAction(card: Card, action: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("$action Card")
            .setMessage("Are you sure you want to $action card ending in ${card.cardNumber.takeLast(4)}?")
            .setPositiveButton(action) { _, _ ->
                performCardAction(card.id, action)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCardAction(cardId: Long, action: String) {
        lifecycleScope.launch {
            try {
                val response = if (action == "Block") repo.blockCard(cardId) else repo.unblockCard(cardId)
                if (response.isSuccessful) {
                    showToast("Card ${action}ed successfully")
                    loadCards()
                } else {
                    showToast("Failed: ${response.errorBody()?.string()}")
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
