package ticket.checker.admin

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityAdmin.Companion.LIST_ALL
import ticket.checker.R
import ticket.checker.admin.listeners.ActionListener
import ticket.checker.admin.listeners.EndlessScrollListener
import ticket.checker.admin.listeners.FilterChangeListener
import ticket.checker.admin.listeners.RecyclerItemClickListener
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType

/**
 * Created by Dani on 09.02.2018.
 */
abstract class AAdminFragment<T,Y> : Fragment(), FilterChangeListener, ActionListener<T>, RecyclerItemClickListener.OnItemClickListener {
    protected var filter : String = "NOT_INITIALISED"
    abstract val loadLimit : Int
    private var firstLoad = true

    private var refreshLayout : SwipeRefreshLayout? = null
    private var loadingSpinner: ProgressBar? = null
    private var recyclerView: RecyclerView? = null
    protected var layoutManager : LinearLayoutManager? = null

    protected val itemsAdapter: AItemsAdapter<T,Y> by lazy {
        setupItemsAdapter()
    }
    private var scrollListener : EndlessScrollListener? = null

    protected val headerCallback = object : Callback<Y> {
        override fun onResponse(call: Call<Y>, response: Response<Y>) {
            if (response.isSuccessful) {
                itemsAdapter.updateHeaderInfo(filter, response.body()!!)
            } else {
                onErrorResponse(response.code())
            }
        }

        override fun onFailure(call: Call<Y>, t: Throwable?) {
            onErrorResponse(-1)
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
                onErrorResponse(response.code())
            }
        }
        override fun onFailure(call: Call<List<T>>?, t: Throwable?) {
            onErrorResponse(-1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (filter == "NOT_INITIALISED") {
            filter = arguments.getString(FILTER) ?: LIST_ALL
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_recycle_view, container, false)
        refreshLayout = view?.findViewById(R.id.refreshLayout)
        refreshLayout?.setOnRefreshListener { onRefresh()  }
        refreshLayout?.setColorSchemeColors(resources.getColor(R.color.colorPrimary),resources.getColor(R.color.noRed))
        loadingSpinner = view?.findViewById(R.id.rvLoadingSpinner)
        recyclerView = view?.findViewById(R.id.rvItems)
        layoutManager = LinearLayoutManager(activity)
        scrollListener = object : EndlessScrollListener(layoutManager as LinearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, recyclerView: RecyclerView) {
                if((totalItemsCount-2) % loadLimit == 0) {
                    itemsAdapter.setLoading(true)
                    loadItems(page, filter)
                }
            }
        }
        recyclerView?.layoutManager = layoutManager
        recyclerView?.adapter = itemsAdapter
        recyclerView?.addOnScrollListener(scrollListener)
        recyclerView?.addOnItemTouchListener(RecyclerItemClickListener(activity, recyclerView!!, this))

        if(firstLoad) {
            reloadAll()
        }
        else {
            scrollListener?.currentPage = arguments.getInt(LOAD_CURRENT_PAGE)
            scrollListener?.previousTotalItemCount = arguments.getInt(LOAD_PREVIOUS_ITEM_COUNT)
            scrollListener?.loading = arguments.getBoolean(LOAD_LOADING)
        }
        return view
    }

    private fun onRefresh() {
        refreshLayout?.isRefreshing = false
        itemsAdapter.resetItemsList()
        scrollListener?.resetState()
        reloadAll()
    }

    override fun onFilterChange(newFilter: String) {
        this.filter = newFilter
        itemsAdapter.resetItemsList()
        scrollListener?.resetState()
        reloadAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        arguments.putInt(LOAD_CURRENT_PAGE, scrollListener?.currentPage ?: 0)
        arguments.putInt(LOAD_PREVIOUS_ITEM_COUNT, scrollListener?.previousTotalItemCount ?: 0)
        arguments.putBoolean(LOAD_LOADING, scrollListener?.loading ?: true)
    }

    private fun reloadAll() {
        onResetFirstLoad()
        loadHeader(filter)
        loadItems(0, filter)
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
        itemsAdapter.launchInfoActivity(view, position)
    }

    override fun onLongItemClick(view: View?, position: Int) { }

    override fun onRemove(removedItemPosition: Int) {
        itemsAdapter.itemRemoved(removedItemPosition)
    }

    private fun onErrorResponse(errorCode: Int) {
        if (firstLoad) {
            firstLoad = false
            loadingSpinner?.visibility = View.GONE
        }
        var dialog: DialogInfo? = null
        when (errorCode) {
            -1 -> {
                dialog = DialogInfo.newInstance("Connection error", "There was an error connecting to the server!", DialogType.ERROR)
            }
            401 -> {
                dialog = DialogInfo.newInstance("Session expired", "You need to provide your authentication once again!", DialogType.AUTH_ERROR)
                dialog.isCancelable = false
            }
            403 -> {
                dialog = DialogInfo.newInstance("Loading failed", "You are not allowed to see ticket information!", DialogType.ERROR)
            }
            in 500..600 -> {
                dialog = DialogInfo.newInstance("Loading failed", "There was an server error while loading more tickets!", DialogType.ERROR)
            }
        }
        dialog?.show(fragmentManager, "DIALOG_TICKETS_RV")
    }

    abstract fun setupItemsAdapter() : AItemsAdapter<T,Y>

    abstract fun loadHeader(filter : String)

    abstract fun loadItems(page: Int, filter : String)

    companion object {
        const val FILTER = "filter"
        private const val LOAD_CURRENT_PAGE = "lastLoadPage"
        private const val LOAD_PREVIOUS_ITEM_COUNT = "previousItemCount"
        private const val LOAD_LOADING = "loading"
    }
}