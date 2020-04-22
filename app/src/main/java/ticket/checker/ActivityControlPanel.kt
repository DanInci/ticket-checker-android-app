package ticket.checker

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.miguelcatalan.materialsearchview.MaterialSearchView
import kotlinx.android.synthetic.main.activity_control.*
import ticket.checker.admin.tickets.TicketsFragment
import ticket.checker.admin.members.OrganizationMembersFragment
import ticket.checker.beans.OrganizationMemberList
import ticket.checker.beans.TicketList
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util.POSITION

class ActivityControlPanel : AppCompatActivity() {

    private var currentFragmentId = -1
    private var currentTicketsMenuItemId = -1
    private var currentTicketsQuery : String? = null
    private var ticketsFilterType : String? = null
    private var ticketsFilterValue : String = ""
    private var currentMembersMenuItemId = -1
    private var currentMembersQuery : String? = null
    private var membersFilterType : String? = null
    private var membersFilterValue: String = ""

    private lateinit var ticketsFragment : TicketsFragment
    private lateinit var membersFragment: OrganizationMembersFragment

    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val toolbarTitle by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private val searchView by lazy {
        findViewById<MaterialSearchView>(R.id.search_view)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        switchFragment(it.itemId)
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        btnBack.setOnClickListener { finish() }

        if(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole == OrganizationRole.ADMIN) {
            navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        }
        else {
            navigation.visibility = View.GONE
        }

        currentFragmentId = savedInstanceState?.getInt(CURRENT_FRAGMENT_ID) ?: R.id.navigation_tickets
        currentTicketsMenuItemId = savedInstanceState?.getInt(CURRENT_TICKETS_MENU_ITEM_ID) ?: R.id.action_ticket_all
        currentTicketsQuery = savedInstanceState?.getString(TICKETS_CURRENT_QUERY)
        ticketsFilterType = savedInstanceState?.getString(TICKETS_FILTER_TYPE)
        ticketsFilterValue = savedInstanceState?.getString(TICKETS_FILTER_VALUE) ?: ""
        currentMembersMenuItemId = savedInstanceState?.getInt(CURRENT_MEMBERS_MENU_ITEM_ID) ?: R.id.action_members_all
        currentMembersQuery = savedInstanceState?.getString(MEMBERS_CURRENT_QUERY)
        membersFilterType = savedInstanceState?.getString(MEMBERS_FILTER_TYPE)
        membersFilterValue = savedInstanceState?.getString(MEMBERS_FILTER_VALUE) ?: ""

        ticketsFragment = if(currentTicketsQuery == null) TicketsFragment.newInstance(ticketsFilterType, ticketsFilterValue)
                                else TicketsFragment.newInstance(FILTER_SEARCH, currentTicketsQuery as String)
        membersFragment = if(currentMembersQuery == null) OrganizationMembersFragment.newInstance(membersFilterType, membersFilterValue)
                                else OrganizationMembersFragment.newInstance(FILTER_SEARCH, currentMembersQuery as String)

        switchFragment(currentFragmentId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        var validSelection = verifySelection(itemId)
        if(validSelection) {
            when (itemId) {
                R.id.action_ticket_add -> {
//                    val dialogAddTicket = DialogAddTicket()
//                    dialogAddTicket.listChangeListener = ticketsFragment
//                    dialogAddTicket.show(supportFragmentManager, "DIALOG_ADD")
                }
                R.id.action_users_add -> {
//                    val dialogAddUser = DialogAddUser()
//                    dialogAddUser.listChangeListener = usersFragment
//                    dialogAddUser.show(supportFragmentManager, "DIALOG_ADD")
                }
                R.id.action_ticket_all -> {
                    ticketsFilterType = null
                    checkMenuItem(item.itemId)
                    updateFilter(ticketsFilterType, ticketsFilterValue)
                    currentTicketsMenuItemId = R.id.action_ticket_all
                }
                R.id.action_ticket_validated -> {
                    ticketsFilterType = FILTER_VALIDATED
                    ticketsFilterValue = "true"
                    checkMenuItem(item.itemId)
                    updateFilter(ticketsFilterType, ticketsFilterValue)
                    currentTicketsMenuItemId = R.id.action_ticket_validated
                }
                R.id.action_ticket_not_validated -> {
                    ticketsFilterType = FILTER_VALIDATED
                    ticketsFilterValue = "false"
                    checkMenuItem(item.itemId)
                    updateFilter(ticketsFilterType, ticketsFilterValue)
                    currentTicketsMenuItemId = R.id.action_ticket_not_validated
                }
                R.id.action_members_all -> {
                    membersFilterType = null
                    checkMenuItem(item.itemId)
                    updateFilter(membersFilterType, membersFilterValue)
                    currentMembersMenuItemId = R.id.action_members_all
                }
                R.id.action_admins -> {
                    membersFilterType = FILTER_ROLE
                    membersFilterValue = OrganizationRole.ADMIN.role
                    checkMenuItem(item.itemId)
                    updateFilter(membersFilterType, membersFilterValue)
                    currentMembersMenuItemId = R.id.action_admins
                }
                R.id.action_publishers -> {
                    membersFilterType = FILTER_ROLE
                    membersFilterValue = OrganizationRole.PUBLISHER.role
                    checkMenuItem(item.itemId)
                    updateFilter(membersFilterType, membersFilterValue)
                    currentMembersMenuItemId = R.id.action_publishers
                }
                R.id.action_validators -> {
                    membersFilterType = FILTER_ROLE
                    membersFilterValue = OrganizationRole.VALIDATOR.role
                    checkMenuItem(item.itemId)
                    updateFilter(membersFilterType, membersFilterValue)
                    currentMembersMenuItemId = R.id.action_validators
                }
                R.id.action_users -> {
                    membersFilterType = FILTER_ROLE
                    membersFilterValue = OrganizationRole.USER.role
                    checkMenuItem(item.itemId)
                    updateFilter(membersFilterType, membersFilterValue)
                    currentMembersMenuItemId = R.id.action_users
                }
                else -> {
                    validSelection = false
                }
            }
        }
        return validSelection
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_FRAGMENT_ID, currentFragmentId)
        outState.putInt(CURRENT_TICKETS_MENU_ITEM_ID, currentTicketsMenuItemId)
        outState.putString(TICKETS_FILTER_TYPE, ticketsFilterType)
        outState.putString(TICKETS_FILTER_VALUE, ticketsFilterValue)
        outState.putString(TICKETS_CURRENT_QUERY, currentTicketsQuery)
        outState.putInt(CURRENT_MEMBERS_MENU_ITEM_ID, currentMembersMenuItemId)
        outState.putString(MEMBERS_FILTER_TYPE, membersFilterType)
        outState.putString(MEMBERS_FILTER_VALUE, membersFilterValue)
        outState.putString(MEMBERS_CURRENT_QUERY, currentMembersQuery)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(currentFragmentId) {
            R.id.navigation_members -> {
                menuInflater.inflate(R.menu.control_panel_menu_members, menu)
                checkMenuItem(currentMembersMenuItemId)
            }
            else -> {
                menuInflater.inflate(R.menu.control_panel_menu_tickets, menu)
                checkMenuItem(currentTicketsMenuItemId)
                if(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole != OrganizationRole.ADMIN && AppTicketChecker.selectedOrganizationMembership!!.pretendedRole != OrganizationRole.PUBLISHER) {
                    menu?.getItem(0)?.isVisible = false
                    toolbarTitle.setPadding(0,0,0,0)
                }
            }
        }
        searchView.setMenuItem(menu?.findItem(R.id.action_search))
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                when(currentFragmentId) {
                    R.id.navigation_members -> {
                        if (query != currentMembersQuery) {
                            currentMembersQuery = query
                            updateFilter(FILTER_SEARCH, query)
                            return true
                        }
                    }
                    R.id.navigation_tickets -> {
                        if (query != currentTicketsQuery) {
                            currentTicketsQuery = query
                            updateFilter(FILTER_SEARCH, query)
                            return true
                        }
                    }
                }
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        searchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewShown() {
                when(currentFragmentId) {
                    R.id.navigation_members -> {
                        searchView.setQuery(currentMembersQuery,false)
                    }
                    R.id.navigation_tickets -> {
                        searchView.setQuery(currentTicketsQuery,false)
                    }
                }
            }
            override fun onSearchViewClosed() {
                when(currentFragmentId) {
                    R.id.navigation_members -> {
                        if(currentMembersQuery != null) {
                            currentMembersQuery = null
                            updateFilter(membersFilterType, membersFilterValue)
                        }
                    }
                    R.id.navigation_tickets -> {
                        if(currentTicketsQuery != null) {
                            currentTicketsQuery = null
                            updateFilter(ticketsFilterType, ticketsFilterValue)
                        }
                    }
                }
            }
        })
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CHANGES_TO_ADAPTER_ITEM) {
            val position = data?.getIntExtra(POSITION, -1)
            if (position != null && position != -1) {
                when (resultCode) {
                    ITEM_REMOVED -> {
                        when (currentFragmentId) {
                            R.id.navigation_tickets -> {
                                ticketsFragment.onRemove(position)
                            }
                            R.id.navigation_members -> {
                                membersFragment.onRemove(position)
                            }
                        }
                    }
                    ITEM_EDITED -> {
                        val editedObject = data.getSerializableExtra(EDITED_OBJECT)
                        when (currentFragmentId) {
                            R.id.navigation_tickets -> {
                                ticketsFragment.onEdit(editedObject as TicketList, position)
                            }
                            R.id.navigation_members -> {
                                membersFragment.onEdit(editedObject as OrganizationMemberList, position)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun switchFragment(fragmentId : Int) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        var shouldInvalidateMenu = false
        if(fragmentId != currentFragmentId) {
            shouldInvalidateMenu = true
        }
        when(fragmentId) {
            R.id.navigation_members -> {
                fragmentTransaction.replace(R.id.fragmentHolder, membersFragment).commit()
                toolbarTitle.text= "Members"
                currentFragmentId = R.id.navigation_members
            }
            else -> { // should be the tickets fragment
                fragmentTransaction.replace(R.id.fragmentHolder, ticketsFragment).commit()
                toolbarTitle.text = "Tickets"
                currentFragmentId = R.id.navigation_tickets
            }
        }
        if(shouldInvalidateMenu) {
            invalidateOptionsMenu()
        }
    }

    private fun verifySelection(itemId : Int) : Boolean {
        return when(currentFragmentId) {
            R.id.navigation_members -> {
                if(currentMembersQuery == null) {
                    currentMembersMenuItemId != itemId
                }
                else {
                    true
                }
            }
            R.id.navigation_tickets -> {
                if(currentTicketsQuery == null) {
                    currentTicketsMenuItemId != itemId
                }
                else {
                    true
                }

            }
            else -> {
                false
            }
        }
    }

    private fun checkMenuItem(menuItemId : Int) {
        val menu = toolbar.menu
        if(menuItemId!=menu.getItem(0).itemId && menuItemId != menu.getItem(1).itemId) { // not the add or search menu item
            (2 until menu.size())
                    .map { menu.getItem(it) }
                    .forEach { it.isChecked = it.itemId == menuItemId }
        }
    }

    private fun updateFilter(filterType : String?, filterValue : String) {
        when(currentFragmentId) {
            R.id.navigation_members -> {
                membersFragment.onFilterChange(filterType, filterValue)
            }
            R.id.navigation_tickets -> {
                ticketsFragment.onFilterChange(filterType, filterValue)
            }
        }
    }

    companion object {
        private const val CURRENT_FRAGMENT_ID = "currentFragmentId"
        private const val CURRENT_TICKETS_MENU_ITEM_ID = "currentTicketsMenuItemId"
        private const val TICKETS_FILTER_TYPE = "ticketsFilterType"
        private const val TICKETS_FILTER_VALUE = "ticketsFilterValue"
        private const val TICKETS_CURRENT_QUERY = "ticketsCurrentQuery"
        private const val CURRENT_MEMBERS_MENU_ITEM_ID = "currentMembersMenuItemId"
        private const val MEMBERS_FILTER_TYPE = "membersFilterType"
        private const val MEMBERS_FILTER_VALUE = "membersFilterValue"
        private const val MEMBERS_CURRENT_QUERY = "membersCurrentQuery"

        const val CHANGES_TO_ADAPTER_ITEM = 0
        const val ITEM_REMOVED = 0
        const val ITEM_EDITED = 1
        const val EDITED_OBJECT = "editedObject"
        const val FILTER_VALIDATED = "validated"
        const val FILTER_SEARCH = "search"
        const val FILTER_ROLE = "role"
    }
}
