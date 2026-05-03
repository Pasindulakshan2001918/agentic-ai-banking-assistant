package com.agenticbank.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.agenticbank.R
import com.agenticbank.databinding.ItemChatMessageBinding

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        with(holder.binding) {
            tvMessage.text = msg.text
            if (msg.isUser) {
                tvMessage.setBackgroundResource(R.drawable.bg_chat_user)
                tvMessage.setTextColor(ContextCompat.getColor(tvMessage.context, R.color.white))
                containerMessage.gravity = Gravity.END
            } else {
                tvMessage.setBackgroundResource(R.drawable.bg_chat_bot)
                tvMessage.setTextColor(ContextCompat.getColor(tvMessage.context, R.color.text_primary))
                containerMessage.gravity = Gravity.START
            }
        }
    }

    override fun getItemCount() = messages.size
}
