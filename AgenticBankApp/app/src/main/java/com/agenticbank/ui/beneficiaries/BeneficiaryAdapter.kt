package com.agenticbank.ui.beneficiaries

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.agenticbank.data.models.BeneficiaryResponse
import com.agenticbank.databinding.ItemBeneficiaryBinding

class BeneficiaryAdapter(
    private val items: List<BeneficiaryResponse>,
    private val onEdit: (BeneficiaryResponse) -> Unit,
    private val onDelete: (BeneficiaryResponse) -> Unit
) : RecyclerView.Adapter<BeneficiaryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBeneficiaryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBeneficiaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvNickname.text = item.nickname
            tvHolderName.text = item.accountHolderName
            tvAccountNumber.text = item.accountNumber
            tvStatus.text = item.status
            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount() = items.size
}
