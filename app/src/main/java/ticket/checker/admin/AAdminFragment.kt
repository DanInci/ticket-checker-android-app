package ticket.checker.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityMenu.Companion.ORGANIZATION_ID
import ticket.checker.R
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.admin.listeners.EndlessScrollListener
import ticket.checker.admin.listeners.FilterChangeListener
import ticket.checker.admin.listeners.RecyclerItemClickListener
import ticket.checker.extras.Util
import java.util.*

/**
 * Created by Dani on 09.02.2018.
 */
abstract class AAdminFragment<T, TList, Y> : Fragment(), FilterChangeListener, ListChangeListener<T>, RecyclerItemClickListener.OnItemClickListener {

    private var firstLoad = true
    protected lateinit var organizationId: UUID

    protected var filterType: String? = null
    protected lateinit var filterValue: String

    private lateinit var fragmentView: View

    private val refreshLayout by lazy {
        fragmentView.findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
    }
    private val recyclerView by lazy {
        fragmentView.findViewById<RecyclerView>(R.id.rvItems)
    }
    private val loadingSpinner by lazy {
        fragmentView.findViewById<ProgressBar>(R.id.rvLoadingSpinner)
    }
    private val scrollListener by lazy {
        object : EndlessScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, recyclerView: RecyclerView) {
                itemsAdapter.setLoading(true)
                loadItems(page, filterType, filterValue)
            }
        }
    }
    private val emptyContainer by lazy {
        fragmentView.findViewById<LinearLayout>(R.id.emptyContainer)
    }
    private val tvEmptyText by lazy {
        fragmentView.findViewById<TextView>(R.id.tvEmptyText)
    }
    protected val layoutManager by lazy {
        LinearLayoutManager(activity)
    }
    protected val itemsAdapter: AItemsAdapterWithHeader<TList, Y> by lazy {
        setupItemsAdapter()
    }

    protected val headerCallback = object : Callback<Y> {
        override fun onResponse(call: Call<Y>, response: Response<Y>) {
            if (response.isSuccessful) {
                itemsAdapter.updateHeaderInfo(filterType, filterValue, response.body()!!)
            } else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<Y>, t: Throwable?) {
            onErrorResponse(call, null)
        }
    }

    protected val itemsCallback = object : Callback<List<TList>> {
        override fun onResponse(call: Call<List<TList>>, response: Response<List<TList>>) {
            if (firstLoad) {
                onFirstLoad()
            }
            if (response.isSuccessful) {
                val items: List<TList> = response.body() as List<TList>
                itemsAdapter.setLoading(false)
                itemsAdapter.updateItemsList(items)
                if(itemsAdapter.getRealItemsCount() == 0) {
                    emptyContainer.visibility = View.VISIBLE
                    tvEmptyText.text = getEmptyText()
                }
            } else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<List<TList>>, t: Throwable?) {
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (filterType == null) {
            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
            filterType = arguments?.getString(FILTER_TYPE) ?: ""
            filterValue = arguments?.getString(FILTER_VALUE) ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.recycle_view, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshLayout.setOnRefreshListener { onRefresh() }
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.colorPrimary))
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = itemsAdapter
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.addOnItemTouchListener(RecyclerItemClickListener(context!!, recyclerView, this))

        if (firstLoad) {
            reloadAll()
        } else {
            scrollListener.currentPage = arguments?.getInt(LOAD_CURRENT_PAGE) ?: 0
            scrollListener.previousTotalItemCount = arguments?.getInt(LOAD_PREVIOUS_ITEM_COUNT) ?: 0
            scrollListener.loading = arguments?.getBoolean(LOAD_LOADING) ?: false
        }
    }

    private fun onRefresh() {
        refreshLayout.isRefreshing = false
        itemsAdapter.resetItemsList()
        scrollListener.resetState()
        reloadAll()
    }

    override fun onFilterChange(filterType: String?, filterValue: String) {
        this.filterType = filterType
        this.filterValue = filterValue
        itemsAdapter.resetItemsList()
        scrollListener.resetState()
        reloadAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        arguments?.putInt(LOAD_CURRENT_PAGE, scrollListener.currentPage)
        arguments?.putInt(LOAD_PREVIOUS_ITEM_COUNT, scrollListener.previousTotalItemCount)
        arguments?.putBoolean(LOAD_LOADING, scrollListener.loading)
    }

    private fun reloadAll() {
        onResetFirstLoad()
        loadHeader(filterType, filterValue)
        loadItems(0, filterType, filterValue)
    }

    private fun onResetFirstLoad() {
        firstLoad = true
        emptyContainer.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE
        scrollListener.enabled = false
        refreshLayout.isEnabled = false
    }

    private fun onFirstLoad() {
        firstLoad = false
        loadingSpinner.visibility = View.GONE
        scrollListener.enabled = true
        refreshLayout.isEnabled = true
    }

    override fun onItemClick(view: View, position: Int) {}

    override fun onLongItemClick(view: View?, position: Int) {}

    override fun onAdd(addedObject: T) {
        emptyContainer.visibility = View.GONE
    }

    override fun onRemove(removedItemPosition: Int) {
        itemsAdapter.itemRemoved(removedItemPosition)
        if(itemsAdapter.getRealItemsCount() == 0) {
            emptyContainer.visibility = View.VISIBLE
            tvEmptyText.text = getEmptyText()
        }
    }

    private fun <K> onErrorResponse(call: Call<K>, response: Response<K>?) {
        if (firstLoad) {
            firstLoad = false
            loadingSpinner?.visibility = View.GONE
        }
        Util.treatBasicError(call, response, fragmentManager!!)
    }

    abstract fun setupItemsAdapter(): AItemsAdapterWithHeader<TList, Y>

    abstract fun loadHeader(filterType: String?, filterValue: String)

    abstract fun loadItems(page: Int, filterType: String?, filterValue: String?)

    abstract fun getEmptyText(): String

    companion object {
        const val FILTER_TYPE = "filterType"
        const val FILTER_VALUE = "filterValue"
        private const val LOAD_CURRENT_PAGE = "lastLoadPage"
        private const val LOAD_PREVIOUS_ITEM_COUNT = "previousItemCount"
        private const val LOAD_LOADING = "loading"
    }
}