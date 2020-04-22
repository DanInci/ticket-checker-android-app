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
import android.widget.ProgressBar
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
abstract class AAdminFragment<T, Y> : Fragment(), FilterChangeListener, ListChangeListener<T>, RecyclerItemClickListener.OnItemClickListener {

    protected var filterType: String? = null
    protected lateinit var filterValue: String
    protected lateinit var organizationId: UUID

    abstract val loadLimit: Int
    private var firstLoad = true

    private var refreshLayout: SwipeRefreshLayout? = null
    private var loadingSpinner: ProgressBar? = null
    private var recyclerView: RecyclerView? = null
    protected var layoutManager: LinearLayoutManager? = null

    protected val itemsAdapter: AItemsAdapterWithHeader<T, Y> by lazy {
        setupItemsAdapter()
    }
    private var scrollListener: EndlessScrollListener? = null

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

    protected val itemsCallback = object : Callback<List<T>> {
        override fun onResponse(call: Call<List<T>>, response: Response<List<T>>) {
            if (firstLoad) {
                onFirstLoad()
            }
            if (response.isSuccessful) {
                val items: List<T> = response.body() as List<T>
                itemsAdapter.setLoading(false)
                itemsAdapter.updateItemsList(items)
            } else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<List<T>>, t: Throwable?) {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recycle_view, container, false)
        refreshLayout = view?.findViewById(R.id.refreshLayout)
        refreshLayout?.setOnRefreshListener { onRefresh() }
        refreshLayout?.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.colorPrimary))
        loadingSpinner = view?.findViewById(R.id.rvLoadingSpinner)
        recyclerView = view?.findViewById(R.id.rvItems)
        layoutManager = LinearLayoutManager(activity)
        scrollListener = object : EndlessScrollListener(layoutManager as LinearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, recyclerView: RecyclerView) {
                itemsAdapter.setLoading(true)
                loadItems(page, filterType, filterValue)
            }
        }
        recyclerView?.layoutManager = layoutManager
        recyclerView?.adapter = itemsAdapter
        recyclerView?.addOnScrollListener(scrollListener!!)
        recyclerView?.addOnItemTouchListener(RecyclerItemClickListener(context!!, recyclerView!!, this))

        if (firstLoad) {
            reloadAll()
        } else {
            scrollListener?.currentPage = arguments?.getInt(LOAD_CURRENT_PAGE) ?: 0
            scrollListener?.previousTotalItemCount = arguments?.getInt(LOAD_PREVIOUS_ITEM_COUNT) ?: 0
            scrollListener?.loading = arguments?.getBoolean(LOAD_LOADING) ?: false
        }
        return view
    }

    private fun onRefresh() {
        refreshLayout?.isRefreshing = false
        itemsAdapter.resetItemsList()
        scrollListener?.resetState()
        reloadAll()
    }

    override fun onFilterChange(filterType: String?, filterValue: String) {
        this.filterType = filterType
        this.filterValue = filterValue
        itemsAdapter.resetItemsList()
        scrollListener?.resetState()
        reloadAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        arguments?.putInt(LOAD_CURRENT_PAGE, scrollListener?.currentPage ?: 0)
        arguments?.putInt(LOAD_PREVIOUS_ITEM_COUNT, scrollListener?.previousTotalItemCount ?: 0)
        arguments?.putBoolean(LOAD_LOADING, scrollListener?.loading ?: true)
    }

    private fun reloadAll() {
        onResetFirstLoad()
        loadHeader(filterType, filterValue)
        loadItems(0, filterType, filterValue)
    }

    private fun onResetFirstLoad() {
        firstLoad = true
        loadingSpinner?.visibility = View.VISIBLE
        scrollListener?.enabled = false
        refreshLayout?.isEnabled = false
    }

    private fun onFirstLoad() {
        firstLoad = false
        loadingSpinner?.visibility = View.GONE
        scrollListener?.enabled = true
        refreshLayout?.isEnabled = true
    }

    override fun onItemClick(view: View, position: Int) {
//        itemsAdapter.launchInfoActivity(view, position)
    }

    override fun onLongItemClick(view: View?, position: Int) {}

    override fun onRemove(removedItemPosition: Int) {
        itemsAdapter.itemRemoved(removedItemPosition)
    }

    private fun <K> onErrorResponse(call: Call<K>, response: Response<K>?) {
        if (firstLoad) {
            firstLoad = false
            loadingSpinner?.visibility = View.GONE
        }
        Util.treatBasicError(call, response, fragmentManager!!)
    }

    abstract fun setupItemsAdapter(): AItemsAdapterWithHeader<T, Y>

    abstract fun loadHeader(filterType: String?, filterValue: String)

    abstract fun loadItems(page: Int, filterType: String?, filterValue: String?)

    companion object {
        const val FILTER_TYPE = "filterType"
        const val FILTER_VALUE = "filterValue"
        private const val LOAD_CURRENT_PAGE = "lastLoadPage"
        private const val LOAD_PREVIOUS_ITEM_COUNT = "previousItemCount"
        private const val LOAD_LOADING = "loading"
    }
}