package ticket.checker.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.nfc.tech.MifareUltralight.PAGE_SIZE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.R
import ticket.checker.admin.invitations.InvitesAdapter
import ticket.checker.admin.listeners.EndlessScrollListener
import ticket.checker.admin.listeners.InviteResponseListener
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.beans.OrganizationInviteList
import ticket.checker.beans.OrganizationList
import ticket.checker.beans.Organization
import ticket.checker.extras.InviteStatus
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class DialogInvitations internal constructor() : DialogFragment(), View.OnClickListener, InviteResponseListener {
    lateinit var listChangeListener: ListChangeListener<Organization>

    private lateinit var userId: UUID
    private var firstLoad = true

    private lateinit var dialogView: View

    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val refreshLayout by lazy {
        dialogView.findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
    }
    private val rvItems by lazy {
        dialogView. findViewById<RecyclerView>(R.id.rvItems)
    }
    private val rvLoadingSpinner by lazy {
        dialogView.findViewById<ProgressBar>(R.id.rvLoadingSpinner)
    }
    private val emptyContainer by lazy {
        dialogView.findViewById<LinearLayout>(R.id.emptyContainer)
    }
    private val tvEmptyText by lazy {
        dialogView.findViewById<TextView>(R.id.tvEmptyText)
    }
    private val itemsAdapter: InvitesAdapter by lazy {
        InvitesAdapter(dialogView.context, this)
    }
    private val layoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(dialogView.context)
    }
    private val scrollListener: EndlessScrollListener by lazy {
        object : EndlessScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, recyclerView: RecyclerView) {
                itemsAdapter.setLoading(true)
                loadPendingInvitations(page, PAGE_SIZE)
            }
        }
    }
    private val listCallback: Callback<List<OrganizationInviteList>> = object : Callback<List<OrganizationInviteList>> {
        override fun onResponse(call: Call<List<OrganizationInviteList>>, response: Response<List<OrganizationInviteList>>) {
            if (firstLoad) {
                onFirstLoad()
            }
            if (response.isSuccessful) {
                val items: List<OrganizationInviteList> = response.body() as List<OrganizationInviteList>
                itemsAdapter.setLoading(false)
                itemsAdapter.updateItemsList(items)
                if(itemsAdapter.getRealItemsCount() == 0) {
                    emptyContainer.visibility = View.VISIBLE
                    tvEmptyText.text = "No invitations"
                }
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<List<OrganizationInviteList>>, t: Throwable) {
            onErrorResponse(call, null)
        }
    }

    private val inviteAcceptCallback = object : Callback<Organization> {
        override fun onResponse(call: Call<Organization>, response: Response<Organization>) {
            if(response.isSuccessful) {
                val organizationId = UUID.fromString(call.request().url().pathSegments()[2])
                val inviteId = UUID.fromString(call.request().url().pathSegments()[4])
                val position = itemsAdapter.getItemPosition(organizationId, inviteId)
                if(position != null) {
                    itemsAdapter.itemRemoved(position)
                    if(itemsAdapter.getRealItemsCount() == 0) {
                        emptyContainer.visibility = View.VISIBLE
                        tvEmptyText.text = "No invitations"
                    }
                    listChangeListener.onAdd(response.body() as Organization)
                }
            } else {
                onInviteErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Organization>, t: Throwable) {
            onInviteErrorResponse(call, null)
        }
    }

    private val inviteDeclineCallback = object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if(response.isSuccessful) {
                val organizationId = UUID.fromString(call.request().url().pathSegments()[2])
                val inviteId = UUID.fromString(call.request().url().pathSegments()[4])
                val position = itemsAdapter.getItemPosition(organizationId, inviteId)
                if(position != null) {
                    itemsAdapter.itemRemoved(position)
                    if(itemsAdapter.getRealItemsCount() == 0) {
                        emptyContainer.visibility = View.VISIBLE
                        tvEmptyText.text = "No invitations"
                    }
                }
            } else {
                onInviteErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Void>, t: Throwable) {
            onInviteErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            userId = arguments?.getSerializable(USER_ID) as UUID
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialogView =  inflater.inflate(R.layout.dialog_invitations, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshLayout.setOnRefreshListener { onRefresh() }
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(dialogView.context, R.color.colorPrimary))
        rvItems.layoutManager = layoutManager
        rvItems.adapter = itemsAdapter
        rvItems.addOnScrollListener(scrollListener)

        if (firstLoad) {
            reloadAll()
        } else {
            scrollListener.currentPage = savedInstanceState?.getInt(LOAD_CURRENT_PAGE) ?: 0
            scrollListener.previousTotalItemCount = savedInstanceState?.getInt(LOAD_PREVIOUS_ITEM_COUNT) ?: 0
            scrollListener.loading = savedInstanceState?.getBoolean(LOAD_LOADING) ?: false
        }

        btnClose.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnClose -> {
                dismiss()
            }
        }
    }

    override fun inviteAccepted(inv: OrganizationInviteList) {
        val call = ServiceManager.getOrganizationService().acceptInvite(inv.organizationId, inv.id)
        call.enqueue(inviteAcceptCallback)
    }

    override fun inviteDeclined(inv: OrganizationInviteList) {
        val call = ServiceManager.getOrganizationService().declineInvite(inv.organizationId, inv.id)
        call.enqueue(inviteDeclineCallback)
    }

    private fun onFirstLoad() {
        firstLoad = false
        rvLoadingSpinner.visibility = View.GONE
        scrollListener.enabled = true
        refreshLayout.isEnabled = true
    }

    private fun onRefresh() {
        refreshLayout.isRefreshing = false
        itemsAdapter.resetItemsList()
        scrollListener.resetState()
        reloadAll()
    }

    private fun reloadAll() {
        onResetFirstLoad()
        loadPendingInvitations(0, PAGE_SIZE)
    }

    private fun onResetFirstLoad() {
        firstLoad = true
        emptyContainer.visibility = View.GONE
        rvLoadingSpinner.visibility = View.VISIBLE
        scrollListener.enabled = false
        refreshLayout.isEnabled = false
    }

    private fun loadPendingInvitations(page: Int, pageSize: Int) {
        val call = ServiceManager.getUserService().getUserInvites(userId, page, pageSize, InviteStatus.PENDING)
        call.enqueue(listCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(LOAD_CURRENT_PAGE, scrollListener.currentPage)
        outState.putInt(LOAD_PREVIOUS_ITEM_COUNT, scrollListener.previousTotalItemCount)
        outState.putBoolean(LOAD_LOADING, scrollListener.loading)
    }

    private fun <K> onErrorResponse(call: Call<K>, response: Response<K>?) {
        if (firstLoad) {
            firstLoad = false
            rvLoadingSpinner.visibility = View.GONE
        }
        Util.treatBasicError(call, response, fragmentManager!!)
    }

    private fun <K> onInviteErrorResponse(call: Call<K>, response: Response<K>?) {
        val organizationId = UUID.fromString(call.request().url().pathSegments()[2])
        val inviteId = UUID.fromString(call.request().url().pathSegments()[4])
        val position = itemsAdapter.getItemPosition(organizationId, inviteId)
        if(position != null) {
            val item = itemsAdapter.getItemByPosition(position)
            itemsAdapter.itemEdited(item!!, position)
        }
        Util.treatBasicError(call, null, fragmentManager!!)
    }

    companion object {
        private const val USER_ID = "userId"
        private const val LOAD_CURRENT_PAGE = "invitesLastLoadPage"
        private const val LOAD_PREVIOUS_ITEM_COUNT = "invitesPreviousItemCount"
        private const val LOAD_LOADING = "invitesLoading"

        fun newInstance(userId: UUID): DialogInvitations {
            val fragment = DialogInvitations()
            val args = Bundle()
            args.putSerializable(USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

}
