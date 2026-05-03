package com.agenticbank.ui.beneficiaries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.data.models.BeneficiaryResponse
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentBeneficiariesBinding
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch

class BeneficiariesFragment : Fragment() {

    private var _binding: FragmentBeneficiariesBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private var beneficiaries: MutableList<BeneficiaryResponse> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBeneficiariesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())

        binding.rvBeneficiaries.layoutManager = LinearLayoutManager(requireContext())
        binding.fabAddBeneficiary.setOnClickListener { showAddDialog() }
        loadBeneficiaries()
    }

    private fun loadBeneficiaries() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            try {
                val response = repo.getBeneficiaries()
                if (response.isSuccessful) {
                    beneficiaries = (response.body()?.beneficiaries ?: emptyList()).toMutableList()
                    updateRecycler()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
            }
        }
    }

    private fun updateRecycler() {
        if (beneficiaries.isEmpty()) {
            binding.tvEmpty.visible()
            binding.rvBeneficiaries.gone()
        } else {
            binding.tvEmpty.gone()
            binding.rvBeneficiaries.visible()
            binding.rvBeneficiaries.adapter = BeneficiaryAdapter(
                beneficiaries,
                onEdit = { showEditDialog(it) },
                onDelete = { showDeleteConfirm(it) }
            )
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(com.agenticbank.R.layout.dialog_beneficiary, null)
        val etNickname = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etNickname)
        val etHolderName = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etHolderName)
        val etAccountId = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etAccountId)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Beneficiary")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val nickname = etNickname.text.toString().trim()
                val holderName = etHolderName.text.toString().trim()
                val accountId = etAccountId.text.toString().toLongOrNull()
                if (nickname.isEmpty() || holderName.isEmpty() || accountId == null) {
                    showToast("Please fill all fields")
                    return@setPositiveButton
                }
                addBeneficiary(nickname, holderName, accountId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(beneficiary: BeneficiaryResponse) {
        val dialogView = layoutInflater.inflate(com.agenticbank.R.layout.dialog_beneficiary, null)
        val etNickname = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etNickname)
        val etHolderName = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etHolderName)
        val etAccountId = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etAccountId)

        etNickname.setText(beneficiary.nickname)
        etHolderName.setText(beneficiary.accountHolderName)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Beneficiary")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val nickname = etNickname.text.toString().trim()
                val holderName = etHolderName.text.toString().trim()
                val accountId = etAccountId.text.toString().toLongOrNull() ?: beneficiary.id
                updateBeneficiary(beneficiary.id, nickname, holderName, accountId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirm(beneficiary: BeneficiaryResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Beneficiary")
            .setMessage("Remove ${beneficiary.nickname} from your contacts?")
            .setPositiveButton("Delete") { _, _ -> deleteBeneficiary(beneficiary.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addBeneficiary(nickname: String, holderName: String, accountId: Long) {
        lifecycleScope.launch {
            try {
                val response = repo.addBeneficiary(nickname, holderName, accountId)
                if (response.isSuccessful) {
                    showToast("Beneficiary added!")
                    loadBeneficiaries()
                } else {
                    showToast("Failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun updateBeneficiary(id: Long, nickname: String, holderName: String, accountId: Long) {
        lifecycleScope.launch {
            try {
                val response = repo.updateBeneficiary(id, nickname, holderName, accountId)
                if (response.isSuccessful) {
                    showToast("Beneficiary updated!")
                    loadBeneficiaries()
                } else {
                    showToast("Failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun deleteBeneficiary(id: Long) {
        lifecycleScope.launch {
            try {
                val response = repo.deleteBeneficiary(id)
                if (response.isSuccessful) {
                    showToast("Beneficiary removed")
                    loadBeneficiaries()
                } else {
                    showToast("Failed to delete")
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
