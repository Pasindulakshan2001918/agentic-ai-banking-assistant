package com.agenticbank.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentChatBinding
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch
import java.util.UUID

class AIChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val sessionId = UUID.randomUUID().toString()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        adapter = ChatAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        // Welcome message
        addMessage("Hello! I'm your AI banking assistant. I can help you check your balance, transfer money, view transactions, pay bills, and much more. How can I help you today?", false)

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage(); true
        }

        // Quick action chips
        binding.chipBalance.setOnClickListener { sendQuickMessage("What's my balance?") }
        binding.chipTransactions.setOnClickListener { sendQuickMessage("Show my recent transactions") }
        binding.chipTransfer.setOnClickListener { sendQuickMessage("I want to transfer money") }
        binding.chipInsights.setOnClickListener { sendQuickMessage("Show my spending insights") }
    }

    private fun sendQuickMessage(text: String) {
        binding.etMessage.setText(text)
        sendMessage()
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        binding.etMessage.text?.clear()
        addMessage(text, true)

        binding.progressBar.visible()
        binding.btnSend.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = repo.sendChatMessage(text, session.getAccountId(), sessionId)
                if (response.isSuccessful) {
                    val reply = response.body()!!
                    addMessage(reply.reply, false)
                } else {
                    // Fallback: show helpful error
                    addMessage("I'm having trouble connecting right now. Please try again or use the app directly.", false)
                }
            } catch (e: Exception) {
                addMessage("Connection error. Please check your internet connection.", false)
            } finally {
                binding.progressBar.gone()
                binding.btnSend.isEnabled = true
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)
