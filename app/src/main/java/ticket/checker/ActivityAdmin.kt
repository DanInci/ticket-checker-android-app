package ticket.checker

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_admin.*
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.tickets.TicketsFragment
import ticket.checker.admin.users.UsersFragment
import ticket.checker.beans.Ticket
import ticket.checker.beans.User

class ActivityAdmin : AppCompatActivity() {

    private var currentFragmentId = -1
    private var currentTicketsMenuItemId = -1
    private var ticketsFilter = LIST_ALL
    private var currentUsersMenuItemId = -1
    private var usersFilter = LIST_ALL

    private var ticketsFragment : AAdminFragment<Ticket, Array<Int>>? = null
    private var usersFragment : AAdminFragment<User,Int>? = null

    private val toolbar : Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        switchFragment(it.itemId)
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        setSupportActionBar(toolbar)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        currentFragmentId = savedInstanceState?.getInt(CURRENT_FRAGMENT_ID) ?: -1
        currentTicketsMenuItemId = savedInstanceState?.getInt(CURRENT_TICKETS_MENU_ITEM_ID) ?: R.id.action_ticket_all
        ticketsFilter = savedInstanceState?.getString(TICKETS_FILTER) ?: LIST_ALL
        currentUsersMenuItemId = savedInstanceState?.getInt(CURRENT_USERS_MENU_ITEM_ID) ?: R.id.action_users_all
        usersFilter = savedInstanceState?.getString(USERS_FILTER) ?: LIST_ALL

        ticketsFragment = TicketsFragment.newInstance(ticketsFilter)
        usersFragment = UsersFragment.newInstance(usersFilter)

        switchFragment(currentFragmentId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        var validSelection = verifySelection(itemId)
        if(validSelection) {
            when (itemId) {
                R.id.action_ticket_add -> {

                }
                R.id.action_users_add -> {

                }
                R.id.action_ticket_all -> {
                    ticketsFilter = LIST_ALL
                    currentTicketsMenuItemId = R.id.action_ticket_all
                }
                R.id.action_ticket_validated -> {
                    ticketsFilter = LIST_VALIDATED
                    currentTicketsMenuItemId = R.id.action_ticket_validated
                }
                R.id.action_ticket_not_validated -> {
                    ticketsFilter = LIST_NOT_VALIDATED
                    currentTicketsMenuItemId = R.id.action_ticket_not_validated
                }
                R.id.action_users_all -> {
                    usersFilter = LIST_ALL
                    currentUsersMenuItemId = R.id.action_users_all
                }
                R.id.action_admins -> {
                    usersFilter = LIST_ADMINS
                    currentUsersMenuItemId = R.id.action_admins
                }
                R.id.action_users -> {
                    usersFilter = LIST_USERS
                    currentUsersMenuItemId = R.id.action_users
                }
                else -> {
                    validSelection = false
                }
            }
            if(validSelection && itemId != R.id.action_ticket_add && itemId != R.id.action_users_add) {
                checkMenuItem(item.itemId)
                updateFilter()
            }
        }
        return validSelection
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_FRAGMENT_ID, currentFragmentId)
        outState.putInt(CURRENT_TICKETS_MENU_ITEM_ID, currentTicketsMenuItemId)
        outState.putString(TICKETS_FILTER, ticketsFilter)
        outState.putInt(CURRENT_USERS_MENU_ITEM_ID, currentUsersMenuItemId)
        outState.putString(USERS_FILTER, usersFilter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(currentFragmentId) {
            R.id.navigation_users -> {
                menuInflater.inflate(R.menu.admin_menu_users, menu)
                checkMenuItem(currentUsersMenuItemId)
            }
            else -> {
                menuInflater.inflate(R.menu.admin_menu_tickets, menu)
                checkMenuItem(currentTicketsMenuItemId)
            }
        }
        return true
    }

    private fun switchFragment(fragmentId : Int) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        var shouldInvalidateMenu = false
        if(fragmentId != currentFragmentId) {
            shouldInvalidateMenu = true
        }
        when(fragmentId) {
            R.id.navigation_users -> {
                fragmentTransaction.replace(R.id.fragmentHolder, usersFragment).commit()
                supportActionBar?.title = "Users"
                currentFragmentId = R.id.navigation_users
            }
            else -> { // should be the tickets fragment
                fragmentTransaction.replace(R.id.fragmentHolder, ticketsFragment).commit()
                supportActionBar?.title = "Tickets"
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
                currentUsersMenuItemId != itemId
            }
            R.id.navigation_tickets -> {
                currentTicketsMenuItemId != itemId
            }
            else -> {
                false
            }
        }
    }

    private fun checkMenuItem(menuItemId : Int) {
        val menu = toolbar.menu
        if(menuItemId!=menu.getItem(0).itemId) { // not the add menu item
            (1 until menu.size())
                    .map { menu.getItem(it) }
                    .forEach { it.isChecked = it.itemId == menuItemId }
        }
    }

    private fun updateFilter() {
        when(currentFragmentId) {
            R.id.navigation_users -> {
                usersFragment?.onFilterChange(usersFilter)
            }
            R.id.navigation_tickets -> {
                ticketsFragment?.onFilterChange(ticketsFilter)
            }
        }
    }

    companion object {
        private const val CURRENT_FRAGMENT_ID = "currentFragmentId"
        private const val CURRENT_TICKETS_MENU_ITEM_ID = "currentTicketsMenuItemId"
        private const val TICKETS_FILTER = "ticketsFilter"
        private const val CURRENT_USERS_MENU_ITEM_ID = "currentUsersMenuItemId"
        private const val USERS_FILTER = "usersFilter"

        const val LIST_ALL = "all"
        const val LIST_VALIDATED = "validated"
        const val LIST_NOT_VALIDATED = "notValidated"
        const val LIST_ADMINS = "admin"
        const val LIST_USERS = "user"
    }
}
