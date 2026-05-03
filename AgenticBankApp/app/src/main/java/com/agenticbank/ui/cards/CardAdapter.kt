package com.agenticbank.ui.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.agenticbank.R
import com.agenticbank.data.models.Card
import com.agenticbank.databinding.ItemCardBinding
import com.agenticbank.utils.maskCardNumber

class CardAdapter(
    private val items: List<Card>,
    private val onAction: (Card, String) -> Unit
) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = items[position]
        with(holder.binding) {
            tvCardNumber.text = card.cardNumber.maskCardNumber()
            tvCardType.text = card.cardType
            tvExpiry.text = card.expiryDate?.let { "Exp: $it" } ?: ""
            tvContactless.text = if (card.isContactless == true) "Contactless" else "Non-contactless"

            val isActive = card.status == "ACTIVE"
            tvStatus.text = card.status
            tvStatus.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    if (isActive) R.color.green_success else R.color.red_error
                )
            )

            if (isActive) {
                btnAction.text = "Block"
                btnAction.setBackgroundColor(ContextCompat.getColor(root.context, R.color.red_error))
                btnAction.setOnClickListener { onAction(card, "BLOCK") }
            } else {
                btnAction.text = "Unblock"
                btnAction.setBackgroundColor(ContextCompat.getColor(root.context, R.color.green_success))
                btnAction.setOnClickListener { onAction(card, "UNBLOCK") }
            }
        }
    }

    override fun getItemCount() = items.size
}
