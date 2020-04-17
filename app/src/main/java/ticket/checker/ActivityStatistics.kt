package ticket.checker

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import ticket.checker.beans.TicketsStatistic
import ticket.checker.extras.IntervalType
import ticket.checker.extras.TicketCategory
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


class ActivityStatistics : AppCompatActivity() {

    private var currentMenuItemId = -1
    private var currentInterval = IntervalType.HOURLY

    private val refreshLayout by lazy {
        findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
    }
    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val toolbarTitle by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private val lsValidated by lazy {
        findViewById<ProgressBar>(R.id.lsValidated)
    }
    private val validatedChart by lazy {
        findViewById<BarChart>(R.id.validatedChart)
    }
    private val lsSold by lazy {
        findViewById<ProgressBar>(R.id.lsSold)
    }
    private val soldChart by lazy {
        findViewById<BarChart>(R.id.soldChart)
    }

    private val statisticsCallback = object : Callback<List<TicketsStatistic>> {
        override fun onResponse(call: Call<List<TicketsStatistic>>, response: Response<List<TicketsStatistic>>) {
            if (response.isSuccessful) {
                refreshLayout.isEnabled = true
                updateGraph(TicketCategory.from(call.request().url().queryParameter("category")!!)!!, response.body() as List<TicketsStatistic>)
            }
            else {
                Util.treatBasicError(call, null, supportFragmentManager)
                if(lsValidated.visibility == View.VISIBLE) {
                    lsValidated.visibility = View.GONE
                    validatedChart.visibility = View.VISIBLE
                }
                if(lsSold.visibility == View.VISIBLE) {
                    lsSold.visibility = View.GONE
                    soldChart.visibility = View.VISIBLE
                }
            }
        }
        override fun onFailure(call: Call<List<TicketsStatistic>>, t: Throwable?) {
            Util.treatBasicError(call, null, supportFragmentManager)
            if(lsValidated.visibility == View.VISIBLE) {
                lsValidated.visibility = View.GONE
                validatedChart.visibility = View.VISIBLE
            }
            if(lsSold.visibility == View.VISIBLE) {
                lsSold.visibility = View.GONE
                soldChart.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        btnBack.setOnClickListener { finish() }
        currentMenuItemId = savedInstanceState?.getInt(CURRENT_MENU_ITEM) ?: R.id.action_hourly
        currentInterval = savedInstanceState?.getSerializable(CURRENT_INTERVAL) as IntervalType
        refreshLayout.setOnRefreshListener { onRefresh() }
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
        customizeChartStyle(validatedChart)
        customizeChartStyle(soldChart)
    }

    override fun onStart() {
        super.onStart()
        reloadData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.statistics_menu, menu)
        checkMenuItem(currentMenuItemId)
        updateTitle(currentInterval)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var validSelection = item.itemId != currentMenuItemId
        if (validSelection) {
            when (item.itemId) {
                R.id.action_hourly -> {
                    currentMenuItemId = R.id.action_hourly
                    currentInterval = IntervalType.HOURLY
                }
                R.id.action_daily -> {
                    currentMenuItemId = R.id.action_daily
                    currentInterval = IntervalType.DAILY
                }
                R.id.action_weekly -> {
                    currentMenuItemId = R.id.action_weekly
                    currentInterval = IntervalType.WEEKLY
                }
                else -> {
                    validSelection = false
                }
            }
            if(validSelection) {
                checkMenuItem(currentMenuItemId)
                updateTitle(currentInterval)
                reloadData()
            }
        }
        return validSelection
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_MENU_ITEM, currentMenuItemId)
        outState.putSerializable(CURRENT_INTERVAL, currentInterval)
    }

    private fun onRefresh() {
        refreshLayout.isRefreshing = false
        refreshLayout.isEnabled = false
        validatedChart.visibility = View.INVISIBLE
        lsValidated.visibility = View.VISIBLE
        soldChart.visibility = View.INVISIBLE
        lsSold.visibility = View.VISIBLE
        reloadData()
    }

    private fun reloadData() {
        refreshLayout.isEnabled = false
        val limit : Int = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { STATS_LIMIT_LANDSCAPE } else { STATS_LIMIT_PORTRAIT }
        val callValidated = ServiceManager.getStatisticsService().getTicketsStatistics(AppTicketChecker.selectedOrganization!!.id, TicketCategory.VALIDATED, currentInterval, limit, null)
        callValidated.enqueue(statisticsCallback)
        val callSold = ServiceManager.getStatisticsService().getTicketsStatistics(AppTicketChecker.selectedOrganization!!.id, TicketCategory.SOLD, currentInterval, limit, null)
        callSold.enqueue(statisticsCallback)
    }

