package com.agenticbank.ui.scheduled

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agenticbank.data.models.ScheduledTransfer
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentScheduledTransfersBinding
import com.agenticbank.utils.SessionManager
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.visible
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduledTransfersFragment : Fragment() {

    private var _binding: FragmentScheduledTransfersBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository
    private lateinit var session: SessionManager
    private var selectedDate: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduledTransfersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        session = SessionManager(requireContext())

        binding.rvScheduled.layoutManager = LinearLayoutManager(requireContext())
        loadScheduledTransfers()

        binding.fabSchedule.setOnClickListener { showScheduleDialog() }
        binding.swipeRefresh.setOnRefreshListener { loadScheduledTransfers() }
    }

    private fun loadScheduledTransfers() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            try {
                val response = repo.getScheduledTransfers()
                if (response.isSuccessful) {
                    val transfers = response.body()?.scheduledTransfers ?: emptyList()
                    if (transfers.isEmpty()) {
                        binding.tvEmpty.visible(); binding.rvScheduled.gone()
                    } else {
                        binding.tvEmpty.gone(); binding.rvScheduled.visible()
                        binding.rvScheduled.adapter = ScheduledTransferAdapter(transfers) { transfer ->
                            confirmCancel(transfer)
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

    private fun showScheduleDialog() {
        val dialogView = layoutInflater.inflate(com.agenticbank.R.layout.dialog_schedule_transfer, null)
        val etToAccountId = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etToAccountId)
        val etAmount = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etAmount)
        val etDescription = dialogView.findViewById<android.widget.EditText>(com.agenticbank.R.id.etDescription)
        val btnPickDate = dialogView.findViewById<android.widget.Button>(com.agenticbank.R.id.btnPickDate)
        val tvDate = dialogView.findViewById<android.widget.TextView>(com.agenticbank.R.id.tvSelectedDate)

        selectedDate = ""
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = "$y-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}T00:00:00"
                tvDate.text = "$y-${m + 1}-$d"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Schedule Transfer")
            .setView(dialogView)
            .setPositiveButton("Schedule") { _, _ ->
                val toAccountId = etToAccountId.text.toString().toLongOrNull() ?: run {
                    showToast("Enter valid account ID"); return@setPositiveButton
                }
                val amount = etAmount.text.toString().toDoubleOrNull() ?: run {
                    showToast("Enter valid amount"); return@setPositiveButton
                }
                if (selectedDate.isEmpty()) { showToast("Select a date"); return@setPositiveButton }
                val fromAccountId = session.getAccountId() ?: return@setPositiveButton
                scheduleTransfer(fromAccountId, toAccountId, amount, selectedDate,
                    etDescription.text.toString().takeIf { it.isNotEmpty() })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleTransfer(from: Long, to: Long, amount: Double, date: String, desc: String?) {
        lifecycleScope.launch {
            try {
                val response = repo.createScheduledTransfer(from, to, amount, date, desc)
                if (response.isSuccessful) {
                    showToast("✅ Transfer scheduled!")
                    loadScheduledTransfers()
                } else {
                    showToast("Failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun confirmCancel(transfer: ScheduledTransfer) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Scheduled Transfer")
            .setMessage("Cancel transfer of ${transfer.amount} scheduled for ${transfer.scheduledDate.take(10)}?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = repo.cancelScheduledTransfer(transfer.id)
                        if (response.isSuccessful) {
                            showToast("Scheduled transfer cancelled")
                            loadScheduledTransfers()
                        }
                    } catch (e: Exception) {
                        showToast("Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Keep", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
