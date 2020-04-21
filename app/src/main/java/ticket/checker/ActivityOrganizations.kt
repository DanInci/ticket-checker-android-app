package ticket.checker

import android.content.Intent
import android.nfc.tech.MifareUltralight.PAGE_SIZE
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
import ticket.checker.ActivityOrganizationDetails.Companion.CURRENT_ORGANIZATION
import ticket.checker.ActivityProfile.Companion.USER_ID
import ticket.checker.admin.listeners.EndlessScrollListener
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.admin.listeners.RecyclerItemClickListener
import ticket.checker.admin.organizations.OrganizationsAdapter
import ticket.checker.beans.OrganizationList
import ticket.checker.beans.OrganizationProfile
import ticket.checker.dialogs.DialogCreateOrganization
import ticket.checker.dialogs.DialogInvitations
import ticket.checker.extras.Util
import ticket.checker.extras.Util.POSITION
import ticket.checker.services.ServiceManager

class ActivityOrganizations : AppCompatActivity(), RecyclerItemClickListener.OnItemClickListener, ListChangeListener<OrganizationList> {

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
                val dialogCreateOrganization = DialogCreateOrganization()
                dialogCreateOrganization.listChangeListener = this
                dialogCreateOrganization.show(supportFragmentManager, "DIALOG_CREATE_ORG")
                true
            }
            R.id.action_invitations -> {
                val dialogMyInvitations = DialogInvitations.newInstance(AppTicketChecker.loggedInUser!!.id)
                dialogMyInvitations.listChangeListener = this
                dialogMyInvitations.show(supportFragmentManager, "DIALOG_INVITATIONS")
                true
            }
            R.id.action_my_profile -> {
                val intent  = Intent(this@ActivityOrganizations, ActivityProfile::class.java)
                intent.putExtra(USER_ID, AppTicketChecker.loggedInUser!!.id)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CHANGES_TO_ORGANIZATION_ITEM) {
            val position = data?.getIntExtra(POSITION, -1)
            if (position != null && position != -1) {
                when (resultCode) {
                    ITEM_REMOVED -> {
                        onRemove(position)
                    }
                    ITEM_EDITED -> {
                        val editedOrganization = data.getSerializableExtra(EDITED_ORGANIZATION) as OrganizationProfile
                        onEdit(editedOrganization.toOrganizationList(), position)
                    }
                }
            }
        }
    }


    override fun onAdd(addedObject: OrganizationList) {
        itemsAdapter.itemAdded(addedObject)
    }

    override fun onEdit(editedObject: OrganizationList, editedObjectPosition: Int) {
        itemsAdapter.itemEdited(editedObject, editedObjectPosition)
    }

    override fun onRemove(removedItemPosition: Int) {
        itemsAdapter.itemRemoved(removedItemPosition)
    }

    override fun onItemClick(view: View, position: Int) {}

    override fun onLongItemClick(view: View?, position: Int) {
        val item = itemsAdapter.getItemByPosition(position)
        if(item != null ) {
            val intent  = Intent(this@ActivityOrganizations, ActivityOrganizationDetails::class.java)
            intent.putExtra(POSITION, position)
            intent.putExtra(CURRENT_ORGANIZATION, item.toOrganizationProfile())
            startActivityForResult(intent, CHANGES_TO_ORGANIZATION_ITEM)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

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
        const val ITEM_REMOVED = 0
        const val ITEM_EDITED = 1
        const val EDITED_ORGANIZATION = "editedOrganization"

        private const val LOAD_CURRENT_PAGE = "organizationsLastLoadPage"
        private const val LOAD_PREVIOUS_ITEM_COUNT = "organizationsPreviousItemCount"
        private const val LOAD_LOADING = "organizationsLoading"
        private const val CHANGES_TO_ORGANIZATION_ITEM = 32
    }

}
