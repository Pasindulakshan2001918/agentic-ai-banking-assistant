package com.agenticbank.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.agenticbank.R
import com.agenticbank.data.models.Transaction
import com.agenticbank.databinding.ItemTransactionBinding
import com.agenticbank.utils.toFormattedCurrency
import com.agenticbank.utils.toShortDate

class TransactionAdapter(private val items: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = items[position]
        with(holder.binding) {
            tvDescription.text = tx.description ?: tx.type
            tvDate.text = tx.createdAt.toShortDate()
            tvStatus.text = tx.status
            tvReference.text = tx.referenceNumber ?: ""

            val isCredit = tx.type.contains("CREDIT", ignoreCase = true) ||
                    tx.type.contains("DEPOSIT", ignoreCase = true)

            if (isCredit) {
                tvAmount.text = "+ ${tx.amount.toFormattedCurrency()}"
                tvAmount.setTextColor(ContextCompat.getColor(root.context, R.color.green_success))
            } else {
                tvAmount.text = "- ${tx.amount.toFormattedCurrency()}"
                tvAmount.setTextColor(ContextCompat.getColor(root.context, R.color.red_error))
            }

            ivIcon.setImageResource(
                when {
                    tx.type.contains("TRANSFER", ignoreCase = true) -> R.drawable.ic_transfer
                    tx.type.contains("BILL", ignoreCase = true) -> R.drawable.ic_bill
                    tx.type.contains("CARD", ignoreCase = true) -> R.drawable.ic_card
                    else -> R.drawable.ic_transaction
                }
            )
        }
    }

    override fun getItemCount() = items.size
}
