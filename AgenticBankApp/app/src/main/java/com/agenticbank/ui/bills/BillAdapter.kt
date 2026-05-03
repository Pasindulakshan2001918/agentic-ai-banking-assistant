package com.agenticbank.ui.bills

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agenticbank.data.models.Bill
import com.agenticbank.databinding.ItemBillBinding
import com.agenticbank.utils.toFormattedCurrency

class BillAdapter(
    private val items: List<Bill>,
    private val onPay: (Bill) -> Unit
) : RecyclerView.Adapter<BillAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = items[position]
        with(holder.binding) {
            tvProvider.text = bill.provider
            tvBillType.text = bill.billType
            tvReference.text = "Ref: ${bill.accountReference}"
            tvAmount.text = bill.amount?.toFormattedCurrency() ?: "Variable"
            tvDueDate.text = bill.dueDate?.let { "Due: $it" } ?: ""
            tvStatus.text = bill.status

            btnPay.setOnClickListener { onPay(bill) }
            btnPay.isEnabled = bill.status != "PAID"
        }
    }

    override fun getItemCount() = items.size
}
