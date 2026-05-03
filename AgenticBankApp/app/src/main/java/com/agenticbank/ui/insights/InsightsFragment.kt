package com.agenticbank.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.agenticbank.data.models.SpendingCategory
import com.agenticbank.data.repository.BankRepository
import com.agenticbank.databinding.FragmentInsightsBinding
import com.agenticbank.utils.gone
import com.agenticbank.utils.showToast
import com.agenticbank.utils.toFormattedCurrency
import com.agenticbank.utils.visible
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.launch

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: BankRepository

    private val chartColors = listOf(
        Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"),
        Color.parseColor("#FF9800"), Color.parseColor("#E91E63"),
        Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"),
        Color.parseColor("#FF5722"), Color.parseColor("#607D8B")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = BankRepository(requireContext())
        setupPieChart()
        loadInsights()
        binding.swipeRefresh.setOnRefreshListener { loadInsights() }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.isEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            setCenterText("Spending")
            setCenterTextSize(14f)
        }
    }

    private fun loadInsights() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            try {
                val response = repo.getInsights()
                if (response.isSuccessful) {
                    val data = response.body()!!
                    binding.tvTotalSpending.text = data.totalSpending.toFormattedCurrency()
                    updatePieChart(data.categories)
                    updateAlerts(data.alerts)
                    updateMonthlyComparison(data.monthlyComparison)
                } else {
                    showToast("Could not load insights")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updatePieChart(categories: List<SpendingCategory>) {
        if (categories.isEmpty()) {
            binding.pieChart.gone()
            binding.tvNoData.visible()
            return
        }
        binding.pieChart.visible()
        binding.tvNoData.gone()

        val entries = categories.map { PieEntry(it.percentage.toFloat(), it.category) }
        val dataSet = PieDataSet(entries, "Categories").apply {
            colors = chartColors.take(entries.size)
            sliceSpace = 3f
            valueTextSize = 12f
        }
        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
        }
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()

        // Build legend text
        val legendText = categories.joinToString("\n") {
            "${it.category}: ${it.amount.toFormattedCurrency()} (${String.format("%.1f", it.percentage)}%)"
        }
        binding.tvCategoryBreakdown.text = legendText
    }

    private fun updateAlerts(alerts: List<String>?) {
        if (alerts.isNullOrEmpty()) {
            binding.cardAlerts.gone()
        } else {
            binding.cardAlerts.visible()
            binding.tvAlerts.text = alerts.joinToString("\n• ", "• ")
        }
    }

    private fun updateMonthlyComparison(monthly: Map<String, Double>?) {
        if (monthly.isNullOrEmpty()) {
            binding.cardMonthly.gone()
        } else {
            binding.cardMonthly.visible()
            val text = monthly.entries.joinToString("\n") { (month, amount) ->
                "$month: ${amount.toFormattedCurrency()}"
            }
            binding.tvMonthlyComparison.text = text
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
