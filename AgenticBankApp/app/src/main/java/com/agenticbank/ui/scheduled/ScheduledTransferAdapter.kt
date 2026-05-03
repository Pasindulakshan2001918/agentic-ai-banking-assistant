package com.agenticbank.ui.scheduled

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agenticbank.data.models.ScheduledTransfer
import com.agenticbank.databinding.ItemScheduledTransferBinding
import com.agenticbank.utils.toFormattedCurrency

class ScheduledTransferAdapter(
    private val items: List<ScheduledTransfer>,
    private val onCancel: (ScheduledTransfer) -> Unit
) : RecyclerView.Adapter<ScheduledTransferAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemScheduledTransferBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduledTransferBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAmount.text = item.amount.toFormattedCurrency()
            tvDate.text = "Scheduled: ${item.scheduledDate.take(10)}"
            tvStatus.text = item.status
            tvDescription.text = item.description ?: "Transfer"
            tvToAccount.text = "To Account: ${item.toAccountId}"

            val isPending = item.status == "PENDING"
            btnCancel.isEnabled = isPending
            btnCancel.alpha = if (isPending) 1f else 0.4f
            btnCancel.setOnClickListener { if (isPending) onCancel(item) }
        }
    }

    override fun getItemCount() = items.size
}