    private fun updateGraph(category: TicketCategory, statistics : List<TicketsStatistic>) {
        val entries : MutableList<BarEntry> = mutableListOf()
        val dates : MutableList<Pair<OffsetDateTime, OffsetDateTime>> = mutableListOf()
        for((index, stats) in statistics.withIndex()) {
            dates.add(Pair(stats.startDate, stats.endDate))
            entries.add(BarEntry(index.toFloat(), stats.count.toFloat()))
        }
        val barDataSet = BarDataSet(entries,"${category.category} Tickets")
        barDataSet.color = ContextCompat.getColor(applicationContext, R.color.materialYellow)
        barDataSet.valueTextColor = ContextCompat.getColor(applicationContext, R.color.darkerGrey)
        barDataSet.valueTextSize = 12f
        barDataSet.valueFormatter = CustomValueFormatter()
        val barData = BarData(barDataSet)
        refreshBarData(category, barData, dates)
    }

    private fun refreshBarData(category : TicketCategory, data : BarData, dates: List<Pair<OffsetDateTime, OffsetDateTime>>) {
        when(category) {
            TicketCategory.VALIDATED -> {
                if(lsValidated.visibility == View.VISIBLE) {
                    lsValidated.visibility = View.GONE
                    validatedChart.visibility = View.VISIBLE
                }
                validatedChart.xAxis.valueFormatter = CustomXAxisFormat(dates, currentInterval)
                validatedChart.data = data
                validatedChart.notifyDataSetChanged()
                validatedChart.invalidate()
                validatedChart.animateY(3000)
                validatedChart.refreshDrawableState()
            }
            TicketCategory.SOLD -> {
                if(lsSold.visibility == View.VISIBLE) {
                    lsSold.visibility = View.GONE
                    soldChart.visibility = View.VISIBLE
                }
                soldChart.xAxis.valueFormatter = CustomXAxisFormat(dates, currentInterval)
                soldChart.data = data
                soldChart.notifyDataSetChanged()
                soldChart.invalidate()
                soldChart.animateY(3000)
                soldChart.refreshDrawableState()
            }
        }
    }

    private fun customizeChartStyle(chart : BarChart) {
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.textColor = ContextCompat.getColor(applicationContext, R.color.textBlack)
        chart.setNoDataText("No ticket data available")
        chart.setNoDataTextColor(ContextCompat.getColor(applicationContext, R.color.textBlack))
        chart.setTouchEnabled(false)

        val left = chart.axisLeft
        left.setDrawLabels(false) // no axis labels
        left.setDrawAxisLine(false) // no axis line
        left.setDrawGridLines(false) // no grid lines
        left.setDrawZeroLine(true) // draw a zero line
        chart.axisRight.isEnabled = false // no right axis

        val bottom = chart.xAxis
        bottom.position = XAxis.XAxisPosition.BOTTOM
        bottom.setDrawAxisLine(true)
        bottom.setDrawGridLines(false)
        bottom.granularity = 1f
        bottom.textColor = ContextCompat.getColor(applicationContext, R.color.textBlack)
    }

    private fun updateTitle(interval : IntervalType) {
        toolbarTitle.text = "${interval.type} Statistics"
    }

    private fun checkMenuItem(menuItemId: Int) {
        val menu = toolbar.menu
        (0 until menu.size())
                .map { menu.getItem(it) }
                .forEach { it.isChecked = it.itemId == menuItemId }
    }

    private class CustomXAxisFormat(private val dates : List<Pair<OffsetDateTime, OffsetDateTime>>, private val type : IntervalType) : ValueFormatter() {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            val intFormat = value.toInt()
            return when(type) {
                IntervalType.HOURLY -> {
                    HOUR_FORMAT.format(dates[intFormat].first)
                }
                IntervalType.DAILY -> {
                    DAY_MONTH_FORMAT.format(dates[intFormat].first)
                }
                IntervalType.WEEKLY -> {
                    formatWeek(dates[intFormat].first, dates[intFormat].second)
                }
            }
        }

        private fun formatWeek(startDate : OffsetDateTime, endDate: OffsetDateTime) : String {
            return if(startDate.month == endDate.month) {
                DAY_FORMAT.format(startDate) + " - " + DAY_MONTH_FORMAT.format(endDate)
            } else {
                " " + DAY_MONTH_FORMAT.format(startDate) + " - " + DAY_MONTH_FORMAT.format(endDate) + " "
            }

        }
    }
    private class CustomValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float, entry: Entry?, dataSetIndex: Int, viewPortHandler: ViewPortHandler?): String {
            return value.toInt().toString()
        }
    }

    companion object {
        private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd")
        private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

        const val CURRENT_MENU_ITEM = "currentMenuItem"
        const val CURRENT_INTERVAL = "currentInterval"
        const val STATS_LIMIT_PORTRAIT = 5
        const val STATS_LIMIT_LANDSCAPE = 7
    }
}
