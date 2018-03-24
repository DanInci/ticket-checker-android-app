package ticket.checker.admin.users

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.LIST_ALL
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.User
import ticket.checker.services.ServiceManager

class UsersFragment : AAdminFragment<User, Int>() {

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapter<User, Int> {
        val usersAdapter = UsersAdapter(context!!)
        usersAdapter.setHasStableIds(true)
        return usersAdapter
    }

    override fun loadHeader(filter: String) {
        val modifiedFilters = if (filter == "all") null else filter
        val call = ServiceManager.getStatisticsService().getUserNumbers(modifiedFilters)
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filter: String) {
        val modifiedFilters = if (filter == "all") null else filter
        val call = ServiceManager.getUserService().getUsers(modifiedFilters, page, loadLimit)
        call.enqueue(itemsCallback)
    }

    override fun onAdd(addedObject: User) {
        val role = addedObject.role
        val modifiedFilter = "ROLE_" + filter.toUpperCase()
        if(modifiedFilter == role || filter == LIST_ALL) {
            itemsAdapter.itemAdded(addedObject)
        }
    }


    companion object {
        fun newInstance(usersFilter : String): UsersFragment {
            val fragment = UsersFragment()
            val args = Bundle()
            args.putString(FILTER, usersFilter)
            fragment.arguments = args
            return fragment
        }
    }
}
