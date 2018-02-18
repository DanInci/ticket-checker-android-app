package ticket.checker

import android.content.res.Configuration
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.Statistic
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler


class ActivityStatistics : AppCompatActivity() {

    private val refreshLayout : SwipeRefreshLayout by lazy {
        findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
    }

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val toolbaTitle : TextView by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private var currentMenuItemId = -1
    private var currentInterval = INTERVAL_HOURLY

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

    private val statisticsCallback = object : Callback<List<Statistic>> {
        override fun onResponse(call: Call<List<Statistic>>, response: Response<List<Statistic>>) {
            if (response.isSuccessful) {
                refreshLayout.isEnabled = true
                updateGraph(call.request().url().queryParameter("type"), response.body() as List<Statistic>)
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
        override fun onFailure(call: Call<List<Statistic>>, t: Throwable?) {
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
        currentMenuItemId = savedInstanceState?.getInt(CURRENT_MENU_ITEM) ?: R.id.action_hourly
        currentInterval = savedInstanceState?.getString(CURRENT_INTERVAL) ?: INTERVAL_HOURLY
        refreshLayout.setOnRefreshListener { onRefresh() }
        refreshLayout.setColorSchemeColors(resources.getColor(R.color.colorPrimary))
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
                    currentInterval = INTERVAL_HOURLY
                }
                R.id.action_daily -> {
                    currentMenuItemId = R.id.action_daily
                    currentInterval = INTERVAL_DAILY
                }
                R.id.action_weekly -> {
                    currentMenuItemId = R.id.action_weekly
                    currentInterval = INTERVAL_WEEKLY
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
        outState.putString(CURRENT_INTERVAL, currentInterval)
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
        val callValidated = ServiceManager.getStatisticsService().getTicketStatisticsForInterval(TYPE_VALIDATED, currentInterval, limit)
        callValidated.enqueue(statisticsCallback)
        val callSold = ServiceManager.getStatisticsService().getTicketStatisticsForInterval(TYPE_SOLD, currentInterval, limit)
        callSold.enqueue(statisticsCallback)
    }

    private fun updateGraph(type : String?, statistics : List<Statistic>) {
        val entries : MutableList<BarEntry> = mutableListOf()
        val dates : MutableList<Date> = mutableListOf()
        val typeTitle = type?.capitalize()
        for((index, stats) in statistics.withIndex()) {
            dates.add(stats.date)
            entries.add(BarEntry(index.toFloat(), stats.count.toFloat()))
        }
        val barDataSet = BarDataSet(entries,"$typeTitle Tickets")
        barDataSet.color = resources.getColor(R.color.materialYellow)
        barDataSet.valueTextColor = resources.getColor(R.color.darkerGrey)
        barDataSet.valueTextSize = 12f
        barDataSet.valueFormatter = CustomValueFormatter()
        val barData = BarData(barDataSet)
        refreshBarData(type, barData, dates)
    }

    private fun refreshBarData(type : String?, data : BarData, dates: List<Date>) {
        when(type) {
            TYPE_VALIDATED -> {
                if(lsValidated.visibility == View.VISIBLE) {
                    lsValidated.visibility = View.GONE
                    validatedChart.visibility = View.VISIBLE
                }
                validatedChart.xAxis.valueFormatter = CustomXAxisFormat(dates, currentInterval)
                validatedChart.data = data
                validatedChart.notifyDataSetChanged()
                validatedChart.invalidate()
                validatedChart.animateXY(3000,3000)
                validatedChart.refreshDrawableState()
            }
            TYPE_SOLD -> {
                if(lsSold.visibility == View.VISIBLE) {
                    lsSold.visibility = View.GONE
                    soldChart.visibility = View.VISIBLE
                }
                soldChart.xAxis.valueFormatter = CustomXAxisFormat(dates, currentInterval)
                soldChart.data = data
                soldChart.notifyDataSetChanged()
                soldChart.invalidate()
                soldChart.animateXY(3000,3000)
                soldChart.refreshDrawableState()
            }
        }
    }

    private fun customizeChartStyle(chart : BarChart) {
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.textColor = resources.getColor(R.color.textBlack)
        chart.setNoDataText("No ticket data available")
        chart.setNoDataTextColor(resources.getColor(R.color.textBlack))
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
        bottom.textColor = resources.getColor(R.color.textBlack)
    }

    private fun updateTitle(interval : String) {
        toolbaTitle.text = "${interval.capitalize()} Statistics"
    }

    private fun checkMenuItem(menuItemId: Int) {
        val menu = toolbar.menu
        (0 until menu.size())
                .map { menu.getItem(it) }
                .forEach { it.isChecked = it.itemId == menuItemId }
    }

    private class CustomXAxisFormat(private val dates : List<Date>, private val type : String) : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            val intFormat = value.toInt()
             when(type) {
                INTERVAL_HOURLY -> {
                    return HOUR_FORMAT.format(dates[intFormat])
                }
                INTERVAL_DAILY -> {
                    return DAILY_FORMAT.format(dates[intFormat])
                }
                INTERVAL_WEEKLY -> {
                    return formatWeek(dates[intFormat])
                }
            }
            return "MISSED"
        }

        private fun formatWeek(startDate : Date) : String {
            val endDate = Date(startDate.time + 518400000)
            val calendar1 = Calendar.getInstance()
            calendar1.time = startDate
            val calendar2 = Calendar.getInstance()
            calendar2.time = endDate
            if(calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)) {
                return SimpleDateFormat("dd").format(startDate) + " - " + DAILY_FORMAT.format(endDate)
            }
            else {
                return DAILY_FORMAT.format(startDate) + " - " + DAILY_FORMAT.format(endDate)
            }

        }
    }
    private class CustomValueFormatter : IValueFormatter {
        override fun getFormattedValue(value: Float, entry: Entry?, dataSetIndex: Int, viewPortHandler: ViewPortHandler?): String {
            return value.toInt().toString()
        }
    }

    companion object {
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")
        val DAILY_FORMAT = SimpleDateFormat("dd MMM")

        const val CURRENT_MENU_ITEM = "currentMenuItem"
        const val CURRENT_INTERVAL = "currentInterval"
        const val TYPE_VALIDATED = "validated"
        const val TYPE_SOLD = "sold"
        const val INTERVAL_HOURLY = "hourly"
        const val INTERVAL_DAILY = "daily"
        const val INTERVAL_WEEKLY = "weekly"
        const val STATS_LIMIT_PORTRAIT = 5
        const val STATS_LIMIT_LANDSCAPE = 7
    }
}
