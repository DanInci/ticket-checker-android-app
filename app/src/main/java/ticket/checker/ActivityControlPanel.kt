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
import ticket.checker.AppTicketChecker.Companion.pretendedUserType
import ticket.checker.admin.tickets.DialogAddTicket
import ticket.checker.admin.tickets.TicketsFragment
import ticket.checker.admin.users.DialogAddUser
import ticket.checker.admin.users.UsersFragment
import ticket.checker.beans.Ticket
import ticket.checker.beans.User
import ticket.checker.extras.UserType
import ticket.checker.extras.Util.POSITION

class ActivityControlPanel : AppCompatActivity() {

    private var currentFragmentId = -1
    private var currentTicketsMenuItemId = -1
    private var currentTicketsQuery : String? = null
    private var ticketsFilterType : String? = null
    private var ticketsFilterValue : String = ""
    private var currentUsersMenuItemId = -1
    private var currentUsersQuery : String? = null
    private var usersFilterType : String? = null
    private var usersFilterValue: String = ""

    private var ticketsFragment : TicketsFragment? = null
    private var usersFragment : UsersFragment? = null

    private val btnBack : ImageView by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val toolbar : Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val toolbarTitle : TextView by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private val searchView : MaterialSearchView by lazy {
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

        if(pretendedUserType == UserType.ADMIN) {
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
        currentUsersMenuItemId = savedInstanceState?.getInt(CURRENT_USERS_MENU_ITEM_ID) ?: R.id.action_users_all
        currentUsersQuery = savedInstanceState?.getString(USERS_CURRENT_QUERY)
        usersFilterType = savedInstanceState?.getString(USERS_FILTER_TYPE)
        usersFilterValue = savedInstanceState?.getString(USERS_FILTER_VALUE) ?: ""

        ticketsFragment = if(currentTicketsQuery == null) TicketsFragment.newInstance(ticketsFilterType, ticketsFilterValue)
                                else TicketsFragment.newInstance(FILTER_SEARCH, currentTicketsQuery as String)
        usersFragment = if(currentUsersQuery == null) UsersFragment.newInstance(usersFilterType, usersFilterValue)
                                else UsersFragment.newInstance(FILTER_SEARCH, currentUsersQuery as String)

        switchFragment(currentFragmentId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        var validSelection = verifySelection(itemId)
        if(validSelection) {
            when (itemId) {
                R.id.action_ticket_add -> {
                    val dialogAddTicket = DialogAddTicket()
                    dialogAddTicket.listChangeListener = ticketsFragment
                    dialogAddTicket.show(supportFragmentManager, "DIALOG_ADD")
                }
                R.id.action_users_add -> {
                    val dialogAddUser = DialogAddUser()
                    dialogAddUser.listChangeListener = usersFragment
                    dialogAddUser.show(supportFragmentManager, "DIALOG_ADD")
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
                R.id.action_users_all -> {
                    usersFilterType = null
                    checkMenuItem(item.itemId)
                    updateFilter(usersFilterType, usersFilterValue)
                    currentUsersMenuItemId = R.id.action_users_all
                }
                R.id.action_admins -> {
                    usersFilterType = FILTER_ROLE
                    usersFilterValue = UserType.ADMIN.role
                    checkMenuItem(item.itemId)
                    updateFilter(usersFilterType, usersFilterValue)
                    currentUsersMenuItemId = R.id.action_admins
                }
                R.id.action_publishers -> {
                    usersFilterType = FILTER_ROLE
                    usersFilterValue = UserType.PUBLISHER.role
                    checkMenuItem(item.itemId)
                    updateFilter(usersFilterType, usersFilterValue)
                    currentUsersMenuItemId = R.id.action_publishers
                }
                R.id.action_validators -> {
                    usersFilterType = FILTER_ROLE
                    usersFilterValue = UserType.VALIDATOR.role
                    checkMenuItem(item.itemId)
                    updateFilter(usersFilterType, usersFilterValue)
                    currentUsersMenuItemId = R.id.action_validators
                }
                R.id.action_users -> {
                    usersFilterType = FILTER_ROLE
                    usersFilterValue = UserType.USER.role
                    checkMenuItem(item.itemId)
                    updateFilter(usersFilterType, usersFilterValue)
                    currentUsersMenuItemId = R.id.action_users
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
        outState.putInt(CURRENT_USERS_MENU_ITEM_ID, currentUsersMenuItemId)
        outState.putString(USERS_FILTER_TYPE, usersFilterType)
        outState.putString(USERS_FILTER_VALUE, usersFilterValue)
        outState.putString(USERS_CURRENT_QUERY, currentUsersQuery)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(currentFragmentId) {
            R.id.navigation_users -> {
                menuInflater.inflate(R.menu.control_panel_menu_users, menu)
                checkMenuItem(currentUsersMenuItemId)
            }
            else -> {
                menuInflater.inflate(R.menu.control_panel_menu_tickets, menu)
                checkMenuItem(currentTicketsMenuItemId)
                if(pretendedUserType != UserType.ADMIN && pretendedUserType != UserType.PUBLISHER) {
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
                    R.id.navigation_users -> {
                        if (query != currentUsersQuery) {
                            currentUsersQuery = query
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
                    R.id.navigation_users -> {
                        searchView.setQuery(currentUsersQuery,false)
                    }
                    R.id.navigation_tickets -> {
                        searchView.setQuery(currentTicketsQuery,false)
                    }
                }
            }
            override fun onSearchViewClosed() {
                when(currentFragmentId) {
                    R.id.navigation_users -> {
                        if(currentUsersQuery != null) {
                            currentUsersQuery = null
                            updateFilter(usersFilterType, usersFilterValue)
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
                                ticketsFragment?.onRemove(position)
                            }
                            R.id.navigation_users -> {
                                usersFragment?.onRemove(position)
                            }
                        }
                    }
                    ITEM_EDITED -> {
                        val editedObject = data.getSerializableExtra(EDITED_OBJECT)
                        when (currentFragmentId) {
                            R.id.navigation_tickets -> {
                                ticketsFragment?.onEdit(editedObject as Ticket, position)
                            }
                            R.id.navigation_users -> {
                                usersFragment?.onEdit(editedObject as User, position)
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
            R.id.navigation_users -> {
                fragmentTransaction.replace(R.id.fragmentHolder, usersFragment!!).commit()
                toolbarTitle.text= "Users"
                currentFragmentId = R.id.navigation_users
            }
            else -> { // should be the tickets fragment
                fragmentTransaction.replace(R.id.fragmentHolder, ticketsFragment!!).commit()
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
            R.id.navigation_users -> {
                if(currentUsersQuery == null) {
                    currentUsersMenuItemId != itemId
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
            R.id.navigation_users -> {
                usersFragment?.onFilterChange(filterType, filterValue)
            }
            R.id.navigation_tickets -> {
                ticketsFragment?.onFilterChange(filterType, filterValue)
            }
        }
    }

    companion object {
        private const val CURRENT_FRAGMENT_ID = "currentFragmentId"
        private const val CURRENT_TICKETS_MENU_ITEM_ID = "currentTicketsMenuItemId"
        private const val TICKETS_FILTER_TYPE = "ticketsFilterType"
        private const val TICKETS_FILTER_VALUE = "ticketsFilterValue"
        private const val TICKETS_CURRENT_QUERY = "ticketsCurrentQuery"
        private const val CURRENT_USERS_MENU_ITEM_ID = "currentUsersMenuItemId"
        private const val USERS_FILTER_TYPE = "usersFilterType"
        private const val USERS_FILTER_VALUE = "usersFilterValue"
        private const val USERS_CURRENT_QUERY = "usersCurrentQuery"

        const val CHANGES_TO_ADAPTER_ITEM = 0
        const val ITEM_REMOVED = 0
        const val ITEM_EDITED = 1
        const val EDITED_OBJECT = "editedObject"
        const val FILTER_VALIDATED = "validated"
        const val FILTER_SEARCH = "search"
        const val FILTER_ROLE = "role"
    }
}
