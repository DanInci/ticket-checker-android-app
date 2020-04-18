package ticket.checker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.admin.listeners.EndlessScrollListener
import ticket.checker.admin.listeners.RecyclerItemClickListener
import ticket.checker.admin.organizations.OrganizationsAdapter
import ticket.checker.beans.OrganizationList
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager

class ActivityOrganizations : AppCompatActivity(), RecyclerItemClickListener.OnItemClickListener {

    private var firstLoad = true

    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val refreshLayout by lazy {
        findViewById< SwipeRefreshLayout>(R.id.refreshLayout)
    }
    private val rvItems by lazy {
        findViewById<RecyclerView>(R.id.rvItems)
    }
    private val rvLoadingSpinner by lazy {
        findViewById<ProgressBar>(R.id.rvLoadingSpinner)
    }

    private val itemsAdapter: OrganizationsAdapter by lazy {
        OrganizationsAdapter(this@ActivityOrganizations)
    }
    private val layoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this@ActivityOrganizations)
    }
    private val scrollListener: EndlessScrollListener by lazy {
        object : EndlessScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, recyclerView: RecyclerView) {
                itemsAdapter.setLoading(true)
                loadMyOrganizations(page+1, PAGE_SIZE)
            }
        }
    }
    private val organizationsCallback = object : Callback<List<OrganizationList>> {
        override fun onResponse(call: Call<List<OrganizationList>>, response: Response<List<OrganizationList>>) {
            if (firstLoad) {
                onFirstLoad()
            }
            if (response.isSuccessful) {
                val items: List<OrganizationList> = response.body() as List<OrganizationList>
                itemsAdapter.setLoading(false)
                itemsAdapter.updateItemsList(items)
            } else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<List<OrganizationList>>, t: Throwable?) {
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organizations)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        refreshLayout.setOnRefreshListener { onRefresh() }
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this@ActivityOrganizations, R.color.colorPrimary))
        rvItems.layoutManager = layoutManager
        rvItems.adapter = itemsAdapter
        rvItems.addOnScrollListener(scrollListener)
        rvItems.addOnItemTouchListener(RecyclerItemClickListener(this@ActivityOrganizations, rvItems, this))

        if (firstLoad) {
            reloadAll()
        } else {
            scrollListener.currentPage = savedInstanceState?.getInt(LOAD_CURRENT_PAGE) ?: 0
            scrollListener.previousTotalItemCount = savedInstanceState?.getInt(LOAD_PREVIOUS_ITEM_COUNT) ?: 0
            scrollListener.loading = savedInstanceState?.getBoolean(LOAD_LOADING) ?: false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_organizations, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_organization_create -> {
                true
            }
            R.id.action_my_profile -> {
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onItemClick(view: View, position: Int) {}

    override fun onLongItemClick(view: View?, position: Int) {}

    private fun loadMyOrganizations(page: Int, pageSize: Int) {
        val call = ServiceManager.getOrganizationService().getOrganizations(page, pageSize)
        call.enqueue(organizationsCallback)
    }

    private fun onRefresh() {
        refreshLayout.isRefreshing = false
        itemsAdapter.resetItemsList()
        scrollListener.resetState()
        reloadAll()
    }

    private fun reloadAll() {
        onResetFirstLoad()
        loadMyOrganizations(1, PAGE_SIZE)
    }

    private fun onResetFirstLoad() {
        firstLoad = true
        rvLoadingSpinner.visibility = View.VISIBLE
        scrollListener.enabled = false
        refreshLayout.isEnabled = false
    }

    private fun <K> onErrorResponse(call: Call<K>, response: Response<K>?) {
        if (firstLoad) {
            firstLoad = false
            rvLoadingSpinner.visibility = View.GONE
        }
        Util.treatBasicError(call, response, supportFragmentManager)
    }

    private fun onFirstLoad() {
        firstLoad = false
        rvLoadingSpinner.visibility = View.GONE
        scrollListener.enabled = true
        refreshLayout.isEnabled = true
    }

    private fun logout() {
        AppTicketChecker.clearSession()
        val intent = Intent(this, ActivityLogin::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(LOAD_CURRENT_PAGE, scrollListener.currentPage)
        outState.putInt(LOAD_PREVIOUS_ITEM_COUNT, scrollListener.previousTotalItemCount)
        outState.putBoolean(LOAD_LOADING, scrollListener.loading)
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val LOAD_CURRENT_PAGE = "lastLoadPage"
        private const val LOAD_PREVIOUS_ITEM_COUNT = "previousItemCount"
        private const val LOAD_LOADING = "loading"
    }

}
